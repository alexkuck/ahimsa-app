package io.ahimsa.ahimsa_app.application;

import android.content.SharedPreferences;

import javax.annotation.Nonnull;

/**
 * Created by askuck on 6/9/14.
 */
public class Configuration {

    private final SharedPreferences prefs;

    public Configuration(@Nonnull final SharedPreferences prefs){

        this.prefs = prefs;
    }


}
