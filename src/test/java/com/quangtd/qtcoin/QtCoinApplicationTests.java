package com.quangtd.qtcoin;

import com.quangtd.qtcoin.utils.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.web3j.crypto.ECKeyPair;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QtCoinApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void testBytesToHex() {
        String s = "hello world";
        String hex = Utils.convertStringToHex(s);
        System.out.println(hex);
        String recovered = Utils.convertHexToString(hex);
        System.out.println(recovered);
        String bin = Utils.convertHexToBinary(hex);
        System.out.println(bin);
        String reHex = Utils.convertBinaryToHex(bin);
        System.out.println(reHex);
        Assert.assertEquals(recovered, s);
        Assert.assertEquals(reHex, hex);
    }

    @Test
    public void testGetKeyPair() {
        ECKeyPair keyPair = Utils.getRandomKeyPair();
        String privateKey = keyPair.getPrivateKey().toString(16);
        String publicKey = keyPair.getPublicKey().toString(16);
        System.out.println("private: " + privateKey);
        System.out.println("public: " + publicKey);
        String address = Utils.getPublicKeyFromPrivateKey(keyPair.getPrivateKey().toString(16));
        System.out.println("address: " + address);
        Assert.assertEquals(publicKey, address);
    }

    @Test
    public void testSignData() {
        String message = "hello world";
        ECKeyPair keyPair = Utils.getRandomKeyPair();
        String privateKey = keyPair.getPrivateKey().toString(16);
        String publicKey = keyPair.getPublicKey().toString(16);
        System.out.println("private: " + privateKey);
        System.out.println("public: " + publicKey);
        String signature = Utils.signData(privateKey, message);
        boolean verify = Utils.verifySignedData(publicKey, message, signature);
        Assert.assertTrue(verify);
        String message2 = "fucking app";
        boolean verify2 = Utils.verifySignedData(publicKey, message2, signature);
        Assert.assertFalse(verify2);
    }

}
