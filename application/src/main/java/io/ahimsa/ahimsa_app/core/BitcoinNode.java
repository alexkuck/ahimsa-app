package io.ahimsa.ahimsa_app.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;

/**
 * Created by askuck on 7/11/14.
 */
public class BitcoinNode
{
    private String TAG = "BitcoinNode";

    private Context context;
    private AhimsaApplication application;
    private Configuration config;

    private PowerManager.WakeLock wakeLock;
    private PeerGroup peerGroup;

    public BitcoinNode(Context context, AhimsaApplication application)
    {
        this.context = context;
        this.application = application;
        this.config = application.getConfig();
    }

    // Start and stop PeerGroup --------------------------------------------
    public void startPeerGroup(@Nonnull AbstractBlockChain chain) throws Exception
    {
        final String lockName = context.getPackageName() + " peer connection";
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);

        ConnectionDetector cd = new ConnectionDetector(context.getApplicationContext());

        if (cd.isConnectingToInternet())
        {
            Log.d(TAG, "acquiring wakelock");
            wakeLock.acquire();

            Log.d(TAG, "starting peergroup");
            peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, chain);
            peerGroup.setFastCatchupTimeSecs(application.getAhimsaWallet().getKeyStore().getEarliestKeyCreationTime());
            peerGroup.setUserAgent(Constants.USER_AGENT, Constants.VERSION);
            Log.d(TAG, "started peergroup");


            final boolean hasTrustedPeer = !config.getTrustedPeer().isEmpty();
            peerGroup.setMaxConnections(hasTrustedPeer ? 1 : config.getMaxConnectedPeers());
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
                        Log.d(TAG, "trusted peer '" + config.getTrustedPeer() + "'" + (hasTrustedPeer ? " only" : ""));

                        final InetSocketAddress addr = new InetSocketAddress(config.getTrustedPeer(), Constants.NETWORK_PARAMETERS.getPort());
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
                        while (peers.size() >= config.getMaxConnectedPeers())
                            peers.remove(peers.size() - 1);

                    return peers.toArray(new InetSocketAddress[0]);
                }

                @Override
                public void shutdown()
                {
                    normalPeerDiscovery.shutdown();
                }
            });

            peerGroup.startAsync();

//            ListenableFuture<PeerGroup> future2 = peerGroup.waitForPeers(config.getMinConnectedPeers());
//            future2.get(config.getTimeout(), TimeUnit.SECONDS);

            Log.d(TAG, "pending peers  : " + peerGroup.getPendingPeers());
            Log.d(TAG, "connected peers: " + peerGroup.getConnectedPeers());
        }
        else
        {
            throw new RuntimeException("No internet connection present");
        }
    }

    public void stopPeerGroup()
    {
        if (peerGroup != null)
        {
            peerGroup.stopAsync();
            Log.d(TAG, "peergroup stopped");
        }

        if (wakeLock.isHeld())
        {
            Log.d(TAG, "wakelock still held, releasing");
            wakeLock.release();
        }
    }

    public void waitForPeers() throws Exception
    {
        peerGroup.awaitRunning();
        peerGroup.waitForPeers(config.getMinConnectedPeers()).get(config.getTimeout(), TimeUnit.SECONDS);

        Log.d(TAG, "pending peers  : " + peerGroup.getPendingPeers());
        Log.d(TAG, "connected peers: " + peerGroup.getConnectedPeers());
    }

    // Public actions on PeerGroup ----------------------------------------------------------
    public Long broadcastTx(Transaction tx) throws Exception
    {
        Log.d(TAG, "Broadcasting transaction: " + Utils.bytesToHex(tx.bitcoinSerialize()));

        if (peerGroup != null)
        {
            waitForPeers();

            Long highest_block = getNetworkHeight();
            ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(new Transaction(Constants.NETWORK_PARAMETERS, tx.bitcoinSerialize()));
            future.get(config.getTimeout(), TimeUnit.SECONDS);

            Log.d(TAG, "Received future, success:" + future.get().toString());
            return highest_block;

        }
        else
        {
            throw new RuntimeException("PeerGroup null");
        }

    }

    public Long getNetworkHeight() throws Exception
    {
        if (peerGroup != null)
        {
            waitForPeers();
            Long peer_best = new Long( peerGroup.getMostCommonChainHeight() );
//            Long peer_best = new Long(peerGroup.getDownloadPeer().getBestHeight());

            return peer_best;
        }
        else
        {
            throw new RuntimeException("PeerGroup null");
        }
    }

    public void downloadBlockChain() throws Exception
    {
        if(peerGroup != null)
        {
//            peerGroup.downloadBlockChain();
//            peerGroup.startBlockChainDownload(new MyDownloadListener());

            waitForPeers();

            MyDownloadListener listener = new MyDownloadListener();
            peerGroup.startBlockChainDownload(listener);
            try {
                listener.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        else
        {
            throw new RuntimeException("PeerGroup null");
        }
    }

    public Block downloadBlock(Sha256Hash hash) throws Exception
    {
        if(peerGroup != null)
        {
            waitForPeers();
            return peerGroup. getDownloadPeer().getBlock(hash).get();
        }
        else
        {
            throw new RuntimeException("PeerGroup null");
        }
    }

    // Utilities -----------------------------------------------------------------------------------
    private class ConnectionDetector
    {
        private Context _context;

        public ConnectionDetector(Context context)
        {
            this._context = context;
        }

        public boolean isConnectingToInternet()
        {
            ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null)
            {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null)
                {
                    for (NetworkInfo anInfo : info){
                        if (anInfo.getState() == NetworkInfo.State.CONNECTED)
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

}