package com.example.blinkify;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    ListView listView;
    Button reset,send;

    final static int BT_CONNECT_REQUEST = 1;
    final static int BT_SCAN_REQUEST=5;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] devices;
    ActivityResultLauncher<Intent> bluetoothDeviceActivityLauncher;
    Connector connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        bluetoothDeviceActivityLauncher=getBluetoothDeviceActivityLauncher();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Device does not support bluetooth!");
            return;
        }

        //Checking and requesting for BLUETOOTH_CONNECT permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {Manifest.permission.BLUETOOTH_CONNECT};
            requestPermissions(permissions, BT_CONNECT_REQUEST);
        } else {
            showToast("Has BLUETOOTH_CONNECT permission");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {Manifest.permission.BLUETOOTH_SCAN};
            requestPermissions(permissions, BT_SCAN_REQUEST);
        } else {
            showToast("Has BLUETOOTH_SCAN permission");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Turning bluetooth on
        if (!bluetoothAdapter.isEnabled()) {
            ActivityResultLauncher<Intent> launcher = getBluetoothLauncher();
            launcher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        } else {
            showToast("Bluetooth is already enabled");
        }

        //Initializing the listview
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        devices = new BluetoothDevice[deviceSet.size()];
        String[] deviceNames = new String[deviceSet.size()];
        int index = 0;
        for (BluetoothDevice device : deviceSet) {
            devices[index] = device;
            deviceNames[index++] = device.getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listView.setAdapter(adapter);

        registerListeners();

    }

    private void registerListeners() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long id) {
                BluetoothDevice device = devices[index];
                Intent i=new Intent(MainActivity.this,BluetoothDeviceActivity.class);
                i.putExtra("device",device);
                bluetoothDeviceActivityLauncher.launch(i);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case BT_CONNECT_REQUEST:
            case BT_SCAN_REQUEST:
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    recreate();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void showToast(String text){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,text,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ActivityResultLauncher<Intent> getBluetoothDeviceActivityLauncher(){
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()== Activity.RESULT_OK){
                    showToast("Select a device to connect");
                }else{
                    showToast("Operation failed");
                }
            }
        });
    }

    private ActivityResultLauncher<Intent> getBluetoothLauncher(){
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()== Activity.RESULT_OK){
                    showToast("Bluetooth successfully turned on");
                }else{
                    showToast("Unable to turn on bluetooth");
                }
            }
        });
    }

    private void findViews(){
        listView=findViewById(R.id.listView);
    }




}