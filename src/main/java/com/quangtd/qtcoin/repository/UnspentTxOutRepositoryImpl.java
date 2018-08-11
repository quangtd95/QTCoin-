package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.TxIn;
import com.quangtd.qtcoin.domain.TxOut;
import com.quangtd.qtcoin.domain.UnspentTxOut;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("unspentTxOutRepository")
public class UnspentTxOutRepositoryImpl implements UnspentTxOutRepository {
    private List<UnspentTxOut> unspentTxOuts = new ArrayList<>();

    @Override
    public void setUnspentTxOuts(List<UnspentTxOut> unspentTxOuts) {
        this.unspentTxOuts = unspentTxOuts;
    }

    @Override
    public List<UnspentTxOut> getUnspentTxOuts() {
        return unspentTxOuts;
    }

    @Override
    public List<UnspentTxOut> updateUnspentTxOuts(List<Transaction> newTransactions) {
        //UnspentTxOut được sinh ra
        List<UnspentTxOut> newUnspentTxOuts = new ArrayList<>();
        //UnspentTxOut được tiêu thụ
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
                        findUTXOByTxIn(unspentTxOut.getTxOutId(), unspentTxOut.getTxOutIndex(), consumedTxOuts) == null)
                .collect(Collectors.toList());
        result.addAll(newUnspentTxOuts);
        return result;
    }

    @Override
    public UnspentTxOut findUTXOByTxIn(String txId, int index) {
        return findUTXOByTxIn(txId, index, unspentTxOuts);
    }

    @Override
    public boolean validateTxInAvailable(TxIn txIn) {
        return findUTXOByTxIn(txIn.getTxOutId(), txIn.getTxOutIndex()) != null;
    }

    @Override
    public UnspentTxOut findUTXOByTxIn(String txId, int index, List<UnspentTxOut> unspentTxOutList) {
        Optional<UnspentTxOut> unspentTxOutOptional = unspentTxOutList
                .stream()
                .filter(u -> u.getTxOutIndex() == index && u.getTxOutId().equals(txId))
                .findFirst();
        return unspentTxOutOptional.orElse(null);
    }

    @Override
    public long getAmount(TxIn txIn) {
        return findUTXOByTxIn(txIn.getTxOutId(), txIn.getTxOutIndex()).getAmount();
    }

    @Override
    public long getBalance(String address) {
        return unspentTxOuts.stream()
                .filter(unspentTxOut -> unspentTxOut.getAddress().equals(address))
                .map(UnspentTxOut::getAmount)
                .reduce(0L, (a, b) -> a + b);
    }

    @Override
    public List<UnspentTxOut> findUTXOByAddress(String ownerAddress) {
        return unspentTxOuts.stream().filter(u -> u.getAddress().equals(ownerAddress)).collect(Collectors.toList());
    }

    @Override
    public String getOwnerOfTx(TxIn txIn) {
        UnspentTxOut unspentTxOut = findUTXOByTxIn(txIn.getTxOutId(), txIn.getTxOutIndex());
        if (unspentTxOut == null) {
            System.out.println("could not find referenced txOut");
            throw new RuntimeException();
        }
        return unspentTxOut.getAddress();
    }
}
