package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import android.support.v4.view.ViewPager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v13.app.FragmentPagerAdapter;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.fund.FundService;

public class MainActivity extends Activity {

    AhimsaApplication application;
    Configuration config;
    ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = (AhimsaApplication) getApplication();
        config = application.getConfig();

        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(new MyPagerAdapter(getFragmentManager()));

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
        MyPagerAdapter adapter = (MyPagerAdapter) pager.getAdapter();
        OverviewFragment frag = (OverviewFragment) adapter.getFragment( 0 );
        if(frag != null){
            frag.updateView(application.getUpdateBundle());
        }

//        OverviewFragment frag2 = (OverviewFragment) adapter.getFragment( 1 );
//        frag2.updateView(application.getUpdateBundle());
    }


    private class MyPagerAdapter extends FragmentPagerAdapter {
        private final SparseArray<Fragment> mPageReferences;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            mPageReferences = new SparseArray<Fragment>();
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

                default:    OverviewFragment frag2 = OverviewFragment.newInstance(application.getUpdateBundle());
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


    }


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
//            FundService.startActionRequestFundingTx(this, config.getFundingIP(), config.getDefaultAddress());
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
