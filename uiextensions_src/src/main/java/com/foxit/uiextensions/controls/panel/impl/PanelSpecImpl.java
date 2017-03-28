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
package com.foxit.uiextensions.controls.panel.impl;

import android.view.View;

import com.foxit.uiextensions.controls.panel.PanelSpec;

public class PanelSpecImpl implements PanelSpec {

    private int mTag;
    private int mIcon;
    private View mTopToolbar;
    private View mContentView;

    public PanelSpecImpl(int icon, View topToolbar, View ContentView, int tag) {
        mIcon = icon;
        mTopToolbar = topToolbar;
        mContentView = ContentView;
        mTag = tag;
    }

    @Override
    public int getTag() {
        return mTag;
    }

    @Override
    public int getIcon() {
        return mIcon;
    }

    @Override
    public View getTopToolbar() {
        return mTopToolbar;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
    }

    @Override
    public void onDeactivated() {
    }

}
