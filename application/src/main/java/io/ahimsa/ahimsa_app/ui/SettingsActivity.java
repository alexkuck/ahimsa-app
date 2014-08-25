package io.ahimsa.ahimsa_app.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Button;

import io.ahimsa.ahimsa_app.R;

/**
 * Created by askuck on 8/18/14.
 */
public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onPause()
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_headers);
        }
    }


    /**
     * Populate the activity with the top-level headers.
     */
//    @Override
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.preference_headers, target);
//    }


}
