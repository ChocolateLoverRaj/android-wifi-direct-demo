package com.example.wifidirectdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    Channel channel;
    WifiP2pManager wifiP2pManager;
    WifiDirectBroadcastReceiver receiver;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private WifiManager wifiManager;
    private final MainActivity activity = this;
    private List<WifiP2pDevice> wifiP2pDevices;
    private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false)) {
            };
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) throws SecurityException {
            final WifiP2pDevice device = wifiP2pDevices.get(position);
            final DeviceType deviceType = DeviceType.codesMap.get(device.primaryDeviceType.split("-", 2)[0]);
            final int deviceIcon = DeviceType.iconsMap.get(deviceType);
            ((ImageView) holder.itemView.findViewById(R.id.deviceIcon)).setImageResource(deviceIcon);
            if (deviceType == DeviceType.UNKNOWN)
                Log.w(getClass().getName(), "Unknown device type: " + device.primaryDeviceType + ". Maybe we should include an icon for this device type. Device name: " + device.deviceName);
            ((TextView) holder.itemView.findViewById(R.id.deviceName)).setText(device.deviceName);
            if (device.status == WifiP2pDevice.INVITED)
                holder.itemView.findViewById(R.id.connecting).setVisibility(View.VISIBLE);
            final Runnable confirmConnect = () -> {
                WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                wifiP2pConfig.deviceAddress = device.deviceAddress;
                wifiP2pManager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        System.out.println("Connected");
                    }

                    @Override
                    public void onFailure(int i) {
                        System.out.println("Not connected " + i);
                    }
                });
            };
            holder.itemView.findViewById(R.id.join).setOnClickListener(view -> {
                if (deviceType == DeviceType.MOBILE_DEVICE)
                    confirmConnect.run();
                 else {
                    new AlertDialog.Builder(activity)
                            .setTitle(deviceType == DeviceType.UNKNOWN ? "Unknown device type" : "Possible accidental connect")
                            .setMessage("Are you sure you want to connect to the " + DeviceType.descriptionsMap.get(deviceType) + " " + device.deviceName + "?")
                            .setIcon(deviceIcon)
                            .setPositiveButton(android.R.string.yes, (dialogue, whichButton) -> confirmConnect.run())
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return wifiP2pDevices != null ? wifiP2pDevices.size() : 0;
        }
    };
    private final AsyncListDiffer<WifiP2pDevice> listDiffer = new AsyncListDiffer<>(adapter, new DiffUtil.ItemCallback<WifiP2pDevice>() {

        @Override
        public boolean areItemsTheSame(@NonNull WifiP2pDevice oldItem, @NonNull WifiP2pDevice newItem) {
            return newItem.deviceAddress.equals(oldItem.deviceAddress);
        }

        @Override
        public boolean areContentsTheSame(@NonNull WifiP2pDevice oldItem, @NonNull WifiP2pDevice newItem) {
            return newItem.equals(oldItem) && oldItem.status == newItem.status;
        }
    });
    private final WifiDirectBroadcastReceiver.Callback wifiDirectBroadcastReceiverCallback = new WifiDirectBroadcastReceiver.Callback() {
        @Override
        public void onWifiStateChanged(boolean on) {
            if (!on) startActivity(new Intent(activity, NoWifi.class));
        }

        @Override
        public void onPeersChanged(Collection<WifiP2pDevice> wifiP2pDevices) {
            activity.wifiP2pDevices = new ArrayList<>(wifiP2pDevices);
            listDiffer.submitList(activity.wifiP2pDevices);
        }

        @Override
        public void onGroupChanged(@Nullable WifiP2pGroup wifiP2pGroup) {
            if (wifiP2pGroup != null)
                startActivity((new Intent(activity, InGroup.class)));
        }
    };
    private ProgressBar creatingProgressBar;

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    startFinding();
                } else {
                    startActivity(new Intent(this, PermissionDeniedActivity.class));
                }
            });
    private final WifiP2pManager.ActionListener discoverPeersHandler = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.d(this.getClass().getName(), "Started discovering peers");
        }

        @Override
        public void onFailure(int i) {
            Snackbar
                    .make(findViewById(R.id.mainActivity), "Error connecting to device", BaseTransientBottomBar.LENGTH_LONG)
                    .show();

        }
    };
    private TextView deviceNameTextView;
    private final WifiP2pDeviceInfoChangeListener infoChangeListener = new WifiP2pDeviceInfoChangeListener(this, (WifiP2pDevice device) -> deviceNameTextView.setText("This device name: " + device.deviceName));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        final RecyclerView recyclerView = findViewById(R.id.peersList);

        creatingProgressBar = findViewById(R.id.createGroup);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        infoChangeListener.onCreate();
        deviceNameTextView = findViewById(R.id.thisDeviceName);
    }

    private void startFinding() throws SecurityException {
        if (wifiManager.isWifiEnabled())
            wifiP2pManager.discoverPeers(channel, discoverPeersHandler);
    }

    private void stopFinding() {
        wifiP2pManager.stopPeerDiscovery(channel, discoverPeersHandler);
    }

    @Override
    protected void onResume() throws SecurityException {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(wifiP2pManager, channel, wifiDirectBroadcastReceiverCallback);
        registerReceiver(receiver, intentFilter);
        startFinding();

        wifiP2pManager.requestConnectionInfo(channel, (WifiP2pInfo wifiP2pInfo) -> {
            if (wifiP2pInfo.groupFormed)
                startActivity((new Intent(this, InGroup.class)));
        });
        creatingProgressBar.setVisibility(View.GONE);
        infoChangeListener.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        stopFinding();
        Log.d(getClass().getName(), "Stopped finding peers");
        infoChangeListener.stop();
    }

    public void createGroup(View view) throws SecurityException {
        creatingProgressBar.setVisibility(View.VISIBLE);

        WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() throws SecurityException {
                Log.d(this.getClass().getName(), "Created group");
                startActivity(new Intent(activity, InGroup.class));
            }

            @Override
            public void onFailure(int i) {
                System.out.println("Failed to create group");
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiP2pManager.createGroup(channel, new WifiP2pConfig.Builder().setNetworkName("DIRECT-MqHELLO").setPassphrase("Wifi direct demo").build(), actionListener);
        } else
            wifiP2pManager.createGroup(channel, actionListener);
    }

    public void exit(View view) {
        ProgressBar exiting = findViewById(R.id.exiting);
        exiting.setVisibility(View.VISIBLE);
        wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                finish();
            }

            @Override
            public void onFailure(int i) {
                Snackbar
                        .make(findViewById(R.id.mainActivity), "Error stopping peer discovery", BaseTransientBottomBar.LENGTH_LONG)
                        .show();
            }
        });
    }
}