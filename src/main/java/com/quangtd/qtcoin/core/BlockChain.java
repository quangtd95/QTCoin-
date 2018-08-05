package com.quangtd.qtcoin.core;

import com.google.gson.Gson;
import com.quangtd.qtcoin.p2p.P2PServer;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    @Getter
    private static List<Block> blockchain = new ArrayList<Block>() {{
        add(getGenesisBlock());
    }};

    // in seconds
    private final static int BLOCK_GENERATION_INTERVAL = 10 * 1000;

    // in blocks
    private final static int DIFFICULTY_ADJUSTMENT_INTERVAL = 10;

    private static Block getGenesisBlock() {
        return new Block(0, "91a73664bc84c0baa1fc75ea6e4aa6d1d20c5df664c724e3159aefc2e1186627", "",
                1533469362328L, "genesisBlock", 4, 0);
    }

    public BlockChain() {
        blockchain = new ArrayList<>();
        blockchain.add(getGenesisBlock());
    }

    public static int getDifficulty(List<Block> blockchain) {
        Block lastBlock = blockchain.get(blockchain.size() - 1);
        if (lastBlock.getIndex() % DIFFICULTY_ADJUSTMENT_INTERVAL == 0 && lastBlock.getIndex() != 0) {
            return getAdjustedDifficulty(lastBlock, blockchain);
        } else {
            return lastBlock.getDifficulty();
        }
    }

    private static int getAdjustedDifficulty(Block lastBlock, List<Block> blockchain) {
        Block prevAdjustmentBLock = blockchain.get(blockchain.size() - DIFFICULTY_ADJUSTMENT_INTERVAL);
        long timeExpected = BLOCK_GENERATION_INTERVAL * DIFFICULTY_ADJUSTMENT_INTERVAL;
        long timeActual = lastBlock.getTimestamp() - prevAdjustmentBLock.getTimestamp();
        if (timeActual < timeExpected / 2) {
            return prevAdjustmentBLock.getDifficulty() + 1;
        } else if (timeActual > timeExpected * 2) {
            return prevAdjustmentBLock.getDifficulty() - 1;
        } else {
            return prevAdjustmentBLock.getDifficulty();
        }
    }

    public static Block getLastBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

    public static Block generateNextBlock(String data) {
        Block lastBlock = getLastBlock();
        int nextIndex = lastBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        int difficulty = getDifficulty(getBlockchain());
        Block newBlock = findBlock(nextIndex,lastBlock.getHash(),nextTimestamp,data,difficulty);
        addBlock(newBlock);
        P2PServer.broadcast(P2PServer.responseLastBlock());
        return newBlock;
    }

    public static Block findBlock(int index, String previousHash,long timestamp,String data,
                                  int difficulty){
        long nonce = 0;
        while (true){
            String hash = calculateHash(index,previousHash,timestamp,data,difficulty,nonce);
            if (hashMatchesDifficulty(hash,difficulty)){
                return new Block(index,hash,previousHash,timestamp,data,difficulty,nonce);
            }
            nonce++;
            if (nonce >= Long.MAX_VALUE){
                nonce = 0;
            }
        }
    }

    private static boolean hashMatchesDifficulty(String hash, int difficulty) {
        String hashInBinary = hexToBinary(hash);
        return hashInBinary.matches("[0]{" + difficulty + "}.*$");
    }

    public static boolean addBlock(Block newBlock) {
        if (isValidNewBlock(newBlock, getLastBlock())) {
            blockchain.add(newBlock);
            return true;
        }
        return false;
    }

    public static void replaceChain(List<Block> newBlockchain) {
        if (isValidChain(newBlockchain) && getAccumulatedDifficulty(newBlockchain) > getAccumulatedDifficulty(getBlockchain())) {
            System.out.println("Received blockchain is valid. Replacing current blockchain with received blockchain");
            blockchain.clear();
            blockchain.addAll(newBlockchain);
            P2PServer.broadcast(P2PServer.responseLastBlock());
        } else {
            System.out.println("Received blockchain invalid");
        }
    }

    public static boolean isValidChain(List<Block> blockchain) {
        if (!isValidGenesis(blockchain.get(0))) {
            return false;
        }
        for (int i = 1; i < blockchain.size(); i++) {
            if (!isValidNewBlock(blockchain.get(i), blockchain.get(i - 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidGenesis(Block block) {
        Gson gson = new Gson();
        return gson.toJson(block).equals(gson.toJson(getGenesisBlock()));
    }

    private static boolean isValidNewBlock(Block newBlock, Block lastBlock) {
        if (lastBlock.getIndex() + 1 != newBlock.getIndex()) {
            System.out.println("invalid index");
            return false;
        }
        if (!lastBlock.getHash().equals(newBlock.getPreviousHash())) {
            System.out.println("invalid previous hash");
            return false;
        }
        if (!newBlock.getHash().equals(calculateHash(newBlock))) {
            System.out.println("invalid hash");
            return false;
        }
        if(!hashMatchesDifficulty(newBlock.getHash(),newBlock.getDifficulty())){
            System.out.println("block difficulty not satisfied.");
            return false;
        }
        return true;
    }

    private static String calculateHash(int index, String prevHash, long timestamp, String data, int difficulty, long nonce) {
        return DigestUtils.sha256Hex(index + prevHash + timestamp + new Gson().toJson(data) + difficulty + nonce);
    }

    private static String calculateHash(Block block) {
        return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getData(),block.getDifficulty(),block.getNonce());
    }

    private static String hexToBinary(String hash) {
        if (StringUtils.isEmpty(hash)) return StringUtils.EMPTY;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hash.length(); i++) {
            StringBuilder binary = new StringBuilder(new BigInteger(hash.charAt(i) + "", 16).toString(2));
            int length = binary.length();
            for (int j = 1; j <= 4 - length; j++) {
                binary.insert(0, "0");
            }
            result.append(binary);
        }
        return result.toString();
    }

    private static int getAccumulatedDifficulty(List<Block> blockchain){
        return blockchain.stream()
                .map(Block::getDifficulty)
                .map(difficulty -> (int) (Math.pow(2, difficulty)))
                .reduce(0, (a, b) -> a + b);
    }

}
