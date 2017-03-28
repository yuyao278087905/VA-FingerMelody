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
package com.foxit.uiextensions.annots.textmarkup.highlight;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Highlight;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class HighlightToolHandler implements ToolHandler {
    private Paint mPaint;
    private int mColor;
    private int mCurrentIndex;
    private int mOpacity;
    private RectF mBBoxRect;

    private BaseItem mAnnotButton;
    private PropertyBar mPropertyBar;
    private PDFViewCtrl mPdfViewCtrl;

    private final TextSelector mTextSelector;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    public HighlightToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mTextSelector = new TextSelector(pdfViewCtrl);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mBBoxRect = new RectF();

        mPropertyBar = new PropertyBarImpl(context, pdfViewCtrl, parent);

        mAnnotButton = new CircleItemImpl(context);
        mAnnotButton.setImageResource(R.drawable.annot_highlight_selector);
        mAnnotButton.setTag(ToolbarItemConfig.ITEM_HIGHLIGHT_TAG);

        mAnnotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextSelector.clear();
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(HighlightToolHandler.this);
            }
        });
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public TextSelector getTextSelector() {
        return mTextSelector;
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_HIGHLIGHT.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_HIGHLIGHT[0];
    }

    private void resetPropertyBar() {
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_HIGHLIGHT, 0, mPBColors, 0, mPBColors.length);
        mPBColors[0] = getPBCustomColor();
        mPropertyBar.setColors(mPBColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(mOpacity));
        mPropertyBar.reset(supportProperty);
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_HIGHLIGHT;
    }

    @Override
    public void onActivate() {
        mTextSelector.clear();
        mBBoxRect.setEmpty();
        mAnnotButton.setSelected(true);
        resetPropertyBar();
    }

    @Override
    public void onDeactivate() {
        mTextSelector.clear();
        mBBoxRect.setEmpty();
        mAnnotButton.setSelected(false);
    }


    private RectF mTmpRectF = new RectF();
    private Rect mTmpRoundRect = new Rect();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        try {
            PDFDoc doc = mPdfViewCtrl.getDoc();
            PDFPage page = doc.getPage(pageIndex);
            if (!page.isParsed()) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);
            int action = motionEvent.getAction();

            PointF pagePt = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mCurrentIndex = pageIndex;
                    int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
                    if (index >= 0) {
                        mTextSelector.start(page, index);
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mCurrentIndex != pageIndex) return true;
                    int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
                    if (index >= 0) {
                        mTextSelector.update(page, index);
                    }
                    invalidateTouch(mPdfViewCtrl, pageIndex, mTextSelector.getBbox());
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (mTextSelector.getRectFList().size() == 0) break;
                    mTextSelector.setContents(mTextSelector.getText(page));
                    addAnnot(mCurrentIndex, null, null);
                    return true;
                }
                default:
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
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
        if (mCurrentIndex != pageIndex) return;
        Rect clipRect = canvas.getClipBounds();
        for (RectF rect : mTextSelector.getRectFList()) {
            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, mTmpRectF, mCurrentIndex);
            mTmpRectF.round(mTmpRoundRect);
            if (mTmpRoundRect.intersect(clipRect)) {
                canvas.save();
                canvas.drawRect(mTmpRoundRect, mPaint);
                canvas.restore();
            }
        }
    }

    public void setPaint(int color, int opacity) {
        mColor = color;
        mOpacity = opacity;
        mPaint.setColor(calColorByMultiply(mColor, mOpacity));
    }

    private int calColorByMultiply(int color, int opacity) {
        int rColor = color | 0xFF000000;
        int r = (rColor & 0xFF0000) >> 16;
        int g = (rColor & 0xFF00) >> 8;
        int b = (rColor & 0xFF);
        float rOpacity = opacity / 255.0f;
        r = (int) (r * rOpacity + 255 * (1 - rOpacity));
        g = (int) (g * rOpacity + 255 * (1 - rOpacity));
        b = (int) (b * rOpacity + 255 * (1 - rOpacity));
        rColor = (rColor & 0xFF000000) | (r << 16) | (g << 8) | (b);
        return rColor;
    }

    protected void addAnnot(final int pageIndex, AnnotContent contentSupplier, final Event.Callback result) {
        int color = mColor;
        final Highlight annot;
        try {
            annot = (Highlight) mPdfViewCtrl.getDoc().getPage(pageIndex).addAnnot(Annot.e_annotHighlight, new RectF(0, 0, 0, 0));
            if (annot == null) {
                if (!misFromSelector) {
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        mAnnotButton.setSelected(false);
                    }
                }
                return;
            }

            annot.setBorderColor(color);

            if (contentSupplier != null && contentSupplier instanceof TextMarkupContent) {
                TextMarkupContent tmContent = TextMarkupContent.class.cast(contentSupplier);
                ArrayList<PointF> pointsList = tmContent.getQuadPoints();
                QuadPoints[] quadPointsArray = new QuadPoints[pointsList.size() / 4];
                for (int i = 0; i < pointsList.size() / 4; i++) {
                    quadPointsArray[i] = new QuadPoints(pointsList.get(4 * i), pointsList.get(4 * i + 1),
                            pointsList.get(4 * i + 2), pointsList.get(4 * i + 3));
                }

                annot.setQuadPoints(quadPointsArray);

                annot.setBorderColor(contentSupplier.getColor());
                annot.setOpacity(contentSupplier.getOpacity() / 255f);
            } else if (contentSupplier != null && contentSupplier instanceof TextMarkupContentAbs) {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(contentSupplier);
                QuadPoints[] quadPoint = new QuadPoints[tmSelector.getTextSelector().getRectFList().size()];
                for (int i = 0; i < tmSelector.getTextSelector().getRectFList().size(); i++) {
                    RectF rect = tmSelector.getTextSelector().getRectFList().get(i);
                    quadPoint[i] = new QuadPoints();
                    PointF point1 = new PointF(rect.left, rect.top);
                    quadPoint[i].setFirst(point1);
                    PointF point2 = new PointF(rect.right, rect.top);
                    quadPoint[i].setSecond(point2);
                    PointF point3 = new PointF(rect.left, rect.bottom);
                    quadPoint[i].setThird(point3);
                    PointF point4 = new PointF(rect.right, rect.bottom);
                    quadPoint[i].setFourth(point4);
                }

                annot.setQuadPoints(quadPoint);
                annot.setContent(tmSelector.getContents());
                annot.setBorderColor(color);
                annot.setOpacity(mOpacity / 255f);
            } else if (mTextSelector != null) {
                annot.setOpacity(mOpacity / 255f);

                annot.setFlags(Annot.e_annotFlagPrint);
                annot.setContent(mTextSelector.getContents());

                QuadPoints[] quadPoint = new QuadPoints[mTextSelector.getRectFList().size()];
                for (int i = 0; i < mTextSelector.getRectFList().size(); i++) {
                    RectF rect = mTextSelector.getRectFList().get(i);
                    quadPoint[i] = new QuadPoints();
                    PointF point1 = new PointF(rect.left, rect.top);
                    quadPoint[i].setFirst(point1);
                    PointF point2 = new PointF(rect.right, rect.top);
                    quadPoint[i].setSecond(point2);
                    PointF point3 = new PointF(rect.left, rect.bottom);
                    quadPoint[i].setThird(point3);
                    PointF point4 = new PointF(rect.right, rect.bottom);
                    quadPoint[i].setFourth(point4);
                }

                annot.setQuadPoints(quadPoint);

            } else {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            annot.setUniqueID(AppDmUtil.randomUUID(null));
            annot.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setTitle(AppDmUtil.getAnnotAuthor());
            annot.resetAppearanceStream();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(mPdfViewCtrl.getDoc().getPage(pageIndex), annot);

            AnnotEventTask eventTask = new AnnotEventTask(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        try {
                            invalidate(mPdfViewCtrl, pageIndex, annot.getRect());
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                    mTextSelector.clear();
                    mBBoxRect.setEmpty();
                    if (!misFromSelector) {
                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                            mAnnotButton.setSelected(false);
                        }
                    }
                    misFromSelector = false;
                    if (result != null) {
                        result.result(null, false);
                    }
                }
            });
            new Thread(eventTask).start();
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void invalidateTouch(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF rectF) {
        if (rectF == null) return;
        RectF rBBox = new RectF(rectF);
        pdfViewCtrl.convertPdfRectToPageViewRect(rBBox, rBBox, pageIndex);
        pdfViewCtrl.convertPageViewRectToDisplayViewRect(rBBox, rBBox, pageIndex);
        RectF rCalRectF = calculate(mBbox, mBBoxRect);
        rCalRectF.roundOut(mInvalidateRect);
        pdfViewCtrl.invalidate(mInvalidateRect);
        mBBoxRect.set(rBBox);
    }

    private RectF calculate(RectF newRectF, RectF oldRectF) {
        if (oldRectF.isEmpty()) return newRectF;
        int count = 0;
        if (newRectF.left == oldRectF.left && newRectF.top == oldRectF.top) count++;
        if (newRectF.right == oldRectF.right && newRectF.top == oldRectF.top) count++;
        if (newRectF.left == oldRectF.left && newRectF.bottom == oldRectF.bottom) count++;
        if (newRectF.right == oldRectF.right && newRectF.bottom == oldRectF.bottom) count++;
        if (count == 2) {
            newRectF.union(oldRectF);
            RectF rectF = new RectF(newRectF);
            newRectF.intersect(oldRectF);
            rectF.intersect(newRectF);
            return rectF;
        } else if (count == 3 || count == 4) {
            return newRectF;
        } else {
            newRectF.union(oldRectF);
            return newRectF;
        }
    }

    private RectF mBbox = new RectF();
    private Rect mInvalidateRect = new Rect();

    private void invalidate(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF rectF) {
        if (rectF == null || !pdfViewCtrl.isPageVisible(pageIndex)) return;
        mBbox.set(rectF);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, mBbox, pageIndex);
        mBbox.roundOut(mInvalidateRect);
        pdfViewCtrl.refresh(pageIndex, mInvalidateRect);
    }

    public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mAnnotButton.setEnable(false);
        } else {
            mAnnotButton.setEnable(true);
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    private boolean mIsContinuousCreate = false;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    private boolean misFromSelector = false;

    public void setFromSelector(boolean b) {
        misFromSelector = b;
    }
}
