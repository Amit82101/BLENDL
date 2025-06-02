package com.example.blendl.bledatalogger;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceData {
    private static String name;
    private final String mac;
    private final String rssi;
    private final BluetoothDevice device;

    public BluetoothDeviceData(String name, String mac, String rssi, BluetoothDevice device) {
        BluetoothDeviceData.name = name;
        this.mac = mac;
        this.rssi = rssi;
        this.device = device;
    }

    public static String getName() { return name; }
    public String getMac() { return mac; }
    public String getRssi() { return rssi; }
    public BluetoothDevice getDevice() { return device; }
}
