package io.ahimsa.ahimsa_app.core;


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

import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;

import io.ahimsa.ahimsa_app.core.WireBulletinProtos.WireBulletin;


public class BulletinBuilder
{

    private static void addInputs(Transaction tx, List<TransactionOutput> unspents)
    {
        for(TransactionOutput out : unspents)
        {
            TransactionOutPoint outpoint = new TransactionOutPoint(Constants.NETWORK_PARAMETERS, indexOf(out), out.getParentTransaction());
            tx.addInput(new TransactionInput(Constants.NETWORK_PARAMETERS, tx, new byte[]{}, outpoint));
        }
    }

    private static int indexOf(TransactionOutput out)
    {
        Transaction parent = out.getParentTransaction();
        for(int i = 0; i < parent.getOutputs().size(); i++)
        {
            if(Arrays.equals(out.bitcoinSerialize(), parent.getOutput(i).bitcoinSerialize()))
            {
                return i;
            }
        }
        return -1;
    }


    private static void addBulletinOutputs(Transaction tx, String topic, String message)
    {
        // Verify topic + message length is valid
        if (topic.length() + message.length() > Constants.MAX_MESSAGE_LEN)
        {
            throw new Error("MESSAGE LENGTH OVER 500-0000000000000");
        }

        // Create protocol buffer builder and set values
        WireBulletin.Builder protobuilder = WireBulletin.newBuilder();
        protobuilder.setVersion(1);
        protobuilder.setTopic(topic);
        protobuilder.setMessage(message);

        // Create a byte array using the builder
        byte[] buffer_bytes = protobuilder.build().toByteArray();

        // Calculate new length of byte array (round up to next factor of char_per_out)
        int msg_len = Constants.AHIMSA_BULLETIN_PREFIX.length + buffer_bytes.length;
        int new_len = msg_len + Constants.CHAR_PER_OUT - (msg_len % Constants.CHAR_PER_OUT);

        Log.d("BB", "msg_len " + msg_len);
        Log.d("BB", "new_len " + new_len);

        // Create array with zeros of length new_len
        byte[] complete_bytes = new byte[new_len];
        Log.d("BB", Arrays.toString( complete_bytes ));

        // Copy ahimsa_bulletin_prefix into first eight bytes
        for(int i = 0; i < Constants.AHIMSA_BULLETIN_PREFIX.length; i++)
        {
            complete_bytes[i] = Constants.AHIMSA_BULLETIN_PREFIX[i];
        }
        Log.d("BB", Arrays.toString( complete_bytes ));

        // Copy buffer_bytes into array
        for(int i = 0; i < buffer_bytes.length; i++)
        {
            complete_bytes[i + Constants.AHIMSA_BULLETIN_PREFIX.length] = buffer_bytes[i];
        }
        Log.d("BB", Arrays.toString( complete_bytes ));

        // Encode 20 byte slices into output addresses, add output to transaction. Rinse and repeat.
        byte[] slice = new byte[Constants.CHAR_PER_OUT];
        for(int i = 0; i < complete_bytes.length; i++)
        {
            slice[i % Constants.CHAR_PER_OUT] = complete_bytes[i];
            if( (i+1) % Constants.CHAR_PER_OUT == 0 )
            {
                Address addr = new Address(Constants.NETWORK_PARAMETERS, slice);
                tx.addOutput(new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, BigInteger.valueOf(Constants.MIN_DUST), addr));
            }
        }
        Log.d("BB", tx.toString());

    }

    private static void addChangeOutput(Wallet keyStore, Transaction tx, List<TransactionOutput> unspents) throws Exception
    {
        BigInteger fee      = BigInteger.valueOf(Constants.MIN_FEE);
        BigInteger in_coin  = totalInCoin(unspents);
        BigInteger out_coin = totalOutCoin(tx);

        BigInteger total = BigInteger.ZERO.add(in_coin).subtract(out_coin).subtract(fee);

        String TAG = "BulletinBuilder";
        Log.d(TAG, "fee |" + fee.toString());
        Log.d(TAG, "in_coin |" + in_coin.toString());
        Log.d(TAG, "out_coin |" + out_coin.toString());
        Log.d(TAG, "total |" + total.toString());


        switch ( total.compareTo(BigInteger.ZERO) )
        {
            case  0:
            case  1:    break;
            case -1:    Log.d(TAG, Utils.bytesToHex(tx.bitcoinSerialize()) );
                throw new Exception("out_coin + fee exceeds in_coin | " + total.toString());
        }

        BigInteger min = BigInteger.valueOf(Constants.getMinCoinNecessary());
//        Address default_addr = new Address(Constants.NETWORK_PARAMETERS, default_addr);
        Address default_addr = keyStore.getChangeAddress();
        while(total.compareTo(BigInteger.ZERO) == 1)
        {
            if(total.compareTo(min) > 0)
            {
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, min, default_addr) );
                total = total.subtract(min);
            }
            else
            {
                tx.addOutput( new TransactionOutput(Constants.NETWORK_PARAMETERS, tx, total, default_addr) );
                total = total.subtract(total);
            }
        }
    }

    private static BigInteger totalInCoin(List<TransactionOutput> db_unspent)
    {
        BigInteger in_coin = BigInteger.ZERO;
        for(TransactionOutput out : db_unspent)
        {
            in_coin = in_coin.add(out.getValue());
        }
        return in_coin;
    }

    private static BigInteger totalOutCoin(Transaction tx)
    {
        BigInteger out_coin = BigInteger.ZERO;
        for(TransactionOutput out : tx.getOutputs())
        {
            out_coin = out_coin.add(out.getValue());
        }
        return out_coin;
    }

    //----------------------------------------------------------------------------------------------
    public static Transaction createTx(Wallet keyStore, List<TransactionOutput> unspents, String topic, String message) throws Exception
    {
        //create new transaction
        Transaction bulletin = new Transaction(Constants.NETWORK_PARAMETERS);

        //add inputs and message outputs
        addInputs(bulletin, unspents);
        addBulletinOutputs(bulletin, topic, message);

        //add change output to transaction
        addChangeOutput(keyStore, bulletin, unspents);

        //sign the inputs
        bulletin.signInputs(SigHash.ALL, keyStore);
        return bulletin;
    }





}
