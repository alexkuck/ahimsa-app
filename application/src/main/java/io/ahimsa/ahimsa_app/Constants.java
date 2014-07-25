package io.ahimsa.ahimsa_app;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

/**
 * Created by askuck on 7/11/14.
 */
public class Constants {

    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";

    public static final String ACTION_AHIMWALL_UPDATE = "action_ahimwall_update";
    public static final String EXTRA_INT_CONF = "int_confirmed";
    public static final String EXTRA_INT_UNCONF = "int_unconfirmed";
    public static final String EXTRA_INT_DRAFT = "int_draft";
    public static final String EXTRA_LONG_UNRESERVED_CONF_BAL = "long_unreserved_confirmed_balance";
    public static final String EXTRA_LONG_CONF_BAL = "long_confirmed_balance";
    public static final String EXTRA_LONG_UNCONF_BAL = "long_unconfirmed_balance";
    public static final String EXTRA_INT_CONF_TXOUTS = "int_confirmed_txouts";
    public static final String EXTRA_INT_UNCONF_TXOUTS = "int_unconfirmed_txouts";
    public static final String EXTRA_STRING_ADDRESS = "string_address";
    public static final String EXTRA_LONG_NET_HEIGHT = "long_net_height";
    public static final String EXTRA_LONG_LOCAL_HEIGHT = "long_local_height";





    public static final String AHIMSA_FUND = "https://ahimsa.io:1050";
    public static final String ROBINHOOD_FUND = "https://24.125.163.221:1050";

    public static final boolean TESTNET = true;
    public static final NetworkParameters NETWORK_PARAMETERS = TESTNET ? TestNet3Params.get() : MainNetParams.get();

    public static final Long MIN_DUST = new Long(546);
    public static final Long MIN_FEE = new Long(10000);

    public static final int MAX_MESSAGE_LEN = 500;
    public static final int MAX_TOPIC_LEN = 15;
    public static final int CHAR_PER_OUT = 20;

    public static final String DEFAULT_TOPIC = "Let it be known";

    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";
    public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
    public static final String BLOCKSTORE_FILENAME = "blockstore" + FILENAME_NETWORK_SUFFIX;
    public static final String CHECKPOINTS_FILENAME = "checkpoints" + FILENAME_NETWORK_SUFFIX;

}
