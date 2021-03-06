package com.quangtd.qtcoin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TxIn implements Cloneable<TxIn> {
    private String txOutId;
    private int txOutIndex;
    private String signature;

    @Override
    public TxIn cloneObject() {
        TxIn txIn = new TxIn();
        txIn.setTxOutIndex(getTxOutIndex());
        txIn.setTxOutId(getTxOutId());
        txIn.setSignature(getSignature());
        return txIn;
    }

}
