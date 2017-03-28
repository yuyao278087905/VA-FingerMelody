package com.example.liuyo.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by FingerMelody on 17/3/8.
 */

public class BaseActivity extends Activity {
    public final static int LEFTTYPE_IMG = 0;
    public final static int LEFTTYPE_STR = 1;
    public final static boolean LEFTON = true;
    public final static boolean LEFTOFF = false;
    public final static int RIGHTTYPE_IMG = 0;
    public final static int RIGHTTYPE_STR = 1;
    public final static boolean RIGHTON = true;
    public final static boolean RIGHTOFF = false;
    private LinearLayout topLayout = null;

    protected void onCreate(Bundle savedInstanceState, int viewId) {
        super.onCreate(savedInstanceState);
        setContentView(viewId);
        initView();
    }

    private void initView() {

    }

    public void setTop(int leftType, boolean leftOnOrOff, String leftStr, int leftImgId, String titleStr, int rightType, boolean rightOnOrOff, String rightStr, int rightImgId, View.OnClickListener leftOnc, View.OnClickListener rightOnc) {
        TextView leftTv = (TextView) findViewById(R.id.tv_title_left);
        TextView rightTv = (TextView) findViewById(R.id.tv_title_right);
        TextView title = (TextView) findViewById(R.id.tv_title);
        ImageView leftImg = (ImageView) findViewById(R.id.iv_title_left);
        ImageView rightImg = (ImageView) findViewById(R.id.iv_title_right);
        title.setText(titleStr);
        if (leftType == LEFTTYPE_IMG && leftOnOrOff) {
            leftTv.setVisibility(View.GONE);
            leftImg.setOnClickListener(leftOnc);
            try {
                leftImg.setImageResource(leftImgId);
            } catch (Exception e) {

            }
        } else {
            leftImg.setVisibility(View.GONE);
            leftTv.setVisibility(View.VISIBLE);
        }
        if (leftType == LEFTTYPE_STR && leftOnOrOff) {
            leftImg.setVisibility(View.GONE);
            leftTv.setText(leftStr);
            leftTv.setOnClickListener(leftOnc);
        } else {
            leftImg.setVisibility(View.VISIBLE);
            leftTv.setVisibility(View.GONE);
        }

        if (rightType == RIGHTTYPE_IMG && rightOnOrOff) {
            rightTv.setVisibility(View.GONE);
            rightImg.setOnClickListener(rightOnc);
            try {
                rightImg.setImageResource(rightImgId);
            } catch (Exception e) {

            }
        } else {
            rightImg.setVisibility(View.GONE);
            rightTv.setVisibility(View.VISIBLE);
        }
        if (rightType == RIGHTTYPE_STR && rightOnOrOff) {
            rightImg.setVisibility(View.GONE);
            rightTv.setText(rightStr);
            rightTv.setOnClickListener(rightOnc);
        } else {
            rightImg.setVisibility(View.VISIBLE);
            rightTv.setVisibility(View.GONE);
        }


    }


    public void toActivity(Activity activity, Class<?> activityClass) {
        startActivity(new Intent(activity, activityClass));
    }


}
