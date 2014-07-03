package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;

public class ImportFundingTxFragment extends Fragment {

    private static final String TAG = "ImportFundingTxFragment";
    private MainApplication application;

    public ImportFundingTxFragment(){
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        this.application = (MainApplication) activity.getApplication();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_import_funding_tx, container, false);

        final EditText heightEditText = (EditText) V.findViewById(R.id.heightOfBlockEditText);
        Button bulletinBroadcastButton = (Button) V.findViewById(R.id.importButton);
        bulletinBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                importByBlockHeight(new Long(heightEditText.getText().toString()));

            }
        });

        final TextView localHeightText = (TextView) V.findViewById(R.id.heightText);
        localHeightText.setText(application.getChainHeight());

        final TextView localHeadHashText = (TextView) V.findViewById(R.id.chainHeadHashText);
        localHeadHashText.setText(application.getChainHeadHash());

        return V;
    }

    public void importByBlockHeight(Long height){
        application.discoverByBlockHeight(height);
    }


}
