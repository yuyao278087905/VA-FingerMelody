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
package com.foxit.uiextensions.controls.toolbar;

import android.view.View;

public interface BaseBar {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public enum TB_Position {
        Position_LT,
        Position_CENTER,
        Position_RB;
    }

    public void addView(BaseItem item, TB_Position position);

    public boolean removeItemByTag(int tag);

    public boolean removeItemByItem(BaseItem item);

    public void removeAllItems();

    public void setName(String name);

    public String getName();

    public void setBarVisible(boolean visible);

    public View getContentView();

    public void setOrientation(int orientation);

    public void setBackgroundColor(int color);

    public void setBackgroundResource(int res);

    public void setInterval(boolean interval);

    /**
     * must use it after setOrientation(int orientation)
     */
    public void setItemSpace(int space);

    public void setWidth(int width);

    public void setHeight(int height);

    public void setContentView(View v);

    public void setInterceptTouch(boolean isInterceptTouch);

    public void setNeedResetItemSize(boolean needResetItemSize);
}
