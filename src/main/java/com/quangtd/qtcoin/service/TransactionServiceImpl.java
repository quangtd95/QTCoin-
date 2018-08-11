package com.quangtd.qtcoin.service;

import com.google.gson.Gson;
import com.quangtd.qtcoin.domain.Transaction;
import com.quangtd.qtcoin.domain.TxIn;
import com.quangtd.qtcoin.domain.TxOut;
import com.quangtd.qtcoin.domain.UnspentTxOut;
import com.quangtd.qtcoin.repository.TransactionRepository;
import com.quangtd.qtcoin.repository.UnspentTxOutRepository;
import com.quangtd.qtcoin.utils.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.quangtd.qtcoin.common.CommonConstants.COINBASE_AMOUNT;
import static com.quangtd.qtcoin.common.CommonConstants.KEY_GROUP_CONSUMED_TXOUT;
import static com.quangtd.qtcoin.common.CommonConstants.KEY_LEFT_OVER_AMOUNT;

@Service("transactionService")
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private UnspentTxOutRepository unspentTxOutRepo;

    @Autowired
    private TransactionRepository transactionRepo;

    @Override
    public List<Transaction> getTransactionPool() {
        return transactionRepo.getTransactionPool();
    }

    @Override
    public String getTransactionId(Transaction transaction) {
        String txInContent = transaction.getTxIns()
                .stream()
                .map(txIn -> txIn.getTxOutId() + txIn.getTxOutIndex())
                .reduce("", (s, s2) -> s + s2);
        String txOutContent = transaction.getTxOuts()
                .stream()
                .map(txOut -> txOut.getAddress() + txOut.getAddress())
                .reduce("", (s, s2) -> s + s2);
        return DigestUtils.sha256Hex(txInContent + txOutContent);
    }

    @Override
    public long getAmountOfTxIn(TxIn txIn) {
        return unspentTxOutRepo.getAmount(txIn);
    }

    public Transaction generateCoinbaseTransaction(String address, int blockIndex) {
        Transaction transaction = new Transaction();
        TxIn txIn = new TxIn();
        txIn.setSignature(StringUtils.EMPTY);
        txIn.setTxOutId(StringUtils.EMPTY);
        txIn.setTxOutIndex(blockIndex);
        transaction.setTxIns(new ArrayList<TxIn>() {{
            add(txIn);
        }});
        transaction.setTxOuts(new ArrayList<TxOut>() {{
            add(new TxOut(address, COINBASE_AMOUNT));
        }});
        transaction.setId(getTransactionId(transaction));
        return transaction;
    }

    private String signTxIn(Transaction transaction, int txInNumber, String privateKey) {
        //retrive txIn
        TxIn txIn = transaction.getTxIns().get(txInNumber);
        //get data to sign
        String dataToSign = transaction.getId();

        String referencedAddress = unspentTxOutRepo.getOwnerOfTx(txIn);
        String address = Utils.getPublicKeyFromPrivateKey(privateKey);
        if (!referencedAddress.equals(address)) {
            System.out.println("trying to sign an input with private +\n" +
                    " key that does not match the address that is referenced in txIn");
            throw new RuntimeException();
        }
        return Utils.signData(privateKey, dataToSign);
    }

    public List<UnspentTxOut> processTransactions(List<Transaction> transactions, int blockIndex) {
        if (!validateBlockTransactions(transactions, blockIndex)) {
            System.out.println("invalid block transactions");
            return null;
        }
        return unspentTxOutRepo.updateUnspentTxOuts(transactions);
    }

    public Transaction getGenesisTransaction() {
        List<TxIn> txIns = new ArrayList<>();
        txIns.add(new TxIn("", 0, ""));
        List<TxOut> txOuts = new ArrayList<>();
        txOuts.add(new TxOut("04bfcab8722991ae774db48f934ca79cfb7dd991229153b9f732ba5334aafcd8e7266e47076996b55a14bf9913ee3145ce0cfc1372ada8ada74bd287450313534a",
                50));
        String id = "1f232d867b92464dd78b02a81c1b4c6e33c39b49e2ad5aafa757ff35b798b0c9";
        return new Transaction(id, txIns, txOuts);
    }

    @Override
    public boolean addToTransactionPool(Transaction transaction) {
        if (!validateTransaction(transaction)) {
            System.out.println("Trying to add invalid tx to pool");
            return false;
        }
        if (!validateTransactionForPool(transaction, transactionRepo.getTransactionPool())) {
            System.out.println("Trying to add invalid tx to pool");
            return false;
        }
        System.out.println("adding to txPool: " + transaction);
        transactionRepo.addToPool(transaction);
        return true;
    }

    @Override
    public void updateTransactionPool() {
        List<Transaction> invalidTxs = new ArrayList<>();
        for (Transaction transaction : transactionRepo.getTransactionPool()) {
            for (TxIn txIn : transaction.getTxIns()) {
                if (!unspentTxOutRepo.validateTxInAvailable(txIn)) {
                    invalidTxs.add(transaction);
                    break;
                }
            }
        }
        if (invalidTxs.size() > 0) {
            System.out.println("removing the following transactions from txPool:");
            transactionRepo.getTransactionPool().removeAll(invalidTxs);
        }
    }

    /*
            loại bỏ những UTxOs đã nằm trong transaction pool
         */
    private List<UnspentTxOut> filterTxPoolTxs(List<UnspentTxOut> myUnspentTxOut, List<Transaction> transactions) {
        List<UnspentTxOut> myUnspentTxOutClone = Utils.cloneList(myUnspentTxOut);
        List<TxIn> txIns = new ArrayList<>();
        transactions.forEach(transaction -> txIns.addAll(transaction.getTxIns()));
        List<UnspentTxOut> removable = new ArrayList<>();
        for (UnspentTxOut unspentTxOut : myUnspentTxOutClone) {
            for (TxIn txIn : txIns) {
                if (txIn.getTxOutId().equals(unspentTxOut.getTxOutId())
                        && txIn.getTxOutIndex() == unspentTxOut.getTxOutIndex()) {
                    removable.add(unspentTxOut);
                    break;
                }
            }
        }
        myUnspentTxOutClone.removeAll(removable);
        return myUnspentTxOutClone;
    }

    private Map<String, Object> findTxOutsForAmount(long amount, List<UnspentTxOut> myUnspentTxOuts) {
        long currentAmount = 0;
        long leftOverAmount;
        List<UnspentTxOut> includedUnspentTxOuts = new ArrayList<>();
        for (UnspentTxOut unspentTxOut : myUnspentTxOuts) {
            includedUnspentTxOuts.add(unspentTxOut);
            currentAmount += unspentTxOut.getAmount();
            if (currentAmount >= amount) {
                leftOverAmount = currentAmount - amount;
                Map<String, Object> result = new HashMap<>();
                result.put(KEY_GROUP_CONSUMED_TXOUT, includedUnspentTxOuts);
                result.put(KEY_LEFT_OVER_AMOUNT, leftOverAmount);
                return result;
            }
        }
        throw new RuntimeException("not enough coins to send transaction");
    }

    /*
        tạo output khi có transaction .
     */
    private List<TxOut> createTxOuts(String receiveAddress, String myAddress, long amount, long leftOverAmount) {
        TxOut txOut = new TxOut(receiveAddress, amount);
        /*
        nếu ko có tiền thừa thì trả về 1 output duy nhất
         */
        if (leftOverAmount == 0) {
            return new ArrayList<TxOut>() {{
                add(txOut);
            }};
        } else {
            /*
                Nếu có tiền thừa thì tạo 2 output, 1 output tới người nhận, 1 output refund trở lại tiền thừa.
             */
            TxOut leftOverTx = new TxOut(myAddress, leftOverAmount);
            return new ArrayList<TxOut>() {{
                add(txOut);
                add(leftOverTx);
            }};
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Transaction createTransaction(String receiverAddress, long amount, String privateKey) {
        List<Transaction> transactionPool = getTransactionPool();
        String myAddress = Utils.getPublicKeyFromPrivateKey(privateKey);

        /*
            lọc những UTxOs của người gửi
         */
        List<UnspentTxOut> myUnspentTxOutsA = unspentTxOutRepo.findUTXOByAddress(myAddress);

        /*
            loại bỏ những UTxOs đã nằm trong transaction pool
         */
        List<UnspentTxOut> myUnspentTxOuts = filterTxPoolTxs(myUnspentTxOutsA, transactionPool);

        /*
            Từ những UTxOs đã lọc, lựa chọn UTxO để tạo transaction mới
         */
        Map<String, Object> txOutForAmount = findTxOutsForAmount(amount, myUnspentTxOuts);

        List<UnspentTxOut> includedUnspentTxOuts = (List<UnspentTxOut>) txOutForAmount.get(KEY_GROUP_CONSUMED_TXOUT);

        /*
            Tiền thừa khi gửi giao dịch
         */
        long leftOverAmount = (long) txOutForAmount.get(KEY_LEFT_OVER_AMOUNT);

        List<TxIn> unsignedTxIns = includedUnspentTxOuts.stream().map(this::toUnsignedTxIn).collect(Collectors.toList());

        Transaction transaction = new Transaction();
        transaction.setTxIns(unsignedTxIns);
        transaction.setTxOuts(createTxOuts(receiverAddress, myAddress, amount, leftOverAmount));
        transaction.setId(getTransactionId(transaction));

        for (int i = 0; i < transaction.getTxIns().size(); i++) {
            TxIn txIn = transaction.getTxIns().get(i);
            txIn.setSignature(signTxIn(transaction, i, privateKey));
        }
        return transaction;

    }

    private TxIn toUnsignedTxIn(UnspentTxOut unspentTxOut) {
        TxIn txIn = new TxIn();
        txIn.setTxOutId(unspentTxOut.getTxOutId());
        txIn.setTxOutIndex(unspentTxOut.getTxOutIndex());
        return txIn;
    }

    /**
     * VALIDATE
     */
    private boolean validateTxIn(TxIn txIn, Transaction transaction) {
        //kiểm tra input của transaction, có reference tới output nào ko
        UnspentTxOut refUnspentTxOut = unspentTxOutRepo.findUTXOByTxIn(txIn.getTxOutId(), txIn.getTxOutIndex());
        if (refUnspentTxOut == null) {
            System.out.println("referenced txOut not found: " + txIn);
            return false;
        }
        //kiểm tra input có sở hữu output này hay ko
        String address = refUnspentTxOut.getAddress();
        boolean validAddress = Utils.verifySignedData(address, transaction.getId(), txIn.getSignature());
        if (!validAddress) {
            System.out.println("invalid txIn signature");
            return false;
        }
        return true;
    }


    private boolean validateAllTxIn(Transaction transaction) {
        return transaction.getTxIns()
                .stream()
                .map(txIn -> validateTxIn(txIn, transaction))
                .reduce(true, (a, b) -> a & b);
    }

    private boolean validateTransaction(Transaction transaction) {
        if (!Objects.equals(getTransactionId(transaction), transaction.getId())) {
            System.out.println("invalid tx id: " + transaction.getId());
            return false;
        }
        if (!validateAllTxIn(transaction)) {
            System.out.println("some of the txIns are invalid in tx: " + transaction.getId());
            return false;
        }
        long totalAmountIn = transaction.getTxIns()
                .stream()
                .map(this::getAmountOfTxIn)
                .reduce(0L, (a, b) -> a + b);
        long totalAmountOut = transaction.getTxOuts()
                .stream()
                .map(TxOut::getAmount)
                .reduce(0L, (a, b) -> a + b);
        if (totalAmountIn != totalAmountOut) {
            System.out.println("totalTxOutValues != totalTxInValues in tx " + transaction.getId());
            return false;
        }
        return true;
    }

    private boolean validateBlockTransactions(List<Transaction> transactions, int blockIndex) {
        Transaction coinbaseTransaction = transactions.get(0);
        if (!validateCoinbaseTx(coinbaseTransaction, blockIndex)) {
            System.out.println("invalid coinbase transaction");
            return false;
        }
        // check for duplicate txIns. Each txIn can be included only once
        List<TxIn> txIns = new ArrayList<>();
        Gson gson = new Gson();
        transactions.forEach(transaction -> txIns.addAll(transaction.getTxIns()));
        int sizeAfterFilter = txIns.stream().map(txIn -> gson.toJson(txIn, TxIn.class))
                .distinct()
                .collect(Collectors.toList()).size();
        if (sizeAfterFilter != txIns.size()) {
            System.out.println("has duplicates");
            return false;
        }
        for (int i = 1; i < transactions.size(); i++) {
            if (!validateTransaction(transactions.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean validateCoinbaseTx(Transaction transaction, int blockIndex) {
        if (transaction == null) return false;
        if (!Objects.equals(getTransactionId(transaction), transaction.getId())) return false;
        if (transaction.getTxIns().size() != 1) return false;
        if (transaction.getTxIns().get(0).getTxOutIndex() != blockIndex) return false;
        if (transaction.getTxOuts().size() != 1) return false;
        if (transaction.getTxOuts().get(0).getAmount() != COINBASE_AMOUNT) return false;
        return true;
    }

    private boolean validateTransactionForPool(Transaction transaction, List<Transaction> transactionPool) {
        List<TxIn> txIns = getTransactionInputFromPool(transactionPool);
        for (TxIn txIn : transaction.getTxIns()) {
            if (validateTransactionInputContainedInList(txIn, txIns)) {
                System.out.println("txIn already found in the txPool");
                return false;
            }
        }
        return true;
    }

    private List<TxIn> getTransactionInputFromPool(List<Transaction> transactionPool) {
        List<TxIn> txIns = new ArrayList<>();
        for (Transaction transaction : transactionPool) {
            txIns.addAll(transaction.getTxIns());
        }
        return txIns;
    }

    private boolean validateTransactionInputContainedInList(TxIn txIn, List<TxIn> txIns) {
        if (txIns.isEmpty()) return false;
        for (TxIn t : txIns) {
            if (t.getTxOutIndex() == txIn.getTxOutIndex() && t.getTxOutId().equals(txIn.getTxOutId())) {
                return false;
            }
        }
        return true;
    }
}
