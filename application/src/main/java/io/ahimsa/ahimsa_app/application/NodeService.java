package io.ahimsa.ahimsa_app.application;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.InetSocketAddress;

import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.core.Transaction;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;


public class NodeService extends Service {

    //Configuration------------------------------------------
    public static final boolean TEST = true;
    public static final String trustedPeerHost = ""; //"24.125.163.221"; //set to empty string for no trusted peer
    public static final int maxConnectedPeers = 6;

    //Constants----------------------------------------------
    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();

    //Intent Actions-----------------------------------------
    public static final String ACTION_BROADCAST_TRANSACTION = "broadcastTransaction";
    public static final String ACTION_TOAST_NUM_PEERS = "toastNumPeers";
    public static final String HEX_STRING_TRANSACTION = "hexstringTransaction";
    public static final String BYTE_ARRAY_TRANSACTION = "byteArrayTransaction";
//    public static final String ACTION_BROADCAST_TRANSACTION = NodeService.class.getPackage().getName() + ".broadcast_transaction";

    //NodeService--------------------------------------------
    private long serviceCreatedAt;
    private static final String TAG = "NodeService";

    private WakeLock wakeLock;
    private PeerGroup peerGroup;


    //-------------------------------------------------------
    //Service Overrides         TODO: onStartCommand-txbroadcast

    @Override
    public void onCreate(){
        serviceCreatedAt = System.currentTimeMillis();
        Log.d(TAG, "onCreate() at " + serviceCreatedAt);

        super.onCreate();

        //not entirely sure wakeLock necessary for pure transaction node
        final String lockName = getPackageName() + " peer connection";
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);

        //removed: application, config, or wallet
        //maybe:
        //peerConnectivityListener [BSI: 593]
        //sendBroadcastPeerState() [BSI: 595]

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId){
        Log.d(TAG, "service start command: " + intent);

        final String action = intent.getAction();
        if(ACTION_BROADCAST_TRANSACTION.equals(action)){
            final byte[] tx_byte_array = intent.getByteArrayExtra(BYTE_ARRAY_TRANSACTION);
            final Transaction tx = new Transaction(NETWORK_PARAMETERS, tx_byte_array);
            //****WORK TO BE DONE HERE****

            if (peerGroup != null){
                Log.d(TAG, "TX.GETHASHASSTRING(): " + tx.getHashAsString());
                Log.d(TAG, "TX_BYTE_ARRAY: " + tx_byte_array);
                peerGroup.broadcastTransaction(tx);
            }

        }

        else if(ACTION_TOAST_NUM_PEERS.equals(action)){
            toastNumberOfPeers();

        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, ".onDestroy");

        if (peerGroup != null){
            peerGroup.stopAndWait();
            Log.d(TAG, "peergroup stopped");
        }

        unregisterReceiver(connectivityReceiver);

        if(wakeLock.isHeld()){
            Log.d(TAG, "wakelock still held, releasing");
            wakeLock.release();
        }

        super.onDestroy();
        Log.d(TAG, "service was up for " + ((System.currentTimeMillis() - serviceCreatedAt) / 1000 / 60) + " minutes");
    }

    //Binder Overrides
    public class LocalBinder extends Binder{
        public NodeService getService() {
            return NodeService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(final Intent intent){
        Log.d(TAG, ".onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent){
        Log.d(TAG, ".onUnbind()");
        return super.onUnbind(intent);
    }

    //-------------------------------------------------------
    //Node Functions

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver(){
        private boolean hasConnectivity;

        @Override
        public void onReceive(final Context context, final Intent intent){
            final String action = intent.getAction();

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
                hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                Log.d(TAG, "network is " + (hasConnectivity ? "up" : "down"));

                execute();
            }
        }

        @SuppressLint("Wakelock")
        private void execute(){
            if (hasConnectivity && peerGroup == null){
                Log.d(TAG, "acquiring wakelock");
                wakeLock.acquire();

                Log.d(TAG, "starting peergroup");
                peerGroup = new PeerGroup(NETWORK_PARAMETERS);
                peerGroup.setUserAgent(USER_AGENT, VERSION);
                //to get peer count add peerConnectivityListener to peerGroup [BSI: 391]

                Log.d(TAG, "started peergroup");
                final boolean hasTrustedPeer = !trustedPeerHost.isEmpty();
                peerGroup.setMaxConnections(hasTrustedPeer ? 1 : maxConnectedPeers);

                peerGroup.addPeerDiscovery(new PeerDiscovery()
                {
                    private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(NETWORK_PARAMETERS);

                    @Override
                    public InetSocketAddress[] getPeers(final long timeoutValue, final TimeUnit timeoutUnit) throws PeerDiscoveryException
                    {
                        final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();

                        boolean needsTrimPeersWorkaround = false;

                        if (hasTrustedPeer)
                        {
                            Log.d(TAG, "trusted peer '" + trustedPeerHost + "'" + (hasTrustedPeer ? " only" : ""));

                            final InetSocketAddress addr = new InetSocketAddress(trustedPeerHost, NETWORK_PARAMETERS.getPort());
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

                peerGroup.start();
            }

            else if (!hasConnectivity && peerGroup != null){
                Log.d(TAG, "stopping peergroup");
                //remove peerConnectivityListener here
                peerGroup.stop();
                peerGroup = null;

                Log.d(TAG, "releasing wakelock");
                wakeLock.release();
            }
        }
    };

    //return list of connected peers
    private List<Peer> getConnectedPeers(){
        if (peerGroup != null)
            return peerGroup.getConnectedPeers();
        else
            return null;
    }

    //toast number of peers.
    private void toastNumberOfPeers(){
        int numPeers = getConnectedPeers().size();
        Toast.makeText(getApplicationContext(), "Connected Peers: " + numPeers,
                Toast.LENGTH_SHORT).show();
    }




}








