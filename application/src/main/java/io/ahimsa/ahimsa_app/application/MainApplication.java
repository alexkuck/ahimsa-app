package io.ahimsa.ahimsa_app.application;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;
import com.google.bitcoin.wallet.WalletFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.application.service.NodeService;

/**
 * Created by askuck on 6/9/14.
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.toString();

    private Configuration config;

    private File walletFile;
    private Wallet wallet;
    private PackageInfo packageInfo;


    @Override
    public void onCreate(){

        super.onCreate();

        //TODO: use the configuration file
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));

        //get wallet file name and load wallet. create new wallet if DNE.
        walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);
        loadWalletFromProtobuf();

        //add wallet listener for auto saving wallet on updates
        //wallet.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, new WalletAutosaveEventListener());
        //the autosaveToFileListener must be removed before app shutdown, where should this be placed?
        //thus you MUST remember to call save wallet after all modifications.

        //check if wallet has a key, create and add new key if DNE.
        ensureKey();

        //check if wallet has a funded address, fund address if DNE.
        ensureCoin();

    }

    //send raw transaction via node intent service
    public void testPeerCount(){
        NodeService.startActionTestPeercount(this);

    }
    //send raw transaction via node intent service
//    public void broadcast(byte[] rawTx){
//        NodeService.startActionBroadcast(this, rawTx);
//
//    }

    //---------------------------------------------------------------------------------

    public Configuration getConfig()
    {
        return config;
    }

    public Wallet getWallet()
    {
        return wallet;
    }


    private static final class WalletAutosaveEventListener implements WalletFiles.Listener
    {
        @Override
        public void onBeforeAutoSave(final File file)
        {
        }

        @Override
        public void onAfterAutoSave(final File file)
        {
        }
    }

    private void ensureKey(){
        if(wallet.getKeys().size() > 0)
            return;

        Log.d(TAG, "Wallet has no usable key - creating");
        addNewKeyToWallet();
    }

    private void addNewKeyToWallet(){
        wallet.addKey(new ECKey());
        saveWallet();
    }

    private void ensureCoin(){

        //TODO: alot here, cases:
        //initial install requiring funding from server
        //funds getting low
        //used all coin (may not be zero, but functionally zero)

        if( !wallet.getBalance().equals(BigInteger.ZERO) )
        {
            Log.d(TAG, "Wallet has funds");
            return;
        }

        fundWallet();
    }

    private void fundWallet(){
        Log.d(TAG, "Requesting service to fund address");

        NodeService.startActionFundWallet(this);

    }

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
                Toast.makeText(MainApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

//                wallet = restoreWalletFromBackup();
            }
            catch (final UnreadableWalletException x)
            {
                Log.e(TAG, "problem loading wallet", x);
                Toast.makeText(MainApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

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
                Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();

//                wallet = restoreWalletFromBackup();
            }

            if (!wallet.getParams().equals(Constants.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + wallet.getParams().getId());
        }
        else
        {
            wallet = new Wallet(Constants.NETWORK_PARAMETERS);
        }


    }

    public void saveWallet()
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

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(walletFile);

        Log.d(TAG, "wallet saved to: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }
}
