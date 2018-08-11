package com.quangtd.qtcoin.service;

import com.google.gson.Gson;
import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.UnspentTxOut;
import com.quangtd.qtcoin.domain.Wallet;
import com.quangtd.qtcoin.p2p.P2PServer;
import com.quangtd.qtcoin.p2p.P2PSocket;
import com.quangtd.qtcoin.repository.BLockChainRepository;
import com.quangtd.qtcoin.repository.UnspentTxOutRepository;
import com.quangtd.qtcoin.repository.WalletRepository;
import com.quangtd.qtcoin.utils.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.quangtd.qtcoin.common.CommonConstants.BLOCK_GENERATION_INTERVAL;
import static com.quangtd.qtcoin.common.CommonConstants.DIFFICULTY_ADJUSTMENT_INTERVAL;

@Component("mainService")
public class MainServiceImpl implements MainService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UnspentTxOutRepository unspentTxOutRepo;

    @Autowired
    private BLockChainRepository blockchainRepo;

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private P2PServer p2PServer;

    @PostConstruct
    public void initBlockchain() {
        blockchainRepo.initBlockChain(transactionService.getGenesisTransaction());
        List<UnspentTxOut> genesisUnspentTxOuts = transactionService.processTransactions(blockchainRepo.getGenesisBlock().getData(),
                blockchainRepo.getGenesisBlock().getIndex());
        unspentTxOutRepo.setUnspentTxOuts(genesisUnspentTxOuts);
    }

    @Override
    public Block getLastBlock() {
        return blockchainRepo.getLastetBlock();
    }

    @Override
    public List<Block> getBlockchain() {
        return blockchainRepo.getBlockchain();
    }

    @Override
    public List<Transaction> getTransactionPool() {
        return transactionService.getTransactionPool();
    }

    @Override
    public long getBalance(String address) {
        return unspentTxOutRepo.getBalance(address);
    }

    @Override
    public Wallet initWallet() {
        return walletRepo.initWallet();
    }

    @Override
    public boolean vaildateWallet(String address, String privateKey) {
        return walletRepo.validateWallet(address, privateKey);
    }

    @Override
    public List<Wallet> generateDumpWallet() {
        return walletRepo.dumpWallet();
    }

    @Override
    public List<Wallet> getWallets() {
        List<Wallet> wallets = walletRepo.getWallets();
        wallets.forEach(it -> it.setBalance(unspentTxOutRepo.getBalance(it.getPublicKey())));
        return wallets;
    }

    //region mine block
    @Override
    public Block mineBlockWithAddress(String address) {
        if (!walletRepo.validateAddress(address)) {
            throw new RuntimeException("adress does not exists.");
        }
        Transaction coinbaseTx = transactionService.generateCoinbaseTransaction(address, getLastBlock().getIndex() + 1);
        List<Transaction> blockData = new ArrayList<Transaction>() {{
            add(coinbaseTx);
        }};
        blockData.addAll(transactionService.getTransactionPool());
        return minePhysicalBlock(blockData);
    }

    /*
        Đào coin + giao dịch
     */
    public Block mineBlockWithTransaction(String privateKey, String receiveAddress, long amout) {
        if (!walletRepo.validateAddress(receiveAddress)) {
            throw new RuntimeException("adress does not exists.");
        }
        Transaction coinbaseTx = transactionService.generateCoinbaseTransaction(receiveAddress, getLastBlock().getIndex() + 1);
        Transaction tx = transactionService.createTransaction(receiveAddress, amout, privateKey);
        List<Transaction> blockData = new ArrayList<Transaction>() {{
            add(coinbaseTx);
            add(tx);
        }};
        return minePhysicalBlock(blockData);
    }

    private Block minePhysicalBlock(List<Transaction> transactions) {
        Block lastBlock = getLastBlock();
        int difficulty = calculateDifficulty(getBlockchain());
        int nextIndex = lastBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        Block block = mineSuitableBlock(nextIndex, lastBlock.getHash(), nextTimestamp, transactions, difficulty);
        if (addBlockToChain(block)) {
            p2PServer.broadcast(p2PServer.responseLastBlock());
            return block;
        } else {
            return null;
        }
    }

    private Block mineSuitableBlock(int index, String previousHash, long timestamp, List<Transaction> data,
                                    int difficulty) {
        long nonce = 0;
        while (true) {
            String hash = calculateHash(index, previousHash, timestamp, data, difficulty, nonce);
            if (validateHashMatchesDifficulty(hash, difficulty)) {
                return new Block(index, hash, previousHash, timestamp, data, difficulty, nonce);
            }
            nonce++;
            if (nonce >= Long.MAX_VALUE) {
                nonce = 0;
            }
        }
    }
    //endregion

    @Override
    public boolean sendCoin(String privateKey, String receiveAddress, long amount) {
        if (!walletRepo.validateAddress(receiveAddress)) {
            throw new RuntimeException("adress does not exists.");
        }
        Transaction transaction = transactionService.createTransaction(receiveAddress, amount, privateKey);
        return transactionService.addToTransactionPool(transaction);
    }

    @Override
    public boolean addBlockToChain(Block newBlock) {
        if (validateNewBlock(newBlock, getLastBlock())) {
            List<UnspentTxOut> retVal = transactionService.processTransactions(newBlock.getData(), newBlock.getIndex());
            if (retVal == null) {
                System.out.println("Block is not valid in terms of transactions");
                return false;
            } else {
                blockchainRepo.addBlockToChain(newBlock);
                unspentTxOutRepo.setUnspentTxOuts(retVal);
                transactionService.updateTransactionPool();
                return true;
            }

        }
        return false;
    }

    @Override
    public void replaceChain(List<Block> newBlockchain) {
        List<UnspentTxOut> unspentTxOuts = validateBlockchain(newBlockchain);
        if (unspentTxOuts != null && calculateAccumulatedDifficulty(newBlockchain) > calculateAccumulatedDifficulty(getBlockchain())) {
            System.out.println("Received blockchain is valid. Replacing current blockchain with received blockchain");
            blockchainRepo.replaceBlockchain(newBlockchain);
            unspentTxOutRepo.setUnspentTxOuts(unspentTxOuts);
            transactionService.updateTransactionPool();
            p2PServer.broadcast(p2PServer.responseLastBlock());
        } else {
            System.out.println("Received blockchain invalid");
        }
    }

    //region validate
    private List<UnspentTxOut> validateBlockchain(List<Block> blockchain) {
        if (!validateGenesisBlock(blockchain.get(0))) {
            return null;
        }
        List<UnspentTxOut> unspentTxOuts = new ArrayList<>();
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            if (!validateNewBlock(currentBlock, blockchain.get(i - 1))) {
                return null;
            }
            unspentTxOuts = transactionService.processTransactions(currentBlock.getData(), currentBlock.getIndex());
            if (unspentTxOuts == null) {
                System.out.println("unvalid transaction in blockchain");
                return null;
            }
        }

        return unspentTxOuts;
    }

    private boolean validateHashMatchesDifficulty(String hash, int difficulty) {
        String hashInBinary = Utils.convertHexToBinary(hash);
        return hashInBinary.matches("[0]{" + difficulty + "}.*$");
    }

    private boolean validateGenesisBlock(Block block) {
        Gson gson = new Gson();
        return gson.toJson(block).equals(gson.toJson(blockchainRepo.getGenesisBlock()));
    }

    private boolean validateNewBlock(Block newBlock, Block lastBlock) {
        if (lastBlock.getIndex() + 1 != newBlock.getIndex()) {
            System.out.println("invalid index");
            return false;
        }
        if (!Objects.equals(lastBlock.getHash(), newBlock.getPreviousHash())) {
            System.out.println("invalid previous hash");
            return false;
        }
        if (!Objects.equals(newBlock.getHash(), calculateHash(newBlock))) {
            System.out.println("invalid hash");
            return false;
        }
        if (!validateHashMatchesDifficulty(newBlock.getHash(), newBlock.getDifficulty())) {
            System.out.println("block difficulty not satisfied.");
            return false;
        }
        return true;
    }
    //endregion


    //region Calculate
    private String calculateHash(int index, String prevHash, long timestamp, List<Transaction> data, int difficulty, long nonce) {
        return DigestUtils.sha256Hex(index + prevHash + timestamp + new Gson().toJson(data) + difficulty + nonce);
    }

    private String calculateHash(Block block) {
        return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getData(), block.getDifficulty(), block.getNonce());
    }

    private int calculateAccumulatedDifficulty(List<Block> blockchain) {
        return blockchain.stream()
                .map(Block::getDifficulty)
                .map(difficulty -> (int) (Math.pow(2, difficulty)))
                .reduce(0, (a, b) -> a + b);
    }

    private int calculateDifficulty(List<Block> blockchain) {
        Block lastBlock = blockchain.get(blockchain.size() - 1);
        if (lastBlock.getIndex() % DIFFICULTY_ADJUSTMENT_INTERVAL == 0 && lastBlock.getIndex() != 0) {
            return calculateAdjustedDifficulty(lastBlock, blockchain);
        } else {
            return lastBlock.getDifficulty();
        }
    }

    private int calculateAdjustedDifficulty(Block lastBlock, List<Block> blockchain) {
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
    //endregion


    //region Network
    public void handleReceivedTransaction(Transaction transaction) {
        transactionService.addToTransactionPool(transaction);
    }

    @Override
    public List<String> getPeers() {
        return p2PServer.getSockets().stream().map(P2PSocket::getSocket).map(socket -> socket.getInetAddress().toString() + ":" + socket.getPort() + "").collect(Collectors.toList());
    }

    @Override
    public boolean joinNetwork(String data) {
        return p2PServer.joinNetwork(data);
    }
    //endregion
}
