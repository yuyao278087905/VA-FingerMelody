package com.example.liuyo.myapplication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by FingerMelody on 17/3/7.
 */

public class FingerMelodyRealSubject implements FingerMelodySubjectInface {


    @Override
    public void doSomeThing(Object subject, String methodName, Object typeVal) {
        if (methodName != null && !methodName.equals("")) {
            Class clazz = subject.getClass();
            try {
                Method m1 = clazz.getDeclaredMethod(methodName, new Class[]{FingerMelodyReservoir.class});
                //2.取消访问检查，是访问私有方法的关键
                m1.setAccessible(true);
                FingerMelodyReservoir fingerMelodyReservoir = new FingerMelodyReservoir();
                fingerMelodyReservoir.setType(typeVal);
                m1.invoke(subject, new Object[]{fingerMelodyReservoir});
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }
}
