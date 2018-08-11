package com.quangtd.qtcoin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnspentTxOut implements Cloneable<UnspentTxOut> {
    private final String txOutId;
    private final int txOutIndex;
    private final String address;
    private final long amount;

    @Override
    public UnspentTxOut cloneObject() {
        return new UnspentTxOut(txOutId, txOutIndex, address, amount);
    }
}
