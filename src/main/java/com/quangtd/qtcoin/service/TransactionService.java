package com.quangtd.qtcoin.service;

import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.TxIn;
import com.quangtd.qtcoin.domain.UnspentTxOut;

import java.util.List;

public interface TransactionService {
    List<Transaction> getTransactionPool();

    String getTransactionId(Transaction transaction);

    long getAmountOfTxIn(TxIn txIn);

    List<UnspentTxOut> processTransactions(List<Transaction> transactions, int blockIndex);

    Transaction getGenesisTransaction();

    Transaction generateCoinbaseTransaction(String address, int blockIndex);

    boolean addToTransactionPool(Transaction transaction);

    void updateTransactionPool();

    @SuppressWarnings("unchecked")
    Transaction createTransaction(String receiverAddress, long amount, String privateKey);
}
