package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;
import io.ahimsa.ahimsa_app.application.service.NodeService;
import io.ahimsa.ahimsa_app.application.util.AhimsaWallet;

/**
 * The txBroadcastFragment class.
 */
public class BulletinFragment extends Fragment {

    //txBroadcastFragment------------------------------------
    private static final String TAG = "BulletinFragment";

    private Activity activity;
    private MainApplication application;
    private AhimsaWallet ahimwall;



    @Override
    public void onAttach(final Activity activity){

        super.onAttach(activity);

        this.activity = activity;
        this.application = (MainApplication) activity.getApplication();
        this.ahimwall = application.getAhimsaWallet();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_bulletin, container, false);

        final EditText topicText = (EditText) V.findViewById(R.id.topicText);
        final EditText messageText = (EditText) V.findViewById(R.id.messageText);
        Button bulletinBroadcastButton = (Button) V.findViewById(R.id.broadcastButton);
        bulletinBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                Log.d(TAG, "txBroadcastButton click");
                broadcastBulletin(messageText.getText().toString(), topicText.getText().toString());

            }
        });

        return V;

    }

    public void broadcastBulletin(String message, String topic){
        ahimwall.broadcastBulletin(message, topic);
    }


}