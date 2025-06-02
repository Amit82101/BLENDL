package com.example.blendl.bledatalogger;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

public class GattCallbackHandler extends BluetoothGattCallback {

    private final Context context;

    public GattCallbackHandler(Context context) {
        this.context = context;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d("GattCallback", "Connection state changed: " + newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d("GattCallback", "Connected to GATT server.");
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d("GattCallback", "Disconnected from GATT server.");
            gatt.close();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) return;

        for (BluetoothGattService service : gatt.getServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                int props = characteristic.getProperties();

                // Read initial value if possible
                if ((props & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    gatt.readCharacteristic(characteristic);
                }

                // Enable notifications if supported
                if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    gatt.setCharacteristicNotification(characteristic, true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("GattCallback", "Characteristic Read: " + characteristic.getUuid());

            if (context instanceof ConnectedActivity) {
                ((ConnectedActivity) context).updateFromCallback(characteristic);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d("GattCallback", "Characteristic Changed: " + characteristic.getUuid());

        if (context instanceof ConnectedActivity) {
            ((ConnectedActivity) context).updateFromCallback(characteristic);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d("GattCallback", "Descriptor written: " + descriptor.getUuid());
    }
}

