package io.ahimsa.ahimsa_app.application.util;

import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;

import java.math.BigInteger;
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

    //Start Up Procedure---------------------------------------------------------
    private void initiate()
    {
        //if  isConsistent: ensureKey
        //if !isConsistent: reset, initiate

        if (isConsistent())
            ensureKey();
        else
            reset();

    }

    private void ensureKey()
    {
        //if  keys > 0: ensureCoin
        //if !keys > 0: newKey, ensureKey

        if(wallet.getKeys().size() > 0)
            ensureCoin();
        else
            newKey();

    }

    private void ensureCoin()
    {
        //if  getBalance > 0:
        //  if  Config.funded:
        //  if !Config.funded:
        //if !getBalance > 0:
        //  if  Config.funded:
        //  if !Config.funded:

        BigInteger min = BigInteger.ZERO;
        BigInteger fee = BigInteger.valueOf(config.getFeeValue());
        BigInteger out = BigInteger.valueOf(config.getDustValue()*Constants.MAX_OUTPUTS);

                   min = min.add(fee).add(out);
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

    //---------------------------------------------------------------------------
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


    //---------------------------------------------------------------------------
    //The Big Honkers------------------------------------------------------------
    public void broadcastBulletin(String topic, String message)
    {
        Log.d(TAG, String.format("Topic: %s | Message: %s", topic, message));

        //get all unspent transactions from ahimsa's sqlite database
        List<Utils.DbTxOutpoint> db_unspent = db.getUnspent();

        //todo: make this it's own function
        // associate transactions from the wallet to each outpointTx
        for(Utils.DbTxOutpoint out : db_unspent){
            Transaction tx = wallet.getTransaction(new Sha256Hash(out.txid));
            if(tx != null)
                out.tx = tx;
            //todo: wallet/db not in sync, must reset !!! (check for consistency before these actions?)
        }

        Log.d(TAG, "outPointsList: " + db_unspent);
        try {
            Transaction bulletin = BulletinBuilder.createTx(config, wallet, db_unspent, message, topic);
            Log.d(TAG, "BULLETIN BULLETIN BULLETIN: " + Utils.bytesToHex(bulletin.bitcoinSerialize()));
            commitTx(bulletin);
            NodeService.startActionBroadcastTx(application, bulletin.bitcoinSerialize());
        } catch (Exception e) {
            Log.d(TAG, "Unsuccessful attempt at creating bulletin: " + e.toString());
            e.printStackTrace();
        }
        //todo todo todo todo todo todo todo todo
        //todo: commitTx / addTx to database method

    }

    public void broadcastFundingTx(Transaction tx) throws Exception
    {
        //add transaction to wallet and its' outpoints to db
        commitTx(tx);

        //add funding txid to configuration but DO NOT switch funded flag
        //this txid in config will be overwritten if unsuccessful broadcast
        //todo: handle transactions that were not broadcasted
        config.setFundingTxid(tx.getHash().toString());

        //broadcast transaction. if future successful, change pending flag to distributed
        NodeService.startActionBroadcastTx(application, tx.bitcoinSerialize());

    }

    public void successOnBroadcastTx(Transaction future){

        //change pending flag of the future transaction in AhimsaDb to successfully distributed
        String txid = future.getHashAsString();
        Log.d(TAG, "succesOnBroadcastTx() | txid: " + txid);
        try{
            db.setStatus(txid, AhimsaDB.DISTRIBUTED);
            Log.d(TAG, "Change tx status from to 'distributed' | " + txid);
        }catch(Exception x){
            //todo: handle
            x.printStackTrace();
            throw new RuntimeException("womp something went wrong. a future txid was returned that DNE in ahimsadb");
        }

        //change the spent flag of transactions used to fund the future transaction
        for(TransactionInput in : future.getInputs()){
            String outpoint_txid = in.getOutpoint().getHash().toString();
            Log.d(TAG, "succesOnBroadcastTx() | outpoint_txid: " + outpoint_txid);
            try{
                db.setSpent(outpoint_txid, true);
            }catch (Exception x){
                //todo: handle
                x.printStackTrace();
                throw new RuntimeException("womp something went wrong. a future txid was returned that DNE in ahimsadb");
            }
        }

        //check if txid was the funding transaction, set flag to true if so.
        if(config.getFundingTxid().equals(txid)){
            config.setIsFunded(true);
            Log.d(TAG, "getIsFunded() | " + config.getIsFunded());
        }
        Log.d(TAG, "Successful broadcast.");
    }

    //todo: informFundingTx()

    //----------------------------------------------------------------------------
    private void commitTx(Transaction tx){

        //add all transactions to wallet
        wallet.commitTx(tx);
        save();

        //AhimsaDB has the official record of transactions to spend
        for(TransactionOutput out : tx.getOutputs()){
            if(out.isMine(wallet)){
                try {
                    db.addTx(tx.getHash().toString(), tx.getOutputs().indexOf(out), out.getValue(), AhimsaDB.PENDING, false);
                } catch (Exception e) {
                    Log.d(TAG, "addTx within the ahimsaDB threw an error");
                    //todo: handle
                }
            }
        }

    }


    //Public Methods--------------------------------------------------------------
    public void toLog(){
        String wallet = toString();

        if (wallet.length() > 4000) {
            int sections = wallet.length() / 4000;
            for (int i = 0; i <= sections; i++) {
                int max = 4000 * (i + 1);
                if (max >= wallet.length()) {
                    Log.d(TAG, "Section| " + i + "/" + sections + "\n" + wallet.substring(4000 * i));
                } else {
                    Log.d(TAG, "Section| " + i + "/" + sections + "\n" + wallet.substring(4000 * i, max));
                }
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
                a +=  "DB_BALANCE: " + db.getBalance().toString() + "\n";
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

    //Utility---------------------------------------------------------------------------
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
