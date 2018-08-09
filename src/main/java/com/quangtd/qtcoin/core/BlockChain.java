package com.quangtd.qtcoin.core;

import com.google.gson.Gson;
import com.quangtd.qtcoin.p2p.P2PServer;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockChain {
    @Getter
    private static List<Block> blockchain = new ArrayList<Block>() {{
        add(getGenesisBlock());
    }};

    @Getter
    private static List<UnspentTxOut> unspentTxOuts = Transaction.processTransactions(blockchain.get(0).getData(), new ArrayList<>(), 0);

    private static List<UnspentTxOut> getUnspentTxOuts = cloneList(unspentTxOuts);

    public static List<UnspentTxOut> cloneList(List<UnspentTxOut> list) {

        try {
            List<UnspentTxOut> clone = new ArrayList<>(list.size());
            for (UnspentTxOut item : list) {
                clone.add((UnspentTxOut) item.clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();

    }

    // in seconds
    private final static int BLOCK_GENERATION_INTERVAL = 10 * 1000;

    // in blocks
    private final static int DIFFICULTY_ADJUSTMENT_INTERVAL = 10;

    private static Block getGenesisBlock() {
        List<Transaction> transactions = new ArrayList<Transaction>() {{
            add(getGenesisTransaction());
        }};
        return new Block(0, "91a73664bc84c0baa1fc75ea6e4aa6d1d20c5df664c724e3159aefc2e1186627", "",
                1533469362328L, transactions, 4, 0);
    }

    private static Transaction getGenesisTransaction() {
        List<TxIn> txIns = new ArrayList<>();
        txIns.add(new TxIn("", 0, ""));
        List<TxOut> txOuts = new ArrayList<>();
        txOuts.add(new TxOut("04bfcab8722991ae774db48f934ca79cfb7dd991229153b9f732ba5334aafcd8e7266e47076996b55a14bf9913ee3145ce0cfc1372ada8ada74bd287450313534a",
                50));
        String id = "e655f6a5f26dc9b4cac6e46f52336428287759cf81ef5ff10854f69d68f43fa3";
        return new Transaction(id, txIns, txOuts);
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

    public static Block generateRawNextBlock(List<Transaction> transactions) {
        Block lastBlock = getLastBlock();
        int difficulty = getDifficulty(getBlockchain());
        int nextIndex = lastBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        Block block = findBlock(nextIndex, lastBlock.getHash(), nextTimestamp, transactions, difficulty);
        if (addBlockToChain(block)) {
            P2PServer.broadcast(P2PServer.responseLastBlock());
            return block;
        } else {
            return null;
        }
    }

    public static Block generateNextBlock(String address) {
        Transaction coinbaseTx = Transaction.getCoinbaseTransaction(address, getLastBlock().getIndex() + 1);
        List<Transaction> blockData = new ArrayList<Transaction>() {{
            add(coinbaseTx);
        }};
        blockData.addAll(TransactionPool.getTransactionPool());
        return generateRawNextBlock(blockData);
    }

    public static Block generateBlockWithTransaction(String privateKey, String receiveAddress, long amout) {
        Transaction coinbaseTx = Transaction.getCoinbaseTransaction(receiveAddress, getLastBlock().getIndex() + 1);
        Transaction tx = Wallet.createTransaction(receiveAddress, amout, privateKey, unspentTxOuts, TransactionPool.getTransactionPool());
        List<Transaction> blockData = new ArrayList<Transaction>() {{
            add(coinbaseTx);
            add(tx);
        }};
        return generateRawNextBlock(blockData);
    }

    public static List<UnspentTxOut> findUnspentTxOuts(String ownerAddress, List<UnspentTxOut> unspentTxOuts) {
        return unspentTxOuts.stream().filter(u -> u.getAddress().equals(ownerAddress)).collect(Collectors.toList());
    }

    public static Block findBlock(int index, String previousHash, long timestamp, List<Transaction> data,
                                  int difficulty) {
        long nonce = 0;
        while (true) {
            String hash = calculateHash(index, previousHash, timestamp, data, difficulty, nonce);
            if (hashMatchesDifficulty(hash, difficulty)) {
                return new Block(index, hash, previousHash, timestamp, data, difficulty, nonce);
            }
            nonce++;
            if (nonce >= Long.MAX_VALUE) {
                nonce = 0;
            }
        }
    }

    private static boolean hashMatchesDifficulty(String hash, int difficulty) {
        String hashInBinary = Utils.convertHexToBinary(hash);
        return hashInBinary.matches("[0]{" + difficulty + "}.*$");
    }

    public static boolean addBlockToChain(Block newBlock) {
        if (isValidNewBlock(newBlock, getLastBlock())) {
            List<UnspentTxOut> retVal = Transaction.processTransactions(newBlock.getData(), getUnspentTxOuts(), newBlock.getIndex());
            if (retVal == null) {
                System.out.println("Block is not valid in terms of transactions");
                return false;
            } else {
                blockchain.add(newBlock);
                setUnspentTxOuts(retVal);
                TransactionPool.updateTransactionPool(unspentTxOuts);
                return true;
            }

        }
        return false;
    }

    public static void setUnspentTxOuts(List<UnspentTxOut> aUnspentTxOuts) {
        System.out.println("replacing unspentTxOut with " + aUnspentTxOuts);
        unspentTxOuts = aUnspentTxOuts;
    }

    public static void replaceChain(List<Block> newBlockchain) {
        List<UnspentTxOut> unspentTxOuts = isValidChain(newBlockchain);
        if (unspentTxOuts != null && getAccumulatedDifficulty(newBlockchain) > getAccumulatedDifficulty(getBlockchain())) {
            System.out.println("Received blockchain is valid. Replacing current blockchain with received blockchain");
            blockchain.clear();
            blockchain.addAll(newBlockchain);
            setUnspentTxOuts(unspentTxOuts);
            TransactionPool.updateTransactionPool(unspentTxOuts);
            P2PServer.broadcast(P2PServer.responseLastBlock());
        } else {
            System.out.println("Received blockchain invalid");
        }
    }

    public static List<UnspentTxOut> isValidChain(List<Block> blockchain) {
        if (!isValidGenesis(blockchain.get(0))) {
            return null;
        }
        List<UnspentTxOut> unspentTxOuts = new ArrayList<>();
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            if (!isValidNewBlock(currentBlock, blockchain.get(i - 1))) {
                return null;
            }
            unspentTxOuts = Transaction.processTransactions(currentBlock.getData(), unspentTxOuts, currentBlock.getIndex());
            if (unspentTxOuts == null) {
                System.out.println("unvalid transaction in blockchain");
                return null;
            }
        }

        return unspentTxOuts;
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
        if (!hashMatchesDifficulty(newBlock.getHash(), newBlock.getDifficulty())) {
            System.out.println("block difficulty not satisfied.");
            return false;
        }
        return true;
    }

    private static String calculateHash(int index, String prevHash, long timestamp, List<Transaction> data, int difficulty, long nonce) {
        return DigestUtils.sha256Hex(index + prevHash + timestamp + new Gson().toJson(data) + difficulty + nonce);
    }

    private static String calculateHash(Block block) {
        return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getData(), block.getDifficulty(), block.getNonce());
    }

    private static int getAccumulatedDifficulty(List<Block> blockchain) {
        return blockchain.stream()
                .map(Block::getDifficulty)
                .map(difficulty -> (int) (Math.pow(2, difficulty)))
                .reduce(0, (a, b) -> a + b);
    }

    public static void handleReceivedTransaction(Transaction transaction) {
        TransactionPool.addToTransactionPool(transaction, getUnspentTxOuts());
    }

}
