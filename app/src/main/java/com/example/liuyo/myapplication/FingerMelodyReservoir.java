package com.example.liuyo.myapplication;

import android.content.Context;

import java.lang.reflect.Method;

/**
 * Created by FingerMelody on 17/3/13.
 */

public class FingerMelodyReservoir<T> {


    public T getType() {
        return type;
    }

    public void setType(T type) {
        this.type = type;
    }

    T  type = null;
    public FingerMelodyReservoir() {

    }


}
