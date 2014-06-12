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
    private static final String TAG = "MainApplication";

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

        //check if wallet has a key, create and add new key if DNE.
        ensureKey();

        //check if wallet has transactions, fund address if DNE.
        ensureCoin();

        //No autosave wallet listener exists, must manually save!
        saveWallet();

    }

    public void queryServer(){
    //todo: this is temporary
        NodeService.startNetworkTest(this);
    }

    public void freshWallet(){
    //todo: this is temporary
        Log.d(TAG, wallet.toString());
        wallet.clearTransactions(0);
        saveWallet();
        Log.d(TAG, wallet.toString());

    }
    //---------------------------------------------------------------------------------

    //PUBLIC METHODS-------------------------------------------------------------------
    public Configuration getConfig()
    {
        return config;
    }

    public Wallet getWallet()
    {
        return wallet;
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

    public ECKey getDefaultECKey()
    {
        String defaultaddr = config.getDefaultAddress();

        if( defaultaddr == null ){
            throw new NullPointerException("Default key is null");
        }

        for(ECKey key : wallet.getKeys()){
            if( key.toAddress(Constants.NETWORK_PARAMETERS).toString().equals(defaultaddr))
                return key;
        }

        //todo: default key is not in wallet, throw error,
        return null;

    }

    //PRIVATE METHODS-------------------------------------------------------------------
    private void ensureKey()
    {
        if(wallet.getKeys().size() > 0)
            return;

        Log.d(TAG, "Wallet has no key - creating");
        addNewKeyToWallet();
    }

    private void addNewKeyToWallet()
    {
        ECKey key = new ECKey();

        wallet.addKey(key);
        config.setDefaultAddress( key.toAddress(Constants.NETWORK_PARAMETERS).toString() );

        saveWallet();
    }

    private void ensureCoin()
    {

        //TODO: a lot here, cases:
        //initial install requiring funding from server
        //funds getting low
        //used all coin (may not be zero, but functionally zero)

//        if( !wallet.getBalance().equals(BigInteger.ZERO) )
        if( wallet.getTransactions(true).size() > 0)
        {
            Log.d(TAG, "Wallet has transaction");
            return;
        }

        fundWallet();
    }

    private void fundWallet()
    {
        Log.d(TAG, "Requesting service to fund address");

        NodeService.startActionFundFreshWallet(this);

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
//            wallet.allowSpendingUnconfirmedTransactions();
        }


    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(walletFile);

        Log.d(TAG, "wallet saved to: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }


}
