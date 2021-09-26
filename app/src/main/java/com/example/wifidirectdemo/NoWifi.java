package com.example.wifidirectdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public class NoWifi extends AppCompatActivity {
    private WifiManager wifiManager;
    private final IntentFilter intentFilter = new IntentFilter();
    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            boolean wifiIsEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
            if (wifiIsEnabled) finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_wifi);

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void turnOnWifi (View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Wifi can be turned on programmatically
            wifiManager.setWifiEnabled(true);
        } else {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    }
}