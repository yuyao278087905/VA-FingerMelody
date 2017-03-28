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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.utils.ToolUtil;


public class TypewriterModule implements Module, PropertyBar.PropertyChangeListener {

    private TypewriterToolHandler mToolHandler;
    private TypewriterAnnotHandler mAnnotHandler;


    private int mCurrentColor;
    private int mCurrentOpacity;
    private String mCurrentFontName;
    private float mCurrentFontSize;

    private BaseItem mToolBtn;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;

    public TypewriterModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
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

    @Override
    public String getName() {
        return Module.MODULE_NAME_TYPEWRITER;
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new TypewriterToolHandler(mContext, mPdfViewCtrl, mParent);
        mAnnotHandler = new TypewriterAnnotHandler(mContext, mPdfViewCtrl, mParent);

        mAnnotHandler.setPropertyChangeListener(this);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mParent));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        initUI();

        return true;
    }

    @Override
    public boolean unloadModule() {
        mAnnotHandler.removePropertyBarListener();

        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        return true;
    }


    private void initUI() {
        initCurrentValue();
        initToolBtn();
    }

    private void initCurrentValue() {
        if (mCurrentColor == 0) mCurrentColor = PropertyBar.PB_COLORS_TYPEWRITER[0];
        if (mCurrentOpacity == 0) mCurrentOpacity = 100;
        if (mCurrentFontName == null) mCurrentFontName = "Courier";
        if (mCurrentFontSize == 0) mCurrentFontSize = 24;

        mToolHandler.onColorValueChanged(mCurrentColor);
        mToolHandler.onOpacityValueChanged(mCurrentOpacity);
        mToolHandler.onFontValueChanged(mCurrentFontName);
        mToolHandler.onFontSizeValueChanged(mCurrentFontSize);
    }

    private void initToolBtn() {
        mToolBtn = new CircleItemImpl(mContext);
        mToolBtn.setImageResource(R.drawable.annot_typewriter_selector);
        mToolBtn.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_TYPEWRITE);

        if (mPdfViewCtrl != null && !DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mToolBtn.setEnable(false);
        }
        mToolBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                    return;
                }
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != mToolHandler) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(mToolHandler);
                    mToolBtn.setSelected(true);
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    mToolBtn.setSelected(false);
                }
            }
        });
        mToolBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                    return true;
                }
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != mToolHandler) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(mToolHandler);
                    mToolBtn.setSelected(true);
                }
                return true;
            }
        });
    }

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.onColorValueChanged(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            }

            if (mColorChangeListener != null) {
                mColorChangeListener.onColorChange(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.onOpacityValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_FONTSIZE) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentFontSize = value;
                mToolHandler.onFontSizeValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onFontSizeValueChanged(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, String value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_FONTNAME) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentFontName = value;
                mToolHandler.onFontValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onFontValueChanged(value);
            }
        }
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int index) {

        }

        @Override
        public void onPageInvisible(int index) {

        }

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
            if (uiExtensionsManager.getCurrentToolHandler() != null && uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                if (mToolHandler.mLastPageIndex != -1 && mToolHandler.mLastPageIndex != curPageIndex) {
                    uiExtensionsManager.setCurrentToolHandler(null);
                }
            }
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null
                    && currentAnnotHandler == mAnnotHandler) {
                try {
                    if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot().getPage().getIndex() != curPageIndex) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onPageJumped() {

        }
    };
}
