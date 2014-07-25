package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaDB;

public class BulletinListFragment extends ListFragment {

    AhimsaApplication application;
//    SimpleCursorAdapter mAdapter;

    public BulletinListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity) {
        this.application = (AhimsaApplication) activity.getApplication();
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //[_id, txid, sent_time, confirmed, highest_block, topic, message, txout_total, txout_count]
//        mAdapter = new SimpleCursorAdapter(getActivity(),
//                R.layout.listview_item_bulletin,
//                application.getBulletinCursor(),
//                new String[] {"_id", AhimsaDB.txid, AhimsaDB.sent_time, AhimsaDB.confirmed, AhimsaDB.highest_block, AhimsaDB.topic, AhimsaDB.message, AhimsaDB.txout_total, AhimsaDB.txout_count},
//                new int[] {-1, -1, R.id.broadcast_time_value, R.id.confirmed_value, -1, R.id.topic_value, R.id.message_value, R.id.cost_value, R.id.change_value}, 0);
//
//
//        setListAdapter(mAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    public static BulletinListFragment newInstance(Activity activity, Cursor cursor)
    {
        BulletinListFragment frag = new BulletinListFragment();

        BulletinCursorAdapter mAdapter = new BulletinCursorAdapter(activity, R.layout.listview_item_bulletin, cursor, -1);
        frag.setListAdapter(mAdapter);

        return frag;
    }

}
