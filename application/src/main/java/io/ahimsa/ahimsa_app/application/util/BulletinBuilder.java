package io.ahimsa.ahimsa_app.application.util;

import android.util.Log;

import java.math.BigInteger;
import java.util.List;

import com.google.bitcoin.core.Wallet;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction.SigHash;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;


public class BulletinBuilder {

    public static void addInputs(Transaction tx, List<Utils.DbTxOutpoint> db_unspent){

        for(Utils.DbTxOutpoint out : db_unspent){
            TransactionOutPoint outpoint = new TransactionOutPoint(Constants.NETWORK_PARAMETERS, out.vout, out.tx);
            tx.addInput(new TransactionInput(Constants.NETWORK_PARAMETERS, tx, new byte[]{}, outpoint));
        }
    }

    public static Address encodeAddress(String slice) {

        if (slice.length() != 20) {
            for (int w = slice.length(); w < 20; w++) {
                slice += "_";
            }
        }
        return new Address(Constants.NETWORK_PARAMETERS, slice.getBytes());
    }


    public static void addMessageOutputs(Configuration config, Transaction tx, String message) {

        if (message.length() > 140) {
            throw new Error("MESSAGE LENGTH OVER 140-0000000000000");
        }

        String slice = "";
        for (int i = 0; i < message.length(); i++) {
            slice += message.charAt(i);
            if (slice.length() == 20) {
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, BigInteger.valueOf(config.getDustValue()), encodeAddress(slice)) );
                slice = "";
            }
        }
        tx.addOutput(new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, BigInteger.valueOf(config.getDustValue()), encodeAddress(slice)));

    }

    public static void addChangeOutput(Configuration config, Transaction tx, List<Utils.DbTxOutpoint> db_unspent) throws Exception{

        BigInteger fee      = BigInteger.valueOf(config.getFeeValue());
        BigInteger in_coin  = totalInCoin(db_unspent);
        BigInteger out_coin = totalOutCoin(tx);

        BigInteger total = BigInteger.ZERO.add(in_coin).subtract(out_coin).subtract(fee);

        Log.d("BB", "fee |" + fee.toString());
        Log.d("BB", "in_coin |" + in_coin.toString());
        Log.d("BB", "out_coin |" + out_coin.toString());
        Log.d("BB", "total |" + total.toString());


        switch ( total.compareTo(BigInteger.ZERO) ){
            case  0:
            case  1:    break;
            case -1:    Log.d("BB", Utils.bytesToHex(tx.bitcoinSerialize()) );
                        throw new Exception("out_coin+fee exceeds in_coin | " + total.toString());
        }

        Address default_addr = new Address(Constants.NETWORK_PARAMETERS, config.getDefaultAddress());
        tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, total, default_addr) );
    }


    public static BigInteger totalInCoin(List<Utils.DbTxOutpoint> db_unspent){
        BigInteger in_coin = BigInteger.ZERO;
        for(Utils.DbTxOutpoint out : db_unspent){
            in_coin = in_coin.add(out.value);
        }
        return in_coin;
    }
    public static BigInteger totalOutCoin(Transaction tx){
        BigInteger out_coin = BigInteger.ZERO;
        for(TransactionOutput out : tx.getOutputs()){
            out_coin = out_coin.add(out.getValue());
        }
        return out_coin;
    }



    //--------------------------------------------------------------------------------
    public static Transaction createTx(Configuration config, Wallet wallet, List<Utils.DbTxOutpoint> db_unspent, String message, String topic) throws Exception{
        //create new transaction
        Transaction bulletin = new Transaction(Constants.NETWORK_PARAMETERS);

        //add inputs and message outputs
        addInputs(bulletin, db_unspent);
        addMessageOutputs(config, bulletin, message);

        //add topic
        //todo

        //add change output to transaction
        addChangeOutput(config, bulletin, db_unspent);

        //sign the inputs
        bulletin.signInputs(SigHash.ALL, wallet);
        return bulletin;
    }





}
