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
 * Created by askuck on 8/12/14.
 */
public class LogListFragment extends ListFragment
{
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
        View view = inflater.inflate(R.layout.fragment_bulletin_list, container, false);
        return view;
    }

    public void update(Cursor cursor)
    {
        BulletinCursorAdapter adapter = (BulletinCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);
    }

    // ---------------------------------------------------------------------------------------------
//    public static LogListFragment newInstance(Activity activity, Cursor cursor)
    public static LogListFragment newInstance()
    {
        LogListFragment frag = new LogListFragment();

//        BulletinCursorAdapter mAdapter = new BulletinCursorAdapter(activity, R.layout.listview_item_bulletin, cursor, -1);
//        frag.setListAdapter(mAdapter);

        return frag;
    }
}
