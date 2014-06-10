package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import io.ahimsa.ahimsa_app.application.R;
import io.ahimsa.ahimsa_app.application.service.OldNodeService;

/**
 * The txBroadcastFragment class.
 */
public class BulletinFragment extends Fragment {

    //txBroadcastFragment------------------------------------
    private static final String TAG = BulletinFragment.class.toString();
    private Activity parent_activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_bulletin, container, false);
        parent_activity = getActivity();

        final EditText messageText = (EditText) V.findViewById(R.id.messageText);
        Button txBroadcastButton = (Button) V.findViewById(R.id.broadcastButton);
        txBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                Log.d(TAG, "txBroadcastButton click");
                broadcastTx(messageText.getText().toString());

            }
        });

        return V;

    }

    public void broadcastTx(String tx_hex_string){
        Intent intent = new Intent().setAction(OldNodeService.ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(OldNodeService.HEX_STRING_TRANSACTION, tx_hex_string);
        parent_activity.sendBroadcast(intent);
    }


}