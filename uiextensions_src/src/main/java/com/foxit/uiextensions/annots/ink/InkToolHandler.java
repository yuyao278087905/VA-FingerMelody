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
package com.foxit.uiextensions.annots.ink;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;
import java.util.List;


class InkToolHandler extends AbstractToolHandler {
    public static int IA_MIN_DIST = 2;
    protected static final String PROPERTY_KEY = "INK";

    protected InkAnnotHandler mAnnotHandler;
    protected InkAnnotUtil mUtil;

    private boolean mTouchCaptured = false;
    private int mCapturedPage = -1;
    protected List<Ink> mCache = new ArrayList<Ink>();

    private ArrayList<ArrayList<PointF>> mLineList;
    private ArrayList<PointF> mLine;
    private ArrayList<Path> mPathList;
    private Path mPath;
    private PointF mLastPt = new PointF(0, 0);
    private Paint mPaint;

    private boolean mConfigChanged = false;

    public InkToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, InkAnnotUtil util) {
        super(context, parent, pdfViewCtrl, Module.MODULE_NAME_INK, PROPERTY_KEY);
        mColor = PropertyBar.PB_COLORS_PENCIL[0];

        mUtil = util;
        mLineList = new ArrayList<ArrayList<PointF>>();
        mPathList = new ArrayList<Path>();

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);


        pdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
                if (mCache != null) {
                    mCache.clear();
                }
            }

            @Override
            public void onDocWillClose(PDFDoc document) {

            }

            @Override
            public void onDocClosed(PDFDoc document, int errCode) {
                if (mCache != null) {
                    mCache.clear();
                }
            }

            @Override
            public void onDocWillSave(PDFDoc document) {

            }

            @Override
            public void onDocSaved(PDFDoc document, int errCode) {

            }
        });
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_INK;
    }

    protected void initUiElements() {

    }

    protected void uninitUiElements() {
        removeToolButton();
    }

    public void updateToolButtonStatus() {

    }

    @Override
    public void setColor(int color) {
        if (mColor == color) return;
        addAnnot(null);
        mColor = color;
    }

    @Override
    public void setOpacity(int opacity) {
        if (mOpacity == opacity) return;
        addAnnot(null);
        mOpacity = opacity;
    }

    @Override
    public void setThickness(float thickness) {
        if (mThickness == thickness) return;
        addAnnot(null);
        mThickness = thickness;
    }

    @Override
    public void onActivate() {
        mCapturedPage = -1;
        mLineList.clear();
        mPathList.clear();

    }

    @Override
    public void onDeactivate() {

        if (mTouchCaptured) {
            mTouchCaptured = false;

            if (mLine != null) {
                mLineList.add(mLine);
                mLine = null;
            }
            mLastPt.set(0, 0);
        }
        addAnnot(null);

    }

    float mbx, mby, mcx, mcy, mex, mey;
    PointF tv_pt = new PointF();
    Rect tv_rect = new Rect();
    RectF tv_invalid = new RectF();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured) {
                    if (mCapturedPage == -1) {
                        mTouchCaptured = true;
                        mCapturedPage = pageIndex;
                    } else if (pageIndex == mCapturedPage) {
                        mTouchCaptured = true;
                    } else {

                    }
                    if (mTouchCaptured) {
                        mPath = new Path();
                        mPath.moveTo(point.x, point.y);
                        mbx = point.x;
                        mby = point.y;
                        mcx = point.x;
                        mcy = point.y;

                        mLine = new ArrayList<PointF>();
                        mLine.add(new PointF(point.x, point.y));
                        mLastPt.set(point.x, point.y);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchCaptured) {
                    tv_pt.set(point);
                    InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                    float dx = Math.abs(tv_pt.x - mcx);
                    float dy = Math.abs(tv_pt.y - mcy);
                    if (mCapturedPage == pageIndex && (dx >= IA_MIN_DIST || dy >= IA_MIN_DIST)) {
                        // history points
                        tv_invalid.set(tv_pt.x, tv_pt.y, tv_pt.x, tv_pt.y);
                        for (int i = 0; i < e.getHistorySize(); i++) {
                            tv_pt.set(e.getHistoricalX(i), e.getHistoricalY(i));
                            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(tv_pt, tv_pt, pageIndex);
                            InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                            if (tv_pt.x - mLastPt.x >= IA_MIN_DIST || tv_pt.y - mLastPt.y >= IA_MIN_DIST) {
                                mex = (mcx + tv_pt.x) / 2;
                                mey = (mcy + tv_pt.y) / 2;
                                mLine.add(new PointF(tv_pt.x, tv_pt.y));
                                mPath.quadTo(mcx, mcy, mex, mey);
                                mLastPt.set(tv_pt);
                                tv_invalid.union(mbx, mby);
                                tv_invalid.union(mcx, mcy);
                                tv_invalid.union(mex, mey);
                                mbx = mex;
                                mby = mey;
                                mcx = tv_pt.x;
                                mcy = tv_pt.y;
                            }
                        }
                        // current point
                        tv_pt.set(point);
                        InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                        mex = (mcx + tv_pt.x) / 2;
                        mey = (mcy + tv_pt.y) / 2;
                        mLine.add(new PointF(tv_pt.x, tv_pt.y));
                        mPath.quadTo(mcx, mcy, mex, mey);
                        mLastPt.set(tv_pt.x, tv_pt.y);
                        tv_invalid.union(mbx, mby);
                        tv_invalid.union(mcx, mcy);
                        tv_invalid.union(mex, mey);
                        mbx = mex;
                        mby = mey;
                        mcx = tv_pt.x;
                        mcy = tv_pt.y;
                        tv_invalid.inset(-thickness, -thickness);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(tv_invalid, tv_invalid, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(tv_invalid));
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        tv_pt.set(point);
                        InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                        if (mLine.size() == 1) {
                            if (tv_pt.equals(mLine.get(0))) {
                                tv_pt.x += 0.1;
                                tv_pt.y += 0.1;
                            }
                            mex = (mcx + tv_pt.x) / 2;
                            mey = (mcy + tv_pt.y) / 2;
                            mLine.add(new PointF(tv_pt.x, tv_pt.y));
                            mPath.quadTo(mcx, mcy, mex, mey);
                            mLastPt.set(tv_pt.x, tv_pt.y);
                        }
                        mPath.lineTo(mLastPt.x, mLastPt.y);
                        mPathList.add(mPath);
                        mPath = null;
                        tv_invalid.set(mbx, mby, mbx, mby);
                        tv_invalid.union(mcx, mcy);
                        tv_invalid.union(mex, mey);
                        tv_invalid.union(mLastPt.x, mLastPt.y);
                        tv_invalid.inset(-thickness, -thickness);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(tv_invalid, tv_invalid, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(tv_invalid));
                        mLineList.add(mLine);
                        mLine = null;
                        mTouchCaptured = false;
                        mLastPt.set(0, 0);
                    }
                    return true;
                }
                break;
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

        if (mPathList != null && mCapturedPage == pageIndex) {
            // draw current creating annotation
            setPaintProperty(mPdfViewCtrl, pageIndex, mPaint);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

            int count = mPathList.size();
            for (int i = 0; i < count; i++) {
                canvas.drawPath(mPathList.get(i), mPaint);
            }
            if (mPath != null) {
                canvas.drawPath(mPath, mPaint);
            }
        }
    }

    @Override
    protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {
        paint.setColor(mColor);
        paint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(UIAnnotFrame.getPageViewThickness(pdfViewCtrl, pageIndex, mThickness));
    }

    @Override
    public long getSupportedProperties() {
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        propertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        super.setPropertyBarProperties(propertyBar);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ToolHandler curToolHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler();
        if (curToolHandler != null && curToolHandler == this && mLineList.size() > 0) {
            mConfigChanged = true;
        } else {
            mConfigChanged = false;
        }

        addAnnot(null);
    }

    protected void addAnnot(final IAnnotTaskResult<PDFPage, Annot, Void> result) {
        if (mCapturedPage == -1) return;
        if (mLineList.size() == 0) return;
        RectF bbox = new RectF();
        ArrayList<ArrayList<PointF>> docLines = mUtil.docLinesFromPageView(mPdfViewCtrl, mCapturedPage, mLineList, bbox);
        bbox.inset(-mThickness, -mThickness);
        Ink annot = (Ink) mAnnotHandler.addAnnot(mCapturedPage, new RectF(bbox),
                mColor,
                AppDmUtil.opacity100To255(mOpacity),
                mThickness,
                docLines,
                new IAnnotTaskResult<PDFPage, Annot, Void>() {
                    public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                        mCache.remove(p2);
                        if (result != null) {
                            result.onResult(success, p1, p2, p3);
                        }
                    }
                });
        if (annot != null) {
            mCache.add(annot);
        }
        mCapturedPage = -1;
        mLineList.clear();
        mPathList.clear();
    }
}
