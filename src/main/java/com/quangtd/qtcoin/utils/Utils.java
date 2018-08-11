package com.quangtd.qtcoin.utils;

import com.google.gson.Gson;
import com.quangtd.qtcoin.domain.Cloneable;
import com.quangtd.qtcoin.domain.Signature;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String convertHexToBinary(String hex) {
        if (StringUtils.isEmpty(hex)) return StringUtils.EMPTY;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            StringBuilder binary = new StringBuilder(new BigInteger(hex.charAt(i) + "", 16).toString(2));
            int length = binary.length();
            for (int j = 1; j <= 4 - length; j++) {
                binary.insert(0, "0");
            }
            result.append(binary);
        }
        return result.toString();
    }

    public static String convertBinaryToHex(String binary) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < binary.length() - 1; i += 4) {
            String output = binary.substring(i, (i + 4));
            int decimal = Integer.parseInt(output, 2);
            sb.append(Integer.toHexString(decimal).toLowerCase());
        }
        return sb.toString();
    }

    public static String convertStringToHex(String s) {
        byte[] bytes = s.getBytes();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

    public static String convertHexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);
        }
        return sb.toString();
    }

    public static ECKeyPair getRandomKeyPair() {
        try {
            return Keys.createEcKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return new ECKeyPair(new BigInteger("0"), new BigInteger("0"));
    }

    /**
     * @param privateKey : hex string
     * @return publicKey {link : String}
     */
    public static String getPublicKeyFromPrivateKey(String privateKey) {
        BigInteger privKey = new BigInteger(privateKey, 16);
        BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
        return pubKey.toString(16);
    }

    private static ECKeyPair getKeyPairFromPrivateKey(String privateKey) {
        BigInteger privKey = new BigInteger(privateKey, 16);
        BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
        return new ECKeyPair(privKey, pubKey);
    }

    public static String signData(String privateKey, String data) {
        ECKeyPair keyPair = getKeyPairFromPrivateKey(privateKey);
        Sign.SignatureData signatureData = Sign.signMessage(data.getBytes(), keyPair, true);
        Signature signature = new Signature(signatureData.getV(), signatureData.getR(), signatureData.getS());
        return new Gson().toJson(signature, Signature.class);
    }

    public static boolean verifySignedData(String publicKey, String dataToVerify, String signature) {
        Signature signatureObject = new Gson().fromJson(signature, Signature.class);
        Sign.SignatureData signatureData = new Sign.SignatureData(signatureObject.getV(), signatureObject.getR(), signatureObject.getS());
        try {
            BigInteger publicKeyRecovered = Sign.signedMessageToKey(dataToVerify.getBytes(), signatureData);
            System.out.println(publicKey);
            System.out.println(publicKeyRecovered);
            return publicKeyRecovered.toString(16).equals(publicKey);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;

    }


    public static <T> List<T> cloneList(List<? extends Cloneable<T>> list) {
        if (list == null) return null;
        List<T> result = new ArrayList<>();
        list.forEach(it -> result.add(it.cloneObject()));
        return result;
    }


}
