package com.example.blendl.blenode;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.blendl.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;

public class NodeSettingsActivity extends AppCompatActivity {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID SERVICE_UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");

    private static final UUID ENVIRONMENTAL_SENSING_SERVICE_UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHAR_UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_290E = UUID.fromString("0000290e-0000-1000-8000-00805f9b34fb");

    private static final UUID Time_Update_Control_Point = UUID.fromString("00002a16-0000-1000-8000-00805f9b34fb");

    private static final UUID Sensor_select = UUID.fromString("00002a56-0000-1000-8000-00805f9b34fb");
    private AutoCompleteTextView valueDropdown;
    private EditText minuteInput, secondInput, editSleepSeconds;
    private BluetoothGatt bluetoothGatt;
    private Button restartButton, syncTimeButton, writeButton, writeDelayButton, btnSetSleep;
    private BluetoothDevice bluetoothDevice;
    BluetoothGattCharacteristic nusRxChar;
    private BluetoothGattCharacteristic nusTxChar;
    private final Queue<UUID> readQueue = new LinkedList<>();
    // private final Handler logHandler = new Handler(Looper.getMainLooper());

    private EditText minTempInput, maxTempInput,minTempTHInput,maxTempTHInput,minHumdInput,maxHumdInput;
    private TextView tempRangeTextView,tempRange2TextView,HumdRangeTextView;
    private Button applyTempButton,applyTemp2Button,applyHumdButton;

    private BluetoothGatt bluetoothGatt;
    private ProgressBar progressBar;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGattCharacteristic nusRxChar;
    private BluetoothGattCharacteristic nusTxChar;


    private TextView connectionStatus, device_info, tvReceivedData,progressText ;


    private Button disconnectButton, btnSend, btnExportPdf;
    private ImageView btnsetting;
    private String deviceAddress;
    private final Queue<UUID> readQueue = new LinkedList<>();
    private final Handler logHandler = new Handler(Looper.getMainLooper());
    private final List<String[]> parsedDataList = new ArrayList<>();
    private int recordCounter = 1;
    private int myIntervalTimeSeconds = 5;






    private final Runnable logRunnable = new Runnable() {
        @Override
        public void run() {
            if (bluetoothGatt != null && hasPermission() && bluetoothGatt.getServices() != null) {
                readQueue.clear();
                readNextCharacteristic();
            }
            logHandler.postDelayed(this, myIntervalTimeSeconds * 1000L);
        }
    };
    //buttons imported
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        connectionStatus = findViewById(R.id.connected_status);
        device_info = findViewById(R.id.device_info);
        tvReceivedData = findViewById(R.id.tvReceivedData);
        //  etSend = findViewById(R.id.etSend);
        btnSend = findViewById(R.id.btnSend);
        disconnectButton = findViewById(R.id.disconnect_button);
        loadPreferences();
        valueDropdown = findViewById(R.id.valueDropdown);
        writeButton = findViewById(R.id.writeButton);
        minuteInput = findViewById(R.id.minute_input);
        secondInput = findViewById(R.id.second_input);
        writeDelayButton = findViewById(R.id.write_delay_button);
        editSleepSeconds = findViewById(R.id.editSleepSeconds);
        btnSetSleep = findViewById(R.id.btnSetSleep);
        Spinner spinnerSensorSelect = findViewById(R.id.spinnerSensorSelect);
        Button btnSetSensor = findViewById(R.id.btnSetSensor);
        //GattManager.getGatt();
        restartButton = findViewById(R.id.button_restart);
        syncTimeButton = findViewById(R.id.button_sync_time);
        syncTimeButton.setOnClickListener(v -> writeUnixTimeWithPadding());


        bluetoothGatt = GattManager.getGatt();
        if (bluetoothGatt == null) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            finish();  // or handle reconnection flow
            return;
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> savePreferences());


        // Fetch values from input fields

        String minute = ((EditText) findViewById(R.id.minute_input)).getText().toString();
        String second = ((EditText) findViewById(R.id.second_input)).getText().toString();
        String recordingInterval = ((EditText) findViewById(R.id.editSleepSeconds)).getText().toString();

        // Save to SharedPreferences

        editor.putString("minute", minute);
        editor.putString("second", second);
        editor.putString("sleepSeconds", recordingInterval);
        editor.apply();

        // Create CSV content
        StringBuilder data = new StringBuilder();
        data.append("Minute,Second,RecordingInterval\n");

        data.append(minute).append(",");
        data.append(second).append(",");
        data.append(recordingInterval).append("\n");

        // Save CSV to internal storage
        try {
            File file = new File(getExternalFilesDir(null), "sensor_settings.csv");
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(data.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, "Saved and CSV created at:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save CSV", Toast.LENGTH_SHORT).show();
        }
    });


    // Set up spinner values
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"T", "TH"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSensorSelect.setAdapter(adapter);

// Handle button click
        btnSetSensor.setOnClickListener(v -> {
        String selected = spinnerSensorSelect.getSelectedItem().toString();
        writeSensorSelect(bluetoothGatt, selected);
    });

    //tx power input
    String[] powerOptions = {
            "-40", "-20", "-16", "-8", "-4", "0", "4"
    };

    ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            powerOptions
    );
        valueDropdown.setAdapter(adapter2);
        valueDropdown.setOnClickListener(v -> valueDropdown.showDropDown());

        writeButton.setOnClickListener(v -> writeTxPowerToCharacteristic());

        writeDelayButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int minutes = Integer.parseInt(minuteInput.getText().toString());
            int seconds = Integer.parseInt(secondInput.getText().toString());
            writeDelayToCharacteristic(bluetoothGatt, minutes, seconds);
        }
    });

        btnSetSleep.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String input = editSleepSeconds.getText().toString();
            if (!input.isEmpty()) {
                int seconds = Integer.parseInt(input);
                writeSleepInterval(bluetoothGatt, seconds);  // Call the method
            } else {
                Toast.makeText(SettingsActivity.this, "Enter seconds", Toast.LENGTH_SHORT).show();
            }
        }
    });
}







//Get device name from adapter
deviceAddress = getIntent().getStringExtra("device_address");
String name = "Unknown Device"; // Declare here so it's usable everywhere
        if (deviceAddress != null && hasPermission()) {
bluetoothDevice = getDefaultAdapter().getRemoteDevice(deviceAddress);
name = bluetoothDevice.getName();
            if (name == null || name.isEmpty()) name = "Unknown Device";
        connectionStatus.setText("Connecting to " + name);
            device_info.setText("Device Name: " + name + "\nAddress: " + deviceAddress);
connectToDevice();
            GattManager.setGatt(bluetoothGatt);

        } else {
                Toast.makeText(this, "Invalid device address or missing permission", Toast.LENGTH_SHORT).show();
        }

                btnSend.setOnClickListener(v -> {
String message = "PRINT";
            Toast.makeText(this, "Receiving Data...", Toast.LENGTH_SHORT).show();

            if (nusRxChar != null && message.length() > 0) {
        nusRxChar.setValue(message.getBytes(StandardCharsets.UTF_8));
        nusRxChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(nusRxChar);

// Simulate progress update (for example)

            }
                    });

                    disconnectButton.setOnClickListener(v -> disconnectGatt());

        }
//check permision for Build andoid 11 and above
private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    } else {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED;
    }
}


private void connectToDevice() {
    if (bluetoothDevice != null && hasPermission()) {
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
    }
}

private void disconnectGatt() {
    logHandler.removeCallbacks(logRunnable);
    if (bluetoothGatt != null) {
        if (hasPermission()) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        bluetoothGatt = null;
    }
    finish();
}

private void savePreferences() {
    SharedPreferences sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();

    // Get input fields

//        valueDropdown = findViewById(R.id.valueDropdown);
//
//        minuteInput = findViewById(R.id.minute_input);
//        secondInput = findViewById(R.id.second_input);
//        Spinner spinnerSensorSelect = findViewById(R.id.spinnerSensorSelect);

    EditText minuteInput = findViewById(R.id.minute_input);
    EditText secondInput = findViewById(R.id.second_input);
    EditText editSleepSeconds = findViewById(R.id.editSleepSeconds);

    // Save to SharedPreferences



    editor.putString("minute", minuteInput.getText().toString());
    editor.putString("second", secondInput.getText().toString());
    editor.putString("sleepSeconds", editSleepSeconds.getText().toString());

    editor.apply(); // or use commit()

    Toast.makeText(this, "Values saved successfully!", Toast.LENGTH_SHORT).show();
}
//load preference

private void loadPreferences() {
    SharedPreferences sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);

    // Get references to all input fields
//        valueDropdown = findViewById(R.id.valueDropdown);
//
//         minuteInput = findViewById(R.id.minute_input);
//         secondInput = findViewById(R.id.second_input);
    Spinner spinnerSensorSelect = findViewById(R.id.spinnerSensorSelect);

    EditText minuteInput = findViewById(R.id.minute_input);
    EditText secondInput = findViewById(R.id.second_input);
    EditText editSleepSeconds = findViewById(R.id.editSleepSeconds);

    // Set saved values to EditTexts
//
    minuteInput.setText(sharedPref.getString("minute", ""));
    secondInput.setText(sharedPref.getString("second", ""));
    editSleepSeconds.setText(sharedPref.getString("sleepSeconds", ""));
}


private void writeDelayToCharacteristic(BluetoothGatt gatt, int minutes, int seconds) {
    int totalSeconds = (minutes * 60) + seconds;

    // Convert to little-endian
    byte[] littleEndianDelay = new byte[2];
    littleEndianDelay[0] = (byte) (totalSeconds & 0xFF);
    littleEndianDelay[1] = (byte) ((totalSeconds >> 8) & 0xFF);

    BluetoothGattService service = gatt.getService(SERVICE_UUID);
    if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_290E);
        if (characteristic != null) {
            characteristic.setValue(littleEndianDelay);
            boolean result = gatt.writeCharacteristic(characteristic);
            Log.d("BLE_WRITE", "Write to 0x290E result: " + result);
        } else {
            Log.e("BLE_WRITE", "Characteristic 0x290E not found");
        }
    } else {
        Log.e("BLE_WRITE", "Service not found");
    }
}

//tx power write operation
private void writeTxPowerToCharacteristic() {
    String input = valueDropdown.getText().toString().trim();
    if (input.isEmpty()) {
        Log.e("BLE", "No value selected.");
        return;
    }

    String[] values = input.split(",");
    byte[] dataToWrite = new byte[values.length];

    try {
        for (int i = 0; i < values.length; i++) {
            int intVal = Integer.parseInt(values[i].trim());
            if (intVal < -128 || intVal > 127) {
                Log.e("BLE", "Value out of byte range: " + intVal);
                return;
            }
            dataToWrite[i] = (byte) intVal;
        }

        if (bluetoothGatt == null) {
            Log.e("BLE", "BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Log.e("BLE", "Service 0x181A not found");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Log.e("BLE", "Characteristic 0x2A07 not found");
            return;
        }

        int props = characteristic.getProperties();
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
                (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            Log.e("BLE", "Characteristic not writable");
            return;
        }

        characteristic.setValue(dataToWrite);
        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        Log.d("BLE", "Writing: " + Arrays.toString(dataToWrite) + " | Success: " + success);

    } catch (NumberFormatException e) {
        Log.e("BLE", "Invalid number format", e);
    }
}
//sensor select
private void writeSensorSelect(BluetoothGatt gatt, String sensor) {
    byte[] value = new byte[2];

    switch (sensor) {
        case "T":
            value[0] = 0x00;
            value[1] = 0x00;
            break;
        case "TH":
            value[0] = 0x10;
            value[1] = 0x00;
            break;
        default:
            Log.e("BLE_WRITE", "Unknown sensor type");
            return;
    }

    BluetoothGattService service = gatt.getService(SERVICE_UUID);
    if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Sensor_select);
        if (characteristic != null) {
            characteristic.setValue(value);
            boolean success = gatt.writeCharacteristic(characteristic);
            Log.d("BLE_WRITE", "Sensor select write to 0x2A56: " + (success ? "Success" : "Failed"));
        } else {
            Log.e("BLE_WRITE", "Characteristic 0x2A56 not found.");
        }
    } else {
        Log.e("BLE_WRITE", "Service not found.");
    }
}


// ✅ Move this method outside of onCreate and any inner classes
private void writeSleepInterval(BluetoothGatt gatt, int sleepSeconds) {
    byte[] value = new byte[2];
    value[0] = (byte) (sleepSeconds & 0xFF);         // LSB
    value[1] = (byte) ((sleepSeconds >> 8) & 0xFF);  // MSB (little endian)

    BluetoothGattService service = gatt.getService(SERVICE_UUID);
    if (service != null) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Time_Update_Control_Point);
        if (characteristic != null) {
            characteristic.setValue(value);
            boolean success = gatt.writeCharacteristic(characteristic);
            Log.d("BLE_WRITE", "Write to 0x2A16: " + (success ? "Success" : "Failed"));
        } else {
            Log.e("BLE_WRITE", "Characteristic 0x2A16 not found.");
        }
    } else {
        Log.e("BLE_WRITE", "Service not found.");
    }
}
}

private void readNextCharacteristic() {
    if (bluetoothGatt == null || readQueue.isEmpty()) return;
    UUID uuid = readQueue.poll();
    if (uuid == null) return;

    for (BluetoothGattService service : bluetoothGatt.getServices()) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            bluetoothGatt.readCharacteristic(characteristic);
            return;
        }
    }
    readNextCharacteristic();
}

private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            runOnUiThread(() -> connectionStatus.setText("Connected"));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean started = gatt.discoverServices();
                Log.i(TAG, "discoverServices started: " + started);
            }, 500);
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            runOnUiThread(() -> {
                connectionStatus.setText("Disconnected");
                Toast.makeText(ConnectedActivity.this, "Device Disconnected", Toast.LENGTH_SHORT).show();
                disconnectGatt();
            });
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
           // runOnUiThread(() -> connectionStatus.setText("Services discovered"));

            BluetoothGattService nusService = gatt.getService(NUS_SERVICE_UUID);
            if (nusService != null) {
                nusRxChar = nusService.getCharacteristic(NUS_RX_CHAR_UUID);
                nusTxChar = nusService.getCharacteristic(NUS_TX_CHAR_UUID);

                if (nusTxChar != null) {
                    gatt.setCharacteristicNotification(nusTxChar, true);
                    BluetoothGattDescriptor descriptor = nusTxChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }

            logHandler.removeCallbacks(logRunnable);
            logHandler.post(logRunnable);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            updateFromCharacteristic(characteristic);
        }
        readNextCharacteristic();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(NUS_TX_CHAR_UUID)) {
            byte[] data = characteristic.getValue();

            // Validate data length and structure
            if (data.length >= 12 && data[0] == 0x00 && data[data.length - 1] == (byte) 0xFF) {
                long timestampSec = ((data[4] & 0xFFL) << 24) | ((data[3] & 0xFFL) << 16) |
                        ((data[2] & 0xFFL) << 8) | (data[1] & 0xFFL);

                int tempRaw = (short) (((data[6] & 0xFF) << 8) | (data[5] & 0xFF));
                int humRaw = ((data[8] & 0xFF) << 8) | (data[7] & 0xFF);
                int batRaw = ((data[10] & 0xFF) << 8) | (data[9] & 0xFF);

                double tempC = tempRaw / 100.0;
                double humPercent = humRaw / 100.0;
                double batVoltage = batRaw / 100.0;

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedTime = sdf.format(new Date(timestampSec * 1000L));
                Log.d("BLE", "Parsed Time (UTC): " + formattedTime);

                // Add parsed data to list for future export or display
                String[] row = new String[]{
                        String.valueOf(recordCounter++),
                        String.valueOf(timestampSec),
                        String.format(Locale.US, "%.2f", tempC),
                        String.format(Locale.US, "%.2f", humPercent),
                        String.format(Locale.US, "%.2f", batVoltage)
                };
                parsedDataList.add(row);

                // Keep only last two entries for display
                final int maxDisplayCount = 2;
                List<String> displayList = new ArrayList<>();

                // Prepare last two entries for display
                int start = Math.max(parsedDataList.size() - maxDisplayCount, 0);
                for (int i = 0; i < parsedDataList.size(); i++) {
                    String[] r = parsedDataList.get(i);

                    if (row.length < 5) continue;
                    boolean isEmptyRow = true;
                    for (int j = 0; j < 5; j++) {
                        if (row[j] != null && !row[j].trim().isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                    if (isEmptyRow) continue;

                    // existing code to draw text and lines...
                }

//                // Update UI on main thread
//                runOnUiThread(() -> {
//                    tvReceivedData.setText(""); // clear previous text
//                    for (String s : displayList) {
//                        tvReceivedData.append(s + "\n");
//                    }


            } else {
                Log.w(TAG, "Invalid packet structure");
            }
        }
    }


    private void updateFromCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if (data != null) {
//            String message = new String(data, StandardCharsets.UTF_8);
//            runOnUiThread(() -> tvReceivedData.append("Read: " + message + "\n"));
        }
    }
};
//progress bar

public void updateFromCallback(BluetoothGattCharacteristic characteristic) {
    updateFromCharacteristic(characteristic);
}

private void updateFromCharacteristic(BluetoothGattCharacteristic characteristic) {
    byte[] data = characteristic.getValue();
    if (data != null) {
       // String message = new String(data, StandardCharsets.UTF_8);
       // runOnUiThread(() -> tvReceivedData.append("Read: " + message + "\n"));
    }
}

