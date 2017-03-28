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

import android.graphics.Typeface;
import android.view.View;

public interface BaseItem {
    /**
     * text_on_left
     */
    public static final int RELATION_LEFT = 10;
    /**
     * text_on_top
     */
    public static final int RELATION_TOP = 11;
    /**
     * text_on_right
     */
    public static final int RELATION_RIGNT = 12;
    /**
     * text_on_below
     */
    public static final int RELATION_BELOW = 13;

    public View getContentView();

    public void setText(String text);

    public String getText();

    public void setTextColor(int selectedTextColor, int disSelectedTextColor);

    public void setTextColor(int color);

    public void setTypeface(Typeface typeface);

    public void setTextSize(float size);

    public void setText(int res);

    public void setTextColorResource(int res);

    public boolean setImageResource(int res);

    public void setContentView(View view);

    public void setBackgroundResource(int res);

    public void setRelation(int relation);

    public void setEnable(boolean enable);

    public void setSelected(boolean selected);

    public void setOnClickListener(View.OnClickListener l);

    public void setOnLongClickListener(View.OnLongClickListener l);

    public void setTag(int tag);

    public int getTag();

    public void setId(int id);

    public int getId();

    public void setInterval(int interval);

    public void setDisplayStyle(ItemType type);

    enum ItemType {
        Item_Text,
        Item_Image,
        Item_Text_Image,
        Item_custom;
    }

    public void onItemLayout(int l, int t, int r, int b);
}
