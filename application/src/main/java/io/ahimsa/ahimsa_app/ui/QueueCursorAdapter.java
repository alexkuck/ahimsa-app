package io.ahimsa.ahimsa_app.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.R;

/**
 * Created by askuck on 8/13/14.
 */
public class QueueCursorAdapter extends ResourceCursorAdapter
{
    final public String TAG = "QueueCursorAdapter";

    public QueueCursorAdapter(Context context, int layout, Cursor c, int flags)
    {
        super(context, layout, c, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        Log.d(TAG, "bindView()");
        TextView action_value = (TextView) view.findViewById(R.id.action_value);
        String action = cursor.getString(3);
        action_value.setText(action);
    }
}
