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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileHandler {
    final private Activity activity;
    private static final String FILE_NAME_WORD_FREQUENCY="corpus.txt";
    private static final String FILE_NAME_MORSE_CODE_MAP="mappings.txt";
    public static final String[] englishValues=new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
    "1","2","3","4","5","6","7","8","9","0"," ","BACKSPACE","BEEP","HUNGRY","WATER","FEELING ILL"};
    public static final String[] morseCodeValues=new String[]{".-","-...","-.-.","-..",".","..-.","--.","....","..",".---",
            "-.-",".-..","--","-.","---",".--.","--.-",".-.","...","-", "..-","...-",".--","-..-","-.--","--..",
            ".----","..---","...--","....-",".....","-....","--...", "---..","----.","-----","......",".......",".....-","........",".........",".........."};
    public FileHandler( Activity activity){
        this.activity=activity;
    }
    public void writeWordFrequencyToFile(HashMap<String,Integer> map){
        try {
            FileOutputStream fos = activity.openFileOutput(FILE_NAME_WORD_FREQUENCY, MODE_PRIVATE);
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

    public void writeMorseCodeMappingsToFile(ArrayList<MorseCodeDataItem> mappings){
        try {
            FileOutputStream fos = activity.openFileOutput(FILE_NAME_MORSE_CODE_MAP, MODE_PRIVATE);
            for (MorseCodeDataItem item:mappings) {
                if(!item.englishValue.equals("")) {
                    String k = item.englishValue;
                    String v = item.morseCodeValue;
                    String s = k + "," + v.toString() + "\n";
                    fos.write(s.getBytes());
                }
            }
            fos.close();
        }catch (Exception e){
            showToast("Unable to write to file");
        }
    }

    public HashMap<String,Integer> readWordFrequencyFromFile(){
        HashMap<String,Integer> map=new HashMap<>();
        String[] results;
        while(true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(activity.openFileInput(FILE_NAME_WORD_FREQUENCY)));
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
                String words[]=new String[]{"hello","hai","water","food","lemon","less"};
                int freq[]=new int[]{5,7,3,6,8,5};
                HashMap<String,Integer> list=new HashMap<>();
                for(int i=0;i<words.length;i++)
                    list.put(words[i],freq[i]);
                writeWordFrequencyToFile(list);
            } catch (IOException e) {
                showToast("Unable to read from file");
                break;
            }
        }
        return map;
    }

    public ArrayList<MorseCodeDataItem> readMorseCodeMappingsFromFile(){
        ArrayList<MorseCodeDataItem> mappings=new ArrayList<>();
        String[] results;
        while(true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(activity.openFileInput(FILE_NAME_MORSE_CODE_MAP)));
                String line = reader.readLine();
                while (line != null) {
                    results = line.split(",");
                    mappings.add(new MorseCodeDataItem(results[0],results[1]));
                    Log.e("Message", results[0] + ":" + results[1]);
                    line=reader.readLine();
                }
                reader.close();
                break;
            } catch (FileNotFoundException e) {
                showToast("Creating and writing to file...");
                try {
                    FileOutputStream fout = activity.openFileOutput(FILE_NAME_MORSE_CODE_MAP, MODE_PRIVATE);
                    for(int i=0;i<englishValues.length;i++)
                        fout.write((englishValues[i]+","+morseCodeValues[i]+"\n").getBytes());
                    fout.close();
                }catch (IOException ioe){
                    showToast("Unable to create file");
                }
            } catch (IOException e) {
                showToast("Unable to read from file");
                break;
            }
        }
        return mappings;
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
