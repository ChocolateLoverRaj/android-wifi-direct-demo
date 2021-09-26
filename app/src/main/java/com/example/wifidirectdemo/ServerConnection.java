package com.example.wifidirectdemo;

import java.net.Socket;
import java.security.acl.Group;
import java.util.List;

public class ServerConnection {
    private final Socket client;

    public ServerConnection(Socket client) {
        this.client = client;
    }

    public void sendGroup(List<InGroup.GroupDevice> groupInfo) {
        new SendGroupThread(client, groupInfo).start();
    }

    public void sendNewDevices (int position, List<InGroup.GroupDevice> newDevices) {
        new SendNewDevicesThread(client, position, newDevices).start();
    };

    public void sendRemovedDevices (int position, int count) {
        new SendRemovedDevicesThread(client, position, count).start();
    }
}
