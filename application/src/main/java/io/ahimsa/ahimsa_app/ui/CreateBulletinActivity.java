package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.core.AhimsaWallet;
import io.ahimsa.ahimsa_app.core.Utils;

public class CreateBulletinActivity extends Activity {

    AhimsaApplication application;
    AhimsaWallet ahimwall;
    Configuration config;
    CreateBulletinFragment frag;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d("CB", "ONSAVEDINSTANCESTATE");
    }

    @Override
    public void onRestart() {
        Log.d("CB", "ON RESTART");
        super.onRestart();
    }

    @Override
    public void onStart() {
        Log.d("CB", "ONSTART");
        super.onStart();
    }

    @Override
    public void onResume(){
        Log.d("CB", "ONRESUME");
        super.onResume();
    }

    @Override
    public void onPause(){
        Log.d("CB", "ONPAUSE");
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onPause();
    }

    @Override
    public void onStop(){
        Log.d("CB", "ONSTOP");
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d("CB", "ONDESTROY");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        application = (AhimsaApplication) getApplication();
        ahimwall = application.getAhimsaWallet();
        config = application.getConfig();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bulletin);

        if(savedInstanceState == null) {
            frag = CreateBulletinFragment.newInstance(application.getUpdateBundle());

            getFragmentManager().beginTransaction()
                    .add(R.id.container, frag)
                    .commit();
        }

        IntentFilter filter = new IntentFilter(Constants.ACTION_AHIMWALL_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);

    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Constants.ACTION_AHIMWALL_UPDATE.equals(action)){
                handleAhimsaWalletUpdate();
            }
        }
    };

    private void handleAhimsaWalletUpdate() {
        if(frag != null) {
            frag.updateView(application.getUpdateBundle());
        }
    }
//
//    @Override
//    public void finish() {
//        super.finish();
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_bulletin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_broadcast_bulletin) {

            Bundle bulletin_bundle = frag.getBulletinBundle();
            String topic   = bulletin_bundle.getString(frag.EXTRA_STRING_TOPIC);
            String message = bulletin_bundle.getString(frag.EXTRA_STRING_MESSAGE);

            Long estimate = Utils.getEstimatedCost(config.getFeeValue(), config.getDustValue(), topic.length(), message.length());
            Log.d("createdBulletinActivity", "estimated cost: " + estimate);

            if(config.getMinCoinNecessary() <= ahimwall.getConfirmedBalance(true)) {
                AhimsaService.startBroadcastBulletin(this, topic, message, config.getFeeValue());
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Womp! Not enough coin.");
                builder.setMessage(String.format("%s confirmed Satoshis are required to create a bulletin. \n\nOur apologies, a future version will not have this limitation...", config.getMinCoinNecessary()));
                final AlertDialog dialog = builder.create();
                dialog.show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Configuration getConfig() {
        return config;
    }
}
