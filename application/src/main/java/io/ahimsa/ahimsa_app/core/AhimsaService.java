package io.ahimsa.ahimsa_app.core;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;

import java.util.ArrayList;
import java.util.List;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Constants;

public class AhimsaService extends IntentService {
    private static final String ACTION_VERIFY_AHIMSA_WALLET = AhimsaService.class.getPackage().getName() + ".verify_ahisma_wallet";
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

    private AhimsaApplication application;
    private AhimsaWallet ahimwall;
    private String TAG = "AhismaService";

    public static void startVerifyAhimsaWallet(Context context) {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_VERIFY_AHIMSA_WALLET);
        context.startService(intent);
    }

    public static void startResetAhimsaWallet(Context context) {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_RESET_AHIMSA_WALLET);
        context.startService(intent);
    }

    public static void startBroadcastBulletin(Context context, String topic, String message, Long fee) {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_BROADCAST_BULLETIN);
        intent.putExtra(EXTRA_STRING_TOPIC, topic);
        intent.putExtra(EXTRA_STRING_MESSAGE, message);
        intent.putExtra(EXTRA_LONG_FEE, fee);
        context.startService(intent);
    }

    public static void startBroadcastTx(Context context, byte[] tx_raw, boolean assume_confirmed){
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_BROADCAST_TX);
        intent.putExtra(EXTRA_BYTE_ARRAY_TX, tx_raw);
        intent.putExtra(EXTRA_BOOLEAN_ASSUME_CONF, assume_confirmed);
        context.startService(intent);
    }

    public static void startSyncBlockChain(Context context) {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.setAction(ACTION_SYNC_BLOCK_CHAIN);
        context.startService(intent);
    }

    public static void startImportBlock(Context context, Long height) {
        Intent intent = new Intent(context, AhimsaService.class);
        intent.putExtra(EXTRA_LONG_HEIGHT, height);
        intent.setAction(ACTION_IMPORT_BLOCK);
        context.startService(intent);
    }

    public AhimsaService() {
        super("AhimsaService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            commence(intent);
            final String action = intent.getAction();

            if (ACTION_VERIFY_AHIMSA_WALLET.equals(action)) {
                handleVerifyAhimsaWallet();
            }
            else if (ACTION_RESET_AHIMSA_WALLET.equals(action)) {
                handleResetAhimsaWallet();
            }
            else if (ACTION_BROADCAST_BULLETIN.equals(action)) {
                final String topic = intent.getStringExtra(EXTRA_STRING_TOPIC);
                final String message = intent.getStringExtra(EXTRA_STRING_MESSAGE);
                final Long fee = intent.getLongExtra(EXTRA_LONG_FEE, Constants.MIN_FEE);
                handleBroadcastBulletin(topic, message, fee);
            }
            else if (ACTION_BROADCAST_TX.equals(action)) {
                final byte[] tx_raw = intent.getByteArrayExtra(EXTRA_BYTE_ARRAY_TX);
                final boolean assume_confirmed = intent.getBooleanExtra(EXTRA_BOOLEAN_ASSUME_CONF, false);
                handleBroadcastTx(tx_raw, assume_confirmed);
            }
            else if (ACTION_SYNC_BLOCK_CHAIN.equals(action)) {
                handleSyncBlockChain();
            }
            else if (ACTION_IMPORT_BLOCK.equals(action)) {
                final long height = intent.getLongExtra(EXTRA_LONG_HEIGHT, -1);
                handleImportBlock(height);
            }

            complete(intent);
        }
    }

    private void commence(Intent intent)
    {
        application = (AhimsaApplication) getApplication();
        ahimwall = application.getAhimsaWallet();

        Log.d(TAG, "Commencing AhimsaService, action | " + intent.getAction());
    }

    private void complete(Intent intent)
    {
        application.getConfig().setTempConfBalance(ahimwall.getConfirmedBalance().longValue());

        Intent update_intent = new Intent();
        update_intent.setAction(Constants.ACTION_AHIMWALL_UPDATE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(update_intent);

        Log.d(TAG, "Completing AhimsaService, action | " + intent.getAction());

    }

    //----------------------------------------------------------------------------------------------
    private void handleVerifyAhimsaWallet() {
        ahimwall.verifyKeyStore();

        BitcoinNode node = new BitcoinNode(this, application);
        try {
            node.startPeerGroup(null);
            Long netheight = node.getNetworkHeight();
            Log.d(TAG, "GETNETWORKHEIGHT: " + netheight);
            node.stopPeerGroup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // todo | ensure consistency within database, wallet.
        // todo | restore from backup if inconsistent. reset if backup also inconsistent.
    }

    private void handleResetAhimsaWallet() {
        // Reset then re-initialization config, database, and keyStore.
        ahimwall.reset();
    }


    private void handleBroadcastBulletin(String topic, String message, Long fee) {
        Transaction bulletin;

        // Create bulletin.  BulletinBuilder can throw an exception.
        try {
            bulletin = ahimwall.createAndAddBulletin(topic, message, fee);
        } catch (Exception e) {
//            application.makeLongToast("Fail: could not construct bulletin.");
            e.printStackTrace();
            return;
        }

        // Broadcast transaction on successful creation of bulletin.
        // If broadcast fails, store the topic, message, and fee.
        BitcoinNode node = new BitcoinNode(this, application);
        try {
            node.startPeerGroup(null);
            Long highest_block = node.broadcastTx(bulletin);
            ahimwall.commitTransaction(bulletin, highest_block, false);
//            application.makeLongToast("Woot woot! Successfully broadcast bulletin: " + bulletin.getHashAsString());
        } catch (Exception e) {
//            application.makeLongToast("Fail: could not broadcast bulletin.");
            e.printStackTrace();
        } finally {
            node.stopPeerGroup();
        }
    }

    private void handleBroadcastTx(byte[] tx_raw, boolean assume_confirmed) {
        Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, tx_raw);

        // Attempt broadcast of transaction. Commit status dependent upon assume_confirmed.
        BitcoinNode node = new BitcoinNode(this, application);
        try {
            node.startPeerGroup(null);
            Long highest_block = node.broadcastTx(tx);

            ahimwall.commitTransaction(tx, highest_block, assume_confirmed);

//            application.makeLongToast("Woot woot! Successfully broadcast transaction: " + tx.getHashAsString());

        } catch (Exception e) {
//            application.makeLongToast("Fail: could not broadcast bulletin.");
            e.printStackTrace();
        } finally {
            node.stopPeerGroup();
        }


    }

    private void handleSyncBlockChain() {
        BitcoinNode node = new BitcoinNode(this, application);
        try{
            node.startPeerGroup(application.getBlockChain());
            node.downloadBlockChain();
        } catch (Exception e) {
//            application.makeLongToast("Fail: could not sync block chain.");
            e.printStackTrace();
        } finally {
            node.stopPeerGroup();
        }
    }

    private void handleImportBlock(Long import_height) {
        AbstractBlockChain chain = application.getBlockChain();
        Block complete_block = null;

        BitcoinNode node = new BitcoinNode(this, application);
        try{
            if(chain.getBestChainHeight() < import_height){
                // The local chain's height less than the requested block's height.
                node.startPeerGroup(chain);
                Long network_best_height = node.getNetworkHeight();

                if(network_best_height >= import_height){
                    // The network's height is greater than or equal to the requested block's height.
                    node.downloadBlockChain();

                } else {
                    // import_height exceeds network_best_height, invalid import_height.
                    node.stopPeerGroup();
                    return;
                }
            } else {
                // The local chain has the requested block.
                node.startPeerGroup(null);
            }

            // At this point, the PeerGroup is alive and the local block chain is synced.
            // Block import_height's header is within the local chain thus we possess its' hash.
            // We now request this block, using the hash, from the PeerGroup then shutdown the PeerGroup.

            StoredBlock targetBlock = getBlock(chain.getBlockStore(), import_height);
            Sha256Hash hash = targetBlock.getHeader().getHash();
            complete_block = node.downloadBlock(hash);

        } catch (Exception e) {
//            application.makeLongToast("Fail: could not import block.");
            e.printStackTrace();
        } finally {
            node.stopPeerGroup();
        }

        // Fantastic! If execution comes this far we have the complete_block at import_height.
        // PeerGroup has successfully shutdown.  Let us search complete_block for any relevant
        // transactions and appropriately deal with each transaction.

        if( complete_block != null ) {
            List<Transaction> relevantTxs = findRelevantTxs(complete_block);
            for(Transaction tx : relevantTxs) {
                ahimwall.commitTransaction(tx, import_height, true);
            }
        }

    }

    // Private Utilities ---------------------------------------------------------------------------
    private StoredBlock getBlock(BlockStore store, Long height){
        try {
            StoredBlock current = store.getChainHead();
            if (height > current.getHeight()) {
                return null;
            }
            for (int i = current.getHeight(); i > height; i -= 1) {
                StoredBlock previous = store.get(current.getHeader().getPrevBlockHash());
                current = previous;
            }
            return current;
        } catch (BlockStoreException e) {
            return null;
        }
    }

    private List<Transaction> findRelevantTxs(Block block) {
        List<Transaction> foundTxs = new ArrayList<Transaction>();
        for (Transaction tx : block.getTransactions()) {
            Log.d(TAG, tx.getHashAsString());
            if (isMyTx(tx, ahimwall.getKeyStore())){
                foundTxs.add(tx);
            }
        }
        return foundTxs;
    }

    public boolean isMyTx(Transaction tx, Wallet wallet) {
        // Determine if a transaction is relevant to a wallet's keys.
        for (TransactionOutput txOut : tx.getOutputs()) {
            if (txOut.isMine(wallet)) {
                return true;
            }
        }
        return false;
    }

}
