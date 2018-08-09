package com.quangtd.qtcoin.core;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope
public class TransactionPool {
    private static List<Transaction> transactionPool = new ArrayList<>();

    public static List<Transaction> getTransactionPool() {
        return Utils.cloneList(transactionPool);
    }

    public static boolean addToTransactionPool(Transaction transaction, List<UnspentTxOut> unspentTxOuts) {
        if (!Transaction.validateTransaction(transaction, unspentTxOuts)) {
            System.out.println("Trying to add invalid tx to pool");
            return false;
        }
        if (!isValidTxForPool(transaction, transactionPool)) {
            System.out.println("Trying to add invalid tx to pool");
            return false;
        }
        System.out.println("adding to txPool: " + transaction);
        transactionPool.add(transaction);
        return true;
    }

    public static  boolean hasTxIn(TxIn txIn, List<UnspentTxOut> unspentTxOuts) {
        for (UnspentTxOut unspentTxOut : unspentTxOuts) {
            if (unspentTxOut.getTxOutId().equals(txIn.getTxOutId()) && unspentTxOut.getTxOutIndex() == txIn.getTxOutIndex()) {
                return true;
            }
        }
        return false;
    }

    public  static void updateTransactionPool(List<UnspentTxOut> unspentTxOuts) {
        List<Transaction> invalidTxs = new ArrayList<>();
        for (Transaction transaction : transactionPool) {
            for (TxIn txIn : transaction.getTxIns()) {
                if (!hasTxIn(txIn, unspentTxOuts)) {
                    invalidTxs.add(transaction);
                    break;
                }
            }
        }
        if (invalidTxs.size() > 0) {
            System.out.println("removing the following transactions from txPool:");
            transactionPool.removeAll(invalidTxs);
        }
    }

    public static  boolean isValidTxForPool(Transaction transaction, List<Transaction> transactionPool) {
        List<TxIn> txIns = getTxPollIns(transactionPool);
        for (TxIn txIn : transaction.getTxIns()) {
            if (containsTxIn(txIn, txIns)) {
                System.out.println("txIn already found in the txPool");
                return false;
            }
        }
        return true;
    }

    public  static List<TxIn> getTxPollIns(List<Transaction> transactionPool) {
        List<TxIn> txIns = new ArrayList<>();
        for (Transaction transaction : transactionPool) {
            txIns.addAll(transaction.getTxIns());
        }
        return txIns;
    }

    public  static boolean containsTxIn(TxIn txIn, List<TxIn> txIns) {
        for (TxIn t : txIns) {
            if (t.getTxOutIndex() == txIn.getTxOutIndex() && t.getTxOutId().equals(txIn.getTxOutId())) {
                return false;
            }
        }
        return true;
    }

}
