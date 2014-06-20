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


public class BulletinBuilderGeneric {

    public static TransactionInput formulateInput(NetworkParameters net, ECKey key, String txid,
                                                  BigInteger in_coin, int vout) {

        //new transaction with hash param
        //.addoutput with correct address, value, **vout**
        //add this transaction to the transaction outpoint
        //add the transactionOutPoint to the transaction input


        Sha256Hash hash = new Sha256Hash(txid);
        Transaction previous = new Transaction(net, 1, hash) {
            @Override
            protected void unCache() {
                //do nothing
            }
        };

        for (int i = 0; i < 1 + vout; i++) {
            previous.addOutput(in_coin, key.toAddress(net));
        }

        TransactionOutPoint outpoint = new TransactionOutPoint(net, vout, previous);
        return new TransactionInput(net, null, new byte[]{}, outpoint);

    }

//    public static TransactionInput formulateInput(NetworkParameters net, List<Utils.outpointTx> ){
//
//        TransactionOutPoint outpoint = new TransactionOutPoint(net, vout, previous);
//        return new TransactionInput(net, null, new byte[]{}, outpoint);
//    }



    public static Address encodeAddress(NetworkParameters net, String slice) {

        if (slice.length() != 20) {
            for (int w = slice.length(); w < 20; w++) {
                slice += "_";
            }
        }
        return new Address(net, slice.getBytes());
    }


    public static ArrayList<TransactionOutput> formulateMessageOutput(NetworkParameters net,
                                                                      BigInteger dust,
                                                                      String message) {

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

    public static TransactionOutput formulateChangeOutput(NetworkParameters net,
                                                          ECKey key,
                                                          BigInteger in_coin,
                                                          List<TransactionOutput> outList,
                                                          BigInteger fee) {

        BigInteger total = new BigInteger("0").subtract(fee).add(in_coin);
        for (TransactionOutput out : outList) {
            total = total.subtract(out.getValue());
        }
        return new TransactionOutput(net, null, total, key.toAddress(net));


    }

    public static void checkValidity(Transaction tx, BigInteger in_coin) throws Exception{

        BigInteger out_coin = BigInteger.ZERO;
        for(TransactionOutput out : tx.getOutputs()){
            out_coin = out_coin.add( out.getValue() );
        }

        switch ( in_coin.compareTo(out_coin) ){
            case  0:
            case  1:    break;
            case -1:    throw new Exception("out_coin exceeds in_coin");
        }

    }


    //--------------------------------------------------------------------------------
    public static Transaction createTx(NetworkParameters net,
                                       BigInteger fee,
                                       BigInteger dust,
                                       ECKey key,
                                       String txid,
                                       BigInteger in_coin,
                                       int vout,
                                       String message) throws Exception {
        //set up inputs and outputs
        TransactionInput input = formulateInput(net, key, txid, in_coin, vout);
        ArrayList<TransactionOutput> msgOutputs = formulateMessageOutput(net, dust, message);

        //add key to wallet
        Wallet wallet = new Wallet(net);
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

        //check
        checkValidity(result, in_coin);


        //sign transaction
        result.signInputs(SigHash.ALL, wallet);

        return result;
    }

    public static byte[] createByteTx(NetworkParameters net,
                                      BigInteger fee,
                                      BigInteger dust,
                                      ECKey key,
                                      String txid,
                                      BigInteger in_coin,
                                      int vout,
                                      String message) throws Exception {

        return createTx(net, fee, dust, key, txid, in_coin, vout, message).bitcoinSerialize();
    }
//    --------------------------------------------------------------------------------
//    public static Transaction createTx(NetworkParameters net,
//                                       BigInteger fee,
//                                       BigInteger dust,
//                                       Transaction parent,
//                                       Wallet wallet,
//                                       String message){
//
//
//    }
//
//    public static byte[] createByteTx(NetworkParameters net,
//                                      BigInteger fee,
//                                      BigInteger dust,
//                                      Transaction parent,
//                                      Wallet wallet,
//                                      String message){
//
//        return createTx(net, fee, dust, parent, wallet, message).bitcoinSerialize();
//    }

}


//    public static void main(String[] args) throws Exception{
//        String network = "testnet";
//        String min_tx_out = "0.00000546";
//        BulletinBuilderGeneric test = new BulletinBuilderGeneric(network, min_tx_out);
//
//
//        String      privkey     = "cSSnuU3up7Gzux7e8iL2utHQCx1GjLGnX5utXiB9Trk4CWtjDWd2";
//        String      txid        = "fb1cd4be27e553baafa00c5b40f59a77ba22d6d5fa2245d7a38b0ca412632ba1";
//        String      value       = "0.4993163";
//        long        vout       =  8;
//        String      message     = "We make our world significant by the courage of our questions and the depth of our answers.";
//        String      fee         = "0.0001";
//
//        Transaction result = test.createTX(privkey, txid, value, vout, message, fee);
//
//        System.out.println("-----------");
//        System.out.println(result.toString());
//        System.out.println("-----------");
//        System.out.println(bytesToHex(result.bitcoinSerialize()));
//
//
//    }
