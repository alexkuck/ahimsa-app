package io.ahimsa.ahimsa_app.core;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;

public class AhimsaService extends IntentService
{
    private static final String ACTION_NETWORK_TEST         = AhimsaService.class.getPackage().getName() + ".network_test";
    private static final String ACTION_RESET_AHIMSA_WALLET  = AhimsaService.class.getPackage().getName() + ".reset";
    private static final String ACTION_BROADCAST_BULLETIN   = AhimsaService.class.getPackage().getName() + ".broadcast_bulletin";
    private static final String ACTION_BROADCAST_TX         = AhimsaService.class.getPackage().getName() + ".broadcast_tx";
    private static final String ACTION_SYNC_BLOCK_CHAIN     = AhimsaService.class.getPackage().getName() + ".sync_block_chain";
    private static final String ACTION_IMPORT_BLOCK         = AhimsaService.class.getPackage().getName() + ".import_block";
    private static final String ACTION_CONFIRM_TX           = AhimsaService.class.getPackage().getName() + ".confirm_tx";

    private static final String EXTRA_STRING_TOPIC          = AhimsaService.class.getPackage().getName() + ".topic";
    private static final String EXTRA_STRING_MESSAGE        = AhimsaService.class.getPackage().getName() + ".message";
    private static final String EXTRA_LONG_FEE              = AhimsaService.class.getPackage().getName() + ".fee";
    private static final String EXTRA_BYTE_ARRAY_TX         = AhimsaService.class.getPackage().getName() + ".tx";
    private static final String EXTRA_BOOLEAN_ASSUME_CONF   = AhimsaService.class.getPackage().getName() + ".assume_confirmed";
    private static final String EXTRA_LONG_HEIGHT           = AhimsaService.class.getPackage().getName() + ".height";
    private static final String EXTRA_STRING_TXID           = AhimsaService.class.getPackage().getName() + ".txid";

    private AhimsaApplication application;
    private Configuration config;
    private AhimsaWallet ahimwall;
    private AhimsaLog ahimlog;
    private BitcoinNode node;


    private String TAG = "AhismaService";

    public static void startNetworkTest(Context context)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_NETWORK_TEST);
        context.startService(intent);
    }

    public static void startResetAhimsaWallet(Context context)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_RESET_AHIMSA_WALLET);
        context.startService(intent);
    }

    public static void startBroadcastBulletin(Context context, String topic, String message, Long fee)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_BROADCAST_BULLETIN);
        intent.putExtra(EXTRA_STRING_TOPIC, topic);
        intent.putExtra(EXTRA_STRING_MESSAGE, message);
        intent.putExtra(EXTRA_LONG_FEE, fee);
        context.startService(intent);
    }

    public static void startBroadcastTx(Context context, byte[] tx_raw, boolean assume_confirmed)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_BROADCAST_TX);
        intent.putExtra(EXTRA_BYTE_ARRAY_TX, tx_raw);
        intent.putExtra(EXTRA_BOOLEAN_ASSUME_CONF, assume_confirmed);
        context.startService(intent);
    }

    public static void startSyncBlockChain(Context context)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_SYNC_BLOCK_CHAIN);
        context.startService(intent);
    }

    public static void startImportBlock(Context context, Long height)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_IMPORT_BLOCK);
        intent.putExtra(EXTRA_LONG_HEIGHT, height);
        context.startService(intent);
    }

    public static void startConfirmTx(Context context, String txid)
    {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_CONFIRM_TX);
        intent.putExtra(EXTRA_STRING_TXID, txid);
        context.startService(intent);
    }

    public AhimsaService()
    {
        super("AhimsaService");
    }

    //----------------------------------------------------------------------------------------------
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        application = (AhimsaApplication) getApplication();
        config      = application.getConfig();
        ahimwall    = application.getAhimsaWallet();
        ahimlog     = application.getAhimsaLog();
        node        = new BitcoinNode(this, application);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        if(intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_NETWORK_TEST.equals(action))
            {
                ahimlog.pushLog(getString(R.string.network_test) + getString(R.string.to_queue), AhimsaLog.queue);
            }
            else if (ACTION_RESET_AHIMSA_WALLET.equals(action))
            {
                ahimlog.pushLog(getString(R.string.reset_ahimwall) + getString(R.string.to_queue), AhimsaLog.queue);
            }
            else if (ACTION_BROADCAST_BULLETIN.equals(action))
            {
                // final String topic = intent.getStringExtra(EXTRA_STRING_TOPIC);
                // final String message = intent.getStringExtra(EXTRA_STRING_MESSAGE);
                // final Long fee = intent.getLongExtra(EXTRA_LONG_FEE, Constants.MIN_FEE);
                // ahimwall.reserveTxOuts(topic, message, fee);

                ahimlog.pushLog(getString(R.string.broadcast_bulletin) + getString(R.string.to_queue), AhimsaLog.queue);
                ahimwall.reserveTxOuts();
                updatedOverview();
            }
            else if (ACTION_BROADCAST_TX.equals(action))
            {
                ahimlog.pushLog(getString(R.string.broadcast_tx) + getString(R.string.to_queue), AhimsaLog.queue);
            }
            else if (ACTION_SYNC_BLOCK_CHAIN.equals(action))
            {
                ahimlog.pushLog(getString(R.string.sync_block_chain) + getString(R.string.to_queue), AhimsaLog.queue);
            }
            else if (ACTION_IMPORT_BLOCK.equals(action))
            {
                ahimlog.pushLog(getString(R.string.import_block) + getString(R.string.to_queue), AhimsaLog.queue);
            }
            else if (ACTION_CONFIRM_TX.equals(action))
            {
                ahimlog.pushLog(getString(R.string.confirm_tx) + getString(R.string.to_queue), AhimsaLog.queue);
            }
        }

        updatedLog();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        ahimwall.removeAllReservations();
        node.stopPeerGroup();

        super.onDestroy();
    }

    //----------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent)
    {
        if(intent != null)
        {
            final String action = intent.getAction();
            Log.d(TAG, "Commencing AhimsaService, action | " + action);

            if(!node.isRunning())
            {
                try
                {
                    node.startPeerGroup();
                }
                catch (Exception e)
                {
                    // no internet connection exists
                    e.printStackTrace();
                }
            }

            if(ACTION_NETWORK_TEST.equals(action))
            {
                ahimlog.pushLog(getString(R.string.start) + getString(R.string.network_test), AhimsaLog.normal);
                handleNetworkTest();
            }
            else if(ACTION_RESET_AHIMSA_WALLET.equals(action))
            {
                ahimlog.pushLog(getString(R.string.start) + getString(R.string.reset_ahimwall), AhimsaLog.normal);
                handleResetAhimsaWallet();
                updatedOverview();
            }
            else if(ACTION_BROADCAST_BULLETIN.equals(action))
            {
                final String topic = intent.getStringExtra(EXTRA_STRING_TOPIC);
                final String message = intent.getStringExtra(EXTRA_STRING_MESSAGE);
                final Long fee = intent.getLongExtra(EXTRA_LONG_FEE, Constants.MIN_FEE);

                ahimlog.pushLog(getString(R.string.start) + getString(R.string.broadcast_bulletin), AhimsaLog.normal);
                handleBroadcastBulletin(topic, message, fee);
                updatedOverview();
            }
            else if(ACTION_BROADCAST_TX.equals(action))
            {
                final byte[] tx_raw = intent.getByteArrayExtra(EXTRA_BYTE_ARRAY_TX);
                final boolean assume_confirmed = intent.getBooleanExtra(EXTRA_BOOLEAN_ASSUME_CONF, false);

                ahimlog.pushLog(getString(R.string.start) + getString(R.string.broadcast_tx), AhimsaLog.normal);
                handleBroadcastTx(tx_raw, assume_confirmed);
                updatedOverview();
            }
            else if(ACTION_SYNC_BLOCK_CHAIN.equals(action))
            {
                handleSyncBlockChain();
            }
            else if(ACTION_IMPORT_BLOCK.equals(action))
            {
                final long height = intent.getLongExtra(EXTRA_LONG_HEIGHT, -1);

                ahimlog.pushLog(getString(R.string.start) + getString(R.string.import_block), AhimsaLog.normal);
                handleImportBlock(height);
                updatedOverview();
            }
            else if(ACTION_CONFIRM_TX.equals(action))
            {
                final String txid = intent.getStringExtra(EXTRA_STRING_TXID);

                ahimlog.pushLog(getString(R.string.start) + getString(R.string.confirm_tx), AhimsaLog.normal);
                handleConfirmTx(txid);
                updatedOverview();
            }

            updatedLog();

            Log.d(TAG, "Completing AhimsaService, action | " + intent.getAction());
        }
    }
    //----------------------------------------------------------------------------------------------
    private void updatedOverview()
    {
        Intent update_intent = new Intent();
        update_intent.setAction(Constants.ACTION_UPDATED_OVERVIEW);
        LocalBroadcastManager.getInstance(this).sendBroadcast(update_intent);
    }

    private void updatedQueue()
    {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_UPDATED_QUEUE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updatedLog()
    {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_UPDATED_LOG);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updatedBulletin()
    {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_UPDATED_BULLETIN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    //----------------------------------------------------------------------------------------------
    private void handleNetworkTest()
    {
        //todo logsssss
        try
        {
            Long peergroup_height = node.getNetworkHeight();
            config.setHighestBlockSeen( Math.max(peergroup_height, config.getHighestBlockSeen()) );
        }
        catch (Exception e)
        {
            //peergroup is null
            e.printStackTrace();
        }

        // todo | ensure consistency within database, wallet.
        // todo | restore from backup if inconsistent. reset if backup also inconsistent.
    }

    private void handleResetAhimsaWallet()
    {
        //todo logsssss

        // todo | implement.
        // Reset then re-initialization config, database, and keyStore.
//        ahimwall.toLog();
//        ahimwall.resetDB();
        ahimwall.toLog();
    }


    private void handleBroadcastBulletin(String topic, String message, Long fee)
    {
        Transaction bulletin = null;

        try
        {
            bulletin = ahimwall.createAndAddBulletin(topic, message, fee);
            ahimwall.toLog();
            Log.d(TAG, bulletin.toString());
        }
        catch (Exception e)
        {
            // insufficient funds
            e.printStackTrace();
        }

        if(bulletin != null)
        {
            try
            {
                Long highest_block = node.broadcastTx(bulletin);
                ahimwall.commitTransaction(bulletin, highest_block, false);
                updatedBulletin();

                String words = getString(R.string.success_broadcast_bulletin);
//                String abbreviated_txid = Utils.abbreviator(bulletin.getHashAsString(), 15);
//                String details = String.format(words, topic, abbreviated_txid);
                String details = String.format(words, topic, bulletin.getHashAsString());
                ahimlog.pushLog( details, AhimsaLog.normal );
            }
            catch (Exception e)
            {
                // Peergroup was null or transaction future was not received
                ahimwall.unreserveTxOuts();

                String words = getString(R.string.fail_broadcast_bulletin);
//                String abbreviated_txid = Utils.abbreviator(bulletin.getHashAsString(), 15);
//                String details = String.format(words, e.getMessage(), abbreviated_txid);
                String details = String.format(words, e.getMessage(), bulletin.getHashAsString());
                ahimlog.pushLog( details, AhimsaLog.error );

                e.printStackTrace();
            }
        }

    }

    private void handleBroadcastTx(byte[] tx_raw, boolean assume_confirmed)
    {
        Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, tx_raw);

        try
        {
            Long highest_block = node.broadcastTx(tx);
            ahimwall.commitTransaction(tx, highest_block, assume_confirmed);
            updatedBulletin();

            String words = getString(R.string.success_broadcast_tx);
//            String abbreviated_txid = Utils.abbreviator(tx.getHashAsString(), 15);
//            String details = String.format(words, abbreviated_txid);
            String details = String.format(words, tx.getHashAsString());
            ahimlog.pushLog( details, AhimsaLog.normal );
        }
        catch (Exception e)
        {
            // Peergroup was null or transaction future was not received
            e.printStackTrace();

            String words = getString(R.string.fail_broadcast_tx);
            String abbreviated_txid = Utils.abbreviator(tx.getHashAsString(), 15);
            String details = String.format(words, e.getMessage(), abbreviated_txid);
//            String details = String.format(words, e.getMessage(), tx.getHashAsString());
            ahimlog.pushLog( details, AhimsaLog.error );
        }
    }

    private void handleSyncBlockChain()
    {
        try
        {
            String before_words = getString(R.string.start_sync_block_chain);
            String before_details = String.format(before_words, application.getBlockChain().getBestChainHeight() );
            ahimlog.pushLog( before_details, AhimsaLog.normal );

            node.downloadBlockChain();

            String after_words = getString(R.string.finish_sync_block_chain);
            String after_details = String.format(after_words, application.getBlockChain().getBestChainHeight() );
            ahimlog.pushLog( after_details, AhimsaLog.normal );
        }
        catch (Exception e) {
            e.printStackTrace();

            String words = getString(R.string.fail_sync_block_chain);
            String details = String.format(words, e.getMessage());
            ahimlog.pushLog( details, AhimsaLog.error );
        }

    }

    private void handleImportBlock(Long import_height)
    {
        AbstractBlockChain chain = application.getBlockChain();
        Block complete_block = null;

        try
        {
            if(import_height > node.getNetworkHeight())
            {
                ahimlog.pushLog(getString(R.string.import_exceeds_network), AhimsaLog.normal);
                return;
            }

            if(import_height >= chain.getBestChainHeight() )
                handleSyncBlockChain();

            StoredBlock targetBlock = getBlock(chain.getBlockStore(), import_height);
            Sha256Hash hash = targetBlock.getHeader().getHash();
            complete_block = node.downloadBlock(hash);
        }
        catch (Exception e)
        {
            e.printStackTrace();

            String words = getString(R.string.fail_to_import);
            String details = String.format(words, e.getMessage());
            ahimlog.pushLog( details, AhimsaLog.error );
        }

        if( complete_block != null )
        {
            List<Transaction> relevantTxs = findRelevantTxs(complete_block);

            String words = getString(R.string.found_relevant_txs);
            String details = String.format(words, import_height, relevantTxs.size());
            ahimlog.pushLog( details, AhimsaLog.normal );

            for(Transaction tx : relevantTxs)
            {
                ahimwall.commitTransaction(tx, import_height, true);
            }
        }
        else
        {
            String words = getString(R.string.fail_to_import);
            String details = String.format(words, "complete_block was null");
            ahimlog.pushLog( details, AhimsaLog.error );
        }

    }

    public void handleConfirmTx(String txid)
    {
        Log.d(TAG, "handleConfirmTx | " + txid);

        Bundle bundle = ahimwall.getTxBundle(txid);
        if(bundle == null)
        {
            String words = getString(R.string.tx_bundle_null);
            String details = String.format(words, txid);
            ahimlog.pushLog(details, AhimsaLog.error);
            return;
        }

        if(bundle.getBoolean(AhimsaDB.confirmed))
        {
            String words = getString(R.string.tx_already_confirmed);
            String details = String.format(words, txid);
            ahimlog.pushLog(details, AhimsaLog.normal);
            return;
        }

        Log.d(TAG, "Bundle is not null and tx is unconfirmed");

        Long upper_time = bundle.getLong(AhimsaDB.sent_time) + Constants.THREE_DAYS;
        AbstractBlockChain chain = application.getBlockChain();

        if(chain.getChainHead().getHeader().getTimeSeconds() < upper_time)
        {
            handleSyncBlockChain();
        }

        Long lower_height = bundle.getLong(AhimsaDB.highest_block);
        Stack<Sha256Hash> hashes = getSequentialHashes(chain.getBlockStore(), lower_height, upper_time);

        Log.d(TAG, "lower_height | " + lower_height.toString());
        Log.d(TAG, "upper_time   | " + upper_time.toString());

        if(hashes == null)
        {
            String words = getString(R.string.sequential_hashes_null);
            String details = String.format(words, txid);
            ahimlog.pushLog(details, AhimsaLog.error);
            return;
        }

        try
        {
            while( !hashes.isEmpty() )
            {
                Block downloaded = node.downloadBlock( hashes.pop() );
                Log.d(TAG, "downloaded hash | " + downloaded.getHashAsString());
                lower_height += 1;
                Log.d(TAG, "lower_height| " + downloaded.getHashAsString());

                if( containsTx(downloaded, txid) )
                {
                    String words = getString(R.string.confirmed_tx);
                    String details = String.format(words, txid, lower_height);
                    ahimlog.pushLog(details, AhimsaLog.normal);

                    ahimwall.confirmTx(txid, lower_height);
                    updatedBulletin();
                    break;
                }

                if( downloaded.getTimeSeconds() > upper_time )
                {
                    String words = getString(R.string.dropped_tx);
                    String details = String.format(words, txid);
                    ahimlog.pushLog(details, AhimsaLog.normal);

                    ahimwall.dropTx(txid, lower_height);
                    updatedBulletin();
                    break;
                }
                Log.d(TAG, "set highest block " + lower_height.toString());
                ahimwall.setHighestBlock(txid, lower_height);
            }
        }
        catch (Exception e)
        {
            String words = getString(R.string.fail_confirm_tx);
            String details = String.format(words, e.getMessage());
            ahimlog.pushLog( details, AhimsaLog.error );

            e.printStackTrace();
        }
    }

    // Private Utilities ---------------------------------------------------------------------------
    private Stack<Sha256Hash> getSequentialHashes(BlockStore store, Long lower_height, Long upper_time)
    {
        Stack<Sha256Hash> hashes = new Stack<Sha256Hash>();

        try
        {
            StoredBlock current = store.getChainHead();
            Log.d(TAG, "initial current | " + current.getHeader().getHashAsString());
            Log.d(TAG, "Current sent_time: " + current.getHeader().getTimeSeconds() );
            Log.d(TAG, "upper_time: " + upper_time );

            // This overshoot is imperative, A transaction is only dropped from AhimsaWallet when a
            // block's timestamp is greater than the upper_time estimate. todo | think of alternatives
            while(current.getHeader().getTimeSeconds() > upper_time  + (Constants.THREE_DAYS / 24))
            {
                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
                current = previous;
                Log.d(TAG, "upper_time current | " + current.getHeader().getHashAsString());
            }

            Log.d(TAG, "lower_height: " + lower_height);
            while(current.getHeight() > lower_height)
            {
                hashes.push(current.getHeader().getHash());
                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
                current = previous;
                Log.d(TAG, "lower_height current | " + current.getHeader().getHashAsString());
                Log.d(TAG, "lower_height current.height | " + current.getHeight());
            }

            return hashes;
        }
        catch (BlockStoreException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private boolean containsTx(Block check_me, String txid)
    {
        for(Transaction tx : check_me.getTransactions() )
        {
            Log.d(TAG, "tx: " + tx.getHashAsString());
            if(tx.getHashAsString().equals(txid))
                return true;
        }

        return false;
    }

    private StoredBlock getBlock(BlockStore store, Long height)
    {
        try
        {
            StoredBlock current = store.getChainHead();
            if(height > current.getHeight() || height < 0)
            {
                return null;
            }

            while(current.getHeight() > height)
            {
                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
                current = previous;
            }

//            for(int i = current.getHeight(); i > height; i -= 1)
//            {
//                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
//                current = previous;
//            }

            return current;
        }
        catch (BlockStoreException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private List<Transaction> findRelevantTxs(Block block)
    {
        List<Transaction> foundTxs = new ArrayList<Transaction>();
        for( Transaction tx : block.getTransactions() )
        {
            Log.d(TAG, tx.getHashAsString());
            if( ahimwall.isRelevant(tx) )
            {
                foundTxs.add(tx);
            }
        }
        return foundTxs;
    }

}
