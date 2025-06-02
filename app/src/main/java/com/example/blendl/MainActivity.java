package com.example.blendl;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blendl.bledatalogger.BleDataLoggerActivity;
import com.example.blendl.blenode.BleNodeActivity;
import com.example.blendl.blenode.DeviceAdapter;

public class MainActivity extends AppCompatActivity {
    Button buttonNode, buttonLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonNode = findViewById(R.id.button_ble_node);
        buttonLogger = findViewById(R.id.button_ble_data_logger);


        // Set layout manager

        buttonNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BleNodeActivity.class);
                startActivity(intent);
            }
        });

        buttonLogger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BleDataLoggerActivity.class);
                startActivity(intent);
            }
        });



    }
}
