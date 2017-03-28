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
package com.foxit.uiextensions.annots.note;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

public class NoteModule implements Module, PropertyBar.PropertyChangeListener {
    private Context mContext;
    private AppDisplay mDisplay;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParentView;
    private NoteAnnotHandler mAnnotHandler;
    private NoteToolHandler mToolHandler;
    private UIExtensionsManager mUIExtensionsManager;

    private int mCurrentColor;
    private int mCurrentOpacity;
    private String mCurrentIconType;

    private ArrayList<BitmapDrawable> mBitmapDrawables;

    private Paint mPaint;

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    public NoteModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mDisplay = new AppDisplay(context);
        mPdfViewCtrl = pdfViewCtrl;
        mParentView = parent;
    }

    @Override
    public String getName() {
        return MODULE_NAME_NOTE;
    }

    @Override
    public boolean loadModule() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setDither(true);

        mAnnotHandler = new NoteAnnotHandler(mContext, mParentView, mPdfViewCtrl, this);
        mToolHandler = new NoteToolHandler(mContext, mPdfViewCtrl);
        mAnnotHandler.setToolHandler(mToolHandler);
        initVariable();

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        initViewBuilding();

        return true;
    }

    private void initVariable() {
        mCurrentColor = Color.argb(255, 255, 159, 64);
        mCurrentOpacity = 100;
        ;
        mCurrentIconType = "Comment";

        mToolHandler.setColor(mCurrentColor);
        mToolHandler.setOpacity(mCurrentOpacity);
        mToolHandler.setIconType(mCurrentIconType);

        Rect rect = new Rect(0, 0, dp2px(32), dp2px(32));
        mBitmapDrawables = new ArrayList<BitmapDrawable>();
        for (int i = 1; i < NoteConstants.TA_ICON_COUNT + 1; i++) {
            Bitmap mBitmap = Bitmap.createBitmap(dp2px(32), dp2px(32), Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            @SuppressWarnings("deprecation")
            BitmapDrawable bd = new BitmapDrawable(mBitmap);
            mPaint.setStyle(Style.FILL);
            mPaint.setColor(Color.YELLOW);
            String iconName = NoteUtil.getIconNameByType(i);
            canvas.drawPath(NoteUtil.GetPathStringByType(iconName, AppDmUtil.rectToRectF(rect)), mPaint);
            mPaint.setStyle(Style.STROKE);
            mPaint.setStrokeWidth(dp2px(1));
            mPaint.setARGB(255, (int) (255 * 0.36f), (int) (255 * 0.36f), (int) (255 * 0.64f));
            canvas.drawPath(NoteUtil.GetPathStringByType(iconName, AppDmUtil.rectToRectF(rect)), mPaint);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
            mBitmapDrawables.add(bd);
        }

    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    private void initViewBuilding() {
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private int mAnnotColor;
    private int mAnnotOpacity;
    private String mAnnotIconType;

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public void onValueChanged(long property, int value) {
        mUIExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(mUIExtensionsManager);
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (mUIExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.setColor(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
                mAnnotColor = value;
            }
            if (mColorChangeListener != null)
                mColorChangeListener.onColorChange(mCurrentColor);
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (mUIExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.setOpacity(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
                mAnnotOpacity = value;
            }

        } else if (property == PropertyBar.PROPERTY_ANNOT_TYPE) {
            String iconName = PropertyBar.ICONNAMES[value - 1];
            if (mUIExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentIconType = iconName;
                mToolHandler.setIconType(iconName);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onIconTypeChanged(iconName);
                mAnnotIconType = iconName;
            }
        }

    }

    @Override
    public void onValueChanged(long property, float value) {
    }

    @Override
    public void onValueChanged(long property, String iconName) {
    }

    private int dp2px(int dip) {
        return mDisplay.dp2px(dip);
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
