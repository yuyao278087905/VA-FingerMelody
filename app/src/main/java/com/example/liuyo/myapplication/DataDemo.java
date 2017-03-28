package com.example.liuyo.myapplication;

import android.content.Context;

/**
 * Created by FingerMeloddy on 17/3/28.
 */

public class DataDemo {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String name = "";

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context context;
}
