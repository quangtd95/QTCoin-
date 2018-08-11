package com.quangtd.qtcoin.domain;

import java.util.Arrays;
import java.util.Optional;

public enum MessageType {
    QUERY_LATEST(0),
    QUERY_ALL(1),
    RESPONSE_BLOCKCHAIN(2),
    CONNECT(3),
    ACCEPT(4);
    private int value;
    MessageType(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    public static MessageType getMessageTypeByValue(int value){
        Optional<MessageType> o = Arrays.stream(values()).filter(it->it.value == value).findAny();
        if (o.isPresent()){
            return o.get();
        }
        throw new IllegalArgumentException();
    }
}
