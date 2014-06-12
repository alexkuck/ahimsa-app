package io.ahimsa.ahimsa_app.application;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

/**
 * Created by askuck on 6/10/14.
 */
public class Constants {

    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";

    public static final boolean TEST = true;
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
    public static final Long MIN_DUST = new Long(546);
    public static final Long MIN_FEE = new Long(10000);


    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";
    public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;


}