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

import org.w3c.dom.Text;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaDB;

public class BulletinListFragment extends ListFragment {

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

    public void update(Cursor cursor)
    {
        BulletinCursorAdapter adapter = (BulletinCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);
        adapter.notifyDataSetChanged();

//        setListAdapter(new BulletinCursorAdapter(getActivity(), R.layout.listview_item_bulletin, cursor, -1));
    }


    public static BulletinListFragment newInstance(Activity activity, Cursor cursor)
    {
        BulletinListFragment frag = new BulletinListFragment();

        BulletinCursorAdapter mAdapter = new BulletinCursorAdapter(activity, R.layout.listview_item_bulletin, cursor, -1);
        frag.setListAdapter(mAdapter);

        return frag;
    }

}
