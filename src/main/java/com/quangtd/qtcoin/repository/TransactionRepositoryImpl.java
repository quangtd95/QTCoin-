package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("transactionRepository")
public class TransactionRepositoryImpl implements TransactionRepository {

    private List<Transaction> transactionPool = new ArrayList<>();

    @Override
    public List<Transaction> getTransactionPool() {
        return transactionPool;
    }

    @Override
    public void addToPool(Transaction transaction) {
        transactionPool.add(transaction);
    }
}
