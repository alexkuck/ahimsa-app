package io.ahimsa.ahimsa_app.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.ahimsa.ahimsa_app.R;


public class BulletinListFragment extends ListFragment
{
    public BulletinListFragment()
    {
        // Required empty public constructor
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

    public void updateView(Cursor cursor)
    {
        BulletinCursorAdapter adapter = (BulletinCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);
    }

    // ---------------------------------------------------------------------------------------------
    public static BulletinListFragment newInstance(Activity activity, Cursor cursor)
    {
        BulletinListFragment frag = new BulletinListFragment();

        BulletinCursorAdapter mAdapter = new BulletinCursorAdapter(activity, R.layout.listview_item_bulletin, cursor, -1);
        frag.setListAdapter(mAdapter);

        return frag;
    }

}
