package com.quangtd.qtcoin.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {
    private int messageType;
    private String data;
}
