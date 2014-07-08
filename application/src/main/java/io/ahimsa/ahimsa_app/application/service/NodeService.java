package io.ahimsa.ahimsa_app.application.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.ahimsa.ahimsa_app.application.Configuration;
import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;

import io.ahimsa.ahimsa_app.application.core.AhimsaWallet;
import io.ahimsa.ahimsa_app.application.core.Utils;

/**
 * Created by askuck on 6/9/14.
 */

public class NodeService extends IntentService {

    private static final String TAG = "NodeService";

    //NodeService | PeerGroup-----------------------------------------------------------------------
    private MainApplication application;
    private Configuration config;

    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;

    private int minConnectedPeers;
    private int maxConnectedPeers;
    private String trustedPeerHost;


    //NodeService | Actions-------------------------------------------------------------------------
    //Broadcast Transaction
    private static final String ACTION_BROADCAST_TX     = NodeService.class.getPackage().getName() + ".broadcast_transaction";private static final String ACTION_BROADCAST_FUNDING_TX = NodeService.class.getPackage().getName() + ".broadcast_funding_transaction";
    private static final String ACTION_SYNC_BLOCKCHAIN  = NodeService.class.getPackage().getName() + ".sync_blockchain";
    private static final String ACTION_CONFIRM_TX       = NodeService.class.getPackage().getName() + ".confirm_tx";
    private static final String ACTION_DISCOVER_TX      = NodeService.class.getPackage().getName() + ".discover_transaction";
    private static final String ACTION_NETWORK_TEST     = NodeService.class.getPackage().getName() + ".network_test";

    private static final String EXTRA_BLOCKHEIGHT       = NodeService.class.getPackage().getName() + ".blockheight";
    private static final String EXTRA_TX                = NodeService.class.getPackage().getName() + ".transaction";
    //----------------------------------------------------------------------------------------------
    //IntentService---------------------------------------------------------------------------------

    public NodeService() {
        super("NodeService");
    }

    //----------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private void initiate()
    {
        application = (MainApplication) getApplication();
        config = application.getConfig();

        maxConnectedPeers = config.getMaxConnectedPeers();
        minConnectedPeers = config.getMinConnectedPeers();
        trustedPeerHost   = config.getTrustedPeer();

    }

    private void startPeerGroup(@Nullable AbstractBlockChain chain)
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

                if(chain != null){
                    Log.d(TAG, "starting peergroup WITH an associated chain");
                    peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, chain);
                    peerGroup.setFastCatchupTimeSecs(application.getCreationTime());
                }else{
                    Log.d(TAG, "starting peergroup WITHOUT an associated chain");
                    peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS);
                }
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
                peerGroup.waitForPeers(minConnectedPeers).get(config.getTimeout(), TimeUnit.SECONDS);
                //TODO: let application know timeout occurred


                Log.d(TAG, "pending peers  : " + peerGroup.getPendingPeers());
                Log.d(TAG, "connected peers: " + peerGroup.getConnectedPeers());


                int peer_best = peerGroup.getMostCommonChainHeight();
                Log.d(TAG, "Is peerGroup.getDownloadPeer() null? " + peer_best);

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
        Log.d(TAG, "Broadcasting transaction: " + Utils.bytesToHex(tx)  );

        if(peerGroup != null){
            Long highest_block = getPeerBestHeight();
            ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(new Transaction(Constants.NETWORK_PARAMETERS, tx));

            try{
                future.get(config.getTimeout(), TimeUnit.SECONDS);
                Log.d(TAG, "Received future, success:" + future.get().toString());

                Intent intent = new Intent().setAction(AhimsaWallet.ACTION_BROADCAST_SUCCESS);
                intent.putExtra(AhimsaWallet.EXTRA_TX_BYTE_ARRAY, future.get().bitcoinSerialize());
                intent.putExtra(AhimsaWallet.EXTRA_HIGHEST_BLOCK_LONG, highest_block);
                application.sendBroadcast(intent);

            } catch (Exception e){
                //todo handle
                e.printStackTrace();
            }

        } else {
            Log.d(TAG, "Broadcast Fail, peerGroup null.");
            //todo handle
        }
    }

    private void broadcastFundingTx(byte[] tx)
    {
        Log.d(TAG, "Broadcasting FUNDING transaction: " + Utils.bytesToHex(tx)  );

        if(peerGroup != null){
            Long highest_block = getPeerBestHeight();
            ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(new Transaction(Constants.NETWORK_PARAMETERS, tx));

            try{
                future.get(config.getTimeout(), TimeUnit.SECONDS);
                Log.d(TAG, "Received future, success:" + future.get().toString());

                Intent intent = new Intent().setAction(AhimsaWallet.ACTION_BROADCAST_FUNDING_SUCCESS);
                intent.putExtra(AhimsaWallet.EXTRA_TX_BYTE_ARRAY, future.get().bitcoinSerialize());
                intent.putExtra(AhimsaWallet.EXTRA_HIGHEST_BLOCK_LONG, highest_block);
                application.sendBroadcast(intent);


            } catch (Exception e){
                e.printStackTrace();
            }


        } else {
            Log.d(TAG, "Broadcast Fail, peerGroup null.");
            //todo handle
        }
    }

    //todo: implement ListenableFuture<Long>
    private void startBlockChainDownload()
    {
        if(peerGroup != null)
        {
            Log.d(TAG, "START | HANDLE_SYNC_BLOCKCHAIN: " + application.getBlockChain().getBestChainHeight());
            peerGroup.downloadBlockChain();
            Log.d(TAG, "FINISH | HANDLE_SYNC_BLOCKCHAIN: " + application.getBlockChain().getBestChainHeight());
        }
    }

    private Long getPeerBestHeight(){
        if(peerGroup != null){
//            return  new Long(peerGroup.getMostCommonChainHeight());
            return new Long(peerGroup.getDownloadPeer().getBestHeight());
        }
        return new Long(0);
    }

    private List<Peer> getConnectedPeers()
    {
        if (peerGroup != null)
            return peerGroup.getConnectedPeers();
        return null;
    }


    //----------------------------------------------------------------------------------------------
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
    //----------------------------------------------------------------------------------------------
    //Public start action methods

    public static void startActionBroadcastTx(@Nonnull Context context, @Nonnull byte[] tx)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_BROADCAST_TX);
        intent.putExtra(EXTRA_TX, tx);
        context.startService(intent);
    }

    public static void startActionBroadcastFundingTx(@Nonnull Context context, @Nonnull byte[] tx)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_BROADCAST_FUNDING_TX);
        intent.putExtra(EXTRA_TX, tx);
        context.startService(intent);
    }

    public static void startActionSyncBlockchain(@Nonnull Context context){
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_SYNC_BLOCKCHAIN);
        context.startService(intent);
    }

    public static void startConfirmTx(@Nonnull Context context, @Nonnull byte[] tx){
        //todo: add time to parameters
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_CONFIRM_TX);
        intent.putExtra(EXTRA_TX, tx);
        context.startService(intent);
    }

    public static void startDiscoverFundingTx(@Nonnull Context context, @Nonnull Long blockheight){
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_DISCOVER_TX);
        intent.putExtra(EXTRA_BLOCKHEIGHT, blockheight);
        context.startService(intent);

    }

    public static void startNetworkTest(@Nonnull Context context)
    {
        Intent intent = new Intent(context, NodeService.class);
        intent.setAction(ACTION_NETWORK_TEST);
        context.startService(intent);
    }
    //----------------------------------------------------------------------------------------------
    //Handle intents
    protected void onHandleIntent(Intent intent){
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "NodeService | onHandleIntent | " + action);

            if (ACTION_BROADCAST_TX.equals(action))
            {
                final byte[] tx = intent.getByteArrayExtra(EXTRA_TX);
                handleActionBroadcastTx(tx);
            }
            else if(ACTION_BROADCAST_FUNDING_TX.equals(action))
            {
                final byte[] tx = intent.getByteArrayExtra(EXTRA_TX);
                handleActionBroadcastFundingTx(tx);
            }

            else if(ACTION_SYNC_BLOCKCHAIN.equals(action))
            {
                handleSyncBlockchain();
            }

            else if(ACTION_CONFIRM_TX.equals(action))
            {
                final byte[] tx = intent.getByteArrayExtra(EXTRA_TX);
                confirmTx(tx);
            }

            else if(ACTION_DISCOVER_TX.equals(action)){
                final Long height = intent.getLongExtra(EXTRA_BLOCKHEIGHT, -1);
                discoverTx(height);
            }

            else if (ACTION_NETWORK_TEST.equals(action))
            {
                handleNetworkTest();
            }

        }
    }
    //----------------------------------------------------------------------------------------------
    private void handleActionBroadcastTx(byte[] tx)
    {
        initiate();
        startPeerGroup(null);
        broadcastTx(tx);
        stopPeerGroup();
    }

    private void handleActionBroadcastFundingTx(byte[] tx){
        initiate();
        startPeerGroup(null);
        broadcastFundingTx(tx);
        stopPeerGroup();
    }


    private void handleSyncBlockchain()
    {
        initiate();
        startPeerGroup(application.getBlockChain());
        if(peerGroup != null){
            peerGroup.downloadBlockChain();
        }
        stopPeerGroup();
    }

    private void confirmTx(byte[] tx)
    {

    }

    private void discoverTx(Long height)
    {
        initiate();
        AbstractBlockChain chain = application.getBlockChain();
        Wallet wallet = application.getKeyWallet();

        if(chain.getBestChainHeight() < height){
            //The local chain's height less than the requested block's height.
            startPeerGroup(chain);
            Long x = getPeerBestHeight();
            Log.d(TAG, String.format("discoverTx() | getPeerBestHeight() (%d) < height = (%d)", x, height));

            if(x >= height){
                //The network's height is greater than or equal to the requested block's height.
                Log.d(TAG, "discoverTx() | BEFORE startBlockChainDownload()");
                if(peerGroup != null){
                    peerGroup.downloadBlockChain();
                }
                Log.d(TAG, "discoverTx() | AFTER startBlockChainDownload()");

            } else {
                //The requested block's height exceeds the network's height.
                //todo: send intent claiming block height exceeds highest in chain

                //todo invalid import
                Log.d(TAG, "Invalid import, stopping peergroup.");
                stopPeerGroup();
                return;


            }
        } else {
            //The local chain has the requested block.
            startPeerGroup(null);
        }

        //We have a chain locally that has block header of the requested block.
        List<Transaction> relevantTxs = findTxInBlock(chain.getBlockStore(), wallet, height);

        for(Transaction tx : relevantTxs){
            Intent intent = new Intent();
            intent.setAction(AhimsaWallet.ACTION_DISCOVERED_TX);
            intent.putExtra(AhimsaWallet.EXTRA_TX_BYTE_ARRAY, tx.bitcoinSerialize());
            application.sendBroadcast(intent);
        }

        Log.d(TAG, "Discover Transaction | relevantTxs\n" + relevantTxs.toString());
        stopPeerGroup();


    }


    private void handleNetworkTest()
    {
        initiate();
        startPeerGroup(null);
        int count = getConnectedPeers().size();
        Log.d(TAG, "connected peers: " + count);
        stopPeerGroup();

    }

    //Discover--------------------------------------------------------------------------------------
    public List<Transaction> findTxInBlock(BlockStore store, Wallet wallet, Long height) {
        // Returns all of the funding transactions in the block at this height
        StoredBlock targetBlock = getBlock(store, height);
        Sha256Hash hash = targetBlock.getHeader().getHash();
        Log.d(TAG, "hash: " + hash);


        // we must download the full block from the network
        List<Transaction> foundTxs = new ArrayList<Transaction>();
        if(peerGroup != null){
            try {
                Block completeBlock = peerGroup.getDownloadPeer().getBlock(hash).get();
                for (Transaction tx : completeBlock.getTransactions()) {
                    if (isMyTx(tx, wallet)){
                        tx.setBlockAppearance(targetBlock, true, 1);
                        foundTxs.add(tx);
                        Log.d(TAG, "***tx.getAppearsInHashes()***\n" + tx.getAppearsInHashes().toString());
                    }
                }
                return foundTxs;

            } catch (InterruptedException e) {
                //todo handle
                e.printStackTrace();
            } catch (ExecutionException e) {
                //todo handle
                e.printStackTrace();
            }
        }
        return foundTxs;
    }

    private StoredBlock getBlock(BlockStore store, Long height){
        try {
            StoredBlock current = store.getChainHead();
            if (height > current.getHeight()) {
                Log.d(TAG, String.format("getBlock() | return null; height (%d) > current.getHeight() (%d)", height, current.getHeight()));
                return null;
            }
            for (int i = current.getHeight(); i > height; i -= 1) {
                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
                current = previous;
            }
            Log.d(TAG, String.format("getBlock() | return block with: hash (%s) and height (%d)", current.getHeader().getHash(), current.getHeight()));
            return current;
        } catch (BlockStoreException e) {
            Log.d(TAG, String.format("Block of height %d is not within the blockstore.", height));
            return null;
        }
    }

    public boolean isMyTx(Transaction tx, Wallet wallet) {
        // Uses a wallet to determine if any of the txouts are sent to a key in the wallet
        for (TransactionOutput txOut : tx.getOutputs()) {
            if (txOut.isMine(wallet)) {
                return true;
            }
        }
        return false;
    }


}
