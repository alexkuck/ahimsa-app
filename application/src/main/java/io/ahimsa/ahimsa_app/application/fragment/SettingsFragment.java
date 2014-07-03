package io.ahimsa.ahimsa_app.application.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.ahimsa.ahimsa_app.application.MainApplication;
import io.ahimsa.ahimsa_app.application.R;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private MainApplication application;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        this.application = (MainApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View V = inflater.inflate(R.layout.fragment_settings, container, false);

        final ImageView image = (ImageView) V.findViewById(R.id.obiwanView);
        image.setImageResource(R.drawable.obiwan_reduced);


        return V;
    }


}
