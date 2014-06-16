package io.ahimsa.ahimsa_app.application.util;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

import java.math.BigInteger;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.service.FundingService;
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

        return a;
    }

    private void reset()
    {
        //clear wallet of transactions and keys
        wallet.clearTransactions(0);
        for(ECKey key : wallet.getKeys()){
            wallet.removeKey(key);
        }
        save();

        //set configuration to default
        config.reset();

        //clear database
        //todo: database reset

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
        FundingService.startActionRequestFundedTx(application, config.getFundingIP());
    }

    private void noBalance(){
        //todo: something

    }


    //---------------------------------------------------------------------------
    //The Big Honkers------------------------------------------------------------
    public void broadcastBulletin(String topic, String message)
    {
//        BulletinBuilder.createByteTX()
//        NodeService.startActionBroadcastTx(context, );
    }

    public void broadcastFundingTx(Transaction tx)
    {
        NodeService.startActionBroadcastTx(application, tx.bitcoinSerialize());
        wallet.commitTx(tx);
        Log.d(TAG, "I do not believed this worked." + toString());
        Log.d(TAG, wallet.getBalance().toString());
        try {
            db.addTx(tx.getHash().toString(), 1, BigInteger.ONE, "distributed", false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //todo: informFundingTx()


    //Public Methods--------------------------------------------------------------
    @Override
    public String toString()
    {
        return wallet.toString();
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
