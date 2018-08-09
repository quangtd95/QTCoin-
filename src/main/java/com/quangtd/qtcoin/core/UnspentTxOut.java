package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class UnspentTxOut implements Cloneable {
    private final String txOutId;
    private final int txOutIndex;
    private final String address;
    private final long amount;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new UnspentTxOut(txOutId, txOutIndex, address, amount);
    }
}
