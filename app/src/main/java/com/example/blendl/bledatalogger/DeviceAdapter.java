package com.example.blendl.bledatalogger;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blendl.R;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final ArrayList<BluetoothDeviceData> deviceList;
    private final OnConnectClickListener onConnectClickListener;

    public interface OnConnectClickListener {
        void onConnectClick(BluetoothDeviceData deviceData);
        void onServicesDiscovered(BluetoothGatt gatt, int status);
        void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);
    }

    public DeviceAdapter(ArrayList<BluetoothDeviceData> deviceList, OnConnectClickListener onConnectClickListener) {
        this.deviceList = deviceList;
        this.onConnectClickListener = onConnectClickListener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item_datalogger, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDeviceData deviceData = deviceList.get(position);
        holder.deviceName.setText(deviceData.getName());
        holder.deviceMac.setText(deviceData.getMac());
        holder.deviceRssi.setText("RSSI: " + deviceData.getRssi() + " dBm");

        holder.connectButton.setOnClickListener(v -> {
            if (onConnectClickListener != null) {
                onConnectClickListener.onConnectClick(deviceData);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceMac;
        TextView deviceRssi;
        Button connectButton;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceMac = itemView.findViewById(R.id.device_mac);
            deviceRssi = itemView.findViewById(R.id.device_rssi);
            connectButton = itemView.findViewById(R.id.connect_button);
        }
    }
}
