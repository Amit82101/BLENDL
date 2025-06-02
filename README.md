# 🌡️ Real-Time Temperature & Humidity Monitoring App (Zephyr RTOS + Android BLE)

This project demonstrates a **real-time embedded IoT system** using **Zephyr RTOS** on the **nRF52840 Dongle** and an accompanying **Android BLE application** to monitor **temperature**, **humidity**, and **battery voltage**. Data is transmitted via **Bluetooth Low Energy (BLE)** in a custom 12-byte advertising payload and visualized on an Android device with real-time logging and basic graphing.

---

## 🔧 Key Features

### 🔌 Embedded System (nRF52840 + Zephyr RTOS)
- Periodically measures and broadcasts temperature, humidity, and battery voltage every 15 seconds via BLE.
- Custom 12-byte raw payload structure for efficient broadcasting.
- Utilizes Zephyr’s power management to implement **deep sleep mode** for energy efficiency.

### 📱 Android BLE App
- Scans nearby BLE advertisements and parses custom payloads.
- Decodes and displays:
  - Device Name
  - MAC Address
  - Temperature (°C)
  - Humidity (% RH)
  - Battery Voltage (V)
- Real-time data logging using **SQLite** with timestamped entries.
- Interactive data visualization using line/bar charts.

---

## 🏗️ System Architecture
[nRF52840 + Zephyr RTOS]
|
| (BLE Advertisements every 15s with 12-byte payload)
|
[Android App (BLE Scanner)]
├── Parses BLE Payload
├── Displays Real-Time Data
├── Logs to SQLite
└── Visualizes via Charts


## 💻 Tech Stack

### Embedded:
- **Zephyr RTOS**
- **nRF52840 Dongle**
- **BLE (Bluetooth Low Energy)**
- **Custom GATT Advertising**
- **Sensor Simulation**

### Android App:
- **Java + Android SDK**
- **BLE Scanning & Parsing**
- **SQLite** (local datalogging)
- **MPAndroidChart** (for visualization)
- **LiveData & Lifecycle Components**
- **XML-based UI**

---
git clone https://github.com/Amit82101/BLENDL.git
