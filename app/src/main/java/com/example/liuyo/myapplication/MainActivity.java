package com.example.liuyo.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    @SuppressLint("MissingSuperCall")
    private FingerMelodyProxy fingerMelodyProxy = null;
    private LinearLayout linearLayout = null;
    private ImageView imageView;
    private  DataDemo dataDemo;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_main);
        setTop(LEFTTYPE_IMG, LEFTON, "", R.mipmap.ic_launcher, "搭建成功", RIGHTTYPE_STR, RIGHTON, "我是右侧", 0, null, this);
        fingerMelodyProxy = new FingerMelodyProxy(Demo.class);
        dataDemo=new DataDemo();
        dataDemo.setContext(this);
        dataDemo.setName("FingerMelody");
        init();
    }

    private void init() {
        imageView = (ImageView) findViewById(R.id.name);
        imageView.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_title_right:
                toActivity(this, DemoActivity.class);
                break;
            case R.id.name:
                fingerMelodyProxy.go("getName", dataDemo);
                break;
        }
    }


}
