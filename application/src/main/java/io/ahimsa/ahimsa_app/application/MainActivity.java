package io.ahimsa.ahimsa_app.application;

import android.app.Activity;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import io.ahimsa.ahimsa_app.application.fragment.NavigationDrawerFragment;
import io.ahimsa.ahimsa_app.application.service.OldNodeService;
import io.ahimsa.ahimsa_app.application.fragment.BulletinFragment;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {


    //TODO: convert fragment broadcast tx from oldnodeservice to nodeservice

    //MainActivity-------------------------------------------
    private static final String TAG = "MainActivity";
    private MainApplication application;

    //NavDrawer----------------------------------------------
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
//        registerReceiver(fragmentReceiver, intentFilter_fragment);

    }

    @Override
    protected void onDestroy(){
//        unregisterReceiver(fragmentReceiver);
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
        if (id == R.id.action_query_server) {
            Log.d(TAG, "Query Server");
            queryServer();
            return true;
        }
        else if (id == R.id.action_fresh_wallet) {
            Log.d(TAG, "Fresh wallet");
            freshWallet();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    //Receiver to communicate with fragment------------------
//    private final BroadcastReceiver fragmentReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action != null) {
//
//
//
//            }
//        }
//    };

    //-------------------------------------------------------

    //start node service
    private void queryServer(){
        application.queryServer();
    }

    private void freshWallet(){
        application.freshWallet();
    }

    //-------------------------------------------------------



}
