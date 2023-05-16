package com.example.blinkify;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MorseCodeDataItemAdapter extends ArrayAdapter<MorseCodeDataItem> {
    private final ArrayList<MorseCodeDataItem> mappings;
    private final Context context;
    public MorseCodeDataItemAdapter(ArrayList<MorseCodeDataItem> morseCodeMappings,@NonNull Context context) {
        super(context, R.layout.morse_value_list_item,morseCodeMappings);

        this.context=context;
        this.mappings=morseCodeMappings;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        MorseCodeDataItem dataItem=getItem(position);
        ViewHolder viewHolder;

        if(convertView==null){
            LayoutInflater inflater=LayoutInflater.from(getContext());
            convertView=inflater.inflate(R.layout.morse_value_list_item,parent,false);
            viewHolder=new ViewHolder((EditText) convertView.findViewById(R.id.englishTextValue),(EditText) convertView.findViewById(R.id.morseCodeValue));
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder=(ViewHolder) convertView.getTag();
        }
        if(dataItem.englishValue.equals(" "))
            viewHolder.englishValue.setText("BLANK SPACE");
        else
            viewHolder.englishValue.setText(dataItem.englishValue);
        viewHolder.morseCodeValue.setText(dataItem.morseCodeValue);


        return convertView;
    }

    public static class ViewHolder{
        EditText englishValue;
        EditText morseCodeValue;

        public ViewHolder(EditText englishValue,EditText morseCodeValue){
            this.englishValue=englishValue;
            this.morseCodeValue=morseCodeValue;
        }
    }
}
