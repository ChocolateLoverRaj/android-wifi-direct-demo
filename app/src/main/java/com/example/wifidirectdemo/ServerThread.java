package com.example.wifidirectdemo;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
    private final ServerSocket serverSocket;
    private  final  OnAcceptListener onAcceptListener;

    public interface OnAcceptListener {
        void onAccept (Socket client);
    }

    public ServerThread(ServerSocket serverSocket, OnAcceptListener onAcceptListener) {
        this.serverSocket = serverSocket;
        this.onAcceptListener = onAcceptListener;
    }

    @Override
    public void run() {
        Log.d(this.getClass().getName(), "Waiting for client to connect");
        while (true) {
            try {
                Socket client = serverSocket.accept();
                onAcceptListener.onAccept(client);
            } catch (IOException e) {
                Log.d(this.getClass().getName(), "Server stopped. Socket#accept() aborted");
                return;
            }
        }
    }
}
