package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;
import io.ahimsa.ahimsa_app.application.util.AhimsaDB;

public class BulletinListFragment extends ListFragment {

    private static final String TAG = "BulletinListFragment";
    private MainApplication application;

    SimpleCursorAdapter mAdapter;

    public BulletinListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        this.application = (MainApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.listview_bulletin_item,
                application.getBulletinCursor(),
                new String[] {"_id", AhimsaDB.txid, AhimsaDB.topic, AhimsaDB.message},
                new int[] {-1, R.id.txidText , R.id.topicText, R.id.messageText}, 0);


        setListAdapter(mAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }


}
