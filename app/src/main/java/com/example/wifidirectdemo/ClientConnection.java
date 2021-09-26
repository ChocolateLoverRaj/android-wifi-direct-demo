package com.example.wifidirectdemo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection {
    private final InetAddress host;
    private final int port;
    private final ReceiveGroupThread.Callback callback;
    private final Socket socket = new Socket();

    public ClientConnection(InetAddress host, int port, ReceiveGroupThread.Callback callback) {
        this.host = host;
        this.port = port;
        this.callback = callback;
    }

    public void start() {
        new ReceiveGroupThread(socket, host, port, callback).start();
    }

    public void stop() throws IOException {
        socket.close();
    }
}
