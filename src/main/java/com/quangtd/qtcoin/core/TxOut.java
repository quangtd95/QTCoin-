package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TxOut {
    private String address;
    private long amount;
}
