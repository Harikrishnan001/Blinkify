package com.example.blinkify;

public class MorseCodeDataItem {
    public String englishValue;
    public String morseCodeValue;

    public MorseCodeDataItem(String text,String code){
        this.englishValue=text;
        this.morseCodeValue=code;
    }
}
