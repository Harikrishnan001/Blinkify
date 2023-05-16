package com.example.blinkify;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spanned;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BluetoothDeviceActivity extends AppCompatActivity {

    TextView  deviceName,message;
    EditText resetDuration, blinkDelay,normalBlinkDuration;
    Button setBlinkDelay, setResetDuration, clear,disconnect,addCustom,setNormalBlinkDuration;
    int count = 0;
    String receivedData = "";
    String englishContent = "";
    Connector connector;
    BluetoothDevice bluetoothDevice;
    HashMap<String,Integer> wordFrequencies;
    HashMap<String,String> characterMapping;

    FileHandler fileHandler;
    WordCompletion wordPredictor;
    ActivityResultLauncher<Intent> customMorseCodeActivityLauncher;
    ArrayList<MorseCodeDataItem> morseCodeMappings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);

        bluetoothDevice = getIntent().getParcelableExtra("device");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            this.finish();
            return;
        }

        findViews();
        registerListeners();

        customMorseCodeActivityLauncher=getCustomMorseCodeActivityLauncher();
        //message.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onStart() {
        super.onStart();

        fileHandler=new FileHandler(this);
        morseCodeMappings=fileHandler.readMorseCodeMappingsFromFile();
        connector=new Connector(bluetoothDevice,this,handler);
        connector.start();
        wordFrequencies=fileHandler.readWordFrequencyFromFile();
        wordPredictor=new WordCompletion(wordFrequencies);
        blinkDelay.setText(String.valueOf(getPreferences(MODE_PRIVATE).getInt("blinkDelay",600)));
        resetDuration.setText(String.valueOf(getPreferences(MODE_PRIVATE).getInt("resetDuration",2000)));
        normalBlinkDuration.setText(String.valueOf(getPreferences(MODE_PRIVATE).getInt("normalBlinkDuration",100)));
    }

    private void findViews() {
        deviceName = findViewById(R.id.deviceName);
        blinkDelay = findViewById(R.id.blinkDelay);
        resetDuration = findViewById(R.id.resetDuration);
        setBlinkDelay = findViewById(R.id.setBlinkDelay);
        setResetDuration = findViewById(R.id.setResetDuration);
        clear = findViewById(R.id.clear);
        disconnect=findViewById(R.id.disconnect);
        message=findViewById(R.id.message);
        addCustom=findViewById(R.id.addCustom);
        normalBlinkDuration=findViewById(R.id.normalBlinkDuration);
        setNormalBlinkDuration=findViewById(R.id.setNormalBlinkDuration);
    }

    private void registerListeners() {
        setBlinkDelay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.send("0:"+blinkDelay.getText().toString());
                getPreferences(MODE_PRIVATE).edit().putInt("blinkDelay",Integer.parseInt(blinkDelay.getText().toString()));
            }
        });
        setResetDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.send("1:"+resetDuration.getText().toString());
                getPreferences(MODE_PRIVATE).edit().putInt("resetDuration",Integer.parseInt(resetDuration.getText().toString()));
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                BluetoothDeviceActivity.this.setResult(Activity.RESULT_OK);
                BluetoothDeviceActivity.this.finish();
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count=0;
                receivedData="";
                englishContent="";
                message.setText("");
            }
        });
        message.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(BluetoothDeviceActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    englishContent=englishContent+getPredictedSuffix();
                    message.setText(englishContent);
                    connector.send("2:0");
                    return super.onDoubleTap(e);
                }
            });
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
        addCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(BluetoothDeviceActivity.this,CustomMorseCodeActivity.class);
                customMorseCodeActivityLauncher.launch(i);
            }
        });
        setNormalBlinkDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.send("3:"+normalBlinkDuration.getText().toString());
                getPreferences(MODE_PRIVATE).edit().putInt("normalBlinkDuration",Integer.parseInt(resetDuration.getText().toString()));
            }
        });
    }

    private ActivityResultLauncher<Intent> getCustomMorseCodeActivityLauncher(){
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()== Activity.RESULT_OK){
                    showToast("Success");
                }else{
                    showToast("Operation failed");
                }
            }
        });
    }

    public void processData() {
        String s = decode(receivedData);
        if (s.equals("BACKSPACE"))
            englishContent = englishContent.substring(0, englishContent.length() - 1);
        else if(s.equals("BEEP"))
            connector.send("2:0");
        else
            englishContent += s;
        receivedData = "";
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Connector.STATE_CONNECTING:
                    deviceName.setText("CONNECTING...");
                    break;
                case Connector.STATE_CONNECTED:
                    try {
                        deviceName.setText(bluetoothDevice.getName());
                        showToast("Connected");
                    }catch (SecurityException e){
                        deviceName.setText("Error!");
                    }
                    break;
                case Connector.STATE_MESSAGE_RECEIVED:
                    count++;
                    int data=msg.arg1;
                    if(data==0)
                        receivedData+=".";
                    else if(data==1)
                        receivedData+="-";
                    else {
                        processData();
                    }
                    if(receivedData.length()>0)
                        message.setText(englishContent+receivedData);
                    else
                        message.setText(getFormattedText());
                    break;
            }
            return true;
        }
    });

    private void showToast(String text){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothDeviceActivity.this,text,Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String decode (String toEncode) {
        String morse = toEncode;
        boolean foundInMap=false;

        for(MorseCodeDataItem dataItem:morseCodeMappings)
            if(toEncode.equalsIgnoreCase(dataItem.morseCodeValue)) {
                morse = dataItem.englishValue;
                foundInMap=true;
            }
        if(!foundInMap)
            morse="";
        return morse;
    }

    public String getPredictedSuffix(){
        String[] words=englishContent.split("\\s+");
        String prefix=words[words.length-1];
        String suffix = "";
        if(prefix.length()>0) {
            List<String> predictions = wordPredictor.completeWord(prefix, 1);
            if (predictions.size() >= 1)
                suffix = predictions.get(0).substring(prefix.length());
        }
        return suffix;
    }

    public Spanned getFormattedText(){
        String suffix=getPredictedSuffix();
        showToast("EnglishContent:"+englishContent);
        showToast("Suffix:"+suffix);
        return HtmlCompat.fromHtml("<font color=#000000>"+englishContent+"</font>"+"<font color=#C0C0C0>"+suffix+"</font>",HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

}