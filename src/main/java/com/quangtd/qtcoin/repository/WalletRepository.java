package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Wallet;

import java.util.List;

public interface WalletRepository {
    Wallet initWallet();

    boolean deleteWallet(String address, String privateKey);

    List<Wallet> dumpWallet();

    boolean validateAddress(String address);

    List<Wallet> getWallets();

    boolean validateWallet(String address, String privateKey);
}
