package io.ahimsa.ahimsa_app.application.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


public class BootlegNodeService extends IntentService {

    //Test Peercount-----------------------------------------------------
    private static final String ACTION_TEST_PEERCOUNT = "io.ahimsa.ahimsa_app.application.service.action.TEST_PEERCOUNT";

    //Broadcast Raw Transaction------------------------------------------
    private static final String ACTION_BROADCAST_TRANSACTION = "io.ahimsa.ahimsa_app.application.service.action.BROADCAST_TX";
    private static final String EXTRA_RAW_TX = "io.ahimsa.ahimsa_app.application.service.extra.RAW_TX";

    //Update Block Headers-----------------------------------------------
    private static final String ACTION_BAZ = "io.ahimsa.ahimsa_app.application.service.action.BAZ";
    private static final String EXTRA_PARAM2 = "io.ahimsa.ahimsa_app.application.service.extra.PARAM2";


    //Node Service-------------------------------------------------------
    private static final String TAG = "BootlegNodeService";

    private PeerConnectivityListener peerConnectivityListener;


    //this stuff should be handled by configuration file
    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";

    public static final boolean TEST = true;
    public static final int maxConnectedPeers = 6;
    public static final int minConnectedPeers = 6;
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
    public static final String trustedPeerHost = ""; //"24.125.163.221"; //set to empty string for no trusted peer

    //------------------------------------------------------------------
    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;


    //startActionBroadcast(rawTX)
    //startActionBroadcast(message, topic, key)
    //startActionSyncBlockchain
    //startActionAutoFund
    //startActionImportFund



    public void onCreate(){
        Log.d(TAG, "BootlegNodeService onCreate()");

        final String lockName = getPackageName() + " peer connection";
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);

        ConnectionDetector cd = new ConnectionDetector(getApplicationContext());

        if(cd.isConnectingToInternet()){
            try{
                startPeergroup();
            }catch(Exception e){
                e.printStackTrace();
            }
        }







//        try {
//            peerGroup.waitForPeers(6).get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }



    }


    public class ConnectionDetector {
        private Context _context;

        public ConnectionDetector(Context context){
            this._context = context;
        }

        public boolean isConnectingToInternet(){
            ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null){
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null)
                    for (int i = 0; i < info.length; i++)
                        if (info[i].getState() == NetworkInfo.State.CONNECTED){
                            return true;
                        }

            }
            return false;
        }
    }


    private boolean startPeergroup(){

        Log.d(TAG, "acquiring wakelock");
        wakeLock.acquire();

        Log.d(TAG, "starting peergroup");
        peerGroup = new PeerGroup(NETWORK_PARAMETERS);
        peerGroup.setUserAgent(USER_AGENT, VERSION);

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


        peerGroup.startAndWait();
        return true;



    }


    @Override
    public void onDestroy(){
        Log.d(TAG, "BootlegNodeService onDestroy()");

        if (peerGroup != null){
            peerGroup.stopAndWait();
            Log.d(TAG, "peergroup stopped");
        }

//        unregisterReceiver(connectivityReceiver);

        if(wakeLock.isHeld()){
            Log.d(TAG, "wakelock still held, releasing");
            wakeLock.release();
        }

        super.onDestroy();


    }

    public static void startActionTestPeercount(@Nonnull Context context) {
        Intent intent = new Intent(context, BootlegNodeService.class);
        intent.setAction(ACTION_TEST_PEERCOUNT);
        context.startService(intent);
    }


    //broadcast raw transaction to peer group
    public static void startActionBroadcast(@Nonnull Context context, @Nonnull byte[] rawTx) {
        Intent intent = new Intent(context, BootlegNodeService.class);
        intent.setAction(ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(EXTRA_RAW_TX, rawTx);
        context.startService(intent);
    }

    public BootlegNodeService() {
        super("BootlegNodeService");
    }

    //----------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_TEST_PEERCOUNT.equals(action)){
                handleTestPeercount();

            }else if (ACTION_BROADCAST_TRANSACTION.equals(action)) {
                final byte[] rawTx = intent.getByteArrayExtra(EXTRA_RAW_TX);
                handleActionBroadcastTx(rawTx);

            }
        }
    }

    private void handleTestPeercount(){

        int numPeers = getConnectedPeers().size();
        Toast.makeText(getApplicationContext(), "Connected Peers: " + numPeers,
                Toast.LENGTH_SHORT).show();



    }



    private void handleActionBroadcastTx(byte[] rawTx) {
        final Transaction tx = new Transaction(NETWORK_PARAMETERS, rawTx);
        Log.d(TAG, "Broadcasting Raw TX: " + tx.getHashAsString());



    }

    //----------------------------------------------------------------------
    private final class PeerConnectivityListener extends AbstractPeerEventListener{
        private int peerCount;

        public PeerConnectivityListener(){
        }

        @Override
        public void onPeerConnected(final Peer peer, final int peerCount){
            Log.d(TAG, "on peerconnected: " + peerCount);
            this.peerCount = peerCount;
            changed(peerCount);
        }

        @Override
        public void onPeerDisconnected(final Peer peer, final int peerCount){
            this.peerCount = peerCount;
            changed(peerCount);
        }

        private void changed(int peerCount){
            if(peerCount == minConnectedPeers){

                Log.d(TAG, "peercount == minconnectedpeers");



            }

        }



    }

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
                peerGroup.addEventListener(peerConnectivityListener);


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
                peerGroup.removeEventListener(peerConnectivityListener);
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



}
