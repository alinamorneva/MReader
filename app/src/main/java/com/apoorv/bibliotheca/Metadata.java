package com.apoorv.bibliotheca;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

// Это содержание
public class Metadata extends AppCompatActivity {
    protected Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);

        navigator = new Navigator(2, this);
        navigator.displayMetadata(0);
    }
}