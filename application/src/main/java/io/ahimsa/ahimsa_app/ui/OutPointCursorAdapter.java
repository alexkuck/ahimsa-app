package io.ahimsa.ahimsa_app.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;

/**
 * Created by askuck on 9/6/14.
 */
public class OutPointCursorAdapter extends ResourceCursorAdapter
{
    public OutPointCursorAdapter(Context context, int layout, Cursor c, int flags)
    {
        super(context, layout, c, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        
    }
}
