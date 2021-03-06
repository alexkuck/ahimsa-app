package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.ahimsa.ahimsa_app.R;

/**
 * Created by askuck on 8/14/14.
 */
public class LogListFragment extends ListFragment
{
    final static String TAG = "LogListFragment";

    public LogListFragment()
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
        View view = inflater.inflate(R.layout.fragment_log_list2, container, false);
        return view;
    }

    public void updateView(Cursor cursor)
    {
        LogCursorAdapter adapter = (LogCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);
    }

    // ---------------------------------------------------------------------------------------------
    public static LogListFragment newInstance(Activity activity, Cursor cursor)
    {
        LogListFragment frag = new LogListFragment();

        LogCursorAdapter mAdapter = new LogCursorAdapter(activity, R.layout.listview_item_log, cursor, -1);
        frag.setListAdapter(mAdapter);

        return frag;
    }

}
