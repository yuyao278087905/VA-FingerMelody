package com.example.liuyo.myapplication;

/**
 * Created by FingerMelody on 17/3/7.
 */

public class FingerMelodyProxy<T> {
    private T subject = null;
    private FingerMelodyRealSubject fingerMelodyRealSubject = null;
    private FingerMelodyProxy fingerMelodyProxy = null;

    public FingerMelodyProxy() {

    }

    public FingerMelodyProxy(Class<T> subject) {

        try {
            this.subject = subject.newInstance();
            fingerMelodyRealSubject = (FingerMelodyRealSubject) this.subject;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public void go(String name, Object object) {

        fingerMelodyRealSubject.doSomeThing(subject, name,object);
    }

}
