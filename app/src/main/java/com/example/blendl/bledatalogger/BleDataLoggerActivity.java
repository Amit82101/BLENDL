package com.example.blendl.bledatalogger;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blendl.R;

import java.util.ArrayList;

public class BleDataLoggerActivity extends Activity implements DeviceAdapter.OnConnectClickListener {

    private static final int REQUEST_PERMISSIONS = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private Button scanButton;
    private TextView scanStatus;
    private DeviceAdapter deviceAdapter;
    private final ArrayList<BluetoothDeviceData> deviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bledatalogger);

        scanButton = findViewById(R.id.scan_button);
        scanStatus = findViewById(R.id.scan_status);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(deviceList, this);
        recyclerView.setAdapter(deviceAdapter);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanButton.setOnClickListener(v -> {
            if (!scanning) {
                checkPermissionsAndStartScan();
            } else {
                stopScan();
            }
        });
    }

    private void checkPermissionsAndStartScan() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        else {
            // For Android 6-11, Location permission needed for scanning
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            enableBluetoothAndScan();
        }
    }

    private void enableBluetoothAndScan() {
        if (!isBluetoothEnabled()) {
            promptEnableBluetooth();
            return;
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Enable Location for BLE scan", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        startBleScan();
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private void promptEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBtIntent);
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void startBleScan() {
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(null, settings, leScanCallback);
        scanning = true;
        scanButton.setText("Stop Scan");
        scanStatus.setText("Scanning...");
    }

    private void stopScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
        }
        scanning = false;
        scanButton.setText("Start Scan");
        scanStatus.setText("Scan stopped");
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null || device.getType() != BluetoothDevice.DEVICE_TYPE_LE) return;

            String name = device.getName();
            if (name == null || name.isEmpty()) return;

            if (!name.toLowerCase().contains("nrf") && !name.toLowerCase().contains("dfu") && !name.toLowerCase().contains("thingy")) {
                return;
            }

            String mac = device.getAddress();
            String rssi = String.valueOf(result.getRssi());

            for (BluetoothDeviceData data : deviceList) {
                if (data.getMac().equals(mac)) return;
            }

            deviceList.add(new BluetoothDeviceData(name, mac, rssi, device));
            deviceAdapter.notifyDataSetChanged();
            scanStatus.setText("Found " + deviceList.size() + " nRF device(s)");
        }
    };

    @Override
    public void onConnectClick(BluetoothDeviceData deviceData) {
        stopScan();
        scanStatus.setText("Connecting...");

        BluetoothDevice device = deviceData.getDevice();
        Intent intent = new Intent(BleDataLoggerActivity.this, ConnectedActivity.class);
        intent.putExtra("device_address", device.getAddress());
        startActivity(intent);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        // Handle services
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        // Handle characteristic read
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                enableBluetoothAndScan();
            } else {
                Toast.makeText(this, "Permissions required to scan BLE devices.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
