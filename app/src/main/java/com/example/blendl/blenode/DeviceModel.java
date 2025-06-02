package com.example.blendl.blenode;

import java.util.Map;

public class DeviceModel {
    private String name;
    private String mac;
    private int rssi;
    private Map<String, String> parsedData;

    public DeviceModel(String name, String mac, int rssi, Map<String, String> parsedData) {
        this.name = name;
        this.mac = mac;
        this.rssi = rssi;
        this.parsedData = parsedData;
    }

    public String getName() { return name != null ? name : "Unnamed"; }
    public String getMac() { return mac; }
    public int getRssi() { return rssi; }
    public Map<String, String> getParsedData() { return parsedData; }
}
