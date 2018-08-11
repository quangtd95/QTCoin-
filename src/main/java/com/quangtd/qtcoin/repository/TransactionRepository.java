package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Transaction;

import java.util.List;

public interface TransactionRepository {
    List<Transaction> getTransactionPool();

    void addToPool(Transaction transaction);
}
