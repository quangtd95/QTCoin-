package com.quangtd.qtcoin.domain;

import com.quangtd.qtcoin.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements Cloneable<Transaction> {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private List<TxIn> txIns;
    @Getter
    @Setter
    private List<TxOut> txOuts;

    public Transaction cloneObject() {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setTxIns(Utils.cloneList(txIns));
        transaction.setTxOuts(Utils.cloneList(txOuts));
        return transaction;
    }
}
