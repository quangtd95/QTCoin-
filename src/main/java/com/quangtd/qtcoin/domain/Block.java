package com.quangtd.qtcoin.domain;

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
    private List<Transaction> data;
    private int difficulty;
    private long nonce;
}
