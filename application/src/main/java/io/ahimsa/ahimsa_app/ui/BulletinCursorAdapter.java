package io.ahimsa.ahimsa_app.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;

/**
 * Created by askuck on 7/25/14.
 */
public class BulletinCursorAdapter extends ResourceCursorAdapter
{
    final public String TAG = "BulletinCursorAdapter";

    public BulletinCursorAdapter(Context context, int layout, Cursor c, int flags)
    {
        super(context, layout, c, flags);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor)
    {
        Log.d(TAG, "bindView()");

        //[_id, txid, sent_time, confirmed, highest_block, topic, message, txout_total, txout_count]

        TextView time_value = (TextView) view.findViewById(R.id.time_value);
        Long time = cursor.getLong(2);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  MMM dd, yyyy");
        Date resultdate = new Date(time);
        time_value.setText( sdf.format(resultdate) );

        TextView status_value = (TextView) view.findViewById(R.id.status_value);
        int confirmed = cursor.getInt(3);
        switch(confirmed)
        {
            case 0:
                status_value.setText("Unconfirmed");
                status_value.setTextColor(Color.rgb(194, 0, 48));
                break;

            default:
                status_value.setText("Confirmed");
//                status_value.setTextColor(status_value.getTextColors().getDefaultColor());
                status_value.setTextColor(Color.BLACK);
        }

        TextView topic_value = (TextView) view.findViewById(R.id.topic_value);
        String topic = cursor.getString(5);
        topic_value.setText(topic);

        TextView message_value = (TextView) view.findViewById(R.id.message_value);
        String message = cursor.getString(6);
        message_value.setText(message);

        TextView change_value = (TextView) view.findViewById(R.id.change_value);
        Long txout_total = cursor.getLong(7);
        Long txout_count = cursor.getLong(8);
        change_value.setText( String.format("%s (%s)", txout_total, txout_count) );

        Button confirm_button = (Button) view.findViewById(R.id.confirm_button);
        confirm_button.setTag( cursor.getString(1) );
        confirm_button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                AhimsaService.startConfirmTx(view.getContext(), view.getTag().toString());
                Toast.makeText(context, "Transaction confirmation request.\n" + view.getTag().toString(), Toast.LENGTH_LONG).show();
            }
        });

        Button details_button = (Button) view.findViewById(R.id.details_button);
        details_button.setTag( cursor.getString(1) );
        details_button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://ahimsa.io" + ":5000/bulletin/" + view.getTag().toString()));
                context.startActivity(browserIntent);
            }
        });

    }
}
