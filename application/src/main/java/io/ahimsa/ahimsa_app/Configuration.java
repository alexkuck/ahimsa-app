package io.ahimsa.ahimsa_app;

import android.content.SharedPreferences;
import android.util.Base64;

import com.google.bitcoin.core.ECKey;

import java.math.BigInteger;

import javax.annotation.Nonnull;

/**
 * Created by askuck on 7/11/14.
 */
public class Configuration {

    public static final String PREF_KEY_PRIVKEY_STRING = "private_key";
    public static final String PREF_KEY_PUBPOINT_STRING = "pubkey_point";
    public static final String PREF_KEY_KEY_CREATION_TIME_LONG = "key_creation_time";

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

    public void setDefaultECKey(ECKey key)
    {
        // NOTE: this method of storage is temporary and is for demo purposes only.
        // We are attending a conference and want something functional to show.
        // Saving a BasicKeyChain to file is the next implementation step.
        // ahimsa-app version-not-proof-of-concept-and-not-experimental-playground will have a Wallet.
        // ...as this app used to have but slow low times have caused us to venture away.

        byte[] privkey_raw = key.getPrivKey().toByteArray();
        byte[] pubpoint_raw = key.getPubKeyPoint().getEncoded();

        String privkey_str = Base64.encodeToString(privkey_raw, Base64.DEFAULT);
        String pubpoint_str = Base64.encodeToString(pubpoint_raw, Base64.DEFAULT);

        prefs.edit().putString(PREF_KEY_PRIVKEY_STRING, privkey_str).commit();
        prefs.edit().putString(PREF_KEY_PUBPOINT_STRING, pubpoint_str).commit();
        prefs.edit().putLong(PREF_KEY_KEY_CREATION_TIME_LONG, key.getCreationTimeSeconds()).commit();

    }

    public ECKey getDefaultECKey()
    {
        String privkey_str  = prefs.getString(PREF_KEY_PRIVKEY_STRING, null);
        String pubpoint_str = prefs.getString(PREF_KEY_PUBPOINT_STRING, null);

        if(privkey_str == null || pubpoint_str == null)
        {
            ECKey my_awesome_key = new ECKey();
            setDefaultECKey(my_awesome_key);

            return my_awesome_key;
        }
        else
        {
            byte[] privkey_raw  = Base64.decode(privkey_str, Base64.DEFAULT);
            byte[] pubpoint_raw  = Base64.decode(pubpoint_str, Base64.DEFAULT);

            return ECKey.fromPrivateAndPrecalculatedPublic(privkey_raw, pubpoint_raw);
        }
    }

    public Long getEarliestKeyCreationTime()
    {
        return prefs.getLong(PREF_KEY_KEY_CREATION_TIME_LONG, 0);
    }

    // AhimsaApplication ---------------------------------------------------------------------------
    public boolean syncBlockChainAtStartup()
    {
        return prefs.getBoolean(PREF_KEY_SYNC_AT_START, false);
    }

    public void setSyncBlockChainAtStartup(boolean x)
    {
        prefs.edit().putBoolean(PREF_KEY_SYNC_AT_START, x).commit();
    }

    // Funding -------------------------------------------------------------------------------------
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

    // Utility--------------------------------------------------------------------------------------
    public void     reset() {
        // boolean funded = getIsFunded();
        prefs.edit().clear().commit();
        // setIsFunded(funded);
    }


}