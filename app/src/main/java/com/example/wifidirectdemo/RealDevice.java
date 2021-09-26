package com.example.wifidirectdemo;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Get a com.example.wifidirectdemo.ReceiveGroupThread.Device from a android.net.wifi.p2p.WifiP2pDevice.
 */
public class RealDevice extends ReceiveGroupThread.Device {
    private final WifiP2pDevice wifiP2pDevice;

    public  RealDevice (WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }

    @Override
    public String getName() {
        return wifiP2pDevice.deviceName;
    }

    @Override
    public byte[] getAddress() {
        return MacAddress.fromString(wifiP2pDevice.deviceAddress).toByteArray();
    }
}
