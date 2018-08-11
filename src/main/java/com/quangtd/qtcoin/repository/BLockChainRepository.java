package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Transaction;

import java.util.List;

public interface BLockChainRepository {

    void initBlockChain(Transaction genesisTransaction);

    void addBlockToChain(Block block);

    void replaceBlockchain(List<Block> newBlockchain);

    List<Block> getBlockchain();

    Block getGenesisBlock();

    Block getLastetBlock();
}
