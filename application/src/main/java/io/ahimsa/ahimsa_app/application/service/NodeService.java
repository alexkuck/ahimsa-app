package io.ahimsa.ahimsa_app.application.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.common.util.concurrent.ListenableFuture;


import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URL;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;

import io.ahimsa.ahimsa_app.application.util.BootlegTransaction;
import io.ahimsa.ahimsa_app.application.util.BulletinBuilder;

/**
 * Created by askuck on 6/9/14.
 */

public class NodeService extends Service {

    private static final String TAG = "NodeService";

    //NodeService | PeerGroup------------------------------------------------------
    private MainApplication application;
    private Configuration config;
    private Wallet wallet;

    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;

    private int maxConnectedPeers;
    private int minConnectedPeers;

    //TODO: implement trusted peer in configuration
    private String trustedPeerHost = ""; //"24.125.163.221"; //set to empty string for no trusted peer


    //NodeService | Actions-------------------------------------------------------
    //Test Peercount
    private static final String ACTION_NETWORK_TEST        = NodeService.class.getPackage().getName() + ".network_test";

    //Sync Blockchain
    private static final String ACTION_SYNC_BLOCKCHAIN     = NodeService.class.getPackage().getName() + ".sync_blockchain";

    //Fund Fresh Wallet
    private static final String ACTION_FUND_FRESH_WALLET   = NodeService.class.getPackage().getName() + ".fresh_wallet";

    //Broadcast Raw Message
    private static final String ACTION_BROADCAST_BULLETIN  = NodeService.class.getPackage().getName() + ".broadcast_bulletin";
    private static final String EXTRA_BULLETIN             = NodeService.class.getPackage().getName() + ".bulletin";
    private static final String EXTRA_TOPIC                = NodeService.class.getPackage().getName() + ".topic";


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
        Log.d(TAG, "onCreate");

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
        Log.d(TAG, "onDestroy");

        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //--------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------

    private void initiate()
    {
        application = (MainApplication) getApplication();
        config = application.getConfig();
        wallet = application.getWallet();

        minConnectedPeers = config.getMinConnectedPeers();
        maxConnectedPeers = config.getMinConnectedPeers();

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
                peerGroup.waitForPeers(minConnectedPeers).get(5, TimeUnit.SECONDS);

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

    private void broadcastTx(Transaction tx)
    {
        Log.d(TAG, "Broadcasting Transaction: " + bytesToHex( tx.bitcoinSerialize() )  );

        ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(tx, minConnectedPeers);

        try {
            future.get(10, TimeUnit.SECONDS);
            Log.d(TAG, "Future succes, txid: " + future.get().toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            //TODO: let application know timeout occurred
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

    //todo: follows stict line of success, handle all cases
    private String httpsConnection(String path, Map params) throws Exception
    {

        //From the example at: http://developer.android.com/training/articles/security-ssl.html
        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = getResources().openRawResource(R.raw.cert);
        Certificate ca;
        try{
            ca = cf.generateCertificate(caInput);
            Log.d(TAG, "0/8|Certificate: " + ((X509Certificate) ca).getSubjectDN());
        }finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        URL url = new URL(path);
        JSONObject holder = new JSONObject(params);
        String message = holder.toString();

        try{
            Log.d(TAG, "1/8|Open connection...");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            Log.d(TAG, "2/8|Set connection parameters...");
            conn.setSSLSocketFactory(context.getSocketFactory());
            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(message.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/json");

            Log.d(TAG, "3/8|Connect...");
            conn.connect();

            Log.d(TAG, "4/8|Declare and write to output stream...");
            OutputStream outStream = new BufferedOutputStream(conn.getOutputStream());
            outStream.write(message.getBytes());

            Log.d(TAG, "5/8|Flush output stream...");
            outStream.flush();

            Log.d(TAG, "6/8|Read input stream...");
            //do something with response
            InputStream inStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String result = reader.readLine();

            Log.d(TAG, "7/8|Close outstream, instream, and conn...");
            outStream.close();
            inStream.close();
            conn.disconnect();

            Log.d(TAG, "8/8|Success, returning result.");
            return result;



        } catch (Exception e){
            Log.d(TAG, "Exception in httpsConnection: " + e.toString());
        }

        Log.d(TAG, "returned null");
        return null;

    }

    //todo: follows stict line of success, handle all cases
    private void successOnHttps(String fundexTx, Long inValue)
    {
        BootlegTransaction bootlegTx = new BootlegTransaction(Constants.NETWORK_PARAMETERS, hexStringToByteArray(fundexTx));

        ECKey defKey = application.getDefaultECKey();
        BigInteger toSelf = satoshiToSelf(bootlegTx, inValue);

        TransactionOutput tout = new TransactionOutput(Constants.NETWORK_PARAMETERS, bootlegTx, toSelf, defKey.toAddress(Constants.NETWORK_PARAMETERS));
        bootlegTx.modifyOutput(1, tout);

        startPeerGroup();
        broadcastTx(bootlegTx.toTransaction());
        stopPeerGroup();

        wallet.commitTx(bootlegTx);
        application.saveWallet();

    }


    //--------------------------------------------------------------------------------
    //Public start action methods
    public static void startNetworkTest(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_NETWORK_TEST);
        context.startService(intent);
    }

    public static void startActionFundFreshWallet(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_FUND_FRESH_WALLET);
        context.startService(intent);
    }

    public static void startActionBroadcastBulletin(@Nonnull Context context, @Nonnull String bulletin, @Nonnull String topic)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_BROADCAST_BULLETIN);
        intent.putExtra(EXTRA_BULLETIN, bulletin);
        intent.putExtra(EXTRA_TOPIC, topic);
        context.startService(intent);
    }
    //--------------------------------------------------------------------------------
    //Handle intents
    protected void onHandleIntent(Intent intent){
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "onHandleIntent | " + action);

            if (ACTION_NETWORK_TEST.equals(action))
            {
                handleNetworkTest();
            }

            else if(ACTION_SYNC_BLOCKCHAIN.equals(action))
            {
                handleSyncBlockchain();
            }

            else if(ACTION_FUND_FRESH_WALLET.equals(action))
            {
                handleFundFreshWallet();
            }

            else if (ACTION_BROADCAST_BULLETIN.equals(action))
            {
                final String bulletin = intent.getStringExtra(EXTRA_BULLETIN);
                final String topic = intent.getStringExtra(EXTRA_TOPIC);
                handleActionBroadcast(bulletin, topic);
            }
        }
    }
    //--------------------------------------------------------------------------------
    private void handleNetworkTest()
    {

    }

    private void handleFundFreshWallet()
    {
        initiate();

        //has key
        //has no tx
        if( isFreshWallet() ){
            throw new NotFreshWalletRuntimeException("handleTestPeerCount in " + getClass().toString());
        }

        Map<String,String> httpParams = new HashMap<String,String>(1);
        //todo: add to constants/config
        httpParams.put("Secret", "bilbo has the ring");


        try {
            //todo: add to constants/config
            String responseString = httpsConnection("https://ahimsa.io:1050", httpParams);
            JSONTokener tokener = new JSONTokener(responseString);
            JSONObject finalResult = new JSONObject(tokener);
            String fundedTx = finalResult.getString("Tx");
            Long inValueTx = finalResult.getLong("Value");
            successOnHttps(fundedTx, inValueTx);

        } catch (Exception e) {
            //todo: handle
            e.printStackTrace();
        }

        //todo: handle


        application.saveWallet();


        //initiate
        //verify wallet is actually fresh, empty in transactions and has key, throw runtime exception if not.
        //start peergroup
        //post request new funded tx via http with timeout
        //broadcast tx, listen for confirmation. implement as utility method.
            //must implement error handling, throws if timeout or unsuccessful
            //informs application, via utility method
        //stop peergroup
        //ensure wallet has recorded this transaction
        //application.save wallet.
        //broadcast intent to application: hey assholes, this address has some coin.


    }

    private void handleSyncBlockchain()
    {



    }

    private void handleActionBroadcast(String bulletin, String topic)
    {
        //ensure length


        Log.d(TAG, "BULLETIN: " + bulletin);
        Log.d(TAG, "TOPIC: " + topic);




    }



    //Utility-------------------------------------------------------------------
    //TODO: good exception handling, maybe respond when they are thrown?
    public class PeerGroupRuntimeException extends RuntimeException
    {
        public PeerGroupRuntimeException() { super(); }
        public PeerGroupRuntimeException(String message) { super(message); }
        public PeerGroupRuntimeException(String message, Throwable cause) { super(message, cause); }
        public PeerGroupRuntimeException(Throwable cause) { super(cause); }
    }

    public class NotFreshWalletRuntimeException extends RuntimeException
    {
        public NotFreshWalletRuntimeException() { super(); }
        public NotFreshWalletRuntimeException(String message) { super(message); }
        public NotFreshWalletRuntimeException(String message, Throwable cause) { super(message, cause); }
        public NotFreshWalletRuntimeException(Throwable cause) { super(cause); }
    }

    private boolean isFreshWallet()
    {
        return wallet.getTransactions(true).size() != 0 && wallet.getKeys().size() == 0;
    }

    private List<Peer> getConnectedPeers()
    {
        if (peerGroup != null)
            return peerGroup.getConnectedPeers();
        else
            return null;
    }

    private static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private BigInteger satoshiToSelf(Transaction tx, Long inValue)
    {

        BigInteger in  = BigInteger.valueOf(inValue);
        BigInteger out = tx.getOutput(0).getValue();
        BigInteger fee = BigInteger.valueOf(config.getFeeValue());

        return in.subtract(out).subtract(fee);

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


}
