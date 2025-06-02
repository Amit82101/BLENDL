package com.example.blendl.bledatalogger;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;
import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.blendl.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.IOException;
import java.io.OutputStream;
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


public class ConnectedActivity extends AppCompatActivity {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");



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
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnsetting=findViewById(R.id.btnsetting);
        btnExportPdf=findViewById(R.id.btnExportPdf);
       progressBar = findViewById(R.id.progressBar);
         progressText = findViewById(R.id.progressText);




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
                simulateProgress();  // Simulate a download-like progress
            }
        });


// Simulated progress (0 to 100% in steps)

        disconnectButton.setOnClickListener(v -> disconnectGatt());



        btnExportPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveParsedDataAsPdf();
            }
        });
 //adding setting button
        btnsetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConnectedActivity.this, SettingsActivity.class);
                Toast.makeText(ConnectedActivity.this, "setting clicked", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });

    }
        private void updateProgress(int progress) {
        progressBar.setProgress(progress);
        progressText.setText("Progress: " + progress + "%");
    }

    private void simulateProgress() {

            new Thread(() -> {
                for (int i = 0; i <= 100; i += 5) {
                    int finalI = i;
                    runOnUiThread(() -> updateProgress(finalI));
                    try {
                        Thread.sleep(100); // simulate data reception delay
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
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
                runOnUiThread(() -> connectionStatus.setText("Services discovered"));

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

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        tvReceivedData.setText(""); // clear previous text
                        for (String s : displayList) {
                            tvReceivedData.append(s + "\n");
                        }
                    });

                } else {
                    Log.w(TAG, "Invalid packet structure");
                }
            }
        }


        private void updateFromCharacteristic(BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null) {
                String message = new String(data, StandardCharsets.UTF_8);
                runOnUiThread(() -> tvReceivedData.append("Read: " + message + "\n"));
            }
        }
    };
    //progress bar


    //generate pdf
    private void saveParsedDataAsPdf() {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 20;
        int rowHeight = 25;

        int[] originalColWidths = {50, 150, 90, 90, 90};
        int totalColWidth = 0;
        for (int w : originalColWidths) totalColWidth += w;

        int availableWidth = pageWidth - 2 * margin;
        float scaleFactor = (float) availableWidth / totalColWidth;

        int[] colWidths = new int[originalColWidths.length];
        int[] colPositions = new int[originalColWidths.length];
        int currentX = margin;
        for (int i = 0; i < originalColWidths.length; i++) {
            colWidths[i] = Math.round(originalColWidths[i] * scaleFactor);
            colPositions[i] = currentX;
            currentX += colWidths[i];
        }
        int rightEdge = currentX;
        int rightMargin = pageWidth - margin;

        int pageNumber = 1;
        int y;

        PdfDocument.Page page = startNewPage(document, pageNumber, pageWidth, pageHeight);
        Canvas canvas = page.getCanvas();

        // Draw Logo
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ideabytes_iot);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 140, 40, true);
        canvas.drawBitmap(scaledLogo, margin, 20, null);

        // Title and header section
        y = 80;
        paint.setFakeBoldText(true);
        paint.setTextSize(16);
        canvas.drawText("Data Report", pageWidth / 2f - 50, y, paint);
        y += 30;


        //header 1 Logging summary
        //header 2  Device Details
        //header 3   Alarm specification
        //header 4   Report Summary




        paint.setFakeBoldText(false);
        paint.setTextSize(12);



        // --- Logging Summary Heading ---
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Logging Summary", margin, y, paint);

// Horizontal line under heading
        canvas.drawLine(margin, y + 2, rightMargin, y + 2, paint);
        y += 20;

// Restore normal text formatting
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

// --- Parse Start and End Time from parsedDataList ---
        String startTimeFormatted = "N/A";
        String endTimeFormatted = "N/A";

        if (!parsedDataList.isEmpty()) {
            try {
                String startTimestamp = parsedDataList.get(0)[1];
                long startTs = Long.parseLong(startTimestamp);
                if (startTimestamp.length() > 10) startTs /= 1000;
                Date startDate = new Date(startTs * 1000);

                String endTimestamp = parsedDataList.get(parsedDataList.size() - 1)[1];
                long endTs = Long.parseLong(endTimestamp);
                if (endTimestamp.length() > 10) endTs /= 1000;
                Date endDate = new Date(endTs * 1000);

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));  // Set to IST
                startTimeFormatted = sdf.format(startDate);
                endTimeFormatted = sdf.format(endDate);

            } catch (Exception e) {
                Log.e("PDF", "Timestamp parsing error: " + e.getMessage());
            }
        }

// Draw Start Time (left)
        canvas.drawText("Start Time:  " + startTimeFormatted, margin, y, paint);

// Draw End Time (right-aligned)
        String endTimeText = "End Time:    " + endTimeFormatted;
        float endTimeWidth = paint.measureText(endTimeText);
        canvas.drawText(endTimeText, rightMargin - endTimeWidth, y, paint);
        y += 20;

// Draw Device Name (left)
//        String deviceName = BluetoothDeviceData.getName();
//        canvas.drawText("Device:      " + deviceName, margin, y, paint);
       // Draw Start Time (left)
       // canvas.drawText("Data Interval:  " + DataInterval, margin, y, paint);
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

// Get raw sleep seconds
        String sleepSeconds = prefs.getString("sleepSeconds", "N/A");
        String dataInterval = sleepSeconds + " seconds";
        canvas.drawText("Data Interval:  " + dataInterval, margin, y, paint);


// Draw Time Zone (right-aligned)
        String timeZoneText = "Time Zone:   GMT+05:30";
        float timeZoneWidth = paint.measureText(timeZoneText);
        canvas.drawText(timeZoneText, rightMargin - timeZoneWidth, y, paint);
        y += 30;

        //adding headers
        // === Custom Section Headers ===
        paint.setTextSize(14);
        paint.setFakeBoldText(true);

        canvas.drawText("Device Details", margin, y, paint);
       // y += 20;
        canvas.drawLine(margin, y, rightMargin, y, paint);
        y += 20;

        //subheadings
        // Set regular font
        paint.setFakeBoldText(false);
        paint.setTextSize(12);

// Line 1: Name (left) and Parameter (right)

        //String name = "Unknown Device"; // Declare here so it's usable everywhere
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
        String nameText = "Name: "+ name ;
        String parameterText = "Parameter: Temperature, Humidity";
        float parameterWidth = paint.measureText(parameterText);
        canvas.drawText(nameText, margin, y, paint);
        canvas.drawText(parameterText, rightMargin - parameterWidth, y, paint);
        y += 18;

// Line 2: Serial No (left) and Frequency (right)

        String serialText = "Serial No: " + deviceAddress;
        String freqText = "Frequency:   " + sleepSeconds + " seconds";


        float freqWidth = paint.measureText(freqText);
        canvas.drawText(serialText, margin, y, paint);
        canvas.drawText(freqText, rightMargin - freqWidth, y, paint);
        y += 30;
//////////////////////////////////////////
        canvas.drawText("Alarm Specification", margin, y, paint);
        //y += 20;
        canvas.drawLine(margin, y, rightMargin, y, paint);
        y += 12;
////////////////////
        //subheadings
        // Alarm Specification Heade

// Sensor: Temperature
        paint.setTextSize(10);
        paint.setFakeBoldText(true);
        canvas.drawText("Sensor", margin, y, paint);
        canvas.drawText("Good", margin + 100, y, paint);
        canvas.drawText("Warning", margin + 250, y, paint);
        canvas.drawText("Critical", margin + 400, y, paint);
        y += 12;

// Row: Temperature
        paint.setFakeBoldText(false);
        canvas.drawText("Temperature", margin, y, paint);
        canvas.drawText("≥15.5–24.5°C", margin + 100, y, paint);
        canvas.drawText("≥24.5–25°C, 15–15.5°C", margin + 250, y, paint);
        canvas.drawText("≥25°C, <15°C", margin + 400, y, paint);
        y += 12;

// Row: Humidity
        canvas.drawText("Humidity", margin, y, paint);
        canvas.drawText("≥20–60%", margin + 100, y, paint);
        canvas.drawText("—", margin + 250, y, paint);
        canvas.drawText("≥60%, <20%", margin + 400, y, paint);
        y += 20;

        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        //
        canvas.drawText("Report Summary", margin, y, paint);
        //y += 30;
        // Horizontal line under heading
        canvas.drawLine(margin, y + 2, rightMargin, y + 2, paint);
        y += 20;
        //subheading
        // Column Titles
        paint.setTextSize(10);
        canvas.drawText("Sensor", margin, y, paint);
        canvas.drawText("Minimum", margin + 200, y, paint);
        canvas.drawText("Average", margin + 320, y, paint);
        canvas.drawText("Maximum", margin + 440, y, paint);
        y += 12;

// Row: Temperature (with MKT)
        paint.setFakeBoldText(false);

        String temperatureTitle = "Temperature (MKT: 18.9°C)";
        canvas.drawText(temperatureTitle, margin, y, paint);
        canvas.drawText("17.9°C", margin + 200, y, paint);
        canvas.drawText("19.3°C", margin + 320, y, paint);
        canvas.drawText("21.8°C", margin + 440, y, paint);
        y += 12;

// Row: Humidity
        canvas.drawText(" Humidity", margin, y, paint);
        canvas.drawText("54.2%", margin + 200, y, paint);
        canvas.drawText("60.1%", margin + 320, y, paint);
        canvas.drawText("66.1%", margin + 440, y, paint);
        y += 20;
        //

        paint.setTextSize(12);
        paint.setFakeBoldText(false);

//header finished

//////////////////////////////////////////////header for slno,temp,humd,batterry////////////////
        // Prepare data for the chart

        // Track table header start Y
        int tableTopY = y;
        y = drawTableHeader(canvas, paint, colPositions, margin, rightEdge, y, rowHeight);
        tableTopY = y - rowHeight;

        for (int i = 0; i < parsedDataList.size(); i++) {
            String[] row = parsedDataList.get(i);
            if (row.length < 5) continue;

            if (y + rowHeight > pageHeight - margin) {
                drawVerticalLines(canvas, colPositions, tableTopY, y, rightEdge);
                document.finishPage(page);
                pageNumber++;

                page = startNewPage(document, pageNumber, pageWidth, pageHeight);
                canvas = page.getCanvas();

                y = margin + 60;
                tableTopY = y;
                y = drawTableHeader(canvas, paint, colPositions, margin, rightEdge, y, rowHeight);
            }

            for (int c = 0; c < 5; c++) {
                String cellData = row[c];

                if (c == 1) {
                    try {
                        long timestamp = Long.parseLong(cellData);
                        Date date;

                        if (cellData.length() > 10) {
                            // Milliseconds
                            date = new Date(timestamp);
                        } else {
                            // Seconds
                            date = new Date(timestamp * 1000);
                        }

                        // Explicitly set IST timezone
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // Force IST
                        cellData = sdf.format(date);
                    } catch (Exception e) {
                        cellData = "Invalid Time";
                    }
                }

                canvas.drawText(cellData, colPositions[c] + 5, y + 17, paint);
            }

            canvas.drawLine(margin, y + rowHeight, rightEdge, y + rowHeight, paint);
            y += rowHeight;
        }

        drawVerticalLines(canvas, colPositions, tableTopY, y, rightEdge);
        document.finishPage(page);

        //we add charts

        // Prepare data for the chart
        int chartWidth = 595;  // PDF page width in points (A4)
        int chartHeight = 400;

        Bitmap tempChart = generateLineChart(parsedDataList, 2, "Temperature vs Time", "Temp (°C)", chartWidth, chartHeight);
        Bitmap humChart = generateLineChart(parsedDataList, 3, "Humidity vs Time", "Humidity (%)", chartWidth, chartHeight);

// --- Temp Chart Page ---
        PdfDocument.Page chartPage1 = startNewPage(document, ++pageNumber, pageWidth, pageHeight);
        Canvas chartCanvas1 = chartPage1.getCanvas();

// Draw heading for Temperature Chart
        Paint headingPaint = new Paint();
        headingPaint.setColor(Color.BLACK);
        headingPaint.setTextSize(18);
        headingPaint.setFakeBoldText(true);
        chartCanvas1.drawText("Temperature vs Time", 40, 40, headingPaint);

// Draw chart bitmap below the heading
        chartCanvas1.drawBitmap(tempChart, 0, 60, null);
        document.finishPage(chartPage1);

// --- Humidity Chart Page ---
        PdfDocument.Page chartPage2 = startNewPage(document, ++pageNumber, pageWidth, pageHeight);
        Canvas chartCanvas2 = chartPage2.getCanvas();

// Draw heading for Humidity Chart
        chartCanvas2.drawText("Humidity vs Time", 40, 40, headingPaint);

// Draw chart bitmap below the heading
        chartCanvas2.drawBitmap(humChart, 0, 60, null);
        document.finishPage(chartPage2);


        // Save PDF
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Data_Report.pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        ContentResolver resolver = getContentResolver();
        OutputStream outputStream = null;

        try {
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            if (uri == null) throw new IOException("Failed to create MediaStore entry.");
            outputStream = resolver.openOutputStream(uri);
            if (outputStream == null) throw new IOException("Failed to open output stream.");

            document.writeTo(outputStream);
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
        }
    }

    private PdfDocument.Page startNewPage(PdfDocument document, int pageNumber, int width, int height) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, pageNumber).create();
        return document.startPage(pageInfo);
    }
    //chart before header

    private void drawVerticalLines(Canvas canvas, int[] colPositions, int topY, int bottomY, int rightEdge) {
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(1);

        for (int i = 0; i < colPositions.length; i++) {
            canvas.drawLine(colPositions[i], topY, colPositions[i], bottomY, linePaint);
        }
        canvas.drawLine(rightEdge, topY, rightEdge, bottomY, linePaint);
    }


    private Bitmap generateLineChart(List<String[]> dataList, int valueIndex, String title, String label, int width, int height) {
        LineChart chart = new LineChart(this);
        List<Entry> entries = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();
        chart.getAxisRight().setEnabled(false);


        for (int i = 0; i < dataList.size(); i++) {
            String[] row = dataList.get(i);
            if (row.length <= valueIndex) continue;

            try {
                long timestamp = Long.parseLong(row[1]);
                if (row[1].length() <= 10) timestamp *= 1000;
                float value = Float.parseFloat(row[valueIndex]);
                entries.add(new Entry(i, value));

                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                timeLabels.add(sdf.format(date));
            } catch (Exception e) {
                Log.e("Chart", "Parse error: " + e.getMessage());
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.BLUE);
        // dataSet.setCircleColor(Color.RED);
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setText(title);
        chart.getDescription().setTextSize(12f);

        // Format X Axis with time labels
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < timeLabels.size()) ? timeLabels.get(index) : "";
            }
        });
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelRotationAngle(-45);

        chart.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        chart.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        chart.layout(0, 0, chart.getMeasuredWidth(), chart.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        chart.draw(canvas);

        return bitmap;
    }
    /// ///

    private int drawTableHeader(Canvas canvas, Paint paint, int[] colPositions, int margin, int rightEdge, int y, int rowHeight) {
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(margin, y, rightEdge, y + rowHeight, paint);

        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        String[] headers = {"S.No", "Date/Time", "Temp (°C)", "Humidity (%)", "Battery (%)"};
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], colPositions[i] + 5, y + 17, paint);
        }

        paint.setFakeBoldText(false);
        return y + rowHeight;
    }





    public void updateFromCallback(BluetoothGattCharacteristic characteristic) {
        updateFromCharacteristic(characteristic);
    }

    private void updateFromCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if (data != null) {
            String message = new String(data, StandardCharsets.UTF_8);
            runOnUiThread(() -> tvReceivedData.append("Read: " + message + "\n"));
        }
    }
};