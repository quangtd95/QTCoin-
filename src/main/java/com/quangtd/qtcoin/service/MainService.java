package com.quangtd.qtcoin.service;

import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.Wallet;

import java.util.List;

public interface MainService {
    Block getLastBlock();

    List<Block> getBlockchain();

    List<Transaction> getTransactionPool();

    long getBalance(String address);

    Wallet initWallet();

    boolean vaildateWallet(String address, String privateKey);

    List<Wallet> generateDumpWallet();

    List<Wallet> getWallets();

    /*
                            đào coin
                         */
    Block mineBlockWithAddress(String address);

    boolean sendCoin(String privateKey, String receiveAddress, long amount);

    boolean addBlockToChain(Block newBlock);

    void replaceChain(List<Block> newBlockchain);

    List<String> getPeers();

    boolean joinNetwork(String data);
}
