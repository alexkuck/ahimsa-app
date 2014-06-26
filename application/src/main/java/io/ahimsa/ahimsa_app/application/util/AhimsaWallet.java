package io.ahimsa.ahimsa_app.application.util;

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
    public void broadcastBulletin(String topic, String message)
    {
        Log.d(TAG, String.format("Topic: %s | Message: %s", topic, message));

        List<Utils.DbTxOutpoint> unspent = getAnUnspent();
        Log.d(TAG, "unspent outpoint list: " + unspent);

        try {
            Transaction bulletin = BulletinBuilder.createTx(config, wallet, unspent, message, topic);
            Log.d(TAG, "BULLETIN BULLETIN BULLETIN: " + Utils.bytesToHex(bulletin.bitcoinSerialize()));
            broadcastTx(bulletin);
        } catch (Exception e) {
            Log.d(TAG, "Unsuccessful attempt at creating bulletin: " + e.toString());
            e.printStackTrace();
        }
    }

    //todo: informFundingTx()

    //----------------------------------------------------------------------------------------------
    public void broadcastTx(Transaction tx) throws Exception{
        //assuming that no returned future means network does not have transaction
        NodeService.startActionBroadcastTx(application, tx.bitcoinSerialize());
    }

    public void commitTx(Transaction future){
    //called during: successOnBroadcast and

        //add transaction to wallet
        wallet.maybeCommitTx(future);
        save();

        //flag all relevant ahimsaDB outpoints (the future's inputs) as spent
        for(TransactionInput in : future.getInputs()){
            String outpoint_txid = in.getOutpoint().getHash().toString();
            Long outpoint_vout = in.getOutpoint().getIndex();
            try{
                db.setSpent(outpoint_txid, outpoint_vout, true);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //add all relevant future outpoints to ahimsaDB
        for(TransactionOutput out : future.getOutputs()){
            if(out.isMine(wallet)){
                try {
                    db.addOutpoint(future.getHash().toString(), future.getOutputs().indexOf(out), out.getValue(), AhimsaDB.DISTRIBUTED, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //if future was the funding transaction from https, set config flag as true.
        if(config.getFundingTxid().equals(future.getHashAsString())){
            config.setIsFunded(true);
            Log.d(TAG, "getIsFunded() | " + config.getIsFunded());
        }

        Log.d(TAG, "Successful broadcast.");
    }

    public void failureOnBroadcastTx(){
        //todo handle
    }


    //----------------------------------------------------------------------------------------------
    private void discoveredTx(Transaction tx){
        //maybe block or blockhash


    }

    private List<Utils.DbTxOutpoint> getAnUnspent(){
        //get unspent transactions from ahimsa's sqlite database sorted in descending order
        List<Utils.DbTxOutpoint> db_unspent = db.getUnspent(config.getOnlyConfirmed());
        BigInteger min = BigInteger.valueOf(config.getMinCoinNecessary());

        ArrayList<Utils.DbTxOutpoint> unspent = new ArrayList<Utils.DbTxOutpoint>();
        BigInteger bal = BigInteger.ZERO;

        for(Utils.DbTxOutpoint out : db_unspent){
            if(bal.compareTo(min) >= 0)
                break;

            unspent.add(out);
            bal = bal.add(out.value);
        }

        // associate transactions from the wallet to each outpointTx
        for(Utils.DbTxOutpoint out : unspent){
            Transaction tx = wallet.getTransaction(new Sha256Hash(out.txid));
            if(tx != null)
                out.tx = tx;
            //todo: wallet/db not in sync, must reset !!! (check for consistency before these actions?)
        }

        return unspent;
    }

    //----------------------------------------------------------------------------------------------
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
                a +=  "DEFAULT_KEY: " + config.getDefaultAddress() + "\n";
                a +=  "----------------------------------------------------\n";

        return a;
    }

    public void save()
    {
        application.save();
    }

    public ECKey getDefaultECKey()
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
    public Wallet getWallet(){
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
