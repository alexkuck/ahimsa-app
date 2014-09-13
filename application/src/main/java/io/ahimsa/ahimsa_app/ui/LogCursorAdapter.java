package io.ahimsa.ahimsa_app.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaLog;

/**
 * Created by askuck on 8/12/14.
 */
public class LogCursorAdapter extends ResourceCursorAdapter
{
    final public String TAG = "LogCursorAdapter";

    public LogCursorAdapter(Context context, int layout, Cursor c, int flags)
    {
        super(context, layout, c, flags);
    }

    @Override
    public boolean isEnabled(int position)
    {
        return false;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // [_id, time, details, color]

        TextView time_value = (TextView) view.findViewById(R.id.time_value);
        Long time = cursor.getLong(1);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss  MMM dd");
        Date resultdate = new Date(time);
        time_value.setText( sdf.format(resultdate) );

        TextView detail_value = (TextView) view.findViewById(R.id.detail_value);
        String detail = cursor.getString(2);
        detail_value.setText(detail);

        String status = cursor.getString(3);
        View vertical_color = view.findViewById(R.id.vertical_color);
        if(AhimsaLog.normal.equals(status))
        {
            vertical_color.setBackgroundColor( Color.rgb(82, 174, 97) );
//            vertical_color.setBackgroundColor( Color.rgb(0, 176, 176) );
        }
        else if(AhimsaLog.queue.equals(status))
        {
//            vertical_color.setBackgroundColor( Color.rgb(94, 130, 200) );
            vertical_color.setBackgroundColor( Color.rgb(176, 176, 176) );
        }
        else if(AhimsaLog.error.equals(status))
        {
            vertical_color.setBackgroundColor( Color.rgb(194, 0, 48) );
        }
    }
}
