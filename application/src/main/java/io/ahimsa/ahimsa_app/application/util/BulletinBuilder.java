package io.ahimsa.ahimsa_app.application.util;

import android.util.Log;

import java.math.BigInteger;
import java.util.Arrays;
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

    private static String TAG = "BulletinBuilder";

    public static void addInputs(Transaction tx, List<TransactionOutput> unspents){

        for(TransactionOutput out : unspents){
            TransactionOutPoint outpoint = new TransactionOutPoint(Constants.NETWORK_PARAMETERS, indexOf(out), out.getParentTransaction());
            tx.addInput(new TransactionInput(Constants.NETWORK_PARAMETERS, tx, new byte[]{}, outpoint));
        }
    }

    public static int indexOf(TransactionOutput out){
        Transaction parent = out.getParentTransaction();
        for(int i = 0; i < parent.getOutputs().size(); i++){
            if(Arrays.equals(out.bitcoinSerialize(), parent.getOutput(i).bitcoinSerialize())){
                return i;
            }
        }
        return -1;
    }

    public static Address encodeAddress(String slice) {

        if (slice.length() != Constants.CHAR_PER_OUT) {
            for (int w = slice.length(); w < Constants.CHAR_PER_OUT; w++) {
                slice += "_";
            }
        }
        return new Address(Constants.NETWORK_PARAMETERS, slice.getBytes());
    }

    public static void addMessageOutputs(Configuration config, Transaction tx, String message) {

        if (message.length() > Constants.MAX_MESSAGE_LEN) {
            throw new Error("MESSAGE LENGTH OVER 140-0000000000000");
        }

        String slice = "";
        for (int i = 0; i < message.length(); i++) {
            slice += message.charAt(i);
            if (slice.length() == Constants.CHAR_PER_OUT) {
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, BigInteger.valueOf(config.getDustValue()), encodeAddress(slice)) );
                slice = "";
            }
        }
        tx.addOutput(new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, BigInteger.valueOf(config.getDustValue()), encodeAddress(slice)));

    }

    public static void addChangeOutput(Configuration config, Transaction tx, List<TransactionOutput> unspents) throws Exception{

        BigInteger fee      = BigInteger.valueOf(config.getFeeValue());
        BigInteger in_coin  = totalInCoin(unspents);
        BigInteger out_coin = totalOutCoin(tx);

        BigInteger total = BigInteger.ZERO.add(in_coin).subtract(out_coin).subtract(fee);

        Log.d(TAG, "fee |" + fee.toString());
        Log.d(TAG, "in_coin |" + in_coin.toString());
        Log.d(TAG, "out_coin |" + out_coin.toString());
        Log.d(TAG, "total |" + total.toString());


        switch ( total.compareTo(BigInteger.ZERO) ){
            case  0:
            case  1:    break;
            case -1:    Log.d(TAG, Utils.bytesToHex(tx.bitcoinSerialize()) );
                        throw new Exception("out_coin+fee exceeds in_coin | " + total.toString());
        }

        BigInteger min = BigInteger.valueOf(config.getMinCoinNecessary());
        Address default_addr = new Address(Constants.NETWORK_PARAMETERS, config.getDefaultAddress());
        while(total.compareTo(BigInteger.ZERO) == 1){
            if(total.compareTo(min) > 0){
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, min, default_addr) );
                total = total.subtract(min);
            } else{
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, total, default_addr) );
                total = total.subtract(total);
            }
        }
    }

    public static BigInteger totalInCoin(List<TransactionOutput> db_unspent){
        BigInteger in_coin = BigInteger.ZERO;
        for(TransactionOutput out : db_unspent){
            in_coin = in_coin.add(out.getValue());
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

    //----------------------------------------------------------------------------------------------
    public static Transaction createTx(Configuration config, Wallet wallet, List<TransactionOutput> unspents, String topic, String message) throws Exception{
        //create new transaction
        Transaction bulletin = new Transaction(Constants.NETWORK_PARAMETERS);

        //add inputs and message outputs
        addInputs(bulletin, unspents);
        addMessageOutputs(config, bulletin, message);

        //add topic
        //todo

        //add change output to transaction
        addChangeOutput(config, bulletin, unspents);

        //sign the inputs
        bulletin.signInputs(SigHash.ALL, wallet);
        return bulletin;
    }





}
