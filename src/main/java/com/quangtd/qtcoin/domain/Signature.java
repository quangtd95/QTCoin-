package com.quangtd.qtcoin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Signature {
    private byte v;
    private byte[] r;
    private byte[] s;
}
