package com.example.wifidirectdemo;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;

public class SendNewDevicesThread extends  Thread {
    private final Socket socket;
    private  final  int position;
    private  final Collection<InGroup.GroupDevice> newGroupDevices;

    public SendNewDevicesThread (Socket socket, int position, Collection<InGroup.GroupDevice> newGroupDevices) {
        this.socket = socket;
        this.position = position;
        this.newGroupDevices = newGroupDevices;
    }

    @Override
    public void run() {
        super.run();
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(Updates.NEW_DEVICES.ordinal());
            outputStream.write(position);
            System.out.println("Sending " + newGroupDevices.size() + " new devices");
            DeviceStreamer.writeDeviceList(outputStream, newGroupDevices);
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Error sending new devices", e);
        }
    }
}
