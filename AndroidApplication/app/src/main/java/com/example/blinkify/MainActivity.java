package com.example.blinkify;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

    TextView status, message,sendData;
    ListView listView;
    Button reset,send;
    int count=0;
    String receivedData="";
    String englishContent="";
    final static int BT_CONNECT_REQUEST = 1;
    final static int BT_SCAN_REQUEST=5;
    final static int STATE_CONNECTING=2;
    final static int STATE_CONNECTED=3;
    final static int STATE_MESSAGE_RECEIVED=4;
    final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] devices;
    Connector connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        message.setMovementMethod(new ScrollingMovementMethod());

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
                connector = new Connector(device);
                connector.start();
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                englishContent="";
                receivedData="";
                count=0;
                status.setText("No messages");
                message.setText("");
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sendData.getText().length()>0)
                    connector.send(sendData.getText().toString());
            }
        });
    }

    private final Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_CONNECTING:
                    status.setText("CONNECTING...");
                    break;
                case STATE_CONNECTED:
                    status.setText("CONNECTED");
                    message.setText("");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    count++;
                    int data=msg.arg1;
                    if(data==0)
                        receivedData+=".";
                    else if(data==1)
                        receivedData+="-";
                    else
                        processData();
                    message.setText(englishContent+receivedData);
                    status.setText("Message No:"+count);
                    break;
            }
            return true;
        }
    });

    public void processData() {
        String s=decode(receivedData);
        if(s.equals("REDUCE"))
            englishContent=englishContent.substring(0,englishContent.length()-1);
        else
            englishContent+=s;
        receivedData="";
    }

    class Connector extends Thread {
        final BluetoothDevice device;
        BluetoothSocket socket;
        OutputStream out;

        public Connector(BluetoothDevice device) {
            this.device = device;
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                showToast("Connector encountered IOException");
                Log.e("ERROR", e.toString());
            } catch (SecurityException e) {
                showToast("Connector encountered SecurityException");
                Log.e("ERROR", e.toString());
            }
        }

        @Override
        public void run() {
            super.run();

            if (socket == null) {
                showToast("Socket was not created!");
                return;
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("BLUETOOTH_CONNECT missing!");
                return;
            }

            Message msg=Message.obtain();
            msg.what=STATE_CONNECTING;
            handler.sendMessage(msg);
            while(true) {
                try {
                    if(bluetoothAdapter.isDiscovering())
                        bluetoothAdapter.cancelDiscovery();
                    socket.connect();
                    break;
                } catch (IOException e) {
                    Log.e("TimeOut", e.toString());
                }
            }
            msg.what=STATE_CONNECTED;
            handler.sendMessage(msg);

            InputStream in=null;
            try {
                in=socket.getInputStream();
                out=socket.getOutputStream();
            } catch (IOException e) {
                showToast("Unable to get InputStream");
                return;
            }

            while(true){
                try {
                    Message dataReaderMsg=Message.obtain();
                    int data=in.read();
                    dataReaderMsg.what=STATE_MESSAGE_RECEIVED;
                    dataReaderMsg.arg1=data;
                    handler.sendMessage(dataReaderMsg);
                } catch (IOException e) {
                    showToast("Unable to read data from InputStream");
                }
            }

        }

        public void send(int data)
        {
            try {
                out.write(data);
                out.flush();
                showToast("Val:"+data);
            }catch (IOException e){
                showToast("Unable to send data");
            }
        }
        public void send(String data)
        {
            try {
                DataOutputStream dout=new DataOutputStream(out);
                dout.writeUTF(data);
                dout.flush();
                showToast("Val:"+data);
            }catch (IOException e){
                showToast("Unable to send data");
            }
        }
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
        reset=findViewById(R.id.reset);
        status=findViewById(R.id.status);
        message=findViewById(R.id.message);
        listView=findViewById(R.id.listView);
        send=findViewById(R.id.send);
        sendData=findViewById(R.id.sendData);
    }

    public String decode (String toEncode) {
        String morse = toEncode;

        if (toEncode.equalsIgnoreCase(".-"))
            morse = "a";
        else if (toEncode.equalsIgnoreCase("-..."))
            morse = "b";
        else if (toEncode.equalsIgnoreCase("-.-."))
            morse = "c";
        else if (toEncode.equalsIgnoreCase("-.."))
            morse = "d";
        else if (toEncode.equalsIgnoreCase("."))
            morse = "e";
        else if (toEncode.equalsIgnoreCase("..-."))
            morse = "f";
        else if (toEncode.equalsIgnoreCase("--."))
            morse = "g";
        else if (toEncode.equalsIgnoreCase("...."))
            morse = "h";
        else if (toEncode.equalsIgnoreCase(".."))
            morse = "i";
        else if (toEncode.equalsIgnoreCase(".---"))
            morse = "j";
        else if (toEncode.equalsIgnoreCase("-.-"))
            morse = "k";
        else if (toEncode.equalsIgnoreCase(".-.."))
            morse = "l";
        else if (toEncode.equalsIgnoreCase("--"))
            morse = "m";
        else if (toEncode.equalsIgnoreCase("-."))
            morse = "n";
        else if (toEncode.equalsIgnoreCase("---"))
            morse = "o";
        else if (toEncode.equalsIgnoreCase(".--."))
            morse = "p";
        else if (toEncode.equalsIgnoreCase("--.-"))
            morse = "q";
        else if (toEncode.equalsIgnoreCase(".-."))
            morse = "r";
        else if (toEncode.equalsIgnoreCase("..."))
            morse = "s";
        else if (toEncode.equalsIgnoreCase("-"))
            morse = "t";
        else if (toEncode.equalsIgnoreCase("..-"))
            morse = "u";
        else if (toEncode.equalsIgnoreCase("...-"))
            morse = "v";
        else if (toEncode.equalsIgnoreCase(".--"))
            morse = "w";
        else if (toEncode.equalsIgnoreCase("-..-"))
            morse = "x";
        else if (toEncode.equalsIgnoreCase("-.--"))
            morse = "y";
        else if (toEncode.equalsIgnoreCase("--.."))
            morse = "z";
        else if (toEncode.equalsIgnoreCase("-----"))
            morse = "0";
        else if (toEncode.equalsIgnoreCase(".----"))
            morse = "1";
        else if (toEncode.equalsIgnoreCase("..---"))
            morse = "2";
        else if (toEncode.equalsIgnoreCase("...--"))
            morse = "3";
        else if (toEncode.equalsIgnoreCase("....-"))
            morse = "4";
        else if (toEncode.equalsIgnoreCase("....."))
            morse = "5";
        else if (toEncode.equalsIgnoreCase("-...."))
            morse = "6";
        else if (toEncode.equalsIgnoreCase("--..."))
            morse = "7";
        else if (toEncode.equalsIgnoreCase("---.."))
            morse = "8";
        else if (toEncode.equalsIgnoreCase("----."))
            morse = "9";
        else if (toEncode.equalsIgnoreCase("......"))//To add space
            morse = " ";
        else if (toEncode.equalsIgnoreCase(".......")) {//To pop previous entry
            morse = "REDUCE";
        }
        else if(toEncode.equalsIgnoreCase("...---...")){//SOS
            morse="SOS";
        }
        else
            morse ="";
        return morse;
    }
}