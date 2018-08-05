package com.quangtd.qtcoin.controller;

import com.google.gson.Gson;
import com.quangtd.qtcoin.core.Block;
import com.quangtd.qtcoin.core.BlockChain;
import com.quangtd.qtcoin.p2p.P2PServer;
import com.quangtd.qtcoin.p2p.P2PSocket;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class MainController {

    @GetMapping("/blocks")
    @ResponseBody
    public ResponseEntity<String> getBLocks(){
        return new ResponseEntity<>(new Gson().toJson(BlockChain.getBlockchain(), List.class), HttpStatus.OK);
    }

    @PostMapping("/mineBlock")
    @ResponseBody
    public ResponseEntity<String> mineBLock(String data){
        Block newBlock = BlockChain.generateNextBlock(data);
        System.out.println(newBlock.toString());
        return new ResponseEntity<>(new Gson().toJson(newBlock,Block.class), HttpStatus.OK);
    }

    @GetMapping("/peers")
    @ResponseBody
    public ResponseEntity<String> peers(){
        List<String> result =  P2PServer.getSockets().stream().map(P2PSocket::getSocket).map(socket -> socket.getInetAddress().toString() + ":" +socket.getPort()+"").collect(Collectors.toList());
        return new ResponseEntity<>(new Gson().toJson(result,List.class),HttpStatus.OK);
    }

    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<String> joinNetwork(String data){
        boolean success = P2PServer.joinNetwork(data);
        if (success){
            return new ResponseEntity<>("ok",HttpStatus.OK);
        } else {
            return new ResponseEntity<>("fail",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
