package io.ahimsa.ahimsa_app.core;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.script.Script;
import java.util.Arrays;
import java.util.List;

import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;

/**
 * Created by askuck on 7/11/14.
 */
public class AhimsaWallet {
    private String TAG = "AhimsaWallet";

    private Configuration config;
    private AhimsaDB db;
    private ECKey key;

    public AhimsaWallet(Configuration config, AhimsaDB db)
    {
        this.config = config;
        this.db = db;
        this.key = config.getDefaultECKey();
    }

    // Reservations --------------------------------------------------------------------------------
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

    // Wallet Actions ------------------------------------------------------------------------------
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
        Transaction bulletin = BulletinBuilder.createTx(key, unspents, topic, message);

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
            int vout = 0;
            for(TransactionOutput out : tx.getOutputs())
            {
                Log.d(TAG, "commitTransaction() | " + out.toString());
                Log.d(TAG, "isRelevant(): " + isRelevant(out));
                if(isRelevant(out))
                {
                    Log.d(TAG, "txout(): " + out.toString());
                    db.addTxOut(out, vout);
                }
                vout++;
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

    public void confirmTx(String txid, Long height)
    {
        db.confirmTx(txid, height);
    }

    public void dropTx(String txid, Long height)
    {
        // TODO | SOMETHING
        Log.d(TAG, String.format("Tx was dropped at height %s | %s", height.toString(), txid));
    }

    public void setHighestBlock(String txid, Long height)
    {
        db.setHighestBlock(txid, height);
    }

    // Is it relevant? -----------------------------------------------------------------------------
    public boolean isRelevant(TransactionOutput out)
    {
        try {
            Script script = out.getScriptPubKey();
            if (script.isSentToRawPubKey())
            {
                return Arrays.equals(key.getPubKey(), script.getPubKey());
            }
            if (script.isPayToScriptHash())
            {
//                return wallet.isPayToScriptHashMine(script.getPubKeyHash());
                return false; // unsupported. todo: support
            }
            else
            {
                return Arrays.equals(key.getPubKeyHash(), script.getPubKeyHash());
            }
        }
        catch (ScriptException e)
        {
            // Just means we didn't understand the output of this transaction: ignore it.
            Log.e(TAG, "Could not parse tx output script: {}", e);
            return false;
        }
    }

    public boolean isRelevant(Transaction tx)
    {
        for(TransactionOutput out : tx.getOutputs())
            if(isRelevant(out))
                return true;

        return false;
    }

    // Getters -------------------------------------------------------------------------------------
    public ECKey getKey()
    {
        return key;
    }

    public Long getEarliestKeyCreationTime()
    {
        return config.getEarliestKeyCreationTime();
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

        update_bundle.putString(Constants.EXTRA_STRING_ADDRESS, key.toAddress(Constants.NETWORK_PARAMETERS).toString());
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

    //----------------------------------------------------------------------------------------------
    public void resetDB()
    {
        db.reset();
    }

    public void toLog(){

        Log.d(TAG, "========================");
        db.getBulletinCursor();
        Log.d(TAG, "========================");

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
        StringBuilder buf = new StringBuilder("\n" +"--------------------AhimsaWallet--------------------\n");
        buf.append("private key: " + key.getPrivKey().toString() + "\n");
        buf.append("public key: " + key.toAddress(Constants.NETWORK_PARAMETERS) + "\n");
        buf.append("earliestKeyCreationTime(): " + getEarliestKeyCreationTime() + "\n");
        buf.append("----------------------Database----------------------\n");
        buf.append(db.toString() + "\n");
        buf.append("----------------------------------------------------\n");
        buf.append("DB_BALANCE: " + db.getConfirmedBalance(true).toString() + "\n");

        return buf.toString();
    }

}
