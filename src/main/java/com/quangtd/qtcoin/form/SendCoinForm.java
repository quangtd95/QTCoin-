package com.quangtd.qtcoin.form;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SendCoinForm {
    private String privateKey;
    private String receiveAddress;
    private long amount;
}
