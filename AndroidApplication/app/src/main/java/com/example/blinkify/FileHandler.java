package com.example.blinkify;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class FileHandler {
    final private String fileName;
    final private Activity activity;
    public FileHandler(String fileName, Activity activity){
        this.fileName=fileName;
        this.activity=activity;
    }
    public void writeToFile(HashMap<String,Integer> map){
        try {
            FileOutputStream fos = activity.openFileOutput(fileName, MODE_PRIVATE);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                String k = entry.getKey();
                Integer v = entry.getValue();
                String s = k + "," + v.toString()+"\n";
                fos.write(s.getBytes());
            }
            fos.close();
        }catch (Exception e){
            showToast("Unable to write to file");
        }
    }

    public HashMap<String,Integer> readFromFile(){
        HashMap<String,Integer> map=new HashMap<>();
        String[] results;
        while(true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(activity.openFileInput(fileName)));
                String line = reader.readLine();
                while (line != null) {
                    results = line.split(",");
                    map.put(results[0], Integer.parseInt(results[1]));
                    Log.e("Message", results[0] + ":" + results[1]);
                    line=reader.readLine();
                }
                reader.close();
                break;
            } catch (FileNotFoundException e) {
                showToast("Creating and writing to file...");
                try {
                    FileOutputStream fout = activity.openFileOutput(fileName, MODE_PRIVATE);
                    fout.close();
                }catch (IOException ioe){
                    showToast("Unable to create file");
                }
            } catch (IOException e) {
                showToast("Unable to read from file");
                break;
            }
        }
        return map;
    }

    private void showToast(String text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
