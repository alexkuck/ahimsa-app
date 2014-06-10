package io.ahimsa.ahimsa_app.application.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
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

import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;

public class NodeService extends Service {
    private static final String TAG = NodeService.class.toString();

    //NodeService | PeerGroup------------------------------------------------------
    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;

    //NodeService | Actions-------------------------------------------------------
    //Test Peercount
    private static final String ACTION_TEST_PEERCOUNT = NodeService.class.getPackage().getName() + ".test_peercount";

    //Fund Empty Wallet
    private static final String ACTION_FUND_WALLET = NodeService.class.getPackage().getName() + ".empty_wallet";

    //Broadcast Raw Transaction
    private static final String ACTION_BROADCAST_TRANSACTION = NodeService.class.getPackage().getName() + ".broadcast_transaction";
    private static final String EXTRA_RAW_TX = NodeService.class.getPackage().getName() + ".raw_tx";


    //Eventually will be placed within Configuration file
    public static final int maxConnectedPeers = 6;
    public static final int minConnectedPeers = 6;
    public static final String trustedPeerHost = ""; //"24.125.163.221"; //set to empty string for no trusted peer

    //----------------------------------------------------------------------------
    //IntentService---------------------------------------------------------------

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;
    private boolean mRedelivery;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            stopSelf(msg.arg1);
        }
    }

    public NodeService() {
        super();
        mName = "NodeService";
    }

    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "NodeService: onCreate");

        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "deprecated onStart function");
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "NodeService: onDestroy");

        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //--------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------
    public void startPeerGroup()
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
                peerGroup.waitForPeers(minConnectedPeers).get();

            }catch(Exception e){
                e.printStackTrace();
            }
        }


    }

    public void stopPeerGroup()
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

    public class ConnectionDetector
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
                    for (int i = 0; i < info.length; i++)
                        if (info[i].getState() == NetworkInfo.State.CONNECTED){
                            return true;
                        }

            }
            return false;
        }
    }
    //--------------------------------------------------------------------------------


    //Public start action methods
    public static void startActionTestPeercount(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_TEST_PEERCOUNT);
        context.startService(intent);
    }

    public static void startActionFundWallet(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_FUND_WALLET);
        context.startService(intent);
    }



    public static void startActionBroadcast(@Nonnull Context context, @Nonnull byte[] rawTx)
    {
        Intent intent = new Intent(context, BootlegNodeService.class);
        intent.setAction(ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(EXTRA_RAW_TX, rawTx);
        context.startService(intent);
    }

    //Handle intents
    protected void onHandleIntent(Intent intent){
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "NodeService: onHandleIntent | " + action);

            if (ACTION_TEST_PEERCOUNT.equals(action))
            {
                try {
                    handleTestPeerCount();
                }catch (PeerGroupException e){
                    //TODO: Alert application / user of failure
                    Log.d(TAG, "Exception at handleTestPeercount: " + e.toString());
                }

            }

            else if(ACTION_FUND_WALLET.equals(action))
            {
                handleFundWallet();
            }

            else if (ACTION_BROADCAST_TRANSACTION.equals(action))
            {
                final byte[] rawTx = intent.getByteArrayExtra(EXTRA_RAW_TX);
                handleActionBroadcast(rawTx);
            }
        }
    }

    //Work for each intent
    private void handleTestPeerCount() throws PeerGroupException
    {
        startPeerGroup();

        if(peerGroup == null)
        {
            throw new PeerGroupException("handleTestPeerCount in " + getClass().toString());
        }

        Log.d(TAG, "handleTestPeerCount: " + getConnectedPeers().size());

        stopPeerGroup();
    }

    private void handleFundWallet()
    {
        MainApplication application = (MainApplication) getApplication();
        Wallet wallet = application.getWallet();

        Log.d(TAG, "Wallet is empty, proceeding with handleFundEmptyWallet in " + getClass().toString());

        Log.d(TAG, "in handleFundEmptyWallet keys: " + wallet.getKeys().get(0).toString());

        application.saveWallet();


        //check if wallet is actually empty, throw exception if not
        //post request new funded tx via http, possibly as own thread
        //get wallet from application
        //generate and add new key to wallet
        //add key's pub address to tx
        //start peergroup
        //broadcast tx, listen for confirmation. implement as utility method.
            //must implement error handling, throws if timeout or unsuccessful
            //informs application, via utility method
        //stop peergroup
        //ensure wallet has recorded this transaction
        //application.save wallet.
        //broadcast intent to application: hey assholes, this address has some coin.

        Log.d(TAG, "funding didn't occur? maybe you should WRITE THE CODE");
    }


    private void handleActionBroadcast(byte[] rawTx)
    {
        startPeerGroup();

        final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, rawTx);
        Log.d(TAG, "Broadcasting Raw TX: " + tx.getHashAsString());
//        peerGroup.broadcastTransaction(tx, 4).get();

        stopPeerGroup();
    }



    //Utility-------------------------------------------------------------------
    public class PeerGroupException extends Exception
    {
        public PeerGroupException() { super(); }
        public PeerGroupException(String message) { super(message); }
        public PeerGroupException(String message, Throwable cause) { super(message, cause); }
        public PeerGroupException(Throwable cause) { super(cause); }
    }

    public class HasKeyRuntimeException extends RuntimeException
    {
        public HasKeyRuntimeException() { super(); }
        public HasKeyRuntimeException(String message) { super(message); }
        public HasKeyRuntimeException(String message, Throwable cause) { super(message, cause); }
        public HasKeyRuntimeException(Throwable cause) { super(cause); }
    }

    private List<Peer> getConnectedPeers()
    {
        if (peerGroup != null)
            return peerGroup.getConnectedPeers();
        else
            return null;
    }


}
