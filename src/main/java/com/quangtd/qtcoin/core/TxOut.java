package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class TxOut implements Cloneable<TxOut> {
    private String address;
    private long amount;

    @Override
    public TxOut clone() {
        return new TxOut(address, amount);
    }
}
