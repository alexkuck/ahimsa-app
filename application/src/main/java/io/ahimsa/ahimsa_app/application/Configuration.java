package io.ahimsa.ahimsa_app.application;

import android.content.SharedPreferences;

import javax.annotation.Nonnull;

/**
 * Created by askuck on 6/9/14.
 */
public class Configuration {

    public static final String TAG = "configuration";
    public static final String PREF_KEY_IS_CONFIGURED = "configured";

    public static final String PREF_KEY_MAX_PEERS = "max_connected_peers";
    public static final String PREF_KEY_MIN_PEERS = "min_connected_peers";
    public static final String PREF_KEY_DUST_VALUE = "dust_value";
    public static final String PREF_KEY_FEE_VALUE = "fee_value";
    public static final String PREF_KEY_HIGHEST_BLOCK_SEEN = "highest_block_seen";
    public static final String PREF_KEY_DEFAULT_ADDRESS = "default_address";


    private final SharedPreferences prefs;

    public Configuration(@Nonnull final SharedPreferences prefs){

        this.prefs = prefs;
    }

    public int getMaxConnectedPeers(){
        return prefs.getInt(PREF_KEY_MAX_PEERS, 6);
    }

    public int getMinConnectedPeers(){
        return prefs.getInt(PREF_KEY_MIN_PEERS, 6);
    }

    public Long getDustValue(){
        return prefs.getLong(PREF_KEY_DUST_VALUE, Constants.MIN_DUST);
    }

    public Long getFeeValue(){
        return prefs.getLong(PREF_KEY_FEE_VALUE, Constants.MIN_FEE);
    }

    public int getHighestBlockSeen(){
        return prefs.getInt(PREF_KEY_HIGHEST_BLOCK_SEEN, 0);
    }

    public void setHighestBlockSeen(int blk){
        prefs.edit().putInt(PREF_KEY_HIGHEST_BLOCK_SEEN, blk).commit();
    }

    public String getDefaultAddress(){
        return prefs.getString(PREF_KEY_DEFAULT_ADDRESS, null);
    }

    public void setDefaultAddress(String addr){
        prefs.edit().putString(PREF_KEY_DEFAULT_ADDRESS, addr).commit();
    }





}
