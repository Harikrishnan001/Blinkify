package com.example.blinkify;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class CustomMorseCodeActivity extends AppCompatActivity {

    ListView listView;
    Button saveMorseCodes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_morse_code);

        listView=findViewById(R.id.morseItemListView);
        saveMorseCodes=findViewById(R.id.saveMorseCodes);

        FileHandler fileHandler=new FileHandler(this);
        ArrayList<MorseCodeDataItem> mappings=fileHandler.readMorseCodeMappingsFromFile();
        for(int i=0;i<10;i++)
            mappings.add(new MorseCodeDataItem("",""));
        MorseCodeDataItemAdapter adapter=new MorseCodeDataItemAdapter(mappings,this);
        listView.setDivider(null);
        listView.setAdapter(adapter);

        saveMorseCodes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileHandler.writeMorseCodeMappingsToFile(mappings);
            }
        });

    }
}