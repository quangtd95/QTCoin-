package com.quangtd.qtcoin.core;

import lombok.Getter;
import lombok.Setter;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Wallet {
    @Getter
    @Setter
    private String privateKey;
    @Getter
    @Setter
    private String publicKey;

    private static Set<String> wallets = new HashSet<>();

    private Wallet(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static Wallet initWallet() {
        ECKeyPair keyPair = Utils.getRandomKeyPair();
        String privKey = keyPair.getPrivateKey().toString(16);
        String pubKey = keyPair.getPublicKey().toString(16);
        if (wallets.contains(pubKey)) {
            return null;
        }
        wallets.add(pubKey);
        return new Wallet(pubKey, privKey);
    }

    public static boolean deleteWallet(String address, String privateKey) {
        BigInteger bigInteger = new BigInteger(privateKey, 16);
        if (!Sign.publicKeyFromPrivate(bigInteger).toString(16).equals(address)) {
            System.out.println("error private key + address");
            return false;
        }
        boolean success = wallets.remove(address);
        if (!success) {
            System.out.println("address doesn't exists");
        }
        return success;
    }

    public static long getBalance(String address, List<UnspentTxOut> unspentTxOuts) {
        return unspentTxOuts.stream()
                .filter(unspentTxOut -> unspentTxOut.getAddress().equals(address))
                .map(UnspentTxOut::getAmount)
                .reduce(0L, (a, b) -> a + b);
    }

    private final static String KEY_GROUP_CONSUMED_TXOUT = "KEY_GROUP_CONSUMED_TXOUT";
    private final static String KEY_LEFT_OVER_AMOUNT = "KEY_LEFT_OVER_AMOUNT";

    public static Map<String, Object> findTxOutsForAmount(long amount, List<UnspentTxOut> unspentTxOuts) {
        long currentAmount = 0;
        long leftOverAmount;
        List<UnspentTxOut> includedUnspentTxOuts = new ArrayList<>();
        for (UnspentTxOut unspentTxOut : unspentTxOuts) {
            includedUnspentTxOuts.add(unspentTxOut);
            currentAmount += unspentTxOut.getAmount();
            if (currentAmount >= amount) {
                leftOverAmount = currentAmount - amount;
                Map<String, Object> result = new HashMap<>();
                result.put(KEY_GROUP_CONSUMED_TXOUT, includedUnspentTxOuts);
                result.put(KEY_LEFT_OVER_AMOUNT, leftOverAmount);
                return result;
            }
        }
        throw new RuntimeException("not enough coins to send transaction");
    }

    public static List<TxOut> createTxOuts(String receiveAddress, String myAddress, long amount, long leftOverAmount) {
        TxOut txOut = new TxOut(receiveAddress, amount);
        if (leftOverAmount == 0) {
            return new ArrayList<TxOut>() {{
                add(txOut);
            }};
        } else {
            TxOut leftOverTx = new TxOut(myAddress, leftOverAmount);
            return new ArrayList<TxOut>() {{
                add(leftOverTx);
            }};
        }
    }

    public static List<UnspentTxOut> filterTxPoolTxs(List<UnspentTxOut> unspentTxOuts, List<Transaction> transactions) {
        List<TxIn> txIns = new ArrayList<>();
        transactions.forEach(transaction -> {
            txIns.addAll(transaction.getTxIns());
        });
        List<UnspentTxOut> removable = new ArrayList<>();
        for (UnspentTxOut unspentTxOut : unspentTxOuts) {
            for (TxIn txIn : txIns) {
                if (txIn.getTxOutId().equals(unspentTxOut.getTxOutId())
                        && txIn.getTxOutIndex() == unspentTxOut.getTxOutIndex()) {
                    removable.add(unspentTxOut);
                    break;
                }
            }
        }
        unspentTxOuts.removeAll(removable);
        return unspentTxOuts;
    }

    public static TxIn toUnsignedTxIn(UnspentTxOut unspentTxOut) {
        TxIn txIn = new TxIn();
        txIn.setTxOutId(unspentTxOut.getTxOutId());
        txIn.setTxOutIndex(unspentTxOut.getTxOutIndex());
        return txIn;
    }

    @SuppressWarnings("unchecked")
    public static Transaction createTransaction(String receiverAddress,
                                                long amount,
                                                String privateKey,
                                                List<UnspentTxOut> unspentTxOuts,
                                                List<Transaction> transactionPool) {

        String myAddress = Utils.getPublicKeyFromPrivateKey(privateKey);
        List<UnspentTxOut> myUnspentTxOutsA = unspentTxOuts.stream()
                .filter(unspentTxOut -> unspentTxOut.getAddress().equals(myAddress))
                .collect(Collectors.toList());
        List<UnspentTxOut> myUnspentTxOuts = filterTxPoolTxs(myUnspentTxOutsA, transactionPool);
        Map<String, Object> txOutForAmount = findTxOutsForAmount(amount, myUnspentTxOuts);
        List<UnspentTxOut> includedUnspentTxOuts = (List<UnspentTxOut>) txOutForAmount.get(KEY_GROUP_CONSUMED_TXOUT);
        long leftOverAmount = (long) txOutForAmount.get(KEY_LEFT_OVER_AMOUNT);

        List<TxIn> unsignedTxIns = includedUnspentTxOuts.stream().map(Wallet::toUnsignedTxIn).collect(Collectors.toList());

        Transaction transaction = new Transaction();
        transaction.setTxIns(unsignedTxIns);
        transaction.setTxOuts(createTxOuts(receiverAddress, myAddress, amount, leftOverAmount));
        transaction.setId(Transaction.getTransactionId(transaction));

        for (int i = 0; i < transaction.getTxIns().size(); i++) {
            TxIn txIn = transaction.getTxIns().get(i);
            txIn.setSignature(Transaction.signTxIn(transaction, i, privateKey, unspentTxOuts));
        }
        return transaction;

    }

}
