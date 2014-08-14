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
    MyPagerAdapter mypager;
    PagerSlidingTabStrip tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = (AhimsaApplication) getApplication();
        config = application.getConfig();

        pager = (ViewPager) findViewById(R.id.viewPager);
        mypager = new MyPagerAdapter(this, getFragmentManager());
        pager.setAdapter(mypager);
        pager.setCurrentItem(1);

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATED_OVERVIEW);
        filter.addAction(Constants.ACTION_UPDATED_QUEUE);
        filter.addAction(Constants.ACTION_UPDATED_LOG);
        filter.addAction(Constants.ACTION_UPDATED_BULLETIN);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);
    }

    // UpdateReceiver ------------------------------------------------------------------------------
    // todo split
    private BroadcastReceiver updateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(Constants.ACTION_UPDATED_OVERVIEW.equals(action))
            {
                mypager.updateOverview();
            }
            else if(Constants.ACTION_UPDATED_QUEUE.equals(action))
            {
                mypager.updateQueue();
            }
            else if(Constants.ACTION_UPDATED_LOG.equals(action))
            {
                mypager.updateLog();
            }
            else if(Constants.ACTION_UPDATED_BULLETIN.equals(action))
            {
                mypager.updateBulletin();
            }
        }
    };

//    private void handleAhimsaWalletUpdate() {
//        MyPagerAdapter adapter = (MyPagerAdapter) pager.getAdapter();
//        adapter.updateFragments();
//    }

    // Adapter -------------------------------------------------------------------------------------
    private class MyPagerAdapter extends FragmentPagerAdapter
    {
        Activity activity;
        private final SparseArray<Fragment> mPageReferences;
        private final String[] TITLES = { "Log", "Wallet", "Bulletins" };

        @Override
        public CharSequence getPageTitle(int position)
        {
            return TITLES[position];
        }

        public MyPagerAdapter(Activity activity, FragmentManager fm)
        {
            super(fm);
            mPageReferences = new SparseArray<Fragment>();
            this.activity = activity;
        }

        @Override
        public Fragment getItem(int index)
        {
            switch(index)
            {
                case 0:     LogListFragment frag0 = LogListFragment.newInstance();
                            mPageReferences.put(index, frag0);
                            return frag0;

                case 1:     OverviewFragment frag1 = OverviewFragment.newInstance(application.getUpdateBundle());
                            mPageReferences.put(index, frag1);
                            return frag1;

                default:    BulletinListFragment frag2 = BulletinListFragment.newInstance(activity, application.getBulletinCursor());
                            mPageReferences.put(index, frag2);
                            return frag2;
            }
        }

        @Override
        public int getCount()
        {
            return 3;
        }

        public Fragment getFragment(int key)
        {
            return mPageReferences.get(key);
        }

        public void updateOverview()
        {
            OverviewFragment overview_frag = (OverviewFragment) getFragment( 1 );
            if(overview_frag != null)
            {
                overview_frag.updateView( application.getUpdateBundle() );
            }
        }

        public void updateQueue()
        {
            LogListFragment log_frag = (LogListFragment) getFragment(0);
            if(log_frag != null)
            {
//                log_frag.up
            }
        }

        public void updateLog()
        {
            LogListFragment log_frag = (LogListFragment) getFragment(0);
        }

        public void updateBulletin()
        {
            BulletinListFragment bullet_frag = (BulletinListFragment) getFragment( 2 );
            if(bullet_frag != null)
            {
                bullet_frag.updateView( application.getBulletinCursor() );
            }
        }

        @Deprecated
        public void updateFragments()
        {
            LogListFragment log_frag            = (LogListFragment) getFragment(0);
            OverviewFragment overview_frag      = (OverviewFragment) getFragment( 1 );
            BulletinListFragment bullet_frag    = (BulletinListFragment) getFragment( 2 );

            if(log_frag != null)
            {
                // todo something
            }

            if(overview_frag != null)
            {
                overview_frag.updateView( application.getUpdateBundle() );
            }

            if(bullet_frag != null)
            {
                bullet_frag.updateView( application.getBulletinCursor() );
            }

        }

    }

    // Menu ----------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        setTitle(R.string.app_name);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            AhimsaService.startResetAhimsaWallet(this);
            AhimsaService.startSyncBlockChain(this);
            return true;
        }
        else if (id == R.id.action_create_bulletin)
        {
            Intent intent = new Intent(this, CreateBulletinActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
