package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.quangtd.qtcoin.common.CommonConstants.DEFAULT_DIFFICULTY;

@Repository
public class BlockChainRepositoryImpl implements BLockChainRepository {

    private List<Block> blockchain;

    @Override
    public void initBlockChain(Transaction genesisTransaction) {
        blockchain = new ArrayList<>();
        blockchain.add(createGenesisBlock(genesisTransaction));
    }

    private Block createGenesisBlock(Transaction genesisTransaction) {
        List<Transaction> lstTransaction = new ArrayList<Transaction>() {{
            add(genesisTransaction);
        }};
        return new Block(0, "91a73664bc84c0baa1fc75ea6e4aa6d1d20c5df664c724e3159aefc2e1186627", "",
                1533469362328L, lstTransaction, DEFAULT_DIFFICULTY, 0);
    }

    @Override
    public void addBlockToChain(Block block) {
        blockchain.add(block);
    }

    @Override
    public void replaceBlockchain(List<Block> newBlockchain) {
        blockchain.clear();
        blockchain.addAll(newBlockchain);
    }

    @Override
    public List<Block> getBlockchain() {
        return blockchain;
    }

    @Override
    public Block getGenesisBlock() {
        return blockchain.get(0);
    }

    @Override
    public Block getLastetBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

}
