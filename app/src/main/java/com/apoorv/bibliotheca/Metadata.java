package com.apoorv.bibliotheca;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

// Это содержание
public class Metadata extends MainView {

    private EpubManipulator[] a_books;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);
        navigator = new EpubNavigator(2, this);

        panelCount = 0;
        settings = new String[8];

        // LOADSTATE
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        loadState(preferences);
        navigator.loadViews(preferences);

        if (panelCount == 0) {
            bookSelector = 0;
            Intent goToChooser = new Intent(this, FileChooser.class);
            startActivityForResult(goToChooser, 0);
        }

navigator.displayMetadata(0);

//        EpubNavigator o_ebupNav = new EpubNavigator();
//
//        boolean res = true;
//
//        if (a_books[2] != null) {
//            DataView dv = new DataView();
//            dv.loadData(a_books[2].metadata());
//            o_ebupNav.changePanel(dv, 2);
//        } else
//            res = false;
    }
}
