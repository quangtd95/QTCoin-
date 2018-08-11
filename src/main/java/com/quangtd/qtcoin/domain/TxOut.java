package com.quangtd.qtcoin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TxOut implements Cloneable<TxOut> {
    private String address;
    private long amount;

    @Override
    public TxOut cloneObject() {
        return new TxOut(address, amount);
    }
}
