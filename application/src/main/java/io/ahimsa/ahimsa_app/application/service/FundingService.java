package io.ahimsa.ahimsa_app.application.service;

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

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;


public class FundingService extends IntentService {
    private static final String TAG = "NodeService";

    //Requets Funded Transaction
    private static final String ACTION_REQUEST_FUNDED_TX = NodeService.class.getPackage().getName() + ".request_funded_tx";
    private static final String EXTRA_URL = NodeService.class.getPackage().getName() + ".url";

    //--------------------------------------------------------------------------------
    public FundingService() {
        super("FundingService");
    }
    //--------------------------------------------------------------------------------
    public static void startActionRequestFundedTx(Context context, String url) {
        Intent intent = new Intent(context, FundingService.class);
        intent.setAction(ACTION_REQUEST_FUNDED_TX);
        intent.putExtra(EXTRA_URL, url);
        context.startService(intent);
    }

    //--------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            if(ACTION_REQUEST_FUNDED_TX.equals(action))
            {
                String url = intent.getStringExtra(EXTRA_URL);
                requestFundedTx(url);
            }
        }
    }

    private void requestFundedTx(String url)
    {
        //todo: add to constants/config
        Map<String,String> httpParams = new HashMap<String,String>(1);
        httpParams.put("Secret", "bilbo has the ring");


        try {
            //todo: add to constants/config
            String responseString = httpsConnection(url, httpParams);
            JSONTokener tokener = new JSONTokener(responseString);
            JSONObject finalResult = new JSONObject(tokener);
            String funded_tx = finalResult.getString("Tx");
            Long in_coin = finalResult.getLong("Value");

            successOnHttps(funded_tx, in_coin);

        } catch (Exception e) {
            failureOnHttps(e);
        }

    }
    //--------------------------------------------------------------------------------

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
            } finally {
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
            //do something with response
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

    private void successOnHttps(String funded_tx, Long in_coin)
    {
        MainApplication application = (MainApplication) getApplication();
        Intent intent = new Intent().setAction(application.ACTION_HTTPS_SUCCESS);
        intent.putExtra(application.EXTRA_TX_HEX_STRING, funded_tx);
        intent.putExtra(application.EXTRA_TX_IN_COIN, in_coin);
        application.sendBroadcast(intent);
    }

    private void failureOnHttps(Exception e)
    {
        MainApplication application = (MainApplication) getApplication();
        Intent intent = new Intent().setAction(application.ACTION_HTTPS_FAILURE);
        intent.putExtra(application.EXTRA_EXCEPTION, e.toString());
        application.sendBroadcast(intent);
    }





}
