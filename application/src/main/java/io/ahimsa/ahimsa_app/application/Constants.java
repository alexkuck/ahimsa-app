package io.ahimsa.ahimsa_app.application;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import java.math.BigInteger;

/**
 * Created by askuck on 6/10/14.
 */
public class Constants {

    public static final String USER_AGENT = "ahimsa-app";
    public static final String VERSION = "alpha";

    public static final String AHIMSA_FUND = "https://ahimsa.io:1050";
    public static final String ROBINHOOD_FUND = "https://24.125.163.221:1050";

    public static final boolean TEST = true;
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
    public static final Long MIN_DUST = new Long(546);
    public static final Long MIN_FEE = new Long(10000);

    //todo: maybe put this figures in bytes not characters
    public static final int MAX_MESSAGE_LEN = 140;
    public static final int MAX_TOPIC_LEN = 15;
    public static final int CHAR_PER_OUT = 20;


    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";
    public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
    public static final String BLOCKSTORE_FILENAME = "blockstore" + FILENAME_NETWORK_SUFFIX;
    public static final String CHECKPOINTS_FILENAME = "checkpoints" + FILENAME_NETWORK_SUFFIX;

}