package com.example.blendl.bledatalogger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blendl.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datalogger_settings);
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

        restartButton.setOnClickListener(v -> sendRestartCommand());
        bluetoothGatt = GattManager.getGatt();
        if (bluetoothGatt == null) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            finish();  // or handle reconnection flow
            return;
        }
        //temp1
        tempRangeTextView = findViewById(R.id.tempRangeTextView);
        minTempInput = findViewById(R.id.minTempInput);
        maxTempInput = findViewById(R.id.maxTempInput);
        applyTempButton = findViewById(R.id.applyTempButton);

        applyTempButton.setOnClickListener(v -> applyTemperatureRange());

        //temp2
        tempRange2TextView = findViewById(R.id.tempRange2TextView);
        minTempTHInput = findViewById(R.id.minTempTHInput);
        maxTempTHInput = findViewById(R.id.maxTempTHInput);
        applyTemp2Button = findViewById(R.id.applyTemp2Button);

        applyTemp2Button.setOnClickListener(v -> applyTemperatureRange2());

        //humidity
        HumdRangeTextView = findViewById(R.id.HumdRangeTextView);
        minHumdInput = findViewById(R.id.minHumdInput);
        maxHumdInput = findViewById(R.id.maxHumdInput);
        applyHumdButton = findViewById(R.id.applyHumdButton);

        applyHumdButton.setOnClickListener(v -> applyHumidityRange());

        //button to sharedpreference




        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> savePreferences());
        btnSave.setOnClickListener(view -> {
            SharedPreferences sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            // Fetch values from input fields
            String minTemp = ((EditText) findViewById(R.id.minTempInput)).getText().toString();
            String maxTemp = ((EditText) findViewById(R.id.maxTempInput)).getText().toString();
            String minute = ((EditText) findViewById(R.id.minute_input)).getText().toString();
            String second = ((EditText) findViewById(R.id.second_input)).getText().toString();
            String recordingInterval = ((EditText) findViewById(R.id.editSleepSeconds)).getText().toString();

            // Save to SharedPreferences
            editor.putString("minTemp", minTemp);
            editor.putString("maxTemp", maxTemp);
            editor.putString("minute", minute);
            editor.putString("second", second);
            editor.putString("sleepSeconds", recordingInterval);
            editor.apply();

            // Create CSV content
            StringBuilder data = new StringBuilder();
            data.append("MinTemp,MaxTemp,Minute,Second,RecordingInterval\n");
            data.append(minTemp).append(",");
            data.append(maxTemp).append(",");
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


    private void applyTemperatureRange() {
        String minStr = minTempInput.getText().toString().trim();
        String maxStr = maxTempInput.getText().toString().trim();

        if (minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "Please enter both min and max temperature", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int minTemp = Integer.parseInt(minStr);
            int maxTemp = Integer.parseInt(maxStr);

            if (minTemp < -20 || maxTemp > 60) {
                Toast.makeText(this, "Temperature must be between -20 and 60°C", Toast.LENGTH_SHORT).show();
                return;
            }

            if (minTemp >= maxTemp) {
                Toast.makeText(this, "Min temperature must be less than Max temperature", Toast.LENGTH_SHORT).show();
                return;
            }

            tempRangeTextView.setText("Temperature Range: " + minTemp + "°C to " + maxTemp + "°C");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Enter numbers only.", Toast.LENGTH_SHORT).show();
        }
    }
    private void applyTemperatureRange2() {
        String minStr = minTempTHInput.getText().toString().trim();
        String maxStr = maxTempTHInput.getText().toString().trim();

        if (minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "Please enter both min and max temperature", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int minTemp = Integer.parseInt(minStr);
            int maxTemp = Integer.parseInt(maxStr);

            if (minTemp < -20 || maxTemp > 60) {
                Toast.makeText(this, "Temperature must be between -20 and 60°C", Toast.LENGTH_SHORT).show();
                return;
            }

            if (minTemp >= maxTemp) {
                Toast.makeText(this, "Min temperature must be less than Max temperature", Toast.LENGTH_SHORT).show();
                return;
            }

            tempRange2TextView.setText("Temperature Range: " + minTemp + "°C to " + maxTemp + "°C");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Enter numbers only.", Toast.LENGTH_SHORT).show();
        }
    }
    private void applyHumidityRange() {
        String minStr = minHumdInput.getText().toString().trim();
        String maxStr = maxHumdInput.getText().toString().trim();

        if (minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "Please enter both min and max temperature", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int minTemp = Integer.parseInt(minStr);
            int maxTemp = Integer.parseInt(maxStr);

            if (minTemp < -20 || maxTemp > 60) {
                Toast.makeText(this, "Temperature must be between -20 and 60°C", Toast.LENGTH_SHORT).show();
                return;
            }

            if (minTemp >= maxTemp) {
                Toast.makeText(this, "Min temperature must be less than Max temperature", Toast.LENGTH_SHORT).show();
                return;
            }

            HumdRangeTextView.setText("Humidity Range: " + minTemp + "% to " + maxTemp + "%");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Enter numbers only.", Toast.LENGTH_SHORT).show();
        }
    }

//sharedpreference

    private void savePreferences() {
        SharedPreferences sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // Get input fields

//        valueDropdown = findViewById(R.id.valueDropdown);
//
//        minuteInput = findViewById(R.id.minute_input);
//        secondInput = findViewById(R.id.second_input);
//        Spinner spinnerSensorSelect = findViewById(R.id.spinnerSensorSelect);
        EditText minTempInput = findViewById(R.id.minTempInput);
        EditText maxTempInput = findViewById(R.id.maxTempInput);
        EditText minTempTHInput = findViewById(R.id.minTempTHInput);
        EditText maxTempTHInput = findViewById(R.id.maxTempTHInput);
        EditText minHumdInput = findViewById(R.id.minHumdInput);
        EditText maxHumdInput = findViewById(R.id.maxHumdInput);
        EditText minuteInput = findViewById(R.id.minute_input);
        EditText secondInput = findViewById(R.id.second_input);
        EditText editSleepSeconds = findViewById(R.id.editSleepSeconds);

        // Save to SharedPreferences
//        editor.putString("minTemp",valueDropdown.getText().toString());
//        editor.putString("minTemp", writeButton.getText().toString());
//        editor.putString("minTemp",minuteInput.getText().toString());
//        editor.putString("minTemp",secondInput.getText().toString());
       // editor.putString("minTemp",Spinner.getText().toString());

        editor.putString("minTemp", minTempInput.getText().toString());
        editor.putString("maxTemp", maxTempInput.getText().toString());
        editor.putString("minTempTH", minTempTHInput.getText().toString());
        editor.putString("maxTempTH", maxTempTHInput.getText().toString());
        editor.putString("minHumd", minHumdInput.getText().toString());
        editor.putString("maxHumd", maxHumdInput.getText().toString());
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
        EditText minTempInput = findViewById(R.id.minTempInput);
        EditText maxTempInput = findViewById(R.id.maxTempInput);
        EditText minTempTHInput = findViewById(R.id.minTempTHInput);
        EditText maxTempTHInput = findViewById(R.id.maxTempTHInput);
        EditText minHumdInput = findViewById(R.id.minHumdInput);
        EditText maxHumdInput = findViewById(R.id.maxHumdInput);
        EditText minuteInput = findViewById(R.id.minute_input);
        EditText secondInput = findViewById(R.id.second_input);
        EditText editSleepSeconds = findViewById(R.id.editSleepSeconds);

        // Set saved values to EditTexts
//        valueDropdown.setText(sharedPref.getString("minTemp", ""));
//        writeButton.setText(sharedPref.getString("minTemp", ""));
//        minuteInput.setText(sharedPref.getString("minTemp", ""));
//        secondInput.setText(sharedPref.getString("minTemp", ""));
//       // Spinner.setText(sharedPref.getString("minTemp", ""));
        minTempInput.setText(sharedPref.getString("minTemp", ""));
        maxTempInput.setText(sharedPref.getString("maxTemp", ""));
        minTempTHInput.setText(sharedPref.getString("minTempTH", ""));
        maxTempTHInput.setText(sharedPref.getString("maxTempTH", ""));
        minHumdInput.setText(sharedPref.getString("minHumd", ""));
        maxHumdInput.setText(sharedPref.getString("maxHumd", ""));
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
    private void writeUnixTimeWithPadding() {
        if (bluetoothGatt == null) {
            Log.e("BLE_WRITE", "BluetoothGatt not connected.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(ENVIRONMENTAL_SENSING_SERVICE_UUID);
        if (service == null) {
            Log.e("BLE_WRITE", "Environmental Sensing Service (0x181A) not found.");
            return;
        }

        BluetoothGattCharacteristic timeChar = service.getCharacteristic(CURRENT_TIME_CHAR_UUID);
        if (timeChar == null) {
            Log.e("BLE_WRITE", "Current Time Characteristic (0x2A2B) not found.");
            return;
        }

        long currentUnixTime = System.currentTimeMillis() / 1000L;
        long istUnixTime = currentUnixTime;
        //long istUnixTime = currentUnixTime + IST_OFFSET_SECONDS;

        // Total 8 bytes: 4 for time, 4 for zero padding
        byte[] timeBytes = new byte[8];
        timeBytes[0] = (byte) (istUnixTime & 0xFF);
        timeBytes[1] = (byte) ((istUnixTime >> 8) & 0xFF);
        timeBytes[2] = (byte) ((istUnixTime >> 16) & 0xFF);
        timeBytes[3] = (byte) ((istUnixTime >> 24) & 0xFF);
        // timeBytes[4] to [7] remain 0x00 by default

        timeChar.setValue(timeBytes);
        boolean result = bluetoothGatt.writeCharacteristic(timeChar);

        Log.d("BLE_WRITE", "IST Unix Time: " + istUnixTime);
        Log.d("BLE_WRITE", "8 bytes written: " + Arrays.toString(timeBytes));
        Toast.makeText(this, result ? "Time synced (8 bytes)" : "Write failed", Toast.LENGTH_SHORT).show();
    }
    private void sendRestartCommand() {
        if (bluetoothGatt == null) {
            Log.e("BLE", "BluetoothGatt not connected!");
            return;
        }

        UUID SERVICE_UUID = UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb"); // Environmental Sensing
        UUID RC_CONTROL_POINT_UUID = UUID.fromString("00002B1F-0000-1000-8000-00805f9b34fb"); // RC Control Point

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic rcControlPointChar = service.getCharacteristic(RC_CONTROL_POINT_UUID);
            if (rcControlPointChar != null) {
                rcControlPointChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                rcControlPointChar.setValue(new byte[]{0x00}); // 0x00 = Reset
                boolean success = bluetoothGatt.writeCharacteristic(rcControlPointChar);
                Log.d("BLE", "Sent reset command: " + success);
                Toast.makeText(this, success ? "Restart command sent" : "Failed to send", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("BLE", "RC Control Point characteristic not found!");
            }
        } else {
            Log.e("BLE", "Environmental Sensing Service not found!");
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

