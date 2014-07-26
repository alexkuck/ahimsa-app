package io.ahimsa.ahimsa_app;

import android.content.SharedPreferences;

import javax.annotation.Nonnull;

/**
 * Created by askuck on 7/11/14.
 */
public class Configuration {

    public static final String PREF_KEY_SYNC_AT_START = "sync_blockchain_at_startup";
    public static final String PREF_KEY_TEMP_CONF_BALANCE = "temp_conf_balance";
    public static final String PREF_KEY_IS_FUNDED = "funded";
    public static final String PREF_FUND_TXID = "fund_txid";
    public static final String PREF_KEY_FUNDING_IP = "funding_ip";
    public static final String PREF_KEY_TRUSTED_PEER = "trusted_peer";
    public static final String PREF_KEY_MAX_PEERS = "max_connected_peers";
    public static final String PREF_KEY_MIN_PEERS = "min_connected_peers";
    public static final String PREF_KEY_TIMEOUT = "timeout";
    public static final String PREF_KEY_DUST_VALUE = "dust_value";
    public static final String PREF_KEY_FEE_VALUE = "fee_value";
    public static final String PREF_KEY_HIGHEST_BLOCK_SEEN = "highest_block_seen";
    public static final String PREF_KEY_DEFAULT_ADDRESS = "default_address";

    private final SharedPreferences prefs;
    public Configuration(@Nonnull final SharedPreferences prefs){
        this.prefs = prefs;
    }


    // AhimsaApplication ---------------------------------------------------------------------------
    public boolean  syncBlockChainAtStartup(){
        return prefs.getBoolean(PREF_KEY_SYNC_AT_START, false);
    }
    public void     setSyncBlockChainAtStartup(boolean x){
        prefs.edit().putBoolean(PREF_KEY_SYNC_AT_START, x).commit();
    }


    // AhimsaWallet --------------------------------------------------------------------------------
//    public Long     getTempConfBalance() {
//        return prefs.getLong(PREF_KEY_TEMP_CONF_BALANCE, 0);
//    }
//    public void setTempConfBalance(Long x) {
//        prefs.edit().putLong(PREF_KEY_TEMP_CONF_BALANCE, x).commit();
//    }


    // Funding -------------------------------------------------------------------------------------
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


    // BitcoinNode ---------------------------------------------------------------------------------
    public String   getTrustedPeer() {return prefs.getString(PREF_KEY_TRUSTED_PEER, "");}
    public void     setTrustedPeer(String x) {prefs.edit().putString(PREF_KEY_TRUSTED_PEER, x);}

    public int      getMaxConnectedPeers(){
        return prefs.getInt(PREF_KEY_MAX_PEERS, 6);
    }
    public void     setMaxConnectedPeers(int x) {
        prefs.edit().putInt(PREF_KEY_MAX_PEERS, x).commit();
    }

    public int      getMinConnectedPeers(){
        return prefs.getInt(PREF_KEY_MIN_PEERS, 3);
    }
    public void     setMinConnectedPeers(int x){
        prefs.edit().putInt(PREF_KEY_MIN_PEERS, x);
    }

    public int      getTimeout(){
        return prefs.getInt(PREF_KEY_TIMEOUT, 15);
    }
    public void     setTimeout(int time) {
        prefs.edit().putInt(PREF_KEY_TIMEOUT, time).commit();
    }

    public long     getHighestBlockSeen(){
        return prefs.getLong(PREF_KEY_HIGHEST_BLOCK_SEEN, -1);
    }
    public void     setHighestBlockSeen(long height) {
        prefs.edit().putLong(PREF_KEY_HIGHEST_BLOCK_SEEN, height).commit();
    }


    // BulletinBuilder -----------------------------------------------------------------------------
    public Long     getMinCoinNecessary(){
        return getFeeValue() + getDustValue()*((Constants.MAX_MESSAGE_LEN + Constants.CHAR_PER_OUT - 1) / Constants.CHAR_PER_OUT);
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

    public String   getDefaultAddress(){
        return prefs.getString(PREF_KEY_DEFAULT_ADDRESS, "");
    }
    public void     setDefaultAddress(String addr) {
        prefs.edit().putString(PREF_KEY_DEFAULT_ADDRESS, addr).commit();
    }


    // Utility--------------------------------------------------------------------------------------
    public void     reset() {
        // boolean funded = getIsFunded();
        prefs.edit().clear().commit();
        // setIsFunded(funded);
    }


}