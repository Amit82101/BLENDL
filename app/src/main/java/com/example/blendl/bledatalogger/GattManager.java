package com.example.blendl.bledatalogger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public class GattManager {

    private static BluetoothGatt bluetoothGatt;
    private static BluetoothDevice bluetoothDevice;
    private static ConnectedActivity connectedActivity;

    public static void setGatt(BluetoothGatt gatt) {
        bluetoothGatt = gatt;
    }

    public static BluetoothGatt getGatt() {
        return bluetoothGatt;
    }

    public static void setDevice(BluetoothDevice device) {
        bluetoothDevice = device;
    }

    public static BluetoothDevice getDevice() {
        return bluetoothDevice;
    }

    public static void clearGatt() {
        bluetoothGatt = null;
    }

    public static void clearDevice() {
        bluetoothDevice = null;
    }

    public static void setConnectedActivity(ConnectedActivity activity) {
        connectedActivity = activity;
    }

    public static ConnectedActivity getConnectedActivity() {
        return connectedActivity;
    }

    public static void clearConnectedActivity() {
        connectedActivity = null;
    }

    public static void cleanup() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        bluetoothDevice = null;
        connectedActivity = null;
    }
}
