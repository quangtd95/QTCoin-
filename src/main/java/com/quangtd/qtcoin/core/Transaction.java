package com.quangtd.qtcoin.core;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements Cloneable<Transaction> {
    private static final long COINBASE_AMOUNT = 50L;
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private List<TxIn> txIns;
    @Getter
    @Setter
    private List<TxOut> txOuts;

    public Transaction clone() {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setTxIns(Utils.cloneList(txIns));
        transaction.setTxOuts(Utils.cloneList(txOuts));
        return transaction;
    }

    public static String getTransactionId(Transaction transaction) {
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
        boolean validAddress = Utils.verifySignedData(address, transaction.getId(), txIn.getSignature());
        if (!validAddress) {
            System.out.println("invalid txIn signature");
            return false;
        }
        return true;
    }

    private static boolean hasValidTxIns(Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
        return transaction.getTxIns()
                .stream()
                .map(txIn -> validateTxIn(txIn, transaction, unspentTxOuts))
                .reduce(true, (a, b) -> a & b);
    }

    public static boolean validateTransaction(Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
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

    public static Transaction getCoinbaseTransaction(String address, int blockIndex) {
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

    public static String signTxIn(Transaction transaction, int txInNumber, String privateKey,
                                  List<UnspentTxOut> unspentTxOuts) {
        TxIn txIn = transaction.getTxIns().get(txInNumber);
        String dataToSign = transaction.getId();
        UnspentTxOut refUnspentOut = findUnspentTxOut(txIn.getTxOutId(), txIn.getTxOutIndex(), unspentTxOuts);
        if (refUnspentOut == null) {
            System.out.println("could not find referenced txOut");
            throw new RuntimeException();
        }
        String referencedAddress = refUnspentOut.getAddress();
        String address = Utils.getPublicKeyFromPrivateKey(privateKey);
        if (!referencedAddress.equals(address)) {
            System.out.println("trying to sign an input with private +\n" +
                    " key that does not match the address that is referenced in txIn");
            throw new RuntimeException();
        }
        return Utils.signData(privateKey, dataToSign);
    }

    public static String getPublickeyFromPrivateKey(String privateKey) {
        BigInteger privKey = new BigInteger(Utils.convertStringToHex(privateKey), 16);
        BigInteger publicKey = Sign.publicKeyFromPrivate(privKey);
        return publicKey.toString(16);
    }

    public static List<UnspentTxOut> updateUnspentTxOuts(List<Transaction> newTransactions, List<UnspentTxOut> unspentTxOuts) {
        List<UnspentTxOut> newUnspentTxOuts = new ArrayList<>();
        List<UnspentTxOut> consumedTxOuts = new ArrayList<>();
        for (Transaction transaction : newTransactions) {
            List<TxOut> txOuts = transaction.getTxOuts();
            for (int i = 0; i < txOuts.size(); i++) {
                newUnspentTxOuts.add(new UnspentTxOut(transaction.getId(), i, txOuts.get(i).getAddress(), txOuts.get(i).getAmount()));
            }
            List<TxIn> txIns = transaction.getTxIns();
            for (TxIn txIn : txIns) {
                consumedTxOuts.add(new UnspentTxOut(txIn.getTxOutId(), txIn.getTxOutIndex(), txIn.getSignature(), 0));
            }
        }
        List<UnspentTxOut> result = unspentTxOuts.stream()
                .filter(unspentTxOut ->
                        findUnspentTxOut(unspentTxOut.getTxOutId(), unspentTxOut.getTxOutIndex(), consumedTxOuts) == null)
                .collect(Collectors.toList());
        result.addAll(newUnspentTxOuts);
        return result;
    }

    public static List<UnspentTxOut> processTransactions(List<Transaction> transactions, List<UnspentTxOut> unspentTxOuts, int blockIndex) {
        if (!validateBlockTransactions(transactions, unspentTxOuts, blockIndex)) {
            System.out.println("invalid block transactions");
            return null;
        }
        return updateUnspentTxOuts(transactions, unspentTxOuts);
    }

}
