package com.quangtd.qtcoin.repository;

import com.quangtd.qtcoin.domain.Wallet;
import com.quangtd.qtcoin.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;

@Repository("walletRepository")
public class WalletRepositoryImpl implements WalletRepository {

    private List<Wallet> wallets = new ArrayList<>();

    @Override
    public Wallet initWallet() {
        ECKeyPair keyPair = Utils.getRandomKeyPair();
        String privKey = keyPair.getPrivateKey().toString(16);
        String pubKey = keyPair.getPublicKey().toString(16);
        if (wallets.contains(pubKey)) {
            return null;
        }
        Wallet wallet = new Wallet(pubKey, privKey);
        wallets.add(wallet);
        return wallet;
    }

    @Override
    public boolean deleteWallet(String address, String privateKey) {
        BigInteger bigInteger = new BigInteger(privateKey, 16);
        if (!Sign.publicKeyFromPrivate(bigInteger).toString(16).equals(address)) {
            System.out.println("error private key + address");
            return false;
        }
        boolean success = wallets.remove(address);
        if (!success) {
            System.out.println("address doesn't exists");
        }
        return success;
    }

    @Override
    public List<Wallet> dumpWallet() {
        List<Wallet> wallets = new ArrayList<>();
        wallets.add(new Wallet("76b65ccd13d63eaeca38410f7765cdfefb7b9b1cc3c53b9bcab02de181dbdbb38cf69df84ba84a6f04bfed6c0eabef7d3f3a05c369205c1dc67c22c91198726b",
                "6847a42954ca4765ddd51941074310128e7319c85891ff8008cf1706a674e59b"));

        wallets.add(new Wallet("68c78179875cb785fd6fc57f8109f9b34c24d0faad33237981244c915c6b8a5d3f65f04adf7b70db2a7420e96a306134d8bd5387829c5a77f0127871c2f9d6af",
                "816344c44f63dc1acf86c626f0a82f940406c368f1336e0bbbc17d68635beeb5"));

        wallets.add(new Wallet("f7004f86b209d3386db0453ca8d9f99d275bb264040d33fbcde46035252fb14bc3c955d2e83b1e0beeafae59530d42dd44e7b10c31260455349f2dcea024a1d4",
                "7f49d4fafd0cc0c6c83dcfd59617fec658733c645d9decd75f4d04c2e97f081e"));

        wallets.add(new Wallet("41af85c572da2349534d8e7dab551b43c4e6448bec6c5a90ea9c56e5999f9cd0159bd9156be92083a66d88467a0258d0f1a795772f457fbff7d370d60f25640d",
                "90f4f43e281eb3eab0fda874cc8ec4d238fac56990d3cfb25841600662322e56"));

        wallets.add(new Wallet("99418afd6c7b62f3b06cb782465a6527ecd978f020c143a414189121afad5154373696a9056f78c1d6c378f6d80436991c711af3654011a950fdd1feebcf6cab",
                "b5e5536c0b23eed18364c02a19620c2ad5e9c090cb536caaddebf6ffc1d7cc9f"));
        this.wallets.addAll(wallets);
        return wallets;
    }

    @PostConstruct
    public void initDumpWallet() {
        dumpWallet();
    }

    @Override
    public boolean validateAddress(String address) {
        for (Wallet wallet : wallets) {
            if (wallet.getPublicKey().equals(address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Wallet> getWallets() {
        List<Wallet> walletsClone = Utils.cloneList(wallets);
        for (Wallet wallet : walletsClone) {
            wallet.setPrivateKey(StringUtils.EMPTY);
        }
        return walletsClone;
    }

    @Override
    public boolean validateWallet(String address, String privateKey) {
        for (Wallet wallet : wallets) {
            if (Objects.equals(wallet.getPrivateKey(), privateKey) && Objects.equals(wallet.getPublicKey(), address)) {
                return true;
            }
        }
        return false;
    }
}
