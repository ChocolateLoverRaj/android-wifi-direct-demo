package com.example.wifidirectdemo;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class SendGroupThread extends Thread {
    private final Socket client;
    private final List<InGroup.GroupDevice> groupDevices;

    public SendGroupThread(Socket client, List<InGroup.GroupDevice> groupInfo) {
        this.client = client;
        this.groupDevices = groupInfo;
    }

    @Override
    public void run() {
        super.run();
        try {
            OutputStream outputStream = client.getOutputStream();
            DeviceStreamer.writeDeviceList(outputStream, groupDevices);
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Error sending device names", e);
        }
    }
}
