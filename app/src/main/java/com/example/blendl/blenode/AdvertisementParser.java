package com.example.blendl.blenode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdvertisementParser {

    public static Map<String, String> parse(byte[] record) {
        Map<String, String> map = new HashMap<>();
        int index = 0;

        while (index < record.length) {
            int length = record[index++] & 0xFF;
            if (length == 0) break;
            int type = record[index++] & 0xFF;

            if (type == 0x16) { // Service Data
                byte[] data = Arrays.copyOfRange(record, index, index + length - 1);
                if (data.length == 12) {
                    String mac = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                            data[0], data[1], data[2], data[3], data[4], data[5]);
                    int temp = ((data[7] & 0xFF) << 8) | (data[6] & 0xFF);
                    int hum = ((data[9] & 0xFF) << 8) | (data[8] & 0xFF);
                    int bat = ((data[11] & 0xFF) << 8) | (data[10] & 0xFF);

                    map.put("MAC ID", mac);
                    map.put("Temperature", temp / 100.0 + " °C");
                    map.put("Humidity", hum / 100.0 + " %");
                    map.put("Battery", bat  + " mV");
                }
            }

            index += (length - 1);
        }

        return map;
    }
}