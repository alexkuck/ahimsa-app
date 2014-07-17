package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;

public class CreateBulletinActivity extends Activity {

    AhimsaApplication application;
    Configuration config;
    CreateBulletinFragment frag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        application = (AhimsaApplication) getApplication();
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

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
            long estimated_cost  = bulletin_bundle.getLong(frag.EXTRA_LONG_ESTIMATED_COST);
            String topic   = bulletin_bundle.getString(frag.EXTRA_STRING_TOPIC);
            String message = bulletin_bundle.getString(frag.EXTRA_STRING_MESSAGE);

            Log.d("createdBulletinActivity", "estimated cost: " + estimated_cost);
            Log.d("createdBulletinActivity", "confirmed bala: " + config.getTempConfBalance());

            // TODO MAJOR | prevent back-to-back bulletin creation
            // TODO MAJOR | use tempory array of txout values, remove txout-value when bulletin submitted
            // TODO MAJOR | refresh this array of txout-vals after each ahimsa_service completion
            if(estimated_cost <= config.getTempConfBalance()){
                config.setTempConfBalance( config.getTempConfBalance() - estimated_cost);
                AhimsaService.startBroadcastBulletin(this, topic, message, config.getFeeValue());
            } else {
                Toast.makeText(this, R.string.toast_insufficient_funds, Toast.LENGTH_LONG).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Configuration getConfig() {
        return config;
    }
}
