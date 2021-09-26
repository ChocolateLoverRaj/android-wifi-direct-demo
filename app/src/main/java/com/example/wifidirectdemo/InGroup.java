package com.example.wifidirectdemo;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InGroup extends AppCompatActivity {
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    @Nullable
    private WifiP2pGroup group;
    private final InGroup activity = this;
    @Nullable
    private WifiP2pInfo wifiP2pInfo;
    private WifiP2pDevice thisDevice;
    private boolean didSocketStuff = false;
    private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_device, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final ImageView imageView = holder.itemView.findViewById(R.id.groupOwnerStar);
            final GroupDevice device = getGroupDevices().get(position);
            if (device.isGroupOwner()) {
                imageView.setVisibility(View.VISIBLE);
            } else
                imageView.setVisibility(View.INVISIBLE);
            final TextView isYou = holder.itemView.findViewById(R.id.isYou);
            // For some reason, when a Google Pixel 2 XL is the group owner and
            // a Samsung Galaxy Tab S2 a client, both the devices have the same address for the tablet.
            // But when the tablet is the group owner and the phone is a client, the phone says it has a
            // Different address than the tablet! For this reason we are comparing device names too.
            final boolean sameAddress = Arrays.equals(device.getDevice().getAddress(), new RealDevice(thisDevice).getAddress());
            final boolean sameName = device.getDevice().getName().equals(thisDevice.deviceName);
            if (!sameAddress && sameName)
                Log.w(getClass().getName(), "This device has a different address but the same name. This could be caused by a device address mismatch or if two group devices have the same name.");
            if (sameAddress || sameName)
                isYou.setVisibility(View.VISIBLE);
            else
                isYou.setVisibility(View.INVISIBLE);
            final TextView textView = holder.itemView.findViewById(R.id.groupDeviceName);
            textView.setText(device.getDevice().getName());
        }

        @Override
        public int getItemCount() {
            return getGroupDevices().size();
        }
    };
    private final DiffUtil.ItemCallback<GroupDevice> itemCallback = new DiffUtil.ItemCallback<GroupDevice>() {
        @Override
        public boolean areItemsTheSame(@NonNull GroupDevice oldItem, @NonNull GroupDevice newItem) {
            return Arrays.equals(oldItem.getDevice().getAddress(), newItem.getDevice().getAddress());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupDevice oldItem, @NonNull GroupDevice newItem) {
            return oldItem.equals(newItem);
        }
    };
    private final AsyncListDiffer<GroupDevice> uiListDiffer = new AsyncListDiffer<>(adapter, itemCallback);
    private final WifiP2PDeviceConnectionChangeListener connectionChangeListener = new WifiP2PDeviceConnectionChangeListener(this, new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) throws SecurityException {
            if (!wifiP2pInfo.groupFormed) {
                Log.d(this.getClass().getName(), "Not in group. Leaving...");
                finish();
                return;
            }
            activity.wifiP2pInfo = wifiP2pInfo;
            doSocketStuff();
            if (wifiP2pInfo.isGroupOwner) {
                wifiP2pManager.requestGroupInfo(channel, wifiP2pGroup -> {
                    if (wifiP2pGroup == null) {
                        Log.w(getClass().getName(), "Group info is null. Leaving...");
                        finish();
                        return;
                    };
                    group = wifiP2pGroup;
                    uiListDiffer.submitList(getGroupDevices());
                    if (serverListDiffer != null)
                        serverListDiffer.submitList(getGroupDevices());
                    doSocketStuff();
                    for (WifiP2pDevice device : wifiP2pGroup.getClientList()) {
                        System.out.println("Device: " + device.deviceName + ". Address: " + device.deviceAddress);
                    }
                });
            }
        }
    });
    private AsyncListDiffer<GroupDevice> serverListDiffer;
    private final WifiP2pDeviceInfoChangeListener infoChangeListener = new WifiP2pDeviceInfoChangeListener(this, (WifiP2pDevice wifiP2pDevice) -> {
        thisDevice = wifiP2pDevice;
        System.out.println("This device address: " + wifiP2pDevice.deviceAddress);
        uiListDiffer.submitList(getGroupDevices());
        if (serverListDiffer != null)
            serverListDiffer.submitList(getGroupDevices());
    });
    private ProgressBar leaveGroupProgressBar;
    private ServerManager serverManager;
    private ClientConnection clientConnection;
    private ProgressBar connectionProgressBar;
    private TextView progressBarDescription;
    private List<GroupDevice> groupDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_group);

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        connectionChangeListener.onCreate();
        infoChangeListener.onCreate();
        leaveGroupProgressBar = findViewById(R.id.leaveGroup);
        RecyclerView recyclerView = findViewById(R.id.groupDevices);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        connectionChangeListener.requestNow();
        connectionProgressBar = findViewById(R.id.inGroupProgressBar);
        progressBarDescription = findViewById(R.id.progressBarDescription);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectionChangeListener.start();
        infoChangeListener.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionChangeListener.stop();
        infoChangeListener.stop();
    }

    public void leaveGroup() {
        leaveGroupProgressBar.setVisibility(View.VISIBLE);
        if (serverManager != null) {
            try {
                serverManager.stop();
            } catch (IOException e) {
                Log.e(this.getClass().getName(), "Error stopping server", e);
                return;
            }
        } else if (clientConnection != null) {
            try {
                clientConnection.stop();
            } catch (IOException e) {
                Log.e(this.getClass().getName(), "Error closing client connection", e);
                return;
            }
        }
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Removed group");
                finish();
            }

            @Override
            public void onFailure(int i) {
                System.out.println("Failed to remove group");
            }
        });
    }

    public void leaveGroup(View view) {
        leaveGroup();
    }

    @Override
    public void onBackPressed() {
        leaveGroup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // FIXME: leaveGroup() is called twice when clicking on leave button
        leaveGroup();
    }

    private void doSocketStuff() {
        if (!didSocketStuff) {
            assert wifiP2pInfo != null;
            if (wifiP2pInfo.isGroupOwner) {
                if (group != null) {
                    connectionProgressBar.setVisibility(View.INVISIBLE);
                    progressBarDescription.setVisibility(View.GONE);
                    try {
                        serverManager = new ServerManager(this::getGroupDevices);
                    } catch (IOException e) {
                        Log.e(this.getClass().getName(), "Error starting server", e);
                        return;
                    }
                    serverListDiffer = new AsyncListDiffer<>(serverManager, new AsyncDifferConfig.Builder<>(itemCallback).build());
                    serverListDiffer.submitList(getGroupDevices());
                    serverManager.start();
                    didSocketStuff = true;
                }
            } else {
                clientConnection = new ClientConnection(wifiP2pInfo.groupOwnerAddress, ServerManager.PORT, new ReceiveGroupThread.Callback() {
                    @Override
                    public StreamedListCallback<ReceiveGroupThread.Device> onInitialDevicesList() {
                        return new StreamedListCallback<ReceiveGroupThread.Device>() {
                            @Override
                            public void onSize(int totalDevices) {
                                runOnUiThread(() -> {
                                    connectionProgressBar.setIndeterminate(false);
                                    connectionProgressBar.setMax(totalDevices);
                                });
                            }

                            @Override
                            public void onItem(ReceiveGroupThread.Device device, final int index) {
                                groupDevices = groupDevices != null ? new ArrayList<>(groupDevices) : new ArrayList<>();
                                groupDevices.add(new GroupDevice() {
                                    @Override
                                    public ReceiveGroupThread.Device getDevice() {
                                        return device;
                                    }

                                    @Override
                                    public boolean isGroupOwner() {
                                        return index == 0;
                                    }
                                });
                                uiListDiffer.submitList(groupDevices);
                                runOnUiThread(() -> {
                                    connectionProgressBar.setProgress(index + 1);
                                    if (connectionProgressBar.getProgress() == connectionProgressBar.getMax()) {
                                        connectionProgressBar.setVisibility(View.INVISIBLE);
                                        progressBarDescription.setVisibility(View.GONE);
                                    }
                                });
                            }
                        };
                    }

                    @Override
                    public StreamedListCallback<ReceiveGroupThread.Device> onInsertedDevices(int position) {
                        return new StreamedListCallback<ReceiveGroupThread.Device>() {
                            @Override
                            public void onSize(int totalDevices) {
                                System.out.println("Inserted " + totalDevices + " new devices at position " + position);
                                runOnUiThread(() -> {
                                    connectionProgressBar.setMax(totalDevices);
                                    connectionProgressBar.setProgress(0);
                                    progressBarDescription.setText("Loading new devices");
                                });
                            }

                            @Override
                            public void onItem(ReceiveGroupThread.Device device, int index) {
                                System.out.println("New device!: " + device.getName());
                                groupDevices = new ArrayList<>(groupDevices);
                                groupDevices.add(new GroupDevice() {
                                    @Override
                                    public ReceiveGroupThread.Device getDevice() {
                                        return device;
                                    }

                                    @Override
                                    public boolean isGroupOwner() {
                                        return false;
                                    }
                                });
                                uiListDiffer.submitList(groupDevices);
                                runOnUiThread(() -> {
                                    connectionProgressBar.setProgress(index + 1);
                                    if (connectionProgressBar.getProgress() == connectionProgressBar.getMax()) {
                                        connectionProgressBar.setVisibility(View.INVISIBLE);
                                        progressBarDescription.setVisibility(View.GONE);
                                    }
                                });

                            }
                        };
                    }

                    @Override
                    public void onRemovedDevices(int position, int count) {
                        System.out.println("removed device " + position);
                        groupDevices = new ArrayList<>(groupDevices);
                        groupDevices.subList(position, position + count).clear();
                        uiListDiffer.submitList(groupDevices);
                    }
                });
                clientConnection.start();
                didSocketStuff = true;
            }
        }
    }

    static abstract class GroupDevice {
        abstract public ReceiveGroupThread.Device getDevice();

        abstract public boolean isGroupOwner();

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof GroupDevice))
                return false;
            return this.getDevice().equals(((GroupDevice) obj).getDevice()) &&
                    this.isGroupOwner() == ((GroupDevice) obj).isGroupOwner();
        }
    }

    List<GroupDevice> getGroupDevices() {
        if (group == null || wifiP2pInfo == null)
            return groupDevices != null ? groupDevices : new ArrayList<>();
        List<GroupDevice> list = new ArrayList<>();
        list.add(new GroupDevice() {
            @Override
            public ReceiveGroupThread.Device getDevice() {
                return new RealDevice(thisDevice);
            }

            @Override
            public boolean isGroupOwner() {
                return wifiP2pInfo.isGroupOwner;
            }
        });
        list.addAll(group.getClientList().stream().map((Function<WifiP2pDevice, GroupDevice>) wifiP2pDevice -> new GroupDevice() {
            @Override
            public ReceiveGroupThread.Device getDevice() {
                return new RealDevice(wifiP2pDevice);
            }

            @Override
            public boolean isGroupOwner() {
                return false;
            }
        }).collect(Collectors.toList()));
        return list;
    }
}