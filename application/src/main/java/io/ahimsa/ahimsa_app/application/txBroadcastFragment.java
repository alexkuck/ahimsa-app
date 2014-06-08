package io.ahimsa.ahimsa_app.application;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.ToggleButton;

import org.w3c.dom.Node;

/**
 * The txBroadcastFragment class.
 */
public class txBroadcastFragment extends Fragment {

    //txBroadcastFragment------------------------------------
    private static final String TAG = "txBroadcastFragment";
    private Activity parent_activity;
    private boolean testnet;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_txbroadcast, container, false);
        parent_activity = getActivity();

        final ToggleButton serviceToggle = (ToggleButton) V.findViewById(R.id.toggleButton);
        serviceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toggleEnabled(buttonView);
                } else {
                    toggleDisabled(buttonView);
                }
            }
        });

        final Switch testnetSwitch = (Switch) V.findViewById(R.id.testnet);
        testnetSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    testnet = true;
                } else {
                    testnet = false;
                }
            }
        });



        final EditText messageText = (EditText) V.findViewById(R.id.messageText);
        Button txBroadcastButton = (Button) V.findViewById(R.id.broadcastButton);
        txBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                Log.d(TAG, "txBroadcastButton click");
                broadcastTx(messageText.getText().toString());

            }
        });

        final Button peerCountButton = (Button) V.findViewById(R.id.peerCount);
        peerCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                getPeerCount(V);
            }
        });




        return V;

    }

    public void toggleEnabled(View V){
        Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION_ENABLE_NODE_SERVICE);
        Log.d(TAG, "TESTNET: " + testnet);

//        intent.putExtra(MainActivity.IS_TESTNET, testnet);

        parent_activity.sendBroadcast(intent);

    }

    public void toggleDisabled(View V){
        parent_activity.sendBroadcast(new Intent().setAction(MainActivity.ACTION_DISABLE_NODE_SERVICE));

    }

    public void getPeerCount(View V){
        parent_activity.sendBroadcast(new Intent().setAction(NodeService.ACTION_TOAST_NUM_PEERS));

    }

    public void broadcastTx(String tx_hex_string){
        Intent intent = new Intent().setAction(NodeService.ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(NodeService.HEX_STRING_TRANSACTION, tx_hex_string);
        parent_activity.sendBroadcast(intent);
    }


}