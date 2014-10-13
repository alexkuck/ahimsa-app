package io.ahimsa.ahimsa_app.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.Utils;

/**
 * Created by askuck on 9/6/14.
 */
public class OutPointCursorAdapter extends ResourceCursorAdapter
{
    final public String TAG = "OutPointCursorAdapter";

    public OutPointCursorAdapter(Context context, int layout, Cursor c, int flags)
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
        //[_id, txouts.txid, txouts.vout, txouts.value, transactions.confirmed]
        String txid = cursor.getString(1);
        int vout = cursor.getInt(2);
        Long value = cursor.getLong(3);
        boolean confirmed = cursor.getInt(4) != 0;

        TextView txid_view = (TextView) view.findViewById(R.id.outpoint_txid);
        TextView vout_view = (TextView) view.findViewById(R.id.outpoint_vout);
        TextView coin_view = (TextView) view.findViewById(R.id.outpoint_coin);

        txid_view.setText( txid );
        vout_view.setText( String.valueOf(vout) );
        coin_view.setText( value.toString() ) ;

        if(!confirmed)
        {
            txid_view.setTextColor( Color.rgb(176, 176, 176) );
            vout_view.setTextColor( Color.rgb(176, 176, 176) );
            coin_view.setTextColor( Color.rgb(176, 176, 176) );
        }
        else
        {
            txid_view.setTextColor( Color.BLACK );
            vout_view.setTextColor( Color.BLACK );
            coin_view.setTextColor( Color.BLACK );
        }

    }
}
