package com.example.liuyo.myapplication;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class DemoActivity extends BaseActivity  {

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_demo);

    }


    public void doSomething() {
        Log.e("awqweqwd", "想做一些事情");
    }
}
