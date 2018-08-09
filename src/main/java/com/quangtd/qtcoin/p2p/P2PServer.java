package com.quangtd.qtcoin.p2p;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quangtd.qtcoin.core.Block;
import com.quangtd.qtcoin.core.BlockChain;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class P2PServer {

    @Getter
    private final static List<P2PSocket> sockets = new ArrayList<>();
    private final static Set<String> socketAddress = new HashSet<>();

    public static void initP2PSever(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        Thread handleConnection = new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("connection : " + Arrays.toString(socket.getLocalAddress().getAddress()) + ":" + socket.getPort());
                    initConnection(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        handleConnection.start();
    }

    private static void initConnection(Socket socket) {
        socketAddress.add(socket.getPort() + "");
        P2PSocket pSocket = new P2PSocket(socket);
        sockets.add(pSocket);
        initMessageHandler(pSocket);
    }

    private static void initMessageHandler(P2PSocket socket) {
        try {
            ObjectOutputStream outputStream = socket.getOs();
            outputStream.writeObject(P2PServer.queryLast());
            outputStream.flush();
            ObjectInputStream inputStream = socket.getIs();
            new HandlerMessageThread(inputStream, outputStream).start();
            System.out.println("start thread to handler messages!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Message responseLastBlock() {
        Message response = new Message();
        Gson gson = new Gson();
        response.setMessageType(MessageType.RESPONSE_BLOCKCHAIN.getValue());
        Type type = new TypeToken<ArrayList<Block>>() {
        }.getType();
        List<Block> blocks = new ArrayList<Block>() {{
            add(BlockChain.getLastBlock());
        }};
        response.setData(gson.toJson(blocks, type));
        return response;
    }

    public static Message responseBlockchain() {
        Message response = new Message();
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Block>>() {
        }.getType();
        response.setMessageType(MessageType.RESPONSE_BLOCKCHAIN.getValue());
        response.setData(gson.toJson(BlockChain.getBlockchain(), type));
        return response;
    }

    private static Message queryAll() {
        Message message = new Message();
        message.setMessageType(MessageType.QUERY_ALL.getValue());
        return message;
    }

    public static Message queryLast() {
        Message message = new Message();
        message.setMessageType(MessageType.QUERY_LATEST.getValue());
        return message;
    }

    public static Message connectMsg() {
        Message message = new Message();
        message.setMessageType(MessageType.CONNECT.getValue());
        return message;
    }

    public static Message acceptMsg() {
        Message message = new Message();
        message.setMessageType(MessageType.ACCEPT.getValue());
        return message;
    }

    public static void handleBlockchainResponse(List<Block> receivedBlockchain) {
        if (receivedBlockchain.size() == 0) {
            System.out.println("received block chain size of 0");
            return;
        }
        Block lastBlockHolding = BlockChain.getLastBlock();
        Block lastBlockReceived = receivedBlockchain.get(receivedBlockchain.size() - 1);
        if (lastBlockReceived.getIndex() > lastBlockHolding.getIndex()) {
            System.out.println("blockchain possibly behind. We got: "
                    + lastBlockHolding.getIndex() + " Peer got: " + lastBlockReceived.getIndex());
            if (lastBlockReceived.getPreviousHash().equals(lastBlockHolding.getHash())) {
                BlockChain.addBlockToChain(lastBlockReceived);
            } else if (receivedBlockchain.size() == 1) {
                System.out.println("We have to query the chain from our peer");
                broadcast(queryAll());
            } else {
                System.out.println("Received blockchain is longer than current blockchain");
                BlockChain.replaceChain(receivedBlockchain);
            }
        } else {
            BlockChain.replaceChain(receivedBlockchain);
        }
    }

    public static void broadcast(Message message) {
        getSockets().forEach(socket -> {
            try {
                ObjectOutputStream os = socket.getOs();
                os.reset();
                os.writeObject(message);
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean joinNetwork(String address) {
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);
        if (!socketAddress.contains(port + "")) {
            try {
                Socket socket = new Socket(host, port);
                initConnection(socket);
                System.out.println("join to BC network by " + address);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("you are already join this network!");
            return false;
        }
    }
}

class HandlerMessageThread extends Thread {
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Gson gson;

    HandlerMessageThread(ObjectInputStream is, ObjectOutputStream os) {
        try {
            this.is = is;
            this.os = os;
        } catch (Exception e) {
            e.printStackTrace();
        }
        gson = new Gson();
    }

    @Override
    public void run() {
        while (true) {
            try {
                messageHandler(is, os);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                try {
                    is.close();
                    os.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println("thread handler message stopped");
                break;
            }
        }
    }

    private void messageHandler(ObjectInputStream is, ObjectOutputStream os) throws IOException, ClassNotFoundException {
        Object o = is.readObject();
        if (o != null) {
            System.out.println(o.toString());
            Message message = (Message) o;
            Message response;
            switch (MessageType.getMessageTypeByValue(message.getMessageType())) {
                case QUERY_LATEST:
                    response = P2PServer.responseLastBlock();
                    os.writeObject(response);
                    os.flush();
                    break;
                case QUERY_ALL:
                    response = P2PServer.responseBlockchain();
                    os.writeObject(response);
                    os.flush();
                    break;
                case RESPONSE_BLOCKCHAIN:
                    Type type = new TypeToken<ArrayList<Block>>() {
                    }.getType();
                    List<Block> blockchain = gson.fromJson(message.getData(), type);
                    if (blockchain == null) {
                        System.out.println("invalid blocks received: ");
                        System.out.println(message.getData());
                    } else {
                        P2PServer.handleBlockchainResponse(blockchain);
                    }
                    break;
                case CONNECT:
                    response = P2PServer.acceptMsg();
                    os.writeObject(response);
                    os.flush();
                    break;
                case ACCEPT:
                    response = new Message();
                    os.writeObject(response);
                    os.flush();
                    break;
            }
        }
    }
}



