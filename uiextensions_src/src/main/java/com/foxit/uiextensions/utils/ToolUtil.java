/**
 * Copyright (C) 2003-2016, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.utils;

import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.UIExtensionsManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ToolUtil {
    public static AnnotHandler getCurrentAnnotHandler(UIExtensionsManager UIExtensionsManager) {
        Class<UIExtensionsManager> clazz = UIExtensionsManager.class;
        AnnotHandler annotHandler = null;
        try {
            Method method = clazz.getDeclaredMethod("getCurrentAnnotHandler");
            method.setAccessible(true);
            try {
                annotHandler = (AnnotHandler) method.invoke(UIExtensionsManager);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            method.setAccessible(false);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return annotHandler;
    }

    public static AnnotHandler getAnnotHandlerByType(UIExtensionsManager UIExtensionsManager, int type) {
        Class<UIExtensionsManager> clazz = UIExtensionsManager.class;
        AnnotHandler annotHandler = null;
        try {

            Method method = clazz.getDeclaredMethod("getAnnotHandlerByType", int.class);
            method.setAccessible(true);
            try {
                annotHandler = (AnnotHandler) method.invoke(UIExtensionsManager, type);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            method.setAccessible(false);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return annotHandler;
    }
}
