package com.example.wifidirectdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import androidx.annotation.Nullable;

import java.util.Collection;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    Callback callback;
    private Channel channel;
    private WifiP2pManager wifiP2pManager;
    private Collection<WifiP2pDevice> previousPeers;

    public WifiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager, Channel channel, Callback callback) {
        super();
        this.callback = callback;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) throws SecurityException {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            callback.onWifiStateChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed! We should probably do something about
            // that.
            wifiP2pManager.requestPeers(channel, wifiP2pDeviceList -> {
                Collection<WifiP2pDevice> refreshedPeers = wifiP2pDeviceList.getDeviceList();
                if (!refreshedPeers.equals(previousPeers)) {
                    previousPeers = refreshedPeers;
                    callback.onPeersChanged(refreshedPeers);
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            wifiP2pManager.requestConnectionInfo(channel, (WifiP2pInfo wifiP2pInfo) -> {
                if (wifiP2pInfo.groupFormed) {
                    wifiP2pManager.requestGroupInfo(channel, callback::onGroupChanged);
                } else
                    callback.onGroupChanged(null);
            });
            // Connection state changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//                DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                        .findFragmentById(R.id.frag_list);
//                fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }

    public static abstract class Callback {
        abstract public void onWifiStateChanged(boolean on);

        abstract public void onPeersChanged(Collection<WifiP2pDevice> wifiP2pDevices);

        abstract public void onGroupChanged(@Nullable WifiP2pGroup wifiP2pGroup);
    }
}
