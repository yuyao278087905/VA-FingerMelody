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
package com.foxit.uiextensions.annots.caret;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.ToolUtil;

public class CaretModule implements Module, PropertyBar.PropertyChangeListener {

    private final Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;

    private CaretToolHandler mIS_ToolHandler;
    private CaretToolHandler mRP_ToolHandler;
    private CaretAnnotHandler mAnnotHandler;


    public CaretModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
    }

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public boolean loadModule() {
        mAnnotHandler = new CaretAnnotHandler(mContext, mParent, mPdfViewCtrl);
        mIS_ToolHandler = new CaretToolHandler(mContext, mParent, mPdfViewCtrl);
        mRP_ToolHandler = new CaretToolHandler(mContext, mParent, mPdfViewCtrl);
        mAnnotHandler.setPropertyChangeListener(this);
        mIS_ToolHandler.setPropertyChangeListener(this);
        mRP_ToolHandler.setPropertyChangeListener(this);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mParent));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));
        mIS_ToolHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));
        mRP_ToolHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));

        mIS_ToolHandler.init(true);
        mRP_ToolHandler.init(false);

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);


        return true;
    }


    @Override
    public boolean unloadModule() {
        mRP_ToolHandler.removePropertyBarListener();
        mIS_ToolHandler.removePropertyBarListener();
        mAnnotHandler.removePropertyBarListener();

        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_CARET;
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mIS_ToolHandler) {
                mIS_ToolHandler.changeCurrentColor(value);
            } else if (uiExtensionsManager.getCurrentToolHandler() == mRP_ToolHandler) {
                mRP_ToolHandler.changeCurrentColor(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            }
            if (mColorChangeListener != null) {
                mColorChangeListener.onColorChange(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mIS_ToolHandler) {
                mIS_ToolHandler.changeCurrentOpacity(value);
            } else if (uiExtensionsManager.getCurrentToolHandler() == mRP_ToolHandler) {
                mRP_ToolHandler.changeCurrentOpacity(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            }
        }
    }

    public ToolHandler getISToolHandler() {
        return mIS_ToolHandler;
    }

    public ToolHandler getRPToolHandler() {
        return mRP_ToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

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
