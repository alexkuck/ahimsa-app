package io.ahimsa.ahimsa_app.core;

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
    }

    // public utilities-----------------------------------------------------------------------------
    public void reserveTxOuts()
    {
        // implement cost_estimation instead of flat max rate?
        // Long estimate_cost = Utils.getEstimatedCost(fee, config.getDustValue(), topic.length(), message.length());

        db.reserveTxOuts(config.getMinCoinNecessary());
    }

    public void unreserveTxOuts()
    {
        db.unreserveTxOuts(config.getMinCoinNecessary());
    }

    public void removeAllReservations()
    {
        db.removeAllReservations();
    }

    public Transaction createAndAddBulletin(String topic, String message, Long fee) throws Exception
    {
        List<TransactionOutput> unspents = db.getUnspentOutputs(config.getMinCoinNecessary());
        Transaction bulletin = BulletinBuilder.createTx(config, keyStore, unspents, topic, message);
        db.addBulletin(bulletin.getHashAsString(), topic, message, fee);
        return bulletin;
    }

    public void commitTransaction(Transaction tx, Long highest_block, Boolean confirmed)
    {
        // This function asks the database to store the change outputs of the bulletin as unspent
        // outputs. Additionally, the transaction is stored as unconfirmed.

        if ( !db.hasTransaction(tx) ){
            // Add raw transaction to database.
            db.addTx(tx, confirmed, highest_block);

            // Add all relevant future outpoints to ahimsaDB.
            for(TransactionOutput out : tx.getOutputs()){
                if(out.isMine(keyStore)){
                    db.addTxOut(out);
                }
            }

            // Flag funding outs as spent.
            boolean unreserve_required = false;
            for(TransactionInput in : tx.getInputs()){
                String previous_txid = in.getOutpoint().getHash().toString();
                Long previous_vout = in.getOutpoint().getIndex();

                if( db.setStatusSpent(previous_txid, previous_vout) ){
                    unreserve_required = true;
                }
            }

            // Spent an unreserved txout, must remove a reserved txout.
            if (unreserve_required) {
                db.unreserveTxOuts(config.getMinCoinNecessary());
            }

        }
    }

    public void confirmTx(Transaction tx)
    {
        // This function informs the database that one of the wallet's transaction has been included
        // in the block chain below a target depth.

        db.confirmTx(tx.getHashAsString());
    }

    public void dropTx(Transaction tx)
    {
        //todo work
    }

    //----------------------------------------------------------------------------------------------
    public Wallet getKeyStore()
    {
        return keyStore;
    }

    public void verifyKeyStore() {
        if(keyStore.getKeys().isEmpty()) {
            ECKey key = new ECKey();
            keyStore.addKey(key);
            config.setDefaultAddress( key.toAddress(Constants.NETWORK_PARAMETERS).toString() );
            saveKeyStore();
        }
    }

    public void reset()
    {
        //reset config, database, and keyStore.
        config.reset();
        db.reset();
        for(ECKey key : keyStore.getKeys()){
            keyStore.removeKey(key);
        }
        verifyKeyStore();
    }

    public Long getConfirmedBalance(boolean only_unreserved)
    {
        return db.getConfirmedBalance(only_unreserved);
    }

    public Bundle getUpdateBundle() {
        Bundle update_bundle = new Bundle();

        update_bundle.putInt(Constants.EXTRA_INT_CONF, db.getConfirmedTxs().getCount());
        update_bundle.putInt(Constants.EXTRA_INT_UNCONF, db.getUnconfirmedTxs().getCount());
        update_bundle.putInt(Constants.EXTRA_INT_DRAFT, db.getDraftTx().getCount());

        update_bundle.putLong(Constants.EXTRA_LONG_UNRESERVED_CONF_BAL, db.getConfirmedBalance(true));
        update_bundle.putLong(Constants.EXTRA_LONG_CONF_BAL, db.getConfirmedBalance(false));
        update_bundle.putLong(Constants.EXTRA_LONG_UNCONF_BAL, db.getUnconfirmedBalance().longValue());
        update_bundle.putInt(Constants.EXTRA_INT_CONF_TXOUTS, db.getConfirmedAndUnspentTxOuts().getCount());
        update_bundle.putInt(Constants.EXTRA_INT_UNCONF_TXOUTS, db.getUnconfirmedAndUnspentTxOuts().getCount());

        return update_bundle;
    }


    // private utilities----------------------------------------------------------------------------
    private void saveKeyStore()
    {
        try {
            protobufSerializeWallet(keyStore);
        }
        catch (final IOException x){
            throw new RuntimeException(x);
        }
    }

    // Load/Save Wallet-----------------------------------------------------------------------------
    private void loadWalletFromProtobuf() {
        if(keyStoreFile.exists()) {
            final long start = System.currentTimeMillis();
            FileInputStream walletStream = null;

            try {
                walletStream = new FileInputStream(keyStoreFile);
                keyStore = new WalletProtobufSerializer().readWallet(walletStream);

                Log.d(TAG, "wallet loaded from: '" + keyStoreFile + "', took " + (System.currentTimeMillis() - start) + "ms");
            }
            catch (final FileNotFoundException x) {
                Log.e(TAG, "problem loading wallet", x);
                //Toast.makeText(AhimsaApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }
            catch (final UnreadableWalletException x) {
                Log.e(TAG, "problem loading wallet", x);
                //Toast.makeText(AhimsaApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }
            finally {
                if (walletStream != null){
                    try{
                        walletStream.close();
                    }
                    catch (final IOException x){
                        // swallow
                    }
                }
            }

            if (!keyStore.isConsistent()){
                Log.d(TAG, "problem loading wallet");
                //Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();
                //wallet = restoreWalletFromBackup();
            }

            if (!keyStore.getParams().equals(Constants.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + keyStore.getParams().getId());
        }
        else{
            keyStore = new Wallet(Constants.NETWORK_PARAMETERS);
        }
    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(keyStoreFile);

        Log.d(TAG, "wallet saved to: '" + keyStoreFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }





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
