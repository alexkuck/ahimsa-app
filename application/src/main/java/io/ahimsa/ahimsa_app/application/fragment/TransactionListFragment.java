package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;
import io.ahimsa.ahimsa_app.application.util.AhimsaDB;

public class TransactionListFragment extends ListFragment {

    private static final String TAG = "TransactionListFragment";
    private MainApplication application;

    SimpleCursorAdapter mAdapter;

    public TransactionListFragment(){
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        this.application = (MainApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.listview_transaction_item,
                application.getTransactionCursor(),
                new String[] {"_id", AhimsaDB.txid, AhimsaDB.raw, AhimsaDB.sent_time, AhimsaDB.confirmed, AhimsaDB.highest_block},
                new int[] {-1, R.id.txidText , -1, R.id.sendTimeText, R.id.confirmedText, R.id.highestBlockText}, 0);


        setListAdapter(mAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

}
