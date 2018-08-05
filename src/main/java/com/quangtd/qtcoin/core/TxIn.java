package com.quangtd.qtcoin.core;

import lombok.Data;

@Data
public class TxIn {
    private String txOutId;
    private int txOutIndex;
    private String signature;
}
