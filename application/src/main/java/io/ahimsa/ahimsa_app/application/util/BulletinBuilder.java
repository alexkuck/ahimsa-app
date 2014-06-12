package io.ahimsa.ahimsa_app.application.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.Wallet;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction.SigHash;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;


public class BulletinBuilder {

    public static NetworkParameters NETWORK_PARAMETERS;
    public static BigInteger MIN_TX_OUT;

    public BulletinBuilder(String _network, String min_tx_out){
        if (_network == "mainnet"){
            NETWORK_PARAMETERS = MainNetParams.get();
        } else if (_network == "testnet"){
            NETWORK_PARAMETERS = TestNet3Params.get();
        } else {
            throw new Error("Invalid network parameter");
        }

        MIN_TX_OUT = Utils.toNanoCoins(min_tx_out);        
    }

    public static ECKey importKey(String privkey) throws Exception{
        //only accepts a very specific type of address. from dumpprivkey functionality in Bitcoin core.
        DumpedPrivateKey dump = new DumpedPrivateKey(NETWORK_PARAMETERS, privkey);
        return dump.getKey();
        // ECKey key = dump.getKey();
        // return key.toAddress(NETWORK_PARAMETERS);
        // return key.getPubKey();

    }

    public static TransactionInput formulateInput(String txid, ECKey key, String value, long index){

        //new transaction with hash param
        //.addoutput with correct address, value, **index**
        //add this transaction to the transaction outpoint
        //add the transactionOutPoint to the transaction input


        Sha256Hash hash = new Sha256Hash(txid);
        Transaction previous = new Transaction(NETWORK_PARAMETERS, 1, hash){
            @Override
            protected void unCache() {
                //do nothing
            }
        };
        
        for(int i=0; i < 1+index; i++){
            previous.addOutput(Utils.toNanoCoins(value), key.toAddress(NETWORK_PARAMETERS));
        }

        TransactionOutPoint outpoint = new TransactionOutPoint(NETWORK_PARAMETERS, index, previous);
        return new TransactionInput(NETWORK_PARAMETERS, null, new byte[] {}, outpoint);

    }

    public static Address encodeAddress(String slice) {

        if(slice.length() != 20){
            for(int w = slice.length(); w < 20; w++){
                slice += "_";
            }
        }

        // byte[] arr = slice.getBytes();
        // for(int i = 0; i < arr.length; i++){
        //     System.out.print(arr[i] + "|");    
        // } System.out.println();
        
        return new Address(NETWORK_PARAMETERS, slice.getBytes());
    }


    public static ArrayList<TransactionOutput> formulateMessageOutput(String message){

        if (message.length() > 140){
            throw new Error("MESSAGE LENGTH OVER 140-0000000000000");
        }

        ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

        String slice = "";
        for(int i = 0; i < message.length(); i++){
            slice += message.charAt(i);
            if(slice.length()==20){
                outputs.add( new TransactionOutput(NETWORK_PARAMETERS, null, MIN_TX_OUT, encodeAddress(slice)) );
                slice = "";
            }
        }
        outputs.add( new TransactionOutput(NETWORK_PARAMETERS, null, MIN_TX_OUT, encodeAddress(slice)) );

        return outputs;
    }

    public static TransactionOutput formulateChangeOutput(ECKey key, String value, List<TransactionOutput> outList, String fee){

        BigInteger total = new BigInteger("0").subtract( Utils.toNanoCoins(fee) ).add( Utils.toNanoCoins(value) );
        for(TransactionOutput out : outList){
            total = total.subtract(out.getValue());
        }
        return new TransactionOutput(NETWORK_PARAMETERS, null, total, key.toAddress(NETWORK_PARAMETERS));


    }


    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public Transaction createTX(String privkey, String txid, String value, long index, String message, String fee) throws Exception{

        //set up variables
        ECKey                        key        = importKey(privkey);
        TransactionInput             input      = formulateInput(txid, key, value, index);
        ArrayList<TransactionOutput> msgOutputs = formulateMessageOutput(message);
        Wallet                       wallet     = new Wallet(NETWORK_PARAMETERS);

        //add key to wallet
        wallet.addKey(key);
        
        //create new transaction
        Transaction result = new Transaction(NETWORK_PARAMETERS);

        //add inputs to transaction
        result.addInput(input);

        //add message outputs to transaction
        for(TransactionOutput out : msgOutputs){
            result.addOutput(out);
        }

        //add hashtags outputs to transaction

        //add useragent outputs to transaction

        //add change output to transaction
        result.addOutput( formulateChangeOutput(key, value, result.getOutputs(), fee) );


        //sign transaction
        result.signInputs(SigHash.ALL, wallet);

        return result;
    }

    public String createRawTX(String privkey, String txid, String value, long index, String message, String fee) throws Exception{
        return bytesToHex( createTX(privkey, txid, value, index, message, fee).bitcoinSerialize() );
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
}


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


    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

*/