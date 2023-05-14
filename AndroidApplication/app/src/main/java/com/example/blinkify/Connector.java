package com.example.blinkify;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class Connector extends Thread {
    final BluetoothDevice device;
    BluetoothSocket socket;
    OutputStream out;
    InputStream in;
    Activity activity;
    Handler handler;
    final static int STATE_CONNECTING=2;
    final static int STATE_CONNECTED=3;
    final static int STATE_MESSAGE_RECEIVED=4;
    final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public Connector(BluetoothDevice device, Activity activity, Handler handler) {
        this.device = device;
        this.activity=activity;
        this.handler=handler;

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
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if (socket == null) {
            showToast("Socket was not created!");
            return;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

        in=null;
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

    private void showToast(String text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();
                Log.e("Message",text);
            }
        });
    }

    public void disconnect(){
        try {
            out.flush();
            out.close();
            in.close();
            socket.close();
        } catch (IOException e){

        }
    }
}
