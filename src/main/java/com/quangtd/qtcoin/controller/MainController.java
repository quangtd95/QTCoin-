package com.quangtd.qtcoin.controller;

import com.quangtd.qtcoin.service.*;
import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Wallet;
import com.quangtd.qtcoin.domain.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MainController {

    @Autowired
    private MainService mainService;

    @GetMapping("/blocks")
    @ResponseBody
    public ResponseEntity getBLocks() {
        return Response.createResponse(mainService.getBlockchain(), List.class);
    }

    @PostMapping("/mineBlock")
    @ResponseBody
    public ResponseEntity mineBLock(String address) {
        Block newBlock = mainService.mineBlockWithAddress(address);
        System.out.println(newBlock.toString());
        return Response.createResponse(newBlock, Block.class);
    }

    @GetMapping("/peers")
    @ResponseBody
    public ResponseEntity peers() {
        List<String> result = mainService.getPeers();
        return Response.createResponse(result, List.class);
    }

    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity joinNetwork(String data) {
        boolean success = mainService.joinNetwork(data);
        if (success) {
            return Response.createResponse("ok");
        } else {
            return Response.createResponse("fail");
        }
    }

    @PostMapping("/balance")
    @ResponseBody
    public ResponseEntity getBalance(String address) {
        long balance = mainService.getBalance(address);
        return Response.createResponse(balance + "");
    }

    @GetMapping("/transactionPool")
    @ResponseBody
    public ResponseEntity getTransactionPool() {
        return Response.createResponse(mainService.getTransactionPool(), List.class);
    }

    @GetMapping("/initWallet")
    @ResponseBody
    public ResponseEntity initWallet() {
        return Response.createResponse(mainService.initWallet(), Wallet.class);
    }

    @GetMapping("/dumpWallets")
    @ResponseBody
    public ResponseEntity dumpWallet() {
        return Response.createResponse(mainService.generateDumpWallet(), List.class);
    }

  /*  @PostMapping("/sendCoins")
    @ResponseBody
    public ResponseEntity sendCoins(String privateKey, String receiveAddress, long amount) {
        return Response.createResponse(mainService.sendCoin(privateKey, receiveAddress, amount), boolean.class);
    }*/
}
