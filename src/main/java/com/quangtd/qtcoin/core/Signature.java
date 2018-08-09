package com.quangtd.qtcoin.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Signature {
    private byte v;
    private byte[] r;
    private byte[] s;
}
