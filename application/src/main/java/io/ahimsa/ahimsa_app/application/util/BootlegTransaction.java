package io.ahimsa.ahimsa_app.application.util;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;

import java.lang.reflect.Field;
import java.util.ArrayList;

import io.ahimsa.ahimsa_app.application.Constants;

/**
 * Created by askuck on 6/12/14.
 */
public class BootlegTransaction extends Transaction {

    public BootlegTransaction(NetworkParameters params, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes);
    }

    public boolean modifyOutput(int index, TransactionOutput out){

        try {
            Field field = Transaction.class.getDeclaredField("outputs");
            field.setAccessible(true);
            ArrayList<TransactionOutput> copy = (ArrayList<TransactionOutput>) field.get(this);
            copy.remove(index);
            copy.add(out);
            field.set(this, copy);
            field.setAccessible(false);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public Transaction toTransaction(){
        return new Transaction(Constants.NETWORK_PARAMETERS, bitcoinSerialize());
    }


}
