package io.ahimsa.ahimsa_app.application;

import android.content.SharedPreferences;

import javax.annotation.Nonnull;

/**
 * Created by askuck on 6/9/14.
 */
public class Configuration {

    public static final String PREF_KEY_MAX_PEERS = "max_connected_peers";
    public static final String PREF_KEY_MIN_PEERS = "min_connected_peers";




    private final SharedPreferences prefs;

    public Configuration(@Nonnull final SharedPreferences prefs){

        this.prefs = prefs;
    }


}
