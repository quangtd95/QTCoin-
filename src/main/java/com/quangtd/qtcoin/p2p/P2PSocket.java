package com.quangtd.qtcoin.p2p;

import lombok.Data;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Data
public class P2PSocket {
    private Socket socket;
    private ObjectInputStream is;
    private ObjectOutputStream os;

    P2PSocket(Socket socket) {
        this.socket = socket;
        try {
            os = new ObjectOutputStream(socket.getOutputStream());
            os.writeObject(P2PServer.connectMsg());
            os.flush();
            is = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
