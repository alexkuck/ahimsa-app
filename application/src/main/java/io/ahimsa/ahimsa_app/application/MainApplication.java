package io.ahimsa.ahimsa_app.application;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;

import javax.annotation.Nonnull;

import io.ahimsa.ahimsa_app.application.util.AhimsaDB;
import io.ahimsa.ahimsa_app.application.util.AhimsaWallet;
import io.ahimsa.ahimsa_app.application.util.BootlegTransaction;
import io.ahimsa.ahimsa_app.application.util.Utils;

/**
 * Created by askuck on 6/9/14.
 */
public class MainApplication extends Application {
    private static final String TAG = "MainApplication";
    //----------------------------------------------------------------------------------------
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


    //----------------------------------------------------------------------------------------

    AhimsaWallet ahimwall;
    Wallet wallet;
    Configuration config;
    AhimsaDB db;
    File walletFile;

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

        //initialize config and database
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        db = new AhimsaDB(this);


        //initialize ahimsawallet
        ahimwall = new AhimsaWallet(this, wallet, db, config);

    }

    //todo: low memory / shutdown handle

    //--------------------------------------------------------------------------------
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

        try{
            ahimwall.broadcastFundingTx(bootlegTx.toTransaction());
        }catch(Exception e){
            Log.d(TAG, "Broadcast function within ahimsaWallet threw an error: " + e);
        }

    }

    private void failureOnHttps(String e){
        Toast.makeText(this, "Uh oh! The funding server did not work as expected: " + e, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "But do not fear, a reattempt will automatically occur. //todo: implement automatic funding reattempt", Toast.LENGTH_LONG).show();
    }

    private void successOnBroadcastTx(byte[] future){
        Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, future);

        Toast.makeText(this, "Successfully broadcast transaction: " + tx.getHashAsString(), Toast.LENGTH_LONG).show();
        ahimwall.successOnBroadcastTx(tx);
    }

    private void failureOnBroadcastTx(byte[] tx){
        //todo handle failure on broadcast
        Toast.makeText(this, "Failed to broadcast transaction. No further currently takes place.", Toast.LENGTH_LONG).show();
    }




    //--------------------------------------------------------------------------------
    //PUBLIC METHODS------------------------------------------------------------------
    public Configuration getConfig()
    {
        return config;
    }

    public AhimsaWallet getAhimsaWallet()
    {
        return ahimwall;
    }

    public void broadcastBulletin(String topic, String message)
    {
        ahimwall.broadcastBulletin(topic, message);
    }

    //--------------------------------------------------------------------------------
    //Load/Save Wallet----------------------------------------------------------------
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

    //Load ----------------------------------------------------------------


}

