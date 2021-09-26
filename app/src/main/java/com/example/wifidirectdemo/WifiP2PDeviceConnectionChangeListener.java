package com.example.wifidirectdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;

public class WifiP2PDeviceConnectionChangeListener {
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();
    private final ConnectionInfoListener connectionInfoListener;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws SecurityException {
            wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener);
        }
    };
    private final Activity activity;

    public WifiP2PDeviceConnectionChangeListener(Activity activity, ConnectionInfoListener connectionInfoListener) {
        this.activity = activity;
        this.connectionInfoListener = connectionInfoListener;
    }

    public void onCreate() {
        wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    }

    public void start() {
        activity.registerReceiver(receiver, intentFilter);
    }

    public void stop() {
        activity.unregisterReceiver(receiver);
    }

    public void requestNow () {
        wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener);
    }
}
