package com.example.wifidirectdemo;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListUpdateCallback;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerManager implements ListUpdateCallback {
    private final ServerThread serverThread;
    private final ServerSocket serverSocket = new ServerSocket(PORT);
    private final List<ServerConnection> serverConnections = new ArrayList<>();
    public final GroupInfoCallback callback;

    public static final int PORT = 37690;

    public interface GroupInfoCallback {
        List<InGroup.GroupDevice> getCurrentInfo();
    }

    public ServerManager(GroupInfoCallback callback) throws IOException {
        this.callback = callback;
        serverThread = new ServerThread(serverSocket, (Socket client) -> {
            ServerConnection serverConnection = new ServerConnection(client);
            serverConnections.add(serverConnection);
            serverConnection.sendGroup(callback.getCurrentInfo());
        });
    }

    public void start() {
        serverThread.start();
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    @Override
    public void onInserted(int position, int count) {
        List<InGroup.GroupDevice> insertedDevices = callback.getCurrentInfo().subList(position, position + count);
        serverConnections.forEach((ServerConnection serverConnection) -> serverConnection.sendNewDevices(position, insertedDevices));
    }

    @Override
    public void onRemoved(int position, int count) {
        serverConnections.forEach((ServerConnection serverConnection) -> serverConnection.sendRemovedDevices(position, count));
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        Log.w(this.getClass().getName(), "Device moved from " + fromPosition + " to " + toPosition + ". Move updates to client not implemented.");
    }

    @Override
    public void onChanged(int position, int count, @Nullable Object payload) {
        Log.w(this.getClass().getName(), "Changed at position " + position + ". Name: " + callback.getCurrentInfo().get(position).getDevice().getName() + ". Change updates to client not implemented.");
    }
}
