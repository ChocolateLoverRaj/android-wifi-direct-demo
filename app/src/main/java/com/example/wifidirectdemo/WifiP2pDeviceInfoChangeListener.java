package com.example.wifidirectdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;

public class WifiP2pDeviceInfoChangeListener {
    private final IntentFilter intentFilter = new IntentFilter();
    private final Callback callback;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            callback.onDeviceInfoChange(device);
        }
    };
    private final Activity activity;

    public WifiP2pDeviceInfoChangeListener(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void onCreate() throws SecurityException {
        final WifiP2pManager wifiP2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        final Channel channel = wifiP2pManager.initialize(activity, activity.getMainLooper(), null);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiP2pManager.requestDeviceInfo(channel, (WifiP2pDevice device) -> {
                if (device != null)
                    callback.onDeviceInfoChange(device);
            });
        }
    }

    public void start() {
        activity.registerReceiver(receiver, intentFilter);
    }

    public void stop() {
        activity.unregisterReceiver(receiver);
    }

    public interface Callback {
        void onDeviceInfoChange(WifiP2pDevice wifiP2pDevice);
    }
}
