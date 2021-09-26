package com.example.wifidirectdemo;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class ReceiveGroupThread extends Thread {
    private final InetAddress host;
    private final int port;
    private final Socket socket;
    private final Callback callback;

    public ReceiveGroupThread(Socket socket, InetAddress host, int port, Callback callback) {
        this.host = host;
        this.port = port;
        this.socket = socket;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        try {
            socket.bind(null);
            Log.d(this.getClass().getName(), "Connecting to host. Host: " + host + ". Port: " + port);
            // 5 Connect attempts with 2 second delay
            for (int i = 0; i < 5; i++) {
                try {
                    socket.connect(new InetSocketAddress(host, port), 2000);
                    break;
                } catch (ConnectException e) {
                    Log.e(getClass().getName(), "Error connecting to host in attempt " + i, e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        return;
                    }
                }
            }
            if (!socket.isConnected()) {
                Log.e(getClass().getName(), "Socket not connected");
            }
            Log.d(this.getClass().getName(), "Client connected");
            InputStream inputStream = socket.getInputStream();

            // The first message is always the initial devices
            DeviceStreamer.readDeviceList(inputStream, callback.onInitialDevicesList());

            // The rest of the messages are updates
            while (true) {
                int update;
                try {
                    update = inputStream.read();
                } catch (IOException e) {
                    Log.e(getName(), "io exception: ", e);
                    break;
                }
                System.out.println("Update type: " + update);
                if (Updates.values()[update] == Updates.NEW_DEVICES) {
                    int position = inputStream.read();
                    DeviceStreamer.readDeviceList(inputStream, callback.onInsertedDevices(position));
                } else if (Updates.values()[update] == Updates.REMOVED_DEVICES) {
                    final int position = inputStream.read();
                    final int count = inputStream.read();
                    callback.onRemovedDevices(position, count);
                } else {
                        Log.e(getClass().getName(), "Unrecognized update type. Maybe there is a version mismatch.");
                }
            }
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Error doing socket client stuff", e);
        }
    }

    public static abstract class Device {
        abstract public String getName();

        abstract public byte[] getAddress();

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Device))
                return false;
            return getName().equals(((Device) obj).getName()) && Arrays.equals(getAddress(), ((Device) obj).getAddress());
        }
    }

    public interface Callback {
        interface StreamedListCallback<T> {
            void onSize(int totalDevices);

            void onItem(T device, int index);
        }

        StreamedListCallback<Device> onInitialDevicesList();

        StreamedListCallback<Device> onInsertedDevices (int position);

        void  onRemovedDevices (int position, int count);
    }
}
