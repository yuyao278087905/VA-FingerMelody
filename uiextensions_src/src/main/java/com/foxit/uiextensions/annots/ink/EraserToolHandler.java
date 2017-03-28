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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.common.PDFPath;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.Collections;


class EraserToolHandler extends AbstractToolHandler {
    private Paint mEraserPaint;
    private Paint mPaint;
    protected float mRadius = 15.0f;
    private int mCtlPtRadius = 5;

    private ArrayList<AnnotInfo> mRootList;
    private ArrayList<Path> mPaths;

    private boolean mTouchCaptured = false;
    private int mCapturedPage = -1;
    private PointF mLastPt = new PointF();

    public EraserToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        super(context, parent, pdfViewCtrl, ToolHandler.TH_TYPE_ERASER, "ERASER");

        mCtlPtRadius = AppDisplay.getInstance(context).dp2px((float) mCtlPtRadius);
        mRootList = new ArrayList<AnnotInfo>();
        mPaths = new ArrayList<Path>();

        mEraserPaint = new Paint();
        mEraserPaint.setStyle(Style.STROKE);
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setDither(true);
        mEraserPaint.setColor(Color.RED);

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        float radius = 0;
        if (radius > 0) {
            setRadius(radius);
        }

        mColor = Color.LTGRAY;
    }

    public void initUiElements() {
    }

    @Override
    public void onActivate() {
        mCapturedPage = -1;
        mRootList.clear();
        mPaths.clear();
    }

    Ink tempAnnot;

    @Override
    public void onDeactivate() {
        // if the ink annotation is not really modified,
        // remove it from the list, and update data in JNI.
        for (int k = 0; k < mRootList.size(); k++) {
            AnnotInfo annotInfo = mRootList.get(k);
            if (!annotInfo.mModifyFlag) {
                mRootList.remove(k);
                invalidateJniAnnots(annotInfo, 1, null);
                continue;
            }
        }
        if (!mRootList.isEmpty()) {
            Collections.sort(mRootList);
            try {
                int pageIndex = mRootList.get(0).mAnnot.getPage().getIndex();
                RectF rect = mRootList.get(0).mAnnot.getRect();
                RectF rmRect = new RectF(rect);
                boolean isLast = false;
                for (int i = mRootList.size() - 1; i >= 0; i--) {
                    if (i == 0) {
                        isLast = true;
                    }
                    final AnnotInfo annotInfo = mRootList.get(i);
                    tempAnnot = annotInfo.mAnnot;
                    if (annotInfo.mModifyFlag) {
                        if (!annotInfo.mNewLines.isEmpty()) {
                            modifyAnnot(pageIndex, annotInfo, isLast);
                        } else {
                            deleteAnnot(tempAnnot, null, isLast);
                        }
                    }
                    invalidateJniAnnots(annotInfo, 1, null);
                    rmRect.union(annotInfo.mAnnot.getRect());
                }

                RectF rectF = rmRect;
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
                    AnnotEventTask task = new AnnotEventTask(new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            mCapturedPage = -1;
                            mRootList.clear();
                            mPaths.clear();
                        }
                    });
                    new Thread(task).start();
                } else {
                    mCapturedPage = -1;
                    mRootList.clear();
                    mPaths.clear();
                }
            } catch (PDFException e) {

            }
        }
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {

        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            return false;
        }
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        float x = point.x;
        float y = point.y;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured) {
                    if (mCapturedPage == -1) {
                        mTouchCaptured = true;
                        mCapturedPage = pageIndex;
                    } else if (pageIndex == mCapturedPage) {
                        mTouchCaptured = true;
                    }
                    if (mTouchCaptured) {
                        mLastPt.set(x, y);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchCaptured && mCapturedPage == pageIndex) {
                    if (!mLastPt.equals(x, y)) {
                        calculateNewLines(mPdfViewCtrl, pageIndex, point);
                        RectF invaRect = getEraserBBox(mLastPt, point);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(invaRect, invaRect, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(invaRect));
                        mLastPt.set(x, y);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchCaptured) {
                    mTouchCaptured = false;
                    RectF invaRect2 = getEraserBBox(mLastPt, point);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(invaRect2, invaRect2, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(invaRect2));

                    mLastPt.set(-mRadius, -mRadius);
                }
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
        if (mCapturedPage == pageIndex) {
            canvas.drawCircle(mLastPt.x, mLastPt.y, mRadius, mEraserPaint);
            if (mRootList.size() == 0) return;
            for (int i = 0; i < mRootList.size(); i++) {
                AnnotInfo annotInfo = mRootList.get(i);
                if (annotInfo.mDrawAtJava == false) continue;
                if (!annotInfo.mIsPSIMode) {
                    setPaint(annotInfo.mAnnot);
                    mPaths = getNewPaths(mPdfViewCtrl, pageIndex, annotInfo);
                    if (mPaths != null) {
                        int count = mPaths.size();
                        for (int j = 0; j < count; j++) {
                            canvas.drawPath(mPaths.get(j), mPaint);
                        }
                    }
                }
            }
        }
    }


    class AnnotInfo implements Comparable<AnnotInfo> {
        Ink mAnnot;
        boolean mModifyFlag;
        boolean mDrawAtJava;
        ArrayList<LineInfo> mNewLines;
        boolean mIsPSIMode = false; //true : psi, false: ink

        public AnnotInfo() {
            mModifyFlag = false;
            mDrawAtJava = false;
            mNewLines = new ArrayList<LineInfo>();
            mIsPSIMode = false;
        }

        //sort mRootlist :delete  and  modify  from small to big by annotation index
        @Override
        public int compareTo(AnnotInfo another) {
            try {
                if (another.mNewLines.isEmpty()) {
                    if (mNewLines.isEmpty()) {
                        if (another.mAnnot.getIndex() > mAnnot.getIndex()) {
                            return -1;
                        } else if (another.mAnnot.getIndex() == mAnnot.getIndex()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        return 1;
                    }
                } else {
                    if (mNewLines.isEmpty()) {
                        return -1;
                    } else {
                        if (another.mAnnot.getIndex() > mAnnot.getIndex()) {
                            return -1;
                        } else if (another.mAnnot.getIndex() == mAnnot.getIndex()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                }
            } catch (PDFException e) {

            }
            return 1;
        }

    }

    class LineInfo {
        RectF mLineBBox;
        ArrayList<PointF> mLine;
        ArrayList<Float> mPresses;

        public LineInfo() {
            mLineBBox = new RectF();
            mLine = new ArrayList<PointF>();
            mPresses = new ArrayList<Float>();
        }
    }

    private void calculateNewLines(final PDFViewCtrl pdfViewCtrl, final int pageIndex, PointF point) {
        RectF rect = new RectF(point.x, point.y, point.x, point.y);
        rect.union(mLastPt.x, mLastPt.y);
        rect.inset(-mRadius, -mRadius);
        mPdfViewCtrl.convertPageViewRectToPdfRect(rect, rect, pageIndex);
        try {
            PDFPage page = pdfViewCtrl.getDoc().getPage(pageIndex);
            ArrayList<Annot> annotList = DocumentManager.getInstance(mPdfViewCtrl).getAnnotsInteractRect(page, new RectF(rect), Annot.e_annotInk);
            PointF tv_pt = new PointF();
            RectF tv_rectF = new RectF();
            RectF eraseBBox = getEraserBBox(mLastPt, point);
            mPdfViewCtrl.convertPageViewRectToPdfRect(eraseBBox, eraseBBox, pageIndex);
            for (final Annot annot : annotList) {
                RectF annotBBox = annot.getRect();
                if (DocumentManager.intersects(annotBBox, eraseBBox)) {
                    boolean isExist = false;
                    for (int i = 0; i < mRootList.size(); i++) {
                        if (mRootList.get(i).mAnnot == annot) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        final AnnotInfo annotInfo = new AnnotInfo();
                        annotInfo.mAnnot = (Ink) annot;
                        PDFPath path = ((Ink) annot).getInkList();

                        LineInfo lineInfo = null;
                        int ptCount = path.getPointCount();
                        for (int j = 0; j < ptCount; j++) {
                            if (path.getPointType(j) == PDFPath.e_pointTypeMoveTo) {
                                lineInfo = new LineInfo();
                            }
                            lineInfo.mLine.add(path.getPoint(j));
                            if (j == ptCount - 1 || ((j + 1) < ptCount && path.getPointType(j + 1) == PDFPath.e_pointTypeMoveTo)) {
                                lineInfo.mLineBBox = getLineBBox(lineInfo.mLine, annot.getBorderInfo().getWidth());
                                annotInfo.mNewLines.add(lineInfo);
                            }
                        }
                        mRootList.add(annotInfo);

                        invalidateJniAnnots(annotInfo, 0, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                try {
                                    RectF viewRect = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                                annotInfo.mDrawAtJava = true;
                            }
                        });
                    }
                }
            }

            PointF pdfDP = new PointF(mLastPt.x, mLastPt.y);
            PointF pdfCP = new PointF(point.x, point.y);
            RectF eBBox = getEraserBBox(mLastPt, point);
            RectF radiusRect = new RectF(0, 0, mRadius, mRadius);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfDP, pdfDP, pageIndex);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfCP, pdfCP, pageIndex);
            mPdfViewCtrl.convertPageViewRectToPdfRect(eBBox, eBBox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToPdfRect(radiusRect, radiusRect, pageIndex);

            float pdfR = radiusRect.width();
            PointF intersectPoint = new PointF();
            PointF pdfPoint1 = new PointF();
            PointF pdfPoint2 = new PointF();

            for (int i = 0; i < mRootList.size(); i++) {
                AnnotInfo annotNode = mRootList.get(i);
                if (!DocumentManager.intersects(annotNode.mAnnot.getRect(), eBBox))
                    continue;
                for (int lineIndex = 0; lineIndex < annotNode.mNewLines.size(); lineIndex++) {
                    LineInfo lineNode = annotNode.mNewLines.get(lineIndex);
                    ArrayList<PointF> pdfLine = lineNode.mLine;
                    ArrayList<Float> presses = lineNode.mPresses;
                    int end1_PointIndex = -1, begin2_PointIndex = -1;

                    //if lineRect intersect eraseRect
                    RectF lineRect = lineNode.mLineBBox;
                    if (!DocumentManager.intersects(lineRect, eBBox))
                        continue;

                    for (int j = 0; j < pdfLine.size(); j++) {
                        // out of circle point  or  first point
                        pdfPoint1.set(pdfLine.get(j).x, pdfLine.get(j).y);
                        boolean createNewLine = false;
                        boolean reachEnd = false;
                        if (j == pdfLine.size() - 1) {
                            reachEnd = true;
                        } else {
                            // in circle point  or  second point
                            pdfPoint2.set(pdfLine.get(j + 1).x, pdfLine.get(j + 1).y);
                        }

                        int type = getIntersection(pdfPoint1, pdfPoint2, pdfDP, pdfCP, intersectPoint);
                        if (!reachEnd && type == 1) {
                            createNewLine = true;
                            tv_rectF.set(intersectPoint.x, intersectPoint.y, intersectPoint.x, intersectPoint.y);
                            for (int p = j; p >= 0; p--) {
                                tv_pt.set(pdfLine.get(p).x, pdfLine.get(p).y);
                                tv_rectF.union(tv_pt.x, tv_pt.y);
                                if (getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, intersectPoint.x, intersectPoint.y) > pdfR) {
                                    end1_PointIndex = p;
                                    if (p > 0) {
                                        tv_rectF.union(pdfLine.get(p - 1).x, pdfLine.get(p - 1).y);
                                    }
                                    break;
                                }
                            }
                            for (int q = j + 1; q < pdfLine.size(); q++) {
                                tv_pt.set(pdfLine.get(q).x, pdfLine.get(q).y);
                                tv_rectF.union(tv_pt.x, tv_pt.y);
                                if (getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, intersectPoint.x, intersectPoint.y) > pdfR) {
                                    begin2_PointIndex = q;
                                    if (q < pdfLine.size() - 1) {
                                        tv_rectF.union(pdfLine.get(q + 1).x, pdfLine.get(q + 1).y);
                                    }
                                    break;
                                }
                            }
                        } else if (getDistanceOfPointToLine(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR) {
                            if (isIntersectPointInLine(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y)
                                    || getDistanceOfTwoPoints(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y) < pdfR
                                    || getDistanceOfTwoPoints(pdfPoint1.x, pdfPoint1.y, pdfCP.x, pdfCP.y) < pdfR) {
                                createNewLine = true;
                                for (int p = j; p >= 0; p--) {
                                    tv_pt.set(pdfLine.get(p).x, pdfLine.get(p).y);
                                    tv_rectF.union(tv_pt.x, tv_pt.y);
                                    if (getDistanceOfPointToLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR &&
                                            (isIntersectPointInLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y) < pdfR ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfCP.x, pdfCP.y) < pdfR)) {
                                        continue;
                                    }
                                    end1_PointIndex = p;
                                    if (p > 0) {
                                        tv_rectF.union(pdfLine.get(p - 1).x, pdfLine.get(p - 1).y);
                                    }
                                    break;
                                }
                                for (int q = j + 1; q < pdfLine.size(); q++) {
                                    tv_pt.set(pdfLine.get(q).x, pdfLine.get(q).y);
                                    tv_rectF.union(tv_pt.x, tv_pt.y);
                                    if (getDistanceOfPointToLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR &&
                                            (isIntersectPointInLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y) < pdfR ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfCP.x, pdfCP.y) < pdfR)) {
                                        continue;
                                    }
                                    begin2_PointIndex = q;
                                    if (q < pdfLine.size() - 1) {
                                        tv_rectF.union(pdfLine.get(q + 1).x, pdfLine.get(q + 1).y);
                                    }
                                    break;
                                }
                            }
                        }

                        if (createNewLine) {
                            createNewLine = false;

                            ArrayList<PointF> newLine1 = new ArrayList<PointF>();
                            ArrayList<Float> newPresses1 = new ArrayList<Float>();
                            if (0 <= end1_PointIndex && end1_PointIndex < pdfLine.size()) {
                                for (int k = 0; k <= end1_PointIndex; k++) {
                                    newLine1.add(pdfLine.get(k));
                                }
                            }

                            ArrayList<PointF> newLine2 = new ArrayList<PointF>();
                            ArrayList<Float> newPresses2 = new ArrayList<Float>();
                            if (0 <= begin2_PointIndex && begin2_PointIndex < pdfLine.size()) {
                                for (int k = pdfLine.size() - 1; k >= begin2_PointIndex; k--) {
                                    newLine2.add(pdfLine.get(k));
                                }
                            }

                            annotNode.mNewLines.remove(lineIndex);
                            if (newLine1.size() == 0 && newLine2.size() == 0) {
                                // current line is removed, and no new line is added
                                // lineIndex -- adjust index continue to erase next line
                                lineIndex--;
                            } else {
                                // insert line2 first, then line1
                                // make sure the line1 is before line2
                                if (newLine2.size() != 0) {
                                    LineInfo info = new LineInfo();
                                    info.mLine = newLine2;
                                    info.mPresses = newPresses2;
                                    info.mLineBBox = getLineBBox(newLine2, annotNode.mAnnot.getBorderInfo().getWidth());
                                    annotNode.mNewLines.add(lineIndex, info);
                                }
                                if (newLine1.size() != 0) {
                                    LineInfo info = new LineInfo();
                                    info.mLine = newLine1;
                                    info.mPresses = newPresses1;
                                    info.mLineBBox = getLineBBox(newLine1, annotNode.mAnnot.getBorderInfo().getWidth());
                                    annotNode.mNewLines.add(lineIndex, info);
                                } else {
                                    // if line1 have no point, add index -- for continue erase line2
                                    lineIndex--;
                                }
                            }
                            annotNode.mModifyFlag = true;
                            invalidateNewLine(pdfViewCtrl, pageIndex, annotNode.mAnnot, tv_rectF);
                            break;
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void invalidateNewLine(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, RectF rect) {
        try {
            rect.inset(annot.getBorderInfo().getWidth(), annot.getBorderInfo().getWidth());
            float tmp = rect.top;
            rect.top = rect.bottom;
            rect.bottom = tmp;

            pdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
            pdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
            pdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private double getDistanceOfPointToLine(float x, float y, float x1, float y1, float x2, float y2) {
        float k = 0;
        float b = 0;

        if (x1 == x2) {
            return Math.abs(x - x1);
        } else if (y1 == y2) {
            return Math.abs(y - y1);
        } else {
            k = (y2 - y1) / (x2 - x1);
            b = y2 - k * x2;
            return Math.abs(k * x - y + b) / Math.sqrt(k * k + 1);
        }
    }

    private boolean isIntersectPointInLine(float x, float y, float x1, float y1, float x2, float y2) {
        boolean result = false;
        double cross = (x2 - x1) * (x - x1) + (y2 - y1) * (y - y1);
        double d = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        double r = cross / d;
        if (r > 0 && r < 1) {
            result = true;
        }
        return result;
    }


    private int getIntersection(PointF a, PointF b, PointF c, PointF d, PointF intersection) {
        if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) + Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0) {
            if ((c.x - a.x) + (c.y - a.y) == 0) {
                //System.out.println("A B C D are the same point");
            } else {
                //System.out.println("A B are the same point.C D are the same point, and different from AC.");
            }
            return 0;
        }

        if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) == 0) {
            if ((a.x - d.x) * (c.y - d.y) - (a.y - d.y) * (c.x - d.x) == 0) {
                //System.out.println("A、B are the same point，and in line CD.");
            } else {
                //System.out.println("A、B are the same point，and not in line CD.");
            }
            return 0;
        }

        if (Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0) {
            if ((d.x - b.x) * (a.y - b.y) - (d.y - b.y) * (a.x - b.x) == 0) {
                //System.out.println("C、D are the same point，and in line AB.");
            } else {
                //System.out.println("C、D are the same point，and not in line AB.");
            }
            return 0;
        }

        if ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y) == 0) {
            //System.out.println("Parallel lines, no intersection!");
            return 0;
        }

        intersection.x = ((b.x - a.x) * (c.x - d.x) * (c.y - a.y) - c.x
                * (b.x - a.x) * (c.y - d.y) + a.x * (b.y - a.y) * (c.x - d.x))
                / ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y));
        intersection.y = ((b.y - a.y) * (c.y - d.y) * (c.x - a.x) - c.y
                * (b.y - a.y) * (c.x - d.x) + a.y * (b.x - a.x) * (c.y - d.y))
                / ((b.x - a.x) * (c.y - d.y) - (b.y - a.y) * (c.x - d.x));

        if ((intersection.x - a.x) * (intersection.x - b.x) <= 0
                && (intersection.x - c.x) * (intersection.x - d.x) <= 0
                && (intersection.y - a.y) * (intersection.y - b.y) <= 0
                && (intersection.y - c.y) * (intersection.y - d.y) <= 0) {
            //System.out.println("Lines intersect at the intersection point(" + intersection.x + "," + intersection.y + ")!");
            return 1;
        } else {
            //System.out.println("Lines intersect at the virtual intersection point(" + intersection.x + "," + intersection.y + ")!");
            return -1;
        }
    }

    private RectF getEraserBBox(PointF downPoint, PointF point) {
        RectF eraserBBox = new RectF();
        eraserBBox.left = Math.min(downPoint.x, point.x);
        eraserBBox.top = Math.min(downPoint.y, point.y);
        eraserBBox.right = Math.max(downPoint.x, point.x);
        eraserBBox.bottom = Math.max(downPoint.y, point.y);
        eraserBBox.inset(-mRadius - 2, -mRadius - 2);
        return eraserBBox;
    }

    private RectF getLineBBox(ArrayList<PointF> line, float thickness) {
        if (line.size() == 0) {
            return new RectF(0, 0, 0, 0);
        }

        RectF lineBBox = new RectF(line.get(0).x, line.get(0).y, line.get(0).x, line.get(0).y);
        for (int i = 0; i < line.size(); i++) {
            lineBBox.left = Math.min(lineBBox.left, line.get(i).x);
            lineBBox.top = Math.max(lineBBox.top, line.get(i).y);
            lineBBox.right = Math.max(lineBBox.right, line.get(i).x);
            lineBBox.bottom = Math.min(lineBBox.bottom, line.get(i).y);
        }
        lineBBox.inset(-thickness / 2, -thickness / 2);
        return lineBBox;
    }


    private float getDistanceOfTwoPoints(float x1, float y1, float x2, float y2) {
        return (float) (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }

    private ArrayList<Path> getNewPaths(PDFViewCtrl pdfViewCtrl, int pageIndex, AnnotInfo info) {
        ArrayList<LineInfo> pdfLines = info.mNewLines;
        ArrayList<Path> paths = new ArrayList<Path>();
        PointF pointF = new PointF();

        float cx = 0, cy = 0, ex, ey;
        for (int i = 0; i < pdfLines.size(); i++) {
            ArrayList<PointF> pdfLine = pdfLines.get(i).mLine;
            int ptCount = pdfLine.size();
            if (ptCount == 0) {
                continue;
            } else if (ptCount == 1) {
                Path path = new Path();
                pointF.set(pdfLine.get(0).x, pdfLine.get(0).y);
                pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                path.moveTo(pointF.x, pointF.y);
                path.lineTo(pointF.x + 0.1f, pointF.y + 0.1f);
                paths.add(path);
            } else {
                Path path = new Path();
                for (int j = 0; j < ptCount; j++) {
                    pointF.set(pdfLine.get(j).x, pdfLine.get(j).y);
                    pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                    if (j == 0) {
                        path.moveTo(pointF.x, pointF.y);
                        cx = pointF.x;
                        cy = pointF.y;
                    } else {
                        ex = (cx + pointF.x) / 2;
                        ey = (cy + pointF.y) / 2;
                        path.quadTo(cx, cy, ex, ey);
                        cx = pointF.x;
                        cy = pointF.y;
                        if (j == pdfLine.size() - 1) {
                            ex = pointF.x;
                            ey = pointF.y;
                            path.lineTo(ex, ey);
                        }
                    }
                }
                paths.add(path);
            }
        }
        return paths;
    }

    private void invalidateJniAnnots(AnnotInfo annotInfo, int flag, Event.Callback result) {

        if (flag == 0) {
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotStartEraser(annotInfo.mAnnot);
        } else if (flag == 1) {
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotEndEraser();
        }
        if (result != null) {
            result.result(null, true);
        }
    }

    private RectF getNewBBox(AnnotInfo annotInfo) {
        Ink annot = annotInfo.mAnnot;
        ArrayList<ArrayList<PointF>> pdfLines = getNewPdfLines(annotInfo);
        RectF newBBox = null;
        for (int i = 0; i < pdfLines.size(); i++) {
            for (int j = 0; j < pdfLines.get(i).size(); j++) {
                PointF pdfPt = pdfLines.get(i).get(j);
                if (newBBox == null) {
                    newBBox = new RectF(pdfPt.x, pdfPt.y, pdfPt.x, pdfPt.y);
                } else {
                    newBBox.left = Math.min(newBBox.left, pdfPt.x);
                    newBBox.bottom = Math.min(newBBox.bottom, pdfPt.y);
                    newBBox.right = Math.max(newBBox.right, pdfPt.x);
                    newBBox.top = Math.max(newBBox.top, pdfPt.y);
                }
            }
        }
        try {
            newBBox.inset(-annot.getBorderInfo().getWidth() * 0.5f - mCtlPtRadius, -annot.getBorderInfo().getWidth() * 0.5f - mCtlPtRadius);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return newBBox;
    }

    private ArrayList<ArrayList<PointF>> getNewPdfLines(AnnotInfo annotInfo) {
        ArrayList<ArrayList<PointF>> pdfLines = new ArrayList<ArrayList<PointF>>();
        for (int i = 0; i < annotInfo.mNewLines.size(); i++) {
            ArrayList<PointF> oldLine = annotInfo.mNewLines.get(i).mLine;
            ArrayList<PointF> newLine = new ArrayList<PointF>();
            for (int j = 0; j < oldLine.size(); j++) {
                newLine.add(oldLine.get(j));
            }
            pdfLines.add(newLine);
        }
        return pdfLines;
    }


    private void modifyAnnot(final int pageIndex, AnnotInfo annotInfo, final boolean isLast) {
        try {
            final Annot annot = annotInfo.mAnnot;
            RectF newBBox = getNewBBox(annotInfo);
            RectF newRectF = new RectF(newBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(newRectF, newRectF, pageIndex);

            ArrayList<ArrayList<PointF>> pdfLines = getNewPdfLines(annotInfo);
            if (pdfLines != null) {
                PDFPath path = PDFPath.create();
                for (int i = 0; i < pdfLines.size(); i++) {
                    ArrayList<PointF> line = pdfLines.get(i);
                    for (int j = 0; j < line.size(); j++) {
                        if (j == 0) {
                            path.moveTo(line.get(j));
                        } else {
                            path.lineTo(line.get(j));
                        }
                    }
                }
                ((Ink) annot).setInkList(path);
            }

            annot.resetAppearanceStream();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(annot.getPage(), annot);

            AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        try {
                            RectF annotRectF = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            new Thread(annotEventTask).start();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnot(final Annot annot, final Event.Callback result, final boolean isLast) {
        // step 1: set current annot to null
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        try {
            RectF viewRect = annot.getRect();
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            ((Ink) annot).removeAllReplies();
            page.removeAnnot(annot);
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {

                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (result != null) {
            result.result(null, true);
        }

    }

    public void setPaint(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            mPaint.setColor((int) annot.getBorderColor());
            mPaint.setStrokeWidth(thickness);
            int opacity = (int) (((Ink) annot).getOpacity() * 255f + 0.5f);
            mPaint.setAlpha(opacity);
            mPaint.setStyle(Style.STROKE);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private float thicknessOnPageView(int pageIndex, float thickness) {
        RectF rectF = new RectF(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    public void setRadius(float radius) {
        mRadius = AppDisplay.getInstance(mContext).dp2px(radius);
    }

    @Override
    public void setThickness(float thickness) {
        super.setThickness(thickness);
        setRadius(thickness);
    }

    @Override
    protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {

    }

    @Override
    public long getSupportedProperties() {
        return PropertyBar.PROPERTY_LINEWIDTH;
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        propertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
        if (AppDisplay.getInstance(mContext).isPad()) {
            propertyBar.setArrowVisible(true);
        } else {
            propertyBar.setArrowVisible(false);
        }
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_ERASER;
    }
}
