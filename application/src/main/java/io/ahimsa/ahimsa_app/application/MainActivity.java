package io.ahimsa.ahimsa_app.application;

import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import io.ahimsa.ahimsa_app.application.ui.BulletinListFragment;
import io.ahimsa.ahimsa_app.application.ui.CreateBulletinFragment;
import io.ahimsa.ahimsa_app.application.ui.ImportFundingTxFragment;
import io.ahimsa.ahimsa_app.application.ui.NavigationDrawerFragment;
import io.ahimsa.ahimsa_app.application.ui.SettingsFragment;
import io.ahimsa.ahimsa_app.application.ui.TransactionListFragment;
import io.ahimsa.ahimsa_app.application.ui.TransactionOutputListFragment;


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

        application = (MainApplication) getApplication();
        setContentView(R.layout.activity_main);

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
        Fragment fragment = null;
        //todo do this properly, doesn't reset fragment

        switch (position){
            case 0:
                mTitle = getString(R.string.title_section1);
                fragment = new CreateBulletinFragment();
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                fragment = new BulletinListFragment();
                break;
            case 2:
                mTitle = getString(R.string.title_section3);
                fragment = new TransactionListFragment();
                break;
            case 3:
                mTitle = getString(R.string.title_section4);
                fragment = new TransactionOutputListFragment();
                break;
            case 4:
                mTitle = getString(R.string.title_section5);
                fragment = new ImportFundingTxFragment();
                break;
            case 5:
                mTitle = getString(R.string.title_section6);
                fragment = new SettingsFragment();
                break;
        }

        if(fragment != null){
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

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
        application.toLog();
    }

    private void resetWallet(){
        application.reset();
    }

    //-------------------------------------------------------



}
