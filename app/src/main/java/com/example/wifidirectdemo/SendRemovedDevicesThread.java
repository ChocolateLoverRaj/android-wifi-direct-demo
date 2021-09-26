package com.example.wifidirectdemo;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class SendRemovedDevicesThread extends Thread {
    private final Socket socket;
    private final int position;
    private final int count;

    public SendRemovedDevicesThread(Socket socket, int position, int count) {
        this.socket = socket;
        this.position = position;
        this.count = count;
    }

    @Override
    public void run() {
        super.run();
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(Updates.REMOVED_DEVICES.ordinal());
            outputStream.write(position);
            outputStream.write(count);
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Error sending new devices", e);
        }
    }

}
