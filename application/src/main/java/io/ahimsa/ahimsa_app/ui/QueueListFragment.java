package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.Arrays;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaLog;

/**
 * Created by askuck on 8/12/14.
 */
public class QueueListFragment extends ListFragment
{
    final static String TAG = "QueueListFragment";

    public QueueListFragment()
    {
        // necessary empty constructor
    }

    @Override
    public void onAttach(final Activity activity)
    {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_queue_list, container, false);
        return view;
    }

    public void updateView(Cursor cursor)
    {
        QueueCursorAdapter adapter = (QueueCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);
    }

    // ---------------------------------------------------------------------------------------------
    public static QueueListFragment newInstance(Activity activity, Cursor cursor)
    {
        QueueListFragment frag = new QueueListFragment();

        QueueCursorAdapter mAdapter = new QueueCursorAdapter(activity, R.layout.listview_item_queue, cursor, -1);
        frag.setListAdapter(mAdapter);

        return frag;
    }
}
