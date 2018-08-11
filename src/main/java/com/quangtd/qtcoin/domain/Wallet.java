package com.quangtd.qtcoin.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class Wallet implements Cloneable<Wallet> {
    @Getter
    @Setter
    private String privateKey;
    @Getter
    @Setter
    private String publicKey;
    @Setter
    @Getter
    long balance;
    @Setter
    @Getter
    private LocalDateTime createdDateTime;

    public Wallet(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        createdDateTime = LocalDateTime.now();
    }

    private Wallet(String publicKey, String privateKey, LocalDateTime createdDateTime) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.createdDateTime = createdDateTime;
    }

    @Override
    public Wallet cloneObject() {
        return new Wallet(publicKey, privateKey, createdDateTime);
    }
}
