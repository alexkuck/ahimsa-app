package io.ahimsa.ahimsa_app.fund;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.core.Utils;


public class FundService extends IntentService {
    private static final String TAG = "FundService";

    //Requets Funded Transaction
    private static final String ACTION_REQUEST_FUNDED_TX = FundService.class.getPackage().getName() + ".request_funded_tx";
    private static final String ACTION_REQUEST_FUNDED_TX_USE_CONFIG = FundService.class.getPackage().getName() + ".request_funded_tx_use_config";
    private static final String EXTRA_URL = FundService.class.getPackage().getName() + ".url";
    private static final String EXTRA_ADDRESS = FundService.class.getPackage().getName() + ".address";

    public static void startRequestFundingTxUseConfig(Context context) {
        Intent intent = new Intent(context, FundService.class);
        intent.setAction(ACTION_REQUEST_FUNDED_TX_USE_CONFIG);
        context.startService(intent);
    }

    public static void startRequestFundingTx(Context context, String url, String address) {
        Intent intent = new Intent(context, FundService.class);
        intent.setAction(ACTION_REQUEST_FUNDED_TX);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_ADDRESS, address);
        context.startService(intent);
    }

    public FundService() {
        super("FundService");
    }

   @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if(ACTION_REQUEST_FUNDED_TX.equals(action)) {
                String url = intent.getStringExtra(EXTRA_URL);
                String address = intent.getStringExtra(EXTRA_ADDRESS);
                handleRequestFundingTx(url, address);
            }

            if(ACTION_REQUEST_FUNDED_TX_USE_CONFIG.equals(action)) {
                AhimsaApplication application = (AhimsaApplication) getApplication();
                Configuration config = application.getConfig();
                handleRequestFundingTx(config.getFundingIP(), config.getDefaultAddress());
            }

        }
    }

    //--------------------------------------------------------------------------------
    private void handleRequestFundingTx(String url, String address)
    {
        //todo: add to constants/config
        Map<String,String> httpParams = new HashMap<String,String>(1);
        httpParams.put("Secret", "bilbo has the ring");
        httpParams.put("Address", address);

        try {
            String responseString = httpsConnection(url, httpParams);
            Log.d(TAG, "responseString | " + responseString);
            JSONTokener tokener = new JSONTokener(responseString);
            JSONObject finalResult = new JSONObject(tokener);
            String funded_tx = finalResult.getString("Tx");

            successOnHttps(funded_tx);

        } catch (Exception e) {
            e.printStackTrace();
            failureOnHttps(e);
        }

    }
    // Private Utilities----------------------------------------------------------------------------
    private String httpsConnection(String path, Map params) throws Exception
    {
        //From the example at: http://developer.android.com/training/articles/security-ssl.html

        SSLContext context = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getResources().openRawResource(R.raw.cert);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                Log.d(TAG, "0/8|Certificate: " + ((X509Certificate) ca).getSubjectDN());
            }catch(Exception x){
                Log.d(TAG, "Exception in certificate generation: " + x.toString());
                throw x;
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
            context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
        } catch (Exception x){
            Log.d(TAG, "Exception in https initiation." + x.toString());
            x.printStackTrace();
            throw x;
        }


        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException x) {
            Log.d(TAG, "Check out your url, my friend: " + path + x.toString());
            x.printStackTrace();
            throw x;
        }

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
            InputStream inStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String result = reader.readLine();

            Log.d(TAG, "7/8|Close outstream, instream, and conn...");
            outStream.close();
            inStream.close();
            conn.disconnect();

            Log.d(TAG, "8/8|Success, returning result.");
            return result;

        } catch (Exception x){
            Log.d(TAG, "Exception in httpsConnection: " + x.toString());
            throw x;
        }

    }

    private void successOnHttps(String funded_tx)
    {
        Log.d(TAG, "funded_tx | " + funded_tx);
        AhimsaService.startBroadcastTx(this, Utils.hexToBytes(funded_tx), true);
    }

    private void failureOnHttps(Exception e)
    {
        //nothing
    }





}
