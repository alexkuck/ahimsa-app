package io.ahimsa.ahimsa_app.application.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;

import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetSocketAddress;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;

import io.ahimsa.ahimsa_app.application.util.BootlegTransaction;
import io.ahimsa.ahimsa_app.application.util.Utils;

/**
 * Created by askuck on 6/9/14.
 */

public class NodeService extends IntentService {

    private static final String TAG = "NodeService";

    //NodeService | PeerGroup------------------------------------------------------
    private MainApplication application;
    private Configuration config;

    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;

    private int maxConnectedPeers;
    private int minConnectedPeers;
    private String trustedPeerHost;


    //NodeService | Actions-------------------------------------------------------
    //Broadcast Raw Message
    private static final String ACTION_BROADCAST_TX      = NodeService.class.getPackage().getName() + ".broadcast_transaction";
    private static final String EXTRA_TX                 = NodeService.class.getPackage().getName() + ".transaction";

    //Sync Blockchain
    private static final String ACTION_SYNC_BLOCKCHAIN   = NodeService.class.getPackage().getName() + ".sync_blockchain";

    //Network Test
    private static final String ACTION_NETWORK_TEST      = NodeService.class.getPackage().getName() + ".network_test";

    //----------------------------------------------------------------------------
    //IntentService---------------------------------------------------------------

    public NodeService() {
        super("NodeService");
    }

    //--------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------

    private void initiate()
    {
        application = (MainApplication) getApplication();
        config = application.getConfig();

        minConnectedPeers = config.getMinConnectedPeers();
        maxConnectedPeers = config.getMinConnectedPeers();
        trustedPeerHost   = config.getTrustedPeer();

    }

    private void startPeerGroup()
    {

        final String lockName = getPackageName() + " peer connection";
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);

        ConnectionDetector cd = new ConnectionDetector(getApplicationContext());

        if(cd.isConnectingToInternet())
        {
            try{
                Log.d(TAG, "acquiring wakelock");
                wakeLock.acquire();

                Log.d(TAG, "starting peergroup");
                peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS);
                peerGroup.setUserAgent(Constants.USER_AGENT, Constants.VERSION);

                Log.d(TAG, "started peergroup");
                final boolean hasTrustedPeer = !trustedPeerHost.isEmpty();
                peerGroup.setMaxConnections(hasTrustedPeer ? 1 : maxConnectedPeers);

                peerGroup.addPeerDiscovery(new PeerDiscovery()
                {
                    private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(Constants.NETWORK_PARAMETERS);

                    @Override
                    public InetSocketAddress[] getPeers(final long timeoutValue, final TimeUnit timeoutUnit) throws PeerDiscoveryException
                    {
                        final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();

                        boolean needsTrimPeersWorkaround = false;

                        if (hasTrustedPeer)
                        {
                            Log.d(TAG, "trusted peer '" + trustedPeerHost + "'" + (hasTrustedPeer ? " only" : ""));

                            final InetSocketAddress addr = new InetSocketAddress(trustedPeerHost, Constants.NETWORK_PARAMETERS.getPort());
                            if (addr.getAddress() != null)
                            {
                                peers.add(addr);
                                needsTrimPeersWorkaround = true;
                            }
                        }

                        if (!hasTrustedPeer)
                            peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(timeoutValue, timeoutUnit)));

                        // workaround because PeerGroup will shuffle peers
                        if (needsTrimPeersWorkaround)
                            while (peers.size() >= maxConnectedPeers)
                                peers.remove(peers.size() - 1);

                        return peers.toArray(new InetSocketAddress[0]);
                    }

                    @Override
                    public void shutdown()
                    {
                        normalPeerDiscovery.shutdown();
                    }
                });

                peerGroup.startAndWait();
                //TODO: let application know timeout occurred
                peerGroup.waitForPeers(minConnectedPeers).get(config.getMinTimeout(), TimeUnit.SECONDS);

            }catch(Exception e){
                e.printStackTrace();
            }
        }


    }

    private void stopPeerGroup()
    {
        if (peerGroup != null)
        {
            peerGroup.stopAndWait();
            Log.d(TAG, "peergroup stopped");
        }

        if(wakeLock.isHeld())
        {
            Log.d(TAG, "wakelock still held, releasing");
            wakeLock.release();
        }
    }

    private void broadcastTx(byte[] tx)
    {
        Log.d(TAG, "Broadcasting Transaction: " + Utils.bytesToHex(tx)  );

        ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(new Transaction(Constants.NETWORK_PARAMETERS, tx), minConnectedPeers);

        try {
            future.get(config.getMinTimeout(), TimeUnit.SECONDS);
            Log.d(TAG, "Future succes, txid: " + future.get().toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    private class ConnectionDetector
    {
        private Context _context;

        public ConnectionDetector(Context context
        ){
            this._context = context;
        }

        public boolean isConnectingToInternet()
        {
            ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null)
            {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null)
                    for (NetworkInfo anInfo : info)
                        if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                            return true;
                        }

            }
            return false;
        }
    }

    //--------------------------------------------------------------------------------
    //Public start action methods
    public static void startNetworkTest(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_NETWORK_TEST);
        context.startService(intent);
    }

    public static void startActionBroadcastTx(@Nonnull Context context, byte[] tx)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_BROADCAST_TX);
        intent.putExtra(EXTRA_TX, tx);
        context.startService(intent);
    }
    //--------------------------------------------------------------------------------
    //Handle intents
    protected void onHandleIntent(Intent intent){
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "onHandleIntent | " + action);

            if (ACTION_BROADCAST_TX.equals(action))
            {
                final byte[] tx = intent.getByteArrayExtra(EXTRA_TX);
                handleActionBroadcastTx(tx);
            }

            else if(ACTION_SYNC_BLOCKCHAIN.equals(action))
            {
                handleSyncBlockchain();
            }

            else if (ACTION_NETWORK_TEST.equals(action))
            {
                handleNetworkTest();
            }

        }
    }
    //--------------------------------------------------------------------------------
    private void handleActionBroadcastTx(byte[] tx)
    {
        initiate();
        startPeerGroup();
        broadcastTx(tx);
        stopPeerGroup();
    }


    private void handleSyncBlockchain()
    {

    }

    private void handleNetworkTest()
    {


    }



    //Utility-------------------------------------------------------------------
    private List<Peer> getConnectedPeers()
    {
        if (peerGroup != null)
            return peerGroup.getConnectedPeers();
        else
            return null;
    }




}
