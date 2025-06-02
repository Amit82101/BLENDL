package com.example.blendl.blenode;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blendl.R;

import java.util.*;

public class BleNodeActivity extends AppCompatActivity {

    private TextView deviceCountTextView;
    private RecyclerView recyclerView; // Declare it
    private DeviceAdapter adapter;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final long REFRESH_INTERVAL_MS = 5000;

    private BluetoothLeScanner bleScanner;
    private final Map<String, ScanResult> resultMap = new LinkedHashMap<>();

    private final Handler handler = new Handler();

    // Refresh UI every 5 seconds with latest advertising data
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.updateDevices(resultMap.values());
            deviceCountTextView.setText("Devices Found: " + resultMap.size());
            handler.postDelayed(this, REFRESH_INTERVAL_MS);  // Schedule next update
        }
    };

    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            String deviceName = result.getDevice().getName();
            if (deviceName != null
                    && deviceName.toLowerCase().contains("nrf")
                    && !deviceName.toLowerCase().contains("dfu")
                    && !deviceName.toLowerCase().contains("thingy")) {
                String mac = result.getDevice().getAddress();
                resultMap.put(mac, result);
            }

        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blenode);

        deviceCountTextView = findViewById(R.id.deviceCountTextView);

        RecyclerView rv = findViewById(R.id.recyclerView);

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        recyclerView = findViewById(R.id.recyclerView);

        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter and set it
        adapter = new DeviceAdapter(this);
        recyclerView.setAdapter(adapter);
        if (checkPermissions()) {
            startScan();
        } else {
            requestPermissions();
        }
    }

    private void startScan() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        bleScanner = adapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(this, "BLE scanner not available", Toast.LENGTH_SHORT).show();
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bleScanner.startScan(null, settings, callback);
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);  // Start periodic updates
        Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
    }


    @Override
    protected void onDestroy() {
        if (bleScanner != null) bleScanner.stopScan(callback);
        handler.removeCallbacks(refreshRunnable);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkPermissions()) startScan();
            else Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
        }
    }
}
