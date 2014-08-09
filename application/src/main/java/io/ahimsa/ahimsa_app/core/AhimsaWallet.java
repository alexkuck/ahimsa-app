package io.ahimsa.ahimsa_app.core;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;

/**
 * Created by askuck on 7/11/14.
 */
public class AhimsaWallet {
    private String TAG = "AhimsaWallet";

    private File keyStoreFile;
    private Wallet keyStore;
    private AhimsaDB db;
    private Configuration config;

    public AhimsaWallet(File keyStoreFile, AhimsaDB db, Configuration config)
    {
        this.keyStoreFile = keyStoreFile;
        this.db = db;
        this.config = config;
        loadWalletFromProtobuf();

        verifyKeyStore();
    }

    // public utilities-----------------------------------------------------------------------------
    public void reserveTxOuts()
    {
        // implement cost_estimation instead of flat max rate?
        // Long estimate_cost = Utils.getEstimatedCost(fee, config.getDustValue(), topic.length(), message.length());

        db.reserveTxOuts(Constants.getMinCoinNecessary());
    }

    public void unreserveTxOuts()
    {
        db.unreserveTxOuts(Constants.getMinCoinNecessary());
    }

    public void removeAllReservations()
    {
        Log.d(TAG, "*** REMOVED ALL RESERVATIONS ****");
        db.removeAllReservations();
    }

    public Transaction createAndAddBulletin(String topic, String message, Long fee) throws Exception
    {
        if(topic == null || topic.equals("") )
        {
            // Ensure topic and message content is proper
            topic = Constants.DEFAULT_TOPIC;
        }

        if(message == null)
        {
            message = "";
        }

        // Get the right amount of unspent transaction outputs to spend from
        List<TransactionOutput> unspents = db.getUnspentOutputs(Constants.getMinCoinNecessary());
        Log.d(TAG, unspents.toString());

        // Create a bulletin using system's configuration file, a bitcoinj wallet, the unspent
        // txouts gathered, and the topic and message.
        Transaction bulletin = BulletinBuilder.createTx(keyStore, unspents, topic, message);

        // Add this bulletin to the bulletin table
        // todo | atomicity (include with commit of transaction)
        db.addBulletin(bulletin.getHashAsString(), topic, message, fee);

        return bulletin;
    }

    public void commitTransaction(Transaction tx, Long highest_block, Boolean confirmed)
    {
        // This function asks the database to store the change outputs of the bulletin as unspent
        // outputs. Additionally, the transaction is stored as unconfirmed.

        if ( !db.hasTx(tx) )
        {
            // Add raw transaction to database.
            db.addTx(tx, confirmed, highest_block);

            // Add all relevant future outpoints to ahimsaDB.
            for(TransactionOutput out : tx.getOutputs())
            {
                Log.d(TAG, "commitTransaction() | " + out.toString());
                Log.d(TAG, "out.isMine(): " + out.isMine(keyStore));
                if(out.isMine(keyStore))
                {
                    db.addTxOut(out);
                }
            }

            // Flag funding outs as spent.
            boolean unreserve_required = false;
            for(TransactionInput in : tx.getInputs())
            {
                String previous_txid = in.getOutpoint().getHash().toString();
                Long previous_vout = in.getOutpoint().getIndex();

                Log.d(TAG, "tx_in previous_txid | " + previous_txid);
                Log.d(TAG, "tx_in previous_vout | " + previous_vout);

                if( db.setStatusSpent(previous_txid, previous_vout) )
                {
                    unreserve_required = true;
                }
            }

            // Spent an unreserved txout, must remove a reserved txout.
            if (unreserve_required)
            {
                db.unreserveTxOuts(Constants.getMinCoinNecessary());
            }

        }
    }

    public void confirmTx(Transaction tx, Long height)
    {
        // This function informs the database that one of the wallet's transaction has been included
        // in the block chain below a target depth.

        confirmTx(tx.getHashAsString(), height);
    }

    public void confirmTx(String txid, Long height)
    {
        db.confirmTx(txid, height);
    }

    public void dropTx(Transaction tx, Long height)
    {
        dropTx(tx.getHashAsString(), height);
    }

    public void dropTx(String txid, Long height)
    {
        // TODO | SOMETHING
        Log.d(TAG, String.format("Tx was dropped at height %s | %s", height.toString(), txid));
    }

    public void setHighestBlock(Transaction tx, Long height)
    {
        db.setHighestBlock(tx.getHashAsString(), height);
    }

    public void setHighestBlock(String txid, Long height)
    {
        db.setHighestBlock(txid, height);
    }

    // Actions -------------------------------------------------------------------------------------
    public void verifyKeyStore()
    {
        if(keyStore.getImportedKeys().isEmpty())
        {
            ECKey key = new ECKey();
            keyStore.importKey(key);
            config.setDefaultAddress( key.toAddress(Constants.NETWORK_PARAMETERS).toString() );
            saveKeyStore();
        }
    }

    public void reset()
    {
        //reset config, database, and keyStore.
        config.reset();
        db.reset();
        for(ECKey key : keyStore.getImportedKeys())
        {
            keyStore.removeKey(key);
        }
        verifyKeyStore();
    }

    // Getters--------------------------------------------------------------------------------------
    public Wallet getKeyStore()
    {
        return keyStore;
    }

    public Long getConfirmedBalance(boolean only_pending)
    {
        return db.getConfirmedBalance(only_pending);
    }

    public Bundle getTxBundle(String txid)
    {
        Cursor tx_cursor = db.getTxCursor(txid);
        if (tx_cursor.getCount() == 0)
            return null;

        Log.d(TAG, "column names: " + Arrays.toString( tx_cursor.getColumnNames() ));

        Bundle tx_bundle = new Bundle();

        tx_cursor.moveToFirst();
        tx_bundle.putString(AhimsaDB.txid, tx_cursor.getString(0));
        tx_bundle.putByteArray(AhimsaDB.raw, tx_cursor.getBlob(1));
        tx_bundle.putLong(AhimsaDB.sent_time, tx_cursor.getLong(2));

        int conf = tx_cursor.getInt(3);
        boolean confirmed = false;
        if(conf == 1)
            confirmed = true;
        tx_bundle.putBoolean(AhimsaDB.confirmed, confirmed);
        tx_bundle.putLong(AhimsaDB.highest_block, tx_cursor.getLong(4));

        return tx_bundle;
    }


    public Bundle getUpdateBundle() {
        Bundle update_bundle = new Bundle();

        update_bundle.putInt(Constants.EXTRA_INT_CONF, db.getConfirmedTxs(true).getCount());
        update_bundle.putInt(Constants.EXTRA_INT_UNCONF, db.getUnconfirmedTxs(true).getCount());
//        update_bundle.putInt(Constants.EXTRA_INT_DRAFT, db.getDraftTx().getCount());
        update_bundle.putLong(Constants.EXTRA_LONG_AVAILABLE_BAL, db.getConfirmedBalance(false));
        update_bundle.putInt(Constants.EXTRA_INT_AVAILABLE_TXOUTS, db.getConfirmedAndUnspentTxOuts(false).getCount());

//        update_bundle.putLong(Constants.EXTRA_LONG_CONF_BAL, db.getConfirmedBalance(false));
//        update_bundle.putLong(Constants.EXTRA_LONG_UNCONF_BAL, db.getUnconfirmedBalance().longValue());
//        update_bundle.putInt(Constants.EXTRA_INT_CONF_TXOUTS, db.getConfirmedAndUnspentTxOuts(false).getCount());
//        update_bundle.putInt(Constants.EXTRA_INT_UNCONF_TXOUTS, db.getUnconfirmedAndUnspentTxOuts().getCount());

        return update_bundle;
    }

    public Cursor getBulletinCursor()
    {
        return db.getBulletinCursor();
    }



    // Load/Save -----------------------------------------------------------------------------------
    private void saveKeyStore()
    {
        try {
            protobufSerializeWallet(keyStore);
        }
        catch (final IOException x){
            throw new RuntimeException(x);
        }
    }

    private void loadWalletFromProtobuf()
    {
        if(keyStoreFile.exists())
        {
            final long start = System.currentTimeMillis();
            FileInputStream walletStream = null;

            try
            {
                walletStream = new FileInputStream(keyStoreFile);
                keyStore = new WalletProtobufSerializer().readWallet(walletStream);

                Log.d(TAG, "wallet loaded from: '" + keyStoreFile + "', took " + (System.currentTimeMillis() - start) + "ms");
            }
            catch (final FileNotFoundException x)
            {
                Log.e(TAG, "problem loading wallet", x);
                //Toast.makeText(AhimsaApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }
            catch (final UnreadableWalletException x)
            {
                Log.e(TAG, "problem loading wallet", x);
                //Toast.makeText(AhimsaApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }
            finally
            {
                if (walletStream != null)
                {
                    try
                    {
                        walletStream.close();
                    }
                    catch (final IOException x)
                    {
                        // swallow
                    }
                }
            }

            if (!keyStore.isConsistent())
            {
                Log.d(TAG, "problem loading wallet");
                //Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }

            if (!keyStore.getParams().equals(Constants.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + keyStore.getParams().getId());
        }
        else
        {
            keyStore = new Wallet(Constants.NETWORK_PARAMETERS);
        }
    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(keyStoreFile);

        Log.d(TAG, "wallet saved to: '" + keyStoreFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }



    //----------------------------------------------------------------------------------------------
    public void toLog(){
        String ahimwall_to_string = toString();

        int sections = ahimwall_to_string.length() / 4000;
        for (int i = 0; i <= sections; i++) {
            int max = 4000 * (i + 1);
            if (max >= ahimwall_to_string.length()) {
                Log.d(TAG, "Section| " + i + "/" + sections + "\n" + ahimwall_to_string.substring(4000 * i));
            } else {
                Log.d(TAG, "Section| " + i + "/" + sections + "\n" + ahimwall_to_string.substring(4000 * i, max));
            }
        }

    }

    @Override
    public String toString()
    {
        Log.d(TAG, "========================");
        db.getBulletinCursor();
        Log.d(TAG, "========================");

        String  a  =  "\n--------------------AhimsaWallet--------------------\n";
        a +=  keyStore.toString() + "\n";
        a +=  "----------------------Database----------------------\n";
        a +=  db.toString() + "\n";
        a +=  "----------------------------------------------------\n";
        a +=  "DB_BALANCE: " + db.getConfirmedBalance(true).toString() + "\n";
        a +=  "CONFIG_IS_FUNDED: " + config.getIsFunded() + "\n";
        a +=  "CONFIG_TXID: " + config.getFundingTxid() + "\n";
        a +=  "DEFAULT_KEY: " + config.getDefaultAddress() + "\n";
        a +=  "----------------------------------------------------\n";

        return a;
    }

}
