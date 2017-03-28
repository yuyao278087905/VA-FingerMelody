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

import android.content.Context;

public class AppResource {
    public static String getString(Context context, int id) {
        return context.getString(id);
    }

    public static int getDimensionPixelSize(Context context, int id) {
        return context.getResources().getDimensionPixelSize(id);
    }

    public static float getDimension(Context context, int id) {
        return context.getResources().getDimension(id);
    }
}