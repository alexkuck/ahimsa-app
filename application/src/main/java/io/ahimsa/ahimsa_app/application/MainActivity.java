package io.ahimsa.ahimsa_app.application;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Application;
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
import io.ahimsa.ahimsa_app.application.util.AhimsaWallet;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

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

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getFragmentManager();

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

    //todo: you may not need this:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_util_one) {
            printWallet();
            return true;
        }
        else if (id == R.id.action_util_two) {
            resetWallet();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    //-------------------------------------------------------

    //start node service
    private void printWallet(){
        application.getAhimsaWallet().toLog();
    }

    private void resetWallet(){
        application.getAhimsaWallet().reset();
    }

    //-------------------------------------------------------



}
