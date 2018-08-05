package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnspentTxOut {
    private final String txOutId;
    private final int txOutIndex;
    private final String address;
    private final long amount;
}
