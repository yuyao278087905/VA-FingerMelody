package com.example.liuyo.myapplication;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by FingerMelody on 17/3/10.
 */

public class Demo extends FingerMelodyRealSubject implements View.OnClickListener {


    private void getName(FingerMelodyReservoir<DataDemo> data) {
        Toast.makeText(data.getType().getContext(), data.getType().getName(), Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onClick(View v) {

    }

    public void doThings() {
        Log.e("nmnmnmnmn", "我已经变化了");
    }
}
