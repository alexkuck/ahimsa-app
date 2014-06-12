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

import io.ahimsa.ahimsa_app.application.R;
import io.ahimsa.ahimsa_app.application.service.NodeService;

/**
 * The txBroadcastFragment class.
 */
public class BulletinFragment extends Fragment {

    //txBroadcastFragment------------------------------------
    private static final String TAG = "BulletinFragment";
    private Activity parent_activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_bulletin, container, false);
        parent_activity = getActivity();

        final EditText topicText = (EditText) V.findViewById(R.id.topicText);
        final EditText bulletinText = (EditText) V.findViewById(R.id.bulletinText);
        Button bulletinBroadcastButton = (Button) V.findViewById(R.id.broadcastButton);
        bulletinBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                Log.d(TAG, "txBroadcastButton click");
                broadcastBulletin(bulletinText.getText().toString(), topicText.getText().toString());

            }
        });

        return V;

    }

    public void broadcastBulletin(String bulletin, String topic){
        NodeService.startActionBroadcastBulletin(parent_activity, bulletin, topic);

    }


}