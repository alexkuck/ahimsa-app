package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore;
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
import io.ahimsa.ahimsa_app.core.AhimsaWallet;
import io.ahimsa.ahimsa_app.core.Utils;

public class CreateBulletinActivity extends Activity
{
    AhimsaApplication application;
    AhimsaWallet ahimwall;
    Configuration config;
    CreateBulletinFragment frag;

    @Override
    public void onPause()
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        application = (AhimsaApplication) getApplication();
        ahimwall = application.getAhimsaWallet();
        config = application.getConfig();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bulletin);

        if(savedInstanceState == null)
        {
            frag = CreateBulletinFragment.newInstance(application.getUpdateBundle());

            getFragmentManager().beginTransaction()
                    .add(R.id.container, frag)
                    .commit();
        }

        IntentFilter filter = new IntentFilter(Constants.ACTION_UPDATED_OVERVIEW);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);

    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(Constants.ACTION_UPDATED_OVERVIEW.equals(action))
            {
                updateOverview();
            }
        }
    };

    private void updateOverview()
    {
        if(frag != null)
        {
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_bulletin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_broadcast_bulletin)
        {
            Bundle bulletin_bundle = frag.getBulletinBundle();
            String topic   = bulletin_bundle.getString(frag.EXTRA_STRING_TOPIC);
            String message = bulletin_bundle.getString(frag.EXTRA_STRING_MESSAGE);

            Long estimate = Utils.getEstimatedCost(Constants.MIN_FEE, Constants.MIN_DUST, topic.length(), message.length());
            Log.d("createdBulletinActivity", "estimated cost: " + estimate);

            if(Constants.getStandardCoin() <= ahimwall.getConfirmedBalance(false))
            {
                AhimsaService.startBroadcastBulletin(this, topic, message, Constants.MIN_FEE);
                Toast.makeText(this, "Broadcast bulletin request.\ntopic: " + topic + "\nestimated cost: " + estimate + " Satoshis", Toast.LENGTH_LONG).show();
                finish();
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Womp! Not enough coin.");
                builder.setMessage(String.format("%s confirmed Satoshis are required to create a bulletin. \n\nOur apologies, a future version will not have this limitation...", Constants.getStandardCoin()));
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
            return true;
        }

        if (id == R.id.action_camera)
        {
            // This intent presents applications that allow the user to choose a picture
            Intent pickIntent = new Intent();
            pickIntent.setType("image/*");
            pickIntent.setAction(Intent.ACTION_GET_CONTENT);

            // This intent launches the camera application to take a new picture
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // This intent prompts the user to choose from a list of possible intents.
            String pickTitle = "Select or Take a new Picture";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
            chooserIntent.putExtra
                    (
                            Intent.EXTRA_INITIAL_INTENTS,
                            new Intent[] { takePhotoIntent }
                    );

            startActivityForResult(chooserIntent, 123);
        }

        return super.onOptionsItemSelected(item);
    }

    public Configuration getConfig()
    {
        return config;
    }
}
