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
package com.foxit.uiextensions.annots.textmarkup.underline;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.ToolUtil;


public class UnderlineModule implements Module, PropertyBar.PropertyChangeListener {
    private UnderlineAnnotHandler mAnnotHandler;
    private UnderlineToolHandler mToolHandler;

    private int mCurrentColor;
    private int mCurrentOpacity;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewer;

    public UnderlineModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewer) {
        mContext = context;
        mParent = parent;
        mPdfViewer = pdfViewer;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_UNDERLINE;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public boolean loadModule() {
        mAnnotHandler = new UnderlineAnnotHandler(mContext, mPdfViewer, mParent);
        mToolHandler = new UnderlineToolHandler(mContext, mParent, mPdfViewer);

        mAnnotHandler.setToolHandler(mToolHandler);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mParent));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewer, mParent));
        mAnnotHandler.setPropertyChangeListener(this);
        mToolHandler.setPropertyChangeListener(this);

        mPdfViewer.registerRecoveryEventListener(memoryEventListener);
        mPdfViewer.registerDrawEventListener(mDrawEventListener);
        mCurrentColor = PropertyBar.PB_COLORS_UNDERLINE[0];//0xFF33CC00;

        mCurrentOpacity = 255;

        mToolHandler.setPaint(mCurrentColor, mCurrentOpacity);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewer.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewer.unregisterDrawEventListener(mDrawEventListener);
        mToolHandler.unInit();

        mAnnotHandler.removeProbarListener();
        mToolHandler.removeProbarListener();

        return true;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewer.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.setPaint(mCurrentColor, mCurrentOpacity);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotColor(value);
            }
            if (mColorChangeListener != null)
                mColorChangeListener.onColorChange(value);
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentOpacity = AppDmUtil.opacity100To255(value);
                mToolHandler.setPaint(mCurrentColor, mCurrentOpacity);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotOpacity(AppDmUtil.opacity100To255(value));
            }
        }
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
            if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
                mAnnotHandler.getAnnotMenu().dismiss();
            }

            if (mAnnotHandler.getPropertyBar() != null && mAnnotHandler.getPropertyBar().isShowing()) {
                mAnnotHandler.getPropertyBar().dismiss();
            }
        }

        @Override
        public void onRecovered() {
        }
    };
}