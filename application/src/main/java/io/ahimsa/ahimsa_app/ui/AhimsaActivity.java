package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import android.support.v4.view.ViewPager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.astuetz.PagerSlidingTabStrip;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.fund.FundService;

public class AhimsaActivity extends Activity {

    AhimsaApplication application;
    Configuration config;
    ViewPager pager;
    PagerSlidingTabStrip tabs;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d("AA", "ONSAVEDINSTANCESTATE");
    }

    @Override
    public void onRestart() {
        Log.d("AA", "ON RESTART");
        super.onRestart();
    }

    @Override
    public void onStart() {
        Log.d("AA", "ONSTART");
        super.onStart();
    }

    @Override
    public void onResume(){
        Log.d("AA", "ONRESUME");
        super.onResume();
    }

    @Override
    public void onPause(){
        Log.d("AA", "ONPAUSE");
        super.onPause();
    }

    @Override
    public void onStop(){
        Log.d("AA", "ONSTOP");
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d("AA", "ONDESTROY");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("AA", "ONCREATE");
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = (AhimsaApplication) getApplication();
        config = application.getConfig();

        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(new MyPagerAdapter(this, getFragmentManager()));

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);

        IntentFilter filter = new IntentFilter(Constants.ACTION_AHIMWALL_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);


    }

    // UpdateReceiver ------------------------------------------------------------------------------
    // todo split
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
        MyPagerAdapter adapter = (MyPagerAdapter) pager.getAdapter();
        adapter.updateFragments();
    }

    // Adapter -------------------------------------------------------------------------------------
    private class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = { "Wallet", "Bulletins"};

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }


        Activity activity;
        private final SparseArray<Fragment> mPageReferences;

        public MyPagerAdapter(Activity activity, FragmentManager fm) {
            super(fm);
            mPageReferences = new SparseArray<Fragment>();
            this.activity = activity;
        }

        @Override
        public Fragment getItem(int index) {
            switch(index) {
                case 0:     OverviewFragment frag0 = OverviewFragment.newInstance(application.getUpdateBundle());
                            mPageReferences.put(index, frag0);
                            return frag0;

//                case 1:     OverviewFragment frag1 = OverviewFragment.newInstance(new Bundle());
//                            mPageReferences.put(index, frag1);
//                            return frag1;

                default:    BulletinListFragment frag2 = BulletinListFragment.newInstance(activity, application.getBulletinCursor());
                            mPageReferences.put(index, frag2);
                            return frag2;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        public Fragment getFragment(int key) {
            return mPageReferences.get(key);
        }

        public void updateFragments() {

            OverviewFragment overview_frag = (OverviewFragment) getFragment( 0 );
            BulletinListFragment list_frag = (BulletinListFragment) getFragment( 1 );

            if(overview_frag != null)
            {
                overview_frag.updateView( application.getUpdateBundle() );
            }

            if(list_frag != null)
            {
                list_frag.update( application.getBulletinCursor() );
            }

        }

    }

    // Menu ----------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        setTitle(R.string.app_name);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            AhimsaService.startResetAhimsaWallet(this);
            AhimsaService.startSyncBlockChain(this);
            return true;
        } else if (id == R.id.action_create_bulletin) {
            Intent intent = new Intent(this, CreateBulletinActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
