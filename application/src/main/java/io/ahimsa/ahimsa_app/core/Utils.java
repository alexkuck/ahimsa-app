package io.ahimsa.ahimsa_app.core;

import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;

import java.math.BigInteger;

import io.ahimsa.ahimsa_app.Constants;

/**
 * Created by askuck on 6/13/14.
 */
public class Utils {

    //TODO MAJOR | make this robust
    public static long getEstimatedCost(Long fee, Long dust, int top_count, int msg_count) {
        return fee + dust * ( (top_count + msg_count+ Constants.CHAR_PER_OUT - 1) / Constants.CHAR_PER_OUT);
    }

    public static byte[] hexToBytes(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static String commarizer(String number)
    {
        return number.replaceAll(Constants.COMMA_REGEX_1, Constants.COMMA_REGEX_2);
    }

    public static String abbreviator(String word, int len)
    {
        return word.substring(0, len) + "...";
    }

    //----------------------------------------------------------------------------------------------
//    public static BigInteger satoshiToSelf(Transaction tx, Long in_coin, Long _fee)
//    {
//        BigInteger in  = BigInteger.valueOf(in_coin);
//        BigInteger out = tx.getOutput(0).getValue();
//        BigInteger fee = BigInteger.valueOf(_fee);
//
//        return in.subtract(out).subtract(fee);
//    }

    public static ECKey importKey(String privkey) throws Exception {
        //only accepts a very specific type of address. from dumpprivkey functionality in Bitcoin core.
        DumpedPrivateKey dump = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, privkey);
        return dump.getKey();
        // ECKey key = dump.getKey();
        // return key.toAddress(NETWORK_PARAMETERS);
        // return key.getPubKey();

    }

    // angles of ahimsa:
    // help change the public perception of bitcoin
    // target specific countries
    // anonymous fund, donate to pool for people to post freely from
    // awareness: twitter vs ahimsa

}
