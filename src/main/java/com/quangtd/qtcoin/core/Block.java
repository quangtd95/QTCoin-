package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Block {
    private int index;
    private String hash;
    private String previousHash;
    private long timestamp;
    private String data;
    private int difficulty;
    private long nonce;
}
