package com.aican.aicanapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.aican.aicanapp.Dashboard;
import com.aican.aicanapp.R;
import com.aican.aicanapp.dataClasses.PhDevice;
import com.aican.aicanapp.interfaces.DashboardListsOptionsClickListener;
import com.aican.aicanapp.ph.PhActivity;
import com.aican.aicanapp.utils.Constants;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

public class PhAdapter extends RecyclerView.Adapter<PhAdapter.PhAdapterViewHolder> {

    ArrayList<PhDevice> phDevices;
    DashboardListsOptionsClickListener optionsClickListener;
    //    WebSocketInit webSocketInit;
    Context context;
    PhAdapterViewHolder holder1;


    public PhAdapter(Context context, ArrayList<PhDevice> phDevices, DashboardListsOptionsClickListener optionsClickListener
//                    , WebSocketInit webSocketInit
    ) {
        this.phDevices = phDevices;
        this.optionsClickListener = optionsClickListener;
//        this.webSocketInit = webSocketInit;
        this.context = context;
    }

    @Override
    public PhAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.ph_item, parent, false);
        return new PhAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhAdapterViewHolder holder, int position) {
        holder1 = holder;
        holder.bind(phDevices.get(position), position);
        holder.setIsRecyclable(true);
    }

    @Override
    public int getItemCount() {
        return phDevices.size();
    }

    public class PhAdapterViewHolder extends RecyclerView.ViewHolder {
        private TextView ph, ec, temp, tds, tvName;
        private ImageView ivOptions;
        private ProgressBar progressBar;
        private Switch offlineModeSwitch;
        LinearLayout onlineStatus;
        LinearLayout offlineStatus;
        LinearLayout offlineLayout;
        CardView ph_Cardview;

        /**
         * ViewHolder
         *
         * @param itemView
         */
        public PhAdapterViewHolder(View itemView) {
            super(itemView);
            ph = itemView.findViewById(R.id.ph);
            ec = itemView.findViewById(R.id.ec);
            temp = itemView.findViewById(R.id.temp);
            tvName = itemView.findViewById(R.id.custom_device_name);
            ivOptions = itemView.findViewById(R.id.ivOptions);
            progressBar = itemView.findViewById(R.id.progress_bar);
            offlineModeSwitch = itemView.findViewById(R.id.offlineModeSwitch);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
            offlineStatus = itemView.findViewById(R.id.offlineStatus);
            ph_Cardview = itemView.findViewById(R.id.ph_Cardview);
            offlineLayout = itemView.findViewById(R.id.offlineLayout);
        }

        public void bind(PhDevice device, int position) {
            String phString;
//            if (device.getPh() < 0 || device.getPh() > 14) {
//                phString = "N/A";
//            } else {
            phString = String.format(Locale.UK, "%.2f", device.getPh());
//            }
            String ecString = String.format(Locale.UK, "mV: %.2f", device.getEc());
            String tempString;
//            if (device.getTemp() < -50 || device.getTemp() > 125) {
//                tempString = "Temp: -";
//            } else {
            tempString = String.format(Locale.UK, "Temp: %d°C", device.getTemp());
//            }

            ph.setText(phString);
            ec.setText(ecString);
            temp.setText(tempString);
            tvName.setText("pH Meter " + device.getId());

            ph_Cardview.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), PhActivity.class);
                intent.putExtra(Dashboard.KEY_DEVICE_ID, device.getId());
                itemView.getContext().startActivity(intent);
            });


            ivOptions.setOnClickListener(v -> {
                optionsClickListener.onOptionsIconClicked(v, device.getId());
            });

//            if (Constants.OFFLINE_MODE) {
//                if (Constants.DeviceIDOffline.equals(device.getId())) {
//                    offlineModeSwitch.setClickable(true);
//                } else {
//                    offlineModeSwitch.setClickable(false);
//                }
//            }

            String ssid = getCurrentSsid(context);

            if (!Constants.OFFLINE_MODE && !Constants.OFFLINE_DATA) {
                offlineLayout.setVisibility(View.GONE);

                offlineModeSwitch.setChecked(false);
                offlineStatus.setVisibility(View.GONE);
                onlineStatus.setVisibility(View.VISIBLE);

//                Toast.makeText(context, "True", Toast.LENGTH_SHORT).show();

//                if (device.getOffline() == 0) {
//                    offlineLayout.setVisibility(View.GONE);
//                }
//
//                if (device.getOffline() == 1) {
//                    offlineLayout.setVisibility(View.VISIBLE);
//                }
                offlineModeSwitch.setClickable(true);

            } else {
                offlineLayout.setVisibility(View.GONE);

            }

            offlineModeSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    notifyItemChanged(position);
                    if (offlineModeSwitch.isChecked()) {
                        if (Constants.wifiSSID == null || Constants.wifiSSID.equals("") || Constants.wifiSSID.equals("N/A")) {
                            Toast.makeText(context, "You are not connected with any wifi device", Toast.LENGTH_SHORT).show();
                            offlineLayout.setVisibility(View.GONE);

//                            Constants.OFFLINE_MODE = true;
                            Constants.OFFLINE_DATA = true;
//                            webSocketInit.updateToggle();

                        } else {
                            if (Constants.wifiSSID.contains(device.getId())) {
                                Toast.makeText(context, "Connecting to Offline mode with " + device.getId(), Toast.LENGTH_LONG).show();
                                Constants.OFFLINE_MODE = true;
                                Constants.OFFLINE_DATA = true;

                                offlineLayout.setVisibility(View.GONE);

                                Constants.DeviceIDOffline = device.getId();
                                Constants.devicePosition = position;
//                                webSocketInit.initWebSocket(device.getId());
//                                webSocketInit.updateToggle();
                                offlineStatus.setVisibility(View.VISIBLE);
                                onlineStatus.setVisibility(View.GONE);
                            } else {
//                                Constants.OFFLINE_MODE = true;
                                Constants.OFFLINE_DATA = true;
                                offlineLayout.setVisibility(View.GONE);

//                                webSocketInit.updateToggle();

                                Toast.makeText(context, "Device you are connecting is not same as the connected wifi, connect with other device", Toast.LENGTH_LONG).show();
                            }
                        }

                    } else {
//                        Constants.OFFLINE_MODE = false;
                        Constants.OFFLINE_DATA = false;
                        Constants.DeviceIDOffline = device.getId();
//                        webSocketInit.cancelWebSocket(device.getId());
                        offlineStatus.setVisibility(View.GONE);
                        onlineStatus.setVisibility(View.VISIBLE);
                    }

                }
            });

            if (!Constants.OFFLINE_MODE && !Constants.OFFLINE_DATA) {

                setFirebaseListeners(device);
            }
        }

        private void setFirebaseListeners(PhDevice device) {

            DatabaseReference dataRef = FirebaseDatabase.getInstance(FirebaseApp.getInstance(device.getId())).getReference().child(Dashboard.DEVICE_TYPE_PH).child(device.getId()).child("Data");

            dataRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                }

                @Override
                public void onChildChanged(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                    if (snapshot.getKey() == null || snapshot.getValue() == null) return;
                    switch (snapshot.getKey()) {
                        case "PH_VAL": {
                            Float ph = snapshot.getValue(Float.class);
                            if (ph == null) return;
                            device.setPh(ph);
                            break;
                        }
                        case "TEMP_VAL": {
                            Integer val = snapshot.getValue(Integer.class);
                            if (val == null) return;
                            device.setTemp(val);
                            break;
                        }
                        case "EC_VAL": {
                            Float val = snapshot.getValue(Float.class);
                            if (val == null) return;
                            device.setEc(val);
                            break;
                        }
                    }
                    notifyItemChanged(getAdapterPosition());
                }

                @Override
                public void onChildRemoved(@NonNull @NotNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });
        }
    }

    public void refreshMv(float mvVal, String deviceID) {
        for (int i = 0; i < phDevices.size(); i++) {
            Log.e("NotifyingS", "DB : " + deviceID);

            PhDevice phDevice = phDevices.get(i);
            if (phDevice.getId().equals(deviceID)) {
                phDevice.setEc(mvVal);
                int finalI = i;
                Log.e("NotifyingS", "D : " + deviceID);
                new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(finalI));
                break; // Exit the loop after finding the matching device
            }
        }
    }

    public void refreshTemp(int tempVal, String deviceID) {
        for (int i = 0; i < phDevices.size(); i++) {
            Log.e("NotifyingS", "DB : " + deviceID);

            PhDevice phDevice = phDevices.get(i);
            if (phDevice.getId().equals(deviceID)) {
                phDevice.setTemp(tempVal);
                int finalI = i;
                Log.e("NotifyingS", "D : " + deviceID);
                new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(finalI));
                break; // Exit the loop after finding the matching device
            }
        }
    }

    public void refreshPh(int position, float phVal, String deviceID) {
        Log.e("NotifyingS", "DNB : " + deviceID);

        for (int i = 0; i < phDevices.size(); i++) {
            Log.e("NotifyingS", "DB : " + deviceID);

            PhDevice phDevice = phDevices.get(i);
            if (phDevice.getId().equals(deviceID)) {
                phDevice.setPh(phVal);
                int finalI = i;
                Log.e("NotifyingS", "D : " + deviceID);
                new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(finalI));
                break; // Exit the loop after finding the matching device
            }
        }
//        holder1.ph.setText(String.valueOf(phVal));
//        notifyItemChanged(position);
    }

    public void changePh(String deviceID, float phVal, float ec, int temp) {
        int i = 0;
        for (PhDevice phDevice : phDevices) {
            if (phDevice.getId().equals(deviceID)) {
                phDevices.get(i).setPh(phVal);
                phDevices.get(i).setTemp(temp);
                phDevices.get(i).setEc(ec);
//                Toast.makeText(context, "Matched", Toast.LENGTH_SHORT).show();

                notifyItemChanged(i);
            }
            i++;
        }
    }

    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !connectionInfo.getSSID().isEmpty()) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

}
