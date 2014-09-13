package io.ahimsa.ahimsa_app;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.ahimsa.ahimsa_app.core.AhimsaLog;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.core.AhimsaWallet;
import io.ahimsa.ahimsa_app.core.AhimsaDB;

/**
 * Created by askuck on 7/11/14.
 */
//todo | implements AbstractAhimsaApplication
public class AhimsaApplication extends Application
{
    public String TAG = "AhimsaApplication";
    private Configuration config;

    private AhimsaWallet ahimwall;
    private AhimsaLog ahimlog;

    private File blockStoreFile;
    private BlockStore store;
    private BlockChain chain;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // initialize AhimsaLog
        ahimlog = new AhimsaLog(this);

        // initialize data stores: config and db
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        AhimsaDB db = new AhimsaDB(this);

        // create ahimsa wallet, verify.
        ahimwall = new AhimsaWallet(config, db);
//        AhimsaService.startNetworkTest(this);

        // load block chain, sync block chain.
        blockStoreFile = new File(getDir("blockstore", Context.MODE_PRIVATE), Constants.BLOCKSTORE_FILENAME);
        if( loadBlockChain() )
        {
            //todo | handle false case
            if( config.syncBlockChainAtStartup())
            {
                AhimsaService.startSyncBlockChain(this);
            }
        }

    }

    // Getters--------------------------------------------------------------------------------------
    public Configuration getConfig()
    {
        return config;
    }

    public AhimsaWallet getAhimsaWallet()
    {
        return ahimwall;
    }

    public AhimsaLog getAhimsaLog()
    {
        return ahimlog;
    }

    public AbstractBlockChain getBlockChain()
    {
        return chain;
    }

    // Public Utilities-----------------------------------------------------------------------------
    public Bundle getUpdateBundle()
    {
        Bundle update_bundle = ahimwall.getUpdateBundle();
        update_bundle.putLong(Constants.EXTRA_LONG_NET_HEIGHT, config.getHighestBlockSeen());
        update_bundle.putLong(Constants.EXTRA_LONG_LOCAL_HEIGHT, chain.getBestChainHeight());

        return update_bundle;
    }

    public Cursor getBulletinCursor()
    {
        return ahimwall.getBulletinCursor();
    }

    public Cursor getOutPointCursor()
    {
        return ahimwall.getOutPointCursor();
    }

    // Load Block Chain-----------------------------------------------------------------------------
    private boolean loadBlockChain()
    {
        try
        {
            final boolean blockStoreFileExists = blockStoreFile.exists();
            store = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockStoreFile);
            //store.getChainHead(); // detect corruptions as early as possible

            final long earliestKeyCreationTime = ahimwall.getEarliestKeyCreationTime();
            Log.d(TAG, String.format("blockChainFileExists: %s | earliestKeyCreationTime %d", blockStoreFileExists, earliestKeyCreationTime));

            if (!blockStoreFileExists  && earliestKeyCreationTime > 0)
            {
                try
                {
                    final InputStream checkpointsInputStream = getAssets().open(Constants.CHECKPOINTS_FILENAME);
                    CheckpointManager.checkpoint(Constants.NETWORK_PARAMETERS, checkpointsInputStream, store, earliestKeyCreationTime);
                }
                catch (final IOException x)
                {
                    Log.e(TAG, "problem reading checkpoints, continuing without", x);
                }
            }
        }
        catch (BlockStoreException e)
        {
            blockStoreFile.delete();

            //todo: look here
            final String msg = "blockstore cannot be created";
            Log.d(TAG, msg);
            throw new Error(msg, e);
        }

        try
        {
            chain = new BlockChain(Constants.NETWORK_PARAMETERS, store);
            return true;
        }
        catch (BlockStoreException e)
        {
            throw new Error("blockchain cannot be created", e);
        }
    }

}
