package com.quangtd.qtcoin.service;

import com.quangtd.qtcoin.domain.UnspentTxOut;
import com.quangtd.qtcoin.domain.Wallet;
import com.quangtd.qtcoin.repository.UnspentTxOutRepositoryImpl;
import com.quangtd.qtcoin.utils.Utils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component("walletSet")
public class WalletSet {

    @Autowired
    private UnspentTxOutRepositoryImpl unspentTxOutRepository;

}
