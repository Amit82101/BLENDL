package com.example.blendl.blenode;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blendl.R;

import java.util.*;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private final Context context;

    public DeviceAdapter(Context context) {
        this.context = context;
    }


    private final List<ScanResult> devices = new ArrayList<>();

    public void updateDevices(Collection<ScanResult> newResults) {
        devices.clear();
        devices.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item_blenode, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        ScanResult result = devices.get(position);

        String deviceName = result.getDevice().getName();
        holder.deviceNameTextView.setText(deviceName != null ? deviceName : "Unnamed Device");

        holder.macTextView.setText("MAC: " + result.getDevice().getAddress());
        holder.rssiTextView.setText("RSSI: " + result.getRssi() + " dBm");

        ScanRecord record = result.getScanRecord();
        if (record != null) {
            byte[] rawData = record.getBytes();
            Map<String, String> parsed = AdvertisementParser.parse(rawData);

            holder.temperatureTextView.setText("Temperature: " + parsed.getOrDefault("Temperature", "-"));
            holder.humidityTextView.setText("Humidity: " + parsed.getOrDefault("Humidity", "-"));
            holder.batteryTextView.setText("Battery: " + parsed.getOrDefault("Battery", "-"));
        } else {
            holder.temperatureTextView.setText("Temperature: -");
            holder.humidityTextView.setText("Humidity: -");
            holder.batteryTextView.setText("Battery: -");
        }

        // Start NodeSettingsActivity on click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NodeSettingsActivity.class);
            intent.putExtra("device_name", result.getDevice().getName());
            intent.putExtra("device_address", result.getDevice().getAddress());
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView, macTextView, rssiTextView;
        TextView temperatureTextView, humidityTextView, batteryTextView;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            macTextView = itemView.findViewById(R.id.macTextView);
            rssiTextView = itemView.findViewById(R.id.rssiTextView);
            temperatureTextView = itemView.findViewById(R.id.temperatureTextView);
            humidityTextView = itemView.findViewById(R.id.humidityTextView);
            batteryTextView = itemView.findViewById(R.id.batteryTextView);
        }
    }
}
