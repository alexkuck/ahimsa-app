package io.ahimsa.ahimsa_app.application.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import javax.annotation.Nonnull;


public class NodeService extends IntentService {

    //Broadcast Raw Transaction------------------------------------------
    private static final String ACTION_BROADCAST_TRANSACTION = "io.ahimsa.ahimsa_app.application.service.action.BROADCAST_TX";
    private static final String EXTRA_RAW_TX = "io.ahimsa.ahimsa_app.application.service.extra.RAW_TX";

    //Update Block Headers-----------------------------------------------
    private static final String ACTION_BAZ = "io.ahimsa.ahimsa_app.application.service.action.BAZ";
    private static final String EXTRA_PARAM2 = "io.ahimsa.ahimsa_app.application.service.extra.PARAM2";


    //Node Service-------------------------------------------------------
    private static final String TAG = "NodeService";

        //this stuff should be handled by configuration file
    public static final boolean TEST = true;
    public static final int maxConnectedPeers = 6;
    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();

    //Configuration------------------------------------------
    public static final String trustedPeerHost = ""; //"24.125.163.221"; //set to empty string for no trusted peer



    //startActionBroadcast(rawTX)
    //startActionBroadcast(message, topic, key)
    //startActionSyncBlockchain
    //startActionAutoFund
    //startActionImportFund


    //broadcast raw transaction to peer group
    public static void startActionBroadcast(@Nonnull Context context, @Nonnull byte[] rawTx) {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(EXTRA_RAW_TX, rawTx);
        context.startService(intent);
    }

    public NodeService() {
        super("NodeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_BROADCAST_TRANSACTION.equals(action)) {
                final byte[] rawTx = intent.getByteArrayExtra(EXTRA_RAW_TX);
                handleActionBroadcastTx(rawTx);

            }
        }
    }


    private void handleActionBroadcastTx(byte[] rawTx) {
        final Transaction tx = new Transaction(NETWORK_PARAMETERS, rawTx);
        Log.d(TAG, "Broadcasting Raw TX: " + tx.getHashAsString());



    }




}
