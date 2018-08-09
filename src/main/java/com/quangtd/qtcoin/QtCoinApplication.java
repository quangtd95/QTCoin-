package com.quangtd.qtcoin;

import com.quangtd.qtcoin.p2p.P2PServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.SocketUtils;

import java.io.IOException;

@SpringBootApplication
public class QtCoinApplication {


    public static void main(String[] args) throws IOException {
        int p2pPort = SocketUtils.findAvailableTcpPort(6000,6001);
        SpringApplication.run(QtCoinApplication.class, args);
        P2PServer.initP2PSever(p2pPort);
        System.out.println("p2p started at port : " + p2pPort);
    }
}
