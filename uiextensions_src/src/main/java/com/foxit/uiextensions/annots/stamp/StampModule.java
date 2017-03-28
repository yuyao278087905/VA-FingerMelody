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
package com.foxit.uiextensions.annots.stamp;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;


public class StampModule implements Module, PropertyBar.PropertyChangeListener {

    private StampToolHandler mToolHandlerSTP;
    private StampAnnotHandler mAnnotHandlerSTP;
    private PropertyBar mPropertyBar;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;

    public StampModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_STAMP;
    }

    @Override
    public boolean loadModule() {
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl, mParent);
        mPropertyBar.setPropertyChangeListener(this);
        mToolHandlerSTP = new StampToolHandler(mContext, mPdfViewCtrl, mParent);
        mToolHandlerSTP.setPropertyBar(mPropertyBar);
        mAnnotHandlerSTP = new StampAnnotHandler(mContext, mPdfViewCtrl, mParent);
        mAnnotHandlerSTP.setAnnotMenu(new AnnotMenuImpl(mContext, mParent));
        mAnnotHandlerSTP.setToolHandler(mToolHandlerSTP);

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);

        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return false;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandlerSTP.onDrawForControls(canvas);
        }
    };

    public ToolHandler getToolHandler() {
        return mToolHandlerSTP;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandlerSTP;
    }

    @Override
    public void onValueChanged(long property, int value) {

    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            if (mAnnotHandlerSTP.getAnnotMenu() != null && mAnnotHandlerSTP.getAnnotMenu().isShowing()) {
                mAnnotHandlerSTP.getAnnotMenu().dismiss();
            }

            if (mToolHandlerSTP.getPropertyBar() != null && mToolHandlerSTP.getPropertyBar().isShowing()) {
                mToolHandlerSTP.getPropertyBar().dismiss();
            }
        }

        @Override
        public void onRecovered() {
        }
    };
}
