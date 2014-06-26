package io.ahimsa.ahimsa_app.application;

import android.content.SharedPreferences;
import javax.annotation.Nonnull;

/**
 * Created by askuck on 6/9/14.
 */
public class Configuration {

    public static final String PREF_KEY_IS_FUNDED = "funded";
    public static final String PREF_FUND_TXID = "fund_txid";
    public static final String PREF_KEY_FUNDING_IP = "funding_ip";

    public static final String PREF_KEY_TRUSTED_PEER = "trusted_peer";
    public static final String PREF_KEY_MAX_PEERS = "max_connected_peers";
    public static final String PREF_KEY_MIN_PEERS = "min_connected_peers";

    public static final String PREF_KEY_MIN_TIMEOUT = "minimum_timeout";
    public static final String PREF_KEY_DUST_VALUE = "dust_value";
    public static final String PREF_KEY_FEE_VALUE = "fee_value";
    public static final String PREF_KEY_ONLY_CONFIRMED = "only_confirmed";

    public static final String PREF_KEY_HIGHEST_BLOCK_SEEN = "highest_block_seen";
    public static final String PREF_KEY_DEFAULT_ADDRESS = "default_address";



    private final SharedPreferences prefs;
    public Configuration(@Nonnull final SharedPreferences prefs){
        this.prefs = prefs;
    }


    public boolean  getIsFunded(){
        return prefs.getBoolean(PREF_KEY_IS_FUNDED, false);
    }
    public void     setIsFunded(Boolean x){
        prefs.edit().putBoolean(PREF_KEY_IS_FUNDED, x).commit();
    }


    public String   getFundingTxid(){ return prefs.getString(PREF_FUND_TXID, "");}
    public void     setFundingTxid(String txid){ prefs.edit().putString(PREF_FUND_TXID, txid).commit();}

    public String   getFundingIP() { return prefs.getString(PREF_KEY_FUNDING_IP, Constants.ROBINHOOD_FUND);}
    public void     setFundingIP(String x) { prefs.edit().putString(PREF_KEY_FUNDING_IP, x);}




    public String   getTrustedPeer() {return prefs.getString(PREF_KEY_TRUSTED_PEER, "");}
    public void     setTrustedPeer(String x) {prefs.edit().putString(PREF_KEY_TRUSTED_PEER, x);}

    public int      getMaxConnectedPeers(){
        return prefs.getInt(PREF_KEY_MAX_PEERS, 6);
    }
    public void     setMaxConnectedPeers(int x) {
        prefs.edit().putInt(PREF_KEY_MAX_PEERS, x).commit();
    }

    public int      getMinConnectedPeers(){
        return prefs.getInt(PREF_KEY_MIN_PEERS, 2);
    }
    public void     setMinConnectedPeers(int x) {
        prefs.edit().putInt(PREF_KEY_MIN_PEERS, x).commit();
    }




    public Long     getDustValue() {
        return prefs.getLong(PREF_KEY_DUST_VALUE, Constants.MIN_DUST);
    }
    public void     setDustValue(long x) {
        prefs.edit().putLong(PREF_KEY_DUST_VALUE, x).commit();
    }

    public Long     getFeeValue(){
        return prefs.getLong(PREF_KEY_FEE_VALUE, Constants.MIN_FEE);
    }
    public void     setFeeValue(long x) {
        prefs.edit().putLong(PREF_KEY_FEE_VALUE, x).commit();
    }

    public Long     getMinCoinNecessary(){
        return getFeeValue() + getDustValue()*((Constants.MAX_MESSAGE_LEN + Constants.CHAR_PER_OUT + 1) / Constants.CHAR_PER_OUT);
    }

    public Boolean  getOnlyConfirmed(){
        return prefs.getBoolean(PREF_KEY_ONLY_CONFIRMED, false);
    }
    public void     setOnlyConfirmed(Boolean x){
        prefs.getBoolean(PREF_KEY_ONLY_CONFIRMED, x);
    }





    public int      getHighestBlockSeen(){
        return prefs.getInt(PREF_KEY_HIGHEST_BLOCK_SEEN, -1);
    }
    public void     setHighestBlockSeen(int blk) {
        prefs.edit().putInt(PREF_KEY_HIGHEST_BLOCK_SEEN, blk).commit();
    }

    public String   getDefaultAddress(){
        return prefs.getString(PREF_KEY_DEFAULT_ADDRESS, null);
    }
    public void     setDefaultAddress(String addr) {
        prefs.edit().putString(PREF_KEY_DEFAULT_ADDRESS, addr).commit();
    }

    public int      getMinTimeout(){
        return prefs.getInt(PREF_KEY_MIN_TIMEOUT, 15);
    }
    public void     setMinTimeout(int time) {
        prefs.edit().putInt(PREF_KEY_MIN_TIMEOUT, time).commit();
    }

    public void     reset() {
//        boolean funded = getIsFunded();
        prefs.edit().clear().commit();

//        setIsFunded(funded);
    }


}
