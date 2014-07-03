package io.ahimsa.ahimsa_app.application.util;

import android.database.Cursor;
import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.service.AnonFundService;
import io.ahimsa.ahimsa_app.application.service.NodeService;

/**
 * Created by askuck on 6/12/14.
 */
public class AhimsaWallet {
    private static final String TAG = "AhimsaWallet";

    private MainApplication application;
    private Wallet wallet;
    private AhimsaDB db;
    private Configuration config;



    public AhimsaWallet(MainApplication application, Wallet wallet, AhimsaDB db, Configuration config)
    {
        this.application = application;
        this.wallet = wallet;
        this.db = db;
        this.config = config;

        initiate();
    }

    //Start Up Procedure----------------------------------------------------------------------------
    private void initiate()
    {
        if (isConsistent())
            ensureKey();
        else
            reset();
    }

    private void ensureKey()
    {
        if(wallet.getKeys().size() > 0)
            ensureCoin();
        else
            newKey();
    }

    private void ensureCoin()
    {
        BigInteger min = BigInteger.valueOf(config.getMinCoinNecessary());
        BigInteger bal = db.getBalance();
        Log.d(TAG, "Balance: " + bal.toString());

        switch(bal.compareTo(min)){
            case -1:    if( !config.getIsFunded() ){ requestFunding(); }
                        else{ noBalance(); }
                        break;
            case  0:
            case  1:
                        break;
        }
    }

    //----------------------------------------------------------------------------------------------
    private boolean isConsistent()
    {
        //todo: something
        boolean a = wallet.isConsistent();
        boolean b = true;
        boolean c = true;

        return a && b && c;
    }

    public void reset()
    {
        Log.d(TAG, "Reseting wallet.");
        //clear wallet of transactions and keys
        wallet.clearTransactions(0);
        for(ECKey key : wallet.getKeys()){
            wallet.removeKey(key);
        }
        save();

        //set configuration to default
        config.reset();

        //clear database
        db.reset();

        Log.d(TAG, toString());

        //return to initiate()
        initiate();

    }

    private void newKey()
    {
        ECKey key = new ECKey();

        wallet.addKey(key);
        config.setDefaultAddress( key.toAddress(Constants.NETWORK_PARAMETERS).toString() );

        save();

        //return to ensureKey()
        ensureKey();
    }

    public BigInteger getBalance()
    {
        return db.getBalance();
    }

    private void requestFunding()
    {
        AnonFundService.startActionRequestFundedTx(application, config.getFundingIP());
    }

    private void noBalance(){
        //todo: something

    }


    //----------------------------------------------------------------------------------------------
    //The Big Honkers-------------------------------------------------------------------------------
    public void createAndBroadcastBulletin(String topic, String message)
    {
        Log.d(TAG, String.format("Topic: %s | Message: %s", topic, message));
        List<TransactionOutput> unspents = getAnUnspent();

        try {
            Transaction bulletin = BulletinBuilder.createTx(config, wallet, unspents, topic, message);
            db.addBulletin(bulletin.getHashAsString(), topic, message);

            // Upon successful creation of the peerGroup and an attempt at broadcast an intent is
            // broadcast. This intent informs the application that a bulletin has been created and broadcast.
            NodeService.startActionBroadcastTx(application, bulletin.bitcoinSerialize());

            Log.d(TAG, "Bulletin txid: " + bulletin.getHashAsString());

        } catch (Exception e) {
            // todo handle
            Log.d(TAG, "Unsuccessful attempt at creating bulletin: " + e.toString());
            e.printStackTrace();
        }
    }
    //----------------------------------------------------------------------------------------------
    public void maybeCommitConfirmedTx(Transaction tx, @Nullable Long highest_block){
        // This function will only add the parameter transaction if it is not within the
        // database.  commitConfirmedTx is called when

        if( !db.hasTransaction(tx) ){
            commitConfirmedTx(tx, highest_block);
        }
    }

    public void commitConfirmedTx(Transaction tx, @Nullable Long highest_block){
        // This function informs the database of a transaction that can be used to fund future
        // bulletins. The transaction must be in the block chain below a target depth. All of
        // the transaction's outputs that the wallet has keys for is treated as spendable.

        // Add raw transaction to database.
        db.addTx(tx, true, highest_block);

        // Add all relevant future outpoints to ahimsaDB.
        for(TransactionOutput out : tx.getOutputs()){
            if(out.isMine(wallet)){
                db.addTxOut(out);
            }
        }
    }

    public void commitBulletin(Transaction tx, Long highest_block){
        // This function asks the database to store the change outputs of the bulletin as unspent
        // outputs. Additionally, the transaction is stored as unconfirmed.

        // Add raw transaction to database.
        db.addTx(tx, false, highest_block);

        // Add all relevant future outpoints to ahimsaDB.
        for(TransactionOutput out : tx.getOutputs()){
            if(out.isMine(wallet)){
                db.addTxOut(out);
            }
        }

        // Flag funding outs as spent.
        for(TransactionInput in : tx.getInputs()){
            String previous_txid = in.getOutpoint().getHash().toString();
            Long previous_vout = in.getOutpoint().getIndex();

            db.setSpent(previous_txid, previous_vout, true);
        }
    }

    public void confirmTx(Transaction tx){
        // This function informs the database that one of the wallet's transaction has been included
        // in the block chain below a target depth.

        db.confirmTx(tx.getHashAsString());
    }

    //----------------------------------------------------------------------------------------------
    private List<TransactionOutput> getAnUnspent(){
        //get unspent transactions from ahimsa's sqlite database sorted in descending order
        List<TransactionOutput> db_unspent = db.getUnspent();
        BigInteger min = BigInteger.valueOf(config.getMinCoinNecessary());

        ArrayList<TransactionOutput> unspents = new ArrayList<TransactionOutput>();
        BigInteger bal = BigInteger.ZERO;

        for(TransactionOutput out : db_unspent){
            if(bal.compareTo(min) >= 0)
                break;

            unspents.add(out);
            bal = bal.add(out.getValue());
        }

        return unspents;
    }

    //----------------------------------------------------------------------------------------------
    public Cursor getTransactionCursor(){
        return db.getTransactionCursor();
    }

    public Cursor getBulletinCursor() {
        return db.getBulletinCursor();
    }

    public Cursor getTransactionOutputsCursor() {
        return db.getTransactionOutputsCursor();
    }

    //Public Methods--------------------------------------------------------------------------------
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
                a +=  wallet.toString() + "\n";
                a +=  "----------------------Database----------------------\n";
                a +=  db.toString() + "\n";
                a +=  "----------------------------------------------------\n";
                a +=  "DB_BALANCE: " + getBalance().toString() + "\n";
                a +=  "CONFIG_IS_FUNDED: " + config.getIsFunded() + "\n";
                a +=  "CONFIG_TXID: " + config.getFundingTxid() + "\n";
                a +=  "DEFAULT_KEY: " + config.getDefaultAddress() + "\n";
                a +=  "----------------------------------------------------\n";

        return a;
    }

    public void save()
    {
        application.save();
    }

    public final ECKey getDefaultECKey()
    {
        String defaultaddr = config.getDefaultAddress();

        if( defaultaddr == null ){
            throw new NullPointerException("Default key is null");
        }

        for(ECKey key : wallet.getKeys()){
            if( key.toAddress(Constants.NETWORK_PARAMETERS).toString().equals(defaultaddr))
                return key;
        }

        //todo: default key is not in wallet, throw error,
        return null;

    }

    //necessary for nodeservice finding relevant transactions
    public final Wallet getWallet(){
        return wallet;
    }

    //Utility---------------------------------------------------------------------------------------
    private boolean validMessage(String msg)
    {
        if(msg.length() > 140){
            return false;
        }

        return true;
    }

    private boolean validTopic(String topic)
    {
        if(topic.length() > 15){
            return false;
        }

        return true;
    }


}
