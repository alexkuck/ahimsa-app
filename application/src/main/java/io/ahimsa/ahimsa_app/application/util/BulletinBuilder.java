package io.ahimsa.ahimsa_app.application.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction.SigHash;


public class BulletinBuilder {

    public static TransactionInput formulateInput(NetworkParameters net, ECKey key, String txid, BigInteger inValue, int index) {

        //new transaction with hash param
        //.addoutput with correct address, value, **index**
        //add this transaction to the transaction outpoint
        //add the transactionOutPoint to the transaction input


        Sha256Hash hash = new Sha256Hash(txid);
        Transaction previous = new Transaction(net, 1, hash) {
            @Override
            protected void unCache() {
                //do nothing
            }
        };

        for (int i = 0; i < 1 + index; i++) {
            previous.addOutput(inValue, key.toAddress(net));
        }

        TransactionOutPoint outpoint = new TransactionOutPoint(net, index, previous);
        return new TransactionInput(net, null, new byte[]{}, outpoint);

    }

    public static Address encodeAddress(NetworkParameters net, String slice) {

        if (slice.length() != 20) {
            for (int w = slice.length(); w < 20; w++) {
                slice += "_";
            }
        }

        // byte[] arr = slice.getBytes();
        // for(int i = 0; i < arr.length; i++){
        //     System.out.print(arr[i] + "|");    
        // } System.out.println();

        return new Address(net, slice.getBytes());
    }


    public static ArrayList<TransactionOutput> formulateMessageOutput(NetworkParameters net, BigInteger dust, String message) {

        if (message.length() > 140) {
            throw new Error("MESSAGE LENGTH OVER 140-0000000000000");
        }

        ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

        String slice = "";
        for (int i = 0; i < message.length(); i++) {
            slice += message.charAt(i);
            if (slice.length() == 20) {
                outputs.add(new TransactionOutput(net, null, dust, encodeAddress(net, slice)));
                slice = "";
            }
        }
        outputs.add(new TransactionOutput(net, null, dust, encodeAddress(net, slice)));

        return outputs;
    }

    public static TransactionOutput formulateChangeOutput(NetworkParameters net, ECKey key, BigInteger in_coin, List<TransactionOutput> outList, BigInteger fee) {

        BigInteger total = new BigInteger("0").subtract(fee).add(in_coin);
        for (TransactionOutput out : outList) {
            total = total.subtract(out.getValue());
        }
        return new TransactionOutput(net, null, total, key.toAddress(net));


    }

    public static Transaction createTX(NetworkParameters net, BigInteger fee, BigInteger dust,
                                       ECKey key, String txid, BigInteger in_coin, int index,
                                       String message) throws Exception {
        //set up variables
        TransactionInput input = formulateInput(net, key, txid, in_coin, index);
        ArrayList<TransactionOutput> msgOutputs = formulateMessageOutput(net, dust, message);
        Wallet wallet = new Wallet(net);

        //add key to wallet
        wallet.addKey(key);

        //create new transaction
        Transaction result = new Transaction(net);

        //add inputs to transaction
        result.addInput(input);

        //add message outputs to transaction
        for (TransactionOutput out : msgOutputs) {
            result.addOutput(out);
        }

        //add hashtags outputs to transaction

        //add useragent outputs to transaction

        //add change output to transaction
        result.addOutput(formulateChangeOutput(net, key, in_coin, result.getOutputs(), fee));


        //sign transaction
        result.signInputs(SigHash.ALL, wallet);

        return result;
    }

    public static byte[] createByteTX(NetworkParameters net, BigInteger fee, BigInteger dust,
                                      ECKey key, String txid, BigInteger in_coin, int index,
                                      String message) throws Exception {

        return createTX(net, fee, dust, key, txid, in_coin, index, message).bitcoinSerialize();
    }

}


//    public static void main(String[] args) throws Exception{
//        String network = "testnet";
//        String min_tx_out = "0.00000546";
//        BulletinBuilder test = new BulletinBuilder(network, min_tx_out);
//
//
//        String      privkey     = "cSSnuU3up7Gzux7e8iL2utHQCx1GjLGnX5utXiB9Trk4CWtjDWd2";
//        String      txid        = "fb1cd4be27e553baafa00c5b40f59a77ba22d6d5fa2245d7a38b0ca412632ba1";
//        String      value       = "0.4993163";
//        long        index       =  8;
//        String      message     = "We make our world significant by the courage of our questions and the depth of our answers.";
//        String      fee         = "0.0001";
//
//        Transaction result = test.createTX(privkey, txid, value, index, message, fee);
//
//        System.out.println("-----------");
//        System.out.println(result.toString());
//        System.out.println("-----------");
//        System.out.println(bytesToHex(result.bitcoinSerialize()));
//
//
//    }



/*
    private void printArray(byte[] array){
        for(int i = 0; i < array.length; i++){
            System.out.print(array[i]+"|");
        }

    }

    private byte[] sliceArray(byte[] array, int begin, int end){
        byte[] result = new byte[end - begin];
        int c = 0;

        for(int i = begin; i < end; i++){
            result[c] = array[i];
            System.out.println(result[c]);
            c++;
        }

        return result;
    }

*/