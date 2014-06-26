package io.ahimsa.ahimsa_app.application;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.application.service.NodeService;
import io.ahimsa.ahimsa_app.application.util.AhimsaDB;
import io.ahimsa.ahimsa_app.application.util.AhimsaWallet;
import io.ahimsa.ahimsa_app.application.util.BootlegTransaction;
import io.ahimsa.ahimsa_app.application.util.Utils;

/**
 * Created by askuck on 6/9/14.
 */
public class MainApplication extends Application {
    private static final String TAG = "MainApplication";
    //----------------------------------------------------------------------------------------------
    //Receiver Actions
    public static final String ACTION_HTTPS_SUCCESS  = MainApplication.class.getPackage().getName() + "https_success";
    public static final String EXTRA_TX_HEX_STRING   = MainApplication.class.getPackage().getName() + "tx_hex_string";
    public static final String EXTRA_TX_IN_COIN      = MainApplication.class.getPackage().getName() + "tx_in_coin";

    public static final String ACTION_HTTPS_FAILURE  = MainApplication.class.getPackage().getName() + "https_failure";
    public static final String EXTRA_EXCEPTION       = MainApplication.class.getPackage().getName() + "tx_in_coin";

    public static final String ACTION_BROADCAST_TX_SUCCESS = MainApplication.class.getPackage().getName() + "broadcast_tx_success";
    public static final String EXTRA_TX_FUTURE = MainApplication.class.getPackage().getName() + "broadcast_tx_txid";

    public static final String ACTION_BROADCAST_TX_FAILURE = MainApplication.class.getPackage().getName() + "broadcast_tx_failure";
    public static final String EXTRA_TX_BYTES   = MainApplication.class.getPackage().getName() + "tx_hex_bytes";


    //----------------------------------------------------------------------------------------------

    AhimsaWallet ahimwall;

    File walletFile;
    Wallet wallet;
    Configuration config;
    AhimsaDB db;

    File blockStoreFile;
    BlockStore store;
    BlockChain chain;

    @Override
    public void onCreate()
    {
        super.onCreate();

        //register receiver
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_HTTPS_SUCCESS);
        intentFilter.addAction(ACTION_HTTPS_FAILURE);
        intentFilter.addAction(ACTION_BROADCAST_TX_SUCCESS);
        intentFilter.addAction(ACTION_BROADCAST_TX_FAILURE);
        registerReceiver(serviceReceiver, intentFilter);

        //initialize wallet
        walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);
        loadWalletFromProtobuf();

        //initialize blockchain
        blockStoreFile = new File(getDir("blockstore", Context.MODE_PRIVATE), Constants.BLOCKSTORE_FILENAME);
        if( loadBlockChain() ){
            //todo: handle false case
//            NodeService.startActionSyncBlockchain(this);
        }

        //initialize config and database, initialize ahimsawallet
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        db = new AhimsaDB(this);
        ahimwall = new AhimsaWallet(this, wallet, db, config);

    }

    //todo: low memory / shutdown handle

    //----------------------------------------------------------------------------------------------
    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (ACTION_HTTPS_SUCCESS.equals(action)) {
                    String funded_tx = intent.getStringExtra(EXTRA_TX_HEX_STRING);
                    Long in_coin = intent.getLongExtra(EXTRA_TX_IN_COIN, 0);
                    successOnHttps(funded_tx, in_coin);
                }
                else if (ACTION_HTTPS_FAILURE.equals(action)) {
                    String e = intent.getStringExtra(EXTRA_EXCEPTION);
                    failureOnHttps(e);
                }
                else if (ACTION_BROADCAST_TX_SUCCESS.equals(action)){
                    byte[] future = intent.getByteArrayExtra(EXTRA_TX_FUTURE);
                    successOnBroadcastTx(future);
                }
                else if (ACTION_BROADCAST_TX_FAILURE.equals(action)){
                    byte[] tx = intent.getByteArrayExtra(EXTRA_TX_BYTES);
                    intent.getSerializableExtra(EXTRA_EXCEPTION);
                    failureOnBroadcastTx(tx);
                }
            }
        }
    };

    private void successOnHttps(String funded_tx, Long in_coin){

        Log.d(TAG, "funded_tx: " + funded_tx);
        Log.d(TAG, "  in_coin: " + in_coin);

        BootlegTransaction bootlegTx = new BootlegTransaction(Constants.NETWORK_PARAMETERS, Utils.hexToBytes(funded_tx));
        ECKey defKey = ahimwall.getDefaultECKey();
        BigInteger toSelf = Utils.satoshiToSelf(bootlegTx, in_coin, config.getFeeValue());

        TransactionOutput tout = new TransactionOutput(Constants.NETWORK_PARAMETERS, bootlegTx, toSelf, defKey.toAddress(Constants.NETWORK_PARAMETERS));
        bootlegTx.modifyOutput(1, tout);

        //add funding txid to configuration but DO NOT switch funded flag
        //this txid in config will be overwritten if unsuccessful broadcast
        config.setFundingTxid(bootlegTx.getHash().toString());

        try{
            ahimwall.broadcastTx(bootlegTx.toTransaction());
        }catch(Exception e){
            Log.d(TAG, "Broadcast function within ahimsaWallet threw an error: " + e);
        }

    }

    private void failureOnHttps(String e){
        Toast.makeText(this, "Uh oh! The funding server did not work as expected: " + e, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "But do not fear, a reattempt will automatically occur. //todo: implement automatic funding reattempt", Toast.LENGTH_LONG).show();
        //todo handle
    }

    private void successOnBroadcastTx(byte[] future){
        Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, future);

        Toast.makeText(this, "Successfully broadcast transaction: " + tx.getHashAsString(), Toast.LENGTH_LONG).show();
        ahimwall.commitTx(tx);
    }

    private void failureOnBroadcastTx(byte[] tx){
        Toast.makeText(this, "Failed to broadcast transaction. No further currently takes place.", Toast.LENGTH_LONG).show();
        //todo handle
    }




    //----------------------------------------------------------------------------------------------
    //PUBLIC METHODS--------------------------------------------------------------------------------
    public Configuration getConfig()
    {
        return config;
    }

    public AhimsaWallet getAhimsaWallet()
    {
        return ahimwall;
    }

    public AbstractBlockChain getBlockChain() {
        return chain;
    }

    public void broadcastBulletin(String topic, String message)
    {
//        ahimwall.broadcastBulletin(topic, message);

        //todo temporary
        Long h = new Long(topic);
        Log.d(TAG, "Discover Height: " + h);
        NodeService.startDiscoverTx(this, h);

    }

    public void toLog(){
        ahimwall.toLog();
        try{
            Log.d(TAG, "BEST BLOCKCHAIN HEIGHT | " + chain.getBestChainHeight());
        }catch (Exception e){
            Log.d(TAG, "application toLog() fail, chain blew something up: " + e.toString());
        }
    }

    //TODO: TEMPORARY, maybe not
    public long getCreationTime(){
        return wallet.getEarliestKeyCreationTime();
    }

    //----------------------------------------------------------------------------------------------
    //Load/Save Wallet------------------------------------------------------------------------------
    public void save()
    {
        try
        {
            protobufSerializeWallet(wallet);
        }
        catch (final IOException x)
        {
            throw new RuntimeException(x);
        }
    }

    //TODO: MAJOR: handle cases when loading wallet. a backup of everything.
    private void loadWalletFromProtobuf()
    {

        if(walletFile.exists())
        {
            final long start = System.currentTimeMillis();
            FileInputStream walletStream = null;

            try
            {
                walletStream = new FileInputStream(walletFile);
                wallet = new WalletProtobufSerializer().readWallet(walletStream);

                Log.d(TAG, "wallet loaded from: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
            }
            catch (final FileNotFoundException x)
            {
                Log.e(TAG, "problem loading wallet", x);
//                Toast.makeText(MainApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
//                wallet = restoreWalletFromBackup();
            }
            catch (final UnreadableWalletException x)
            {
                Log.e(TAG, "problem loading wallet", x);
//                Toast.makeText(MainApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
//                wallet = restoreWalletFromBackup();
            }
            finally
            {
                if (walletStream != null)
                {
                    try
                    {
                        walletStream.close();
                    }
                    catch (final IOException x)
                    {
                        // swallow
                    }
                }
            }

            if (!wallet.isConsistent())
            {
                Log.d(TAG, "problem loading wallet");
//                Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();
//                wallet = restoreWalletFromBackup();
            }

            if (!wallet.getParams().equals(Constants.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + wallet.getParams().getId());
        }
        else
        {
            wallet = new Wallet(Constants.NETWORK_PARAMETERS);
//            wallet.allowSpendingUnconfirmedTransactions();
        }


    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(walletFile);

        Log.d(TAG, "wallet saved to: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }

    //Load Blockchain-------------------------------------------------------------------------------
    //TODO: MAJOR: handle cases when loading blockchain.
    //todo: this is a mess
    private boolean loadBlockChain(){

        try {
            final boolean blockStoreFileExists = blockStoreFile.exists();
            store = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockStoreFile);
//            store.getChainHead(); // detect corruptions as early as possible

            final long earliestKeyCreationTime = wallet.getEarliestKeyCreationTime();
            Log.d(TAG, String.format("blockChainFileExists: %s | earliestKeyCreationTime %d", blockStoreFileExists, earliestKeyCreationTime));
            if (!blockStoreFileExists  && earliestKeyCreationTime > 0){
                try{
                    final InputStream checkpointsInputStream = getAssets().open(Constants.CHECKPOINTS_FILENAME);
                    CheckpointManager.checkpoint(Constants.NETWORK_PARAMETERS, checkpointsInputStream, store, earliestKeyCreationTime);
                }catch (final IOException x){
                    Log.e(TAG, "problem reading checkpoints, continuing without", x);
                }
            }
        } catch (BlockStoreException e) {
            blockStoreFile.delete();

            //todo: look here
            final String msg = "blockstore cannot be created";
            Log.d(TAG, msg);
            throw new Error(msg, e);
        }

        try {
            chain = new BlockChain(Constants.NETWORK_PARAMETERS, store);
            return true;
        } catch (BlockStoreException e) {
            throw new Error("blockchain cannot be created", e);
        }

    }


}

