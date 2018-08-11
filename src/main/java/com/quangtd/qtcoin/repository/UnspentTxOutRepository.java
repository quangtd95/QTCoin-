package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.TxIn;
import com.quangtd.qtcoin.domain.UnspentTxOut;

import java.util.List;

public interface UnspentTxOutRepository {
    void setUnspentTxOuts(List<UnspentTxOut> unspentTxOuts);

    List<UnspentTxOut> getUnspentTxOuts();

    List<UnspentTxOut> updateUnspentTxOuts(List<Transaction> newTransactions);

    UnspentTxOut findUTXOByTxIn(String txId, int index);

    boolean validateTxInAvailable(TxIn txIn);

    UnspentTxOut findUTXOByTxIn(String txId, int index, List<UnspentTxOut> unspentTxOutList);

    long getAmount(TxIn txIn);

    long getBalance(String address);

    List<UnspentTxOut> findUTXOByAddress(String ownerAddress);

    String getOwnerOfTx(TxIn txIn);
}
