package com.example.wifidirectdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public class DeviceStreamer {
    private static void writeDevice (OutputStream outputStream, InGroup.GroupDevice device) throws IOException {
        outputStream.write(device.getDevice().getName().getBytes().length);
        outputStream.write(device.getDevice().getName().getBytes());
        outputStream.write(device.getDevice().getAddress());
    }

    public static void writeDeviceList (OutputStream outputStream, Collection<InGroup.GroupDevice> devices) throws  IOException {
        outputStream.write(devices.size());
        for (InGroup.GroupDevice device : devices) {
            writeDevice(outputStream, device);
        }
    }

    public static void readDeviceList (InputStream inputStream, ReceiveGroupThread.Callback.StreamedListCallback<ReceiveGroupThread.Device> callback) throws  IOException {
        final int devices = inputStream.read();
        callback.onSize(devices);
        for (int i = 0; i < devices; i++) {
            byte[] nameBytes = new byte[inputStream.read()];
            inputStream.read(nameBytes);
            byte[] addressBytes = new byte[6];
            inputStream.read(addressBytes);
            callback.onItem(new ReceiveGroupThread.Device() {
                @Override
                public String getName() {
                    return new String(nameBytes);
                }

                @Override
                public byte[] getAddress() {
                    return addressBytes;
                }
            }, i);
        }
    }
}
