package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;

public class CreateBulletinFragment extends Fragment {

    private static final String TAG = "CreateBulletinFragment";
    private MainApplication application;

    public CreateBulletinFragment(){
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        this.application = (MainApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_create_bulletin, container, false);

        final EditText topicEditText = (EditText) V.findViewById(R.id.topicEditText);

        final EditText messageEditText = (EditText) V.findViewById(R.id.messageEditText);
        Button bulletinBroadcastButton = (Button) V.findViewById(R.id.broadcastButton);
        bulletinBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                broadcastBulletin(messageEditText.getText().toString(), topicEditText.getText().toString());
            }
        });

        final TextView balanceText = (TextView) V.findViewById(R.id.balanceText);
        balanceText.setText(application.getBalanceAsString() + " Satoshis");

        final TextView keyText = (TextView) V.findViewById(R.id.defaultKeyText);
        keyText.setText(application.getDefaultKeyAsString());


        return V;
    }

    public void broadcastBulletin(String message, String topic){
        application.createAndBroadcastBulletin(topic, message);
    }




}