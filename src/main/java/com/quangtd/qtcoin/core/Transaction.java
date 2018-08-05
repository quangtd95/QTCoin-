package com.quangtd.qtcoin.core;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Transaction {
    private static final long COINBASE_AMOUNT = 50L;
    @Getter
    private String id;
    @Getter
    @Setter
    private List<TxIn> txIns;
    @Getter
    @Setter
    private List<TxOut> txOuts;

    private static String getTransactionId(Transaction transaction) {
        String txInContent = transaction.getTxIns()
                .stream()
                .map(txIn -> txIn.getTxOutId() + txIn.getTxOutIndex())
                .reduce("", (s, s2) -> s + s2);
        String txOutContent = transaction.getTxOuts()
                .stream()
                .map(txOut -> txOut.getAddress() + txOut.getAddress())
                .reduce("", (s, s2) -> s + s2);
        return DigestUtils.sha256Hex(txInContent + txOutContent);
    }

    private static boolean validateTxIn(TxIn txIn, Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
        Optional<UnspentTxOut> refTxOutOptional = unspentTxOuts
                .stream()
                .filter(u -> u.getTxOutId().equals(txIn.getTxOutId()) && u.getTxOutIndex() == txIn.getTxOutIndex())
                .findFirst();
        if (!refTxOutOptional.isPresent()) {
            System.out.println("referenced txOut not found: " + txIn);
            return false;
        }
        UnspentTxOut referencedUTxOut = refTxOutOptional.get();
        String address = referencedUTxOut.getAddress();
        try {
            ECPublicKey ecPublicKey = decodeKey(address.getBytes());
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(ecPublicKey);
            ecdsaVerify.update(transaction.getId().getBytes());
            return ecdsaVerify.verify(txIn.getSignature().getBytes());
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static ECPublicKey decodeKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return (ECPublicKey) fact.generatePublic(keySpec);
    }

    private static boolean hasValidTxIns(Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
        return transaction.getTxIns()
                .stream()
                .map(txIn -> validateTxIn(txIn, transaction, unspentTxOuts))
                .reduce(true, (a, b) -> a & b);
    }

    private static boolean validateTransaction(Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
        if (!getTransactionId(transaction).equals(transaction.getId())) {
            System.out.println("invalid tx id: " + transaction.getId());
            return false;
        }
        if (!hasValidTxIns(transaction, unspentTxOuts)) {
            System.out.println("some of the txIns are invalid in tx: " + transaction.getId());
            return false;
        }
        long totalAmountIn = transaction.getTxIns()
                .stream()
                .map(txIn -> getTxInAmount(txIn, unspentTxOuts))
                .reduce(0L, (a, b) -> a + b);
        long totalAmountOut = transaction.getTxOuts()
                .stream()
                .map(TxOut::getAmount)
                .reduce(0L, (a, b) -> a + b);
        if (totalAmountIn != totalAmountOut) {
            System.out.println("totalTxOutValues != totalTxInValues in tx " + transaction.getId());
            return false;
        }
        return true;
    }

    private static boolean validateBlockTransactions(List<Transaction> transactions, List<UnspentTxOut> unspentTxOuts, int blockIndex) {
        Transaction coinbaseTransaction = transactions.get(0);
        if (!validateCoinbaseTx(coinbaseTransaction, blockIndex)) {
            System.out.println("invalid coinbase transaction");
            return false;
        }
        // check for duplicate txIns. Each txIn can be included only once
        List<TxIn> txIns = new ArrayList<>();
        Gson gson = new Gson();
        transactions.forEach(transaction -> txIns.addAll(transaction.txIns));
        int sizeAfterFilter = txIns.stream().map(txIn -> gson.toJson(txIn, TxIn.class))
                .distinct()
                .collect(Collectors.toList()).size();
        if (sizeAfterFilter != txIns.size()) {
            System.out.println("has duplicates");
            return false;
        }
        for (int i = 1; i < transactions.size(); i++) {
            if (!validateTransaction(transactions.get(i), unspentTxOuts)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateCoinbaseTx(Transaction transaction, int blockIndex) {
        if (transaction == null) return false;
        if (!getTransactionId(transaction).equals(transaction.id)) return false;
        if (transaction.txIns.size() != 1) return false;
        if (transaction.txIns.get(0).getTxOutIndex() != blockIndex) return false;
        if (transaction.txOuts.size() != 1) return false;
        if (transaction.txOuts.get(0).getAmount() != COINBASE_AMOUNT) return false;
        return true;
    }

    private static long getTxInAmount(TxIn txIn, List<UnspentTxOut> unspentTxOuts) {
        return findUnspentTxOut(txIn.getTxOutId(), txIn.getTxOutIndex(), unspentTxOuts).getAmount();
    }

    private static UnspentTxOut findUnspentTxOut(String txId, int index, List<UnspentTxOut> unspentTxOuts) {
        Optional<UnspentTxOut> unspentTxOutOptional = unspentTxOuts
                .stream()
                .filter(u -> u.getTxOutIndex() == index && u.getTxOutId().equals(txId))
                .findFirst();
        return unspentTxOutOptional.orElse(null);
    }

    private static Transaction getCoinbaseTransaction(String address, int blockIndex) {
        Transaction transaction = new Transaction();
        TxIn txIn = new TxIn();
        txIn.setSignature("");
        txIn.setTxOutId("");
        txIn.setTxOutIndex(blockIndex);
        transaction.setTxIns(new ArrayList<TxIn>() {{
            add(txIn);
        }});
        transaction.setTxOuts(new ArrayList<TxOut>() {{
            add(new TxOut(address, COINBASE_AMOUNT));
        }});
        transaction.id = getTransactionId(transaction);
        return transaction;
    }

    private static String signTxIn(Transaction transaction, int txInNumber, String privateKey,
                                   List<UnspentTxOut> unspentTxOuts) {
        TxIn txIn = transaction.getTxIns().get(txInNumber);
        String dataToSign = transaction.getId();
        UnspentTxOut refUnspentOut = findUnspentTxOut(txIn.getTxOutId(), txIn.getTxOutIndex(), unspentTxOuts);
        if (refUnspentOut == null) {
            throw new RuntimeException();
        }
        String referencedAddress = refUnspentOut.getAddress();
        BigInteger privKey = new BigInteger(privateKey, 16);
        BigInteger publicKey = Sign.publicKeyFromPrivate(privKey);
        if (!publicKey.toString(16).equals(referencedAddress)) {
            throw new RuntimeException();
        }
        ECKeyPair keyPair = new ECKeyPair(privKey, publicKey);
        return bytesToHex(keyPair.sign(dataToSign.getBytes()).toString().getBytes());
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
