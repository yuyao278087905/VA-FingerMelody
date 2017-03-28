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
package com.foxit.uiextensions.annots.square;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Square;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

public class SquareToolHandler implements ToolHandler {

    private Context mContext;
    private ViewGroup mParent;
    private int mColor;
    private int mOpacity;
    private float mThickness;
    private int mControlPtEx = 5;// Refresh the scope expansion width
    static private float mCtlPtLineWidth = 2;
    static private float mCtlPtRadius = 5;

    private boolean mTouchCaptured = false;
    private int mLastPageIndex = -1;

    private PointF mStartPoint = new PointF(0, 0);
    private PointF mStopPoint = new PointF(0, 0);
    private PointF mDownPoint = new PointF(0, 0);// whether moving point

    private RectF mNowRect = new RectF(0, 0, 0, 0);
    /**
     * one of mCreateList to draw the Square
     */
    private Paint mPaint;
    private Paint mLastAnnotPaint;

    /**
     * toolbar
     */
    private PropertyBar mPropertyBar;

    private BaseItem mToolBtn;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;


    private PDFViewCtrl mPdfViewCtrl;
    public SquareToolHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mParent = parent;
        mControlPtEx = AppDisplay.getInstance(context).dp2px(mControlPtEx);

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mLastAnnotPaint = new Paint();
        mLastAnnotPaint.setStyle(Style.STROKE);
        mLastAnnotPaint.setAntiAlias(true);
        mLastAnnotPaint.setDither(true);
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void setPaint(int pageIndex) {
        mPaint.setColor(mColor);
        mPaint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
        mPaint.setAntiAlias(true);
        PointF tranPt = new PointF(thicknessOnPageView(pageIndex, mThickness), thicknessOnPageView(pageIndex, mThickness));
        mPaint.setStrokeWidth(tranPt.x);
    }

    private RectF mPageViewThickness = new RectF(0, 0, 0, 0);

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mPageViewThickness.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewThickness, mPageViewThickness, pageIndex);
        return Math.abs(mPageViewThickness.width());
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    public float getLineWidth() {
        return mThickness;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_SQUARE;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mCtlPtRadius = 5;
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
    }

    @Override
    public void onDeactivate() {
    }

    private Rect mTempRectInTouch = new Rect(0, 0, 0, 0);
    private Rect mInvalidateRect = new Rect(0, 0, 0, 0);

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        PointF disPoint = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pvPoint = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(disPoint, pvPoint, pageIndex);
        float x = pvPoint.x;
        float y = pvPoint.y;

        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured && mLastPageIndex == -1 || mLastPageIndex == pageIndex) {
                    mTouchCaptured = true;
                    mStartPoint.x = x;
                    mStartPoint.y = y;
                    mStopPoint.x = x;
                    mStopPoint.y = y;
                    mDownPoint.set(x, y);
                    mTempRectInTouch.setEmpty();
                    if (mLastPageIndex == -1) {
                        mLastPageIndex = pageIndex;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mTouchCaptured || mLastPageIndex != pageIndex)
                    break;
                if (!mDownPoint.equals(x, y)) {
                    mStopPoint.x = x;
                    mStopPoint.y = y;
                    float thickness = thicknessOnPageView(pageIndex, mThickness);
                    float deltaXY = thickness / 2 + mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                    float line_k = (y - mStartPoint.y) / (x - mStartPoint.x);
                    float line_b = mStartPoint.y - line_k * mStartPoint.x;
                    if (y <= deltaXY && line_k != 0) {
                        // whether created annot beyond a PDF page(pageView)
                        mStopPoint.y = deltaXY;
                        mStopPoint.x = (mStopPoint.y - line_b) / line_k;
                    } else if (y >= (mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) && line_k != 0) {
                        mStopPoint.y = (mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY);
                        mStopPoint.x = (mStopPoint.y - line_b) / line_k;
                    }
                    if (mStopPoint.x <= deltaXY) {
                        mStopPoint.x = deltaXY;
                    } else if (mStopPoint.x >= mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                        mStopPoint.x = mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY;
                    }

                    getDrawRect(mStartPoint.x, mStartPoint.y, mStopPoint.x, mStopPoint.y);

                    mInvalidateRect.set((int) mNowRect.left, (int) mNowRect.top, (int) mNowRect.right, (int) mNowRect.bottom);
                    mInvalidateRect.inset((int) (-mThickness * 12f - mControlPtEx), (int) (-mThickness * 12f - mControlPtEx));
                    if (!mTempRectInTouch.isEmpty()) {
                        mInvalidateRect.union(mTempRectInTouch);
                    }
                    mTempRectInTouch.set(mInvalidateRect);
                    RectF _rect = AppDmUtil.rectToRectF(mInvalidateRect);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(_rect, _rect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(  _rect));
                    mDownPoint.set(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTouchCaptured || mLastPageIndex != pageIndex)
                    break;
                if (!mStartPoint.equals(mStopPoint.x, mStopPoint.y)) {
                    createAnnot();
                } else {
                    mStartPoint.set(0, 0);
                    mStopPoint.set(0, 0);
                    mNowRect.setEmpty();
                    mDownPoint.set(0, 0);

                    mTouchCaptured = false;
                    mLastPageIndex = -1;
                    mDownPoint.set(0, 0);
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        mToolBtn.setSelected(false);
                    }
                }
                return true;
            default:
                return true;
        }

        return true;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mStartPoint == null || mStopPoint == null) {
            return;
        }
        if (mLastPageIndex == pageIndex) {
            canvas.save();
            setPaint(pageIndex);
            canvas.drawRect(mNowRect,mPaint);
            canvas.restore();
        }
    }

    private RectF getBBox(int pageIndex) {
        RectF bboxRect = new RectF();
        mTempRect.set(mNowRect);

        mTempRect.inset(-thicknessOnPageView(pageIndex, mThickness) / 2f, -thicknessOnPageView(pageIndex, mThickness) / 2f);

        mPdfViewCtrl.convertPageViewRectToPdfRect(mTempRect, mTempRect, pageIndex);
        bboxRect.left = mTempRect.left;
        bboxRect.right = mTempRect.right;
        bboxRect.top = mTempRect.top;
        bboxRect.bottom = mTempRect.bottom;

        return bboxRect;
    }

    RectF mTempRect = new RectF(0, 0, 0, 0);

    private void createAnnot() {
        // create annotation in c++
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            RectF bboxRect = getBBox(mLastPageIndex);

            try {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(mLastPageIndex);
                final Square newAnnot = (Square) page.addAnnot(Annot.e_annotSquare, bboxRect);
                newAnnot.setBorderColor(mColor);
                newAnnot.setUniqueID(AppDmUtil.randomUUID(null));
                newAnnot.setOpacity(mOpacity / 255f);
                newAnnot.setTitle(AppDmUtil.getAnnotAuthor());
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mThickness);// line width
                borderInfo.setStyle(Annot.e_borderStyleSolid);// solid line
                newAnnot.setBorderInfo(borderInfo);
                newAnnot.setFlags(4);
                newAnnot.setSubject("Oval");
                newAnnot.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
                newAnnot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                newAnnot.resetAppearanceStream();
                DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, newAnnot);

                AnnotEventTask eventTask = new AnnotEventTask(new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {

                            RectF viewRect = null;
                            try {
                                viewRect = newAnnot.getRect();
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, mLastPageIndex);
                            Rect rect = new Rect();
                            viewRect.roundOut(rect);
                            rect.inset(-10, -10);
                            mPdfViewCtrl.refresh(mLastPageIndex, rect);

                            mTouchCaptured = false;
                            mLastPageIndex = -1;
                            mDownPoint.set(0, 0);
                            if (!mIsContinuousCreate) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                                mToolBtn.setSelected(false);
                            }
                        }
                    }
                });
                new Thread(eventTask).start();
            } catch (PDFException e) {
                if (e.getLastError() == PDFException.e_errOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
            }
        }
    }

    public void getDrawRect(float x1, float y1, float x2, float y2) {
        float minx = Math.min(x1, x2);
        float miny = Math.min(y1, y2);
        float maxx = Math.max(x1, x2);
        float maxy = Math.max(y1, y2);

        mNowRect.left = minx;
        mNowRect.top = miny;
        mNowRect.right = maxx;
        mNowRect.bottom = maxy;
    }

    /**
     * init toolbar
     */
    public void init() {
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl, mParent);
        mToolBtn = new CircleItemImpl(mContext);

        mToolBtn.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_CIR_TAG);
        mToolBtn.setImageResource(R.drawable.annot_circle_selector);
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mToolBtn.setEnable(false);
        }

        if (0 == mColor) {
            mColor = PropertyBar.PB_COLORS_SQUARE[0];
        }
        if (0 == mOpacity) {
            mOpacity = 255;
        }
        if (0 == mThickness) {
            mThickness = 5.0f;
        }

        mToolBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                changeStatusForClick();
            }
        });
        mToolBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                    return true;
                }
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != SquareToolHandler.this) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(SquareToolHandler.this);
                }
                return true;
            }
        });

    }

    private void changeStatusForClick() {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            return;
        }
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != SquareToolHandler.this) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(this);
            mToolBtn.setSelected(true);
        } else {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            mToolBtn.setSelected(false);
        }
    }

    public void changeCurrentColor(int currentColor) {
        mColor = currentColor;
    }

    public void changeCurrentOpacity(int currentOpacity) {
        mOpacity = currentOpacity;
    }

    public void changeCurrentThickness(float currentThickness) {
        mThickness = currentThickness;
    }


    /**
     * in StatusChangeListener
     */
    public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mToolBtn.setEnable(true);
        } else {
            mToolBtn.setEnable(false);
        }
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            mToolBtn.setSelected(true);
            preparePropertyBar();
        } else {
            mToolBtn.setSelected(false);
        }
    }

    public void removePropertyListener() {
        mPropertyChangeListener = null;
    }

    private void preparePropertyBar() {

        int[] colors = new int[PropertyBar.PB_COLORS_SQUARE.length];
        System.arraycopy(PropertyBar.PB_COLORS_SQUARE, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_SQUARE[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, mThickness);
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH;
    }

    private boolean mIsContinuousCreate = false;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }
}
