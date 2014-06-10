package io.ahimsa.ahimsa_app.application;

import android.app.Activity;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import io.ahimsa.ahimsa_app.application.fragment.NavigationDrawerFragment;
import io.ahimsa.ahimsa_app.application.service.OldNodeService;
import io.ahimsa.ahimsa_app.application.fragment.BulletinFragment;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {


    //TODO: convert fragment broadcast tx from oldnodeservice to nodeservice

    //MainActivity-------------------------------------------
    private static final String TAG = MainActivity.class.toString();
    private MainApplication application;

    //NavDrawer----------------------------------------------
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, ".onCreate()");

        super.onCreate(savedInstanceState);

        //Get application
        application = (MainApplication) getApplication();

        setContentView(R.layout.activity_main);

        //Navigation Drawer Nonsense----------------------------------------------
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));


        //Set up intent for fragment receiver-----------------------------------
        final IntentFilter intentFilter_fragment = new IntentFilter();
        intentFilter_fragment.addAction(OldNodeService.ACTION_BROADCAST_TRANSACTION);
        registerReceiver(fragmentReceiver, intentFilter_fragment);

    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(fragmentReceiver);
        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragment
        FragmentManager fragmentManager = getFragmentManager();
        Log.d(TAG, "Position: " + position);

        switch (position){
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;

        }

        fragmentManager.beginTransaction()
                .replace(R.id.container, new BulletinFragment())
                .commit();
    }



    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
//        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_nodeservice) {
            Log.d(TAG, "Node Service Toggle");
            testNodeService();
            return true;
        }

        if (id == R.id.action_peercount) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Receiver to communicate with fragment------------------
    private final BroadcastReceiver fragmentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (OldNodeService.ACTION_BROADCAST_TRANSACTION.equals(action)){
                    broadcastTx(intent);
                }
            }
        }
    };

    //-------------------------------------------------------

    //start node service
    private void testNodeService(){
        application.testPeerCount();
    }

    //send byte array to node service for broadcast
    private void broadcastTx(Intent intent_from_fragment){
        final String tx_hex_string = intent_from_fragment.getStringExtra(OldNodeService.HEX_STRING_TRANSACTION);

        Intent intent_to_NodeService = new Intent(this, OldNodeService.class);
        intent_to_NodeService.setAction(OldNodeService.ACTION_BROADCAST_TRANSACTION);

        intent_to_NodeService.putExtra(OldNodeService.BYTE_ARRAY_TRANSACTION, hexStringToByteArray(tx_hex_string));

        startService(intent_to_NodeService);
    }

    //-------------------------------------------------------

    //good utility method. hex string to byte array. eventually will live within message library.
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



}
