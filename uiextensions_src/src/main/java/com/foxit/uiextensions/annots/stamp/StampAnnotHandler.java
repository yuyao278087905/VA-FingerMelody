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
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;


public class StampAnnotHandler implements AnnotHandler {
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private Annot mBitmapAnnot;
    private boolean mIsModify;
    private Context mContext;
    private Paint mPaint;
    private Paint mPaintOut;
    private int mBBoxSpace;
    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private RectF tempUndoBBox;
    static private float mCtlPtLineWidth = 2;
    static private float mCtlPtRadius = 5;////
    static private float mCtlPtTouchExt = 20;
    static private float mCtlPtDeltyXY = 20;

    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_T = 2;
    public static final int CTR_RT = 3;
    public static final int CTR_R = 4;
    public static final int CTR_RB = 5;
    public static final int CTR_B = 6;
    public static final int CTR_LB = 7;
    public static final int CTR_L = 8;
    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_T = 2;
    public static final int OPER_SCALE_RT = 3;
    public static final int OPER_SCALE_R = 4;
    public static final int OPER_SCALE_RB = 5;
    public static final int OPER_SCALE_B = 6;
    public static final int OPER_SCALE_LB = 7;
    public static final int OPER_SCALE_L = 8;
    public static final int OPER_TRANSLATE = 9;
    private int mLastOper = OPER_DEFAULT;

    private Paint mCtlPtPaint;
    private Paint mFrmPaint;

    private PointF mPdfPointF = new PointF(0, 0);
    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);

    private float mThickness = 0f;

    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    StampAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mPaintOut.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuItems = new ArrayList<Integer>();

        mCtlPtPaint = new Paint();
        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }


    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    @Override
    public int getType() {
        return Annot.e_annotStamp;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }


    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return annot.getRect();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF bbox = getAnnotBBox(annot);
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bbox.contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            tempUndoBBox = annot.getRect();
            mBitmapAnnot = annot;
            RectF _rect = annot.getRect();
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);

            mAnnotMenu.dismiss();
            mMenuItems.clear();
            if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            } else {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    mAnnotMenu.dismiss();
                    if (btType == AnnotMenu.AM_BT_COMMENT) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, annot);
                    } else if (btType == AnnotMenu.AM_BT_REPLY) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, annot);
                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
                        delAnnot(annot, null);
                    }
                }
            });
            RectF viewRect = new RectF(annot.getRect());
            RectF modifyRectF = new RectF(viewRect);
            int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
            mAnnotMenu.show(viewRect);

            // change modify status
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
                AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                            mBitmapAnnot = annot;
                        }
                    }
                });
                new Thread(annotEventTask).start();

            } else {
                mBitmapAnnot = annot;
            }
            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        mAnnotMenu.dismiss();
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            if (page != null) {
                RectF pdfRect = annot.getRect();
                RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                if (mIsModify && reRender) {
                    if (tempUndoBBox.equals(annot.getRect())) {
                        modifyAnnot(pageIndex, annot, annot.getRect(), annot.getBorderInfo().getWidth(), false, null);
                    } else {
                        modifyAnnot(pageIndex, annot, annot.getRect(), annot.getBorderInfo().getWidth(), true, null);
                    }
                } else if (mIsModify) {
                    annot.move(tempUndoBBox);
                    annot.resetAppearanceStream();
                }
                mIsModify = false;
                if (mPdfViewCtrl.isPageVisible(pageIndex) && reRender) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            mBitmapAnnot = null;
                        }
                    });
                    new Thread(annotEventTask).start();
                    return;
                }
            }
            mBitmapAnnot = null;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, Event.Callback result) {
        if (mToolHandler != null) {
            mToolHandler.addAnnot(pageIndex, content, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();

            RectF bbox = annot.getRect();
            float lineWidth = annot.getBorderInfo().getWidth();

            tempUndoBBox = annot.getRect();

            if (content.getBBox() != null)
                bbox = content.getBBox();

            if (content.getLineWidth() != 0)
                lineWidth = content.getLineWidth();


            modifyAnnot(pageIndex, annot, bbox, lineWidth, true, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    public void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, float lineWidth, boolean isModifyJni, final Event.Callback result) {

        if (isModifyJni) {
            AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF rectF = new RectF(0, 0, mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
                    }
                    if (result != null)
                        result.result(null, false);
                }
            });

            new Thread(annotEventTask).start();
        }

        try {
            annot.setFlags(annot.getFlags());
            annot.move(bbox);
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.resetAppearanceStream();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(annot.getPage(), annot);
            mIsModify = true;

            // step 3: update pageview
            if (!isModifyJni) {
                RectF annotRectF = annot.getRect();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private RectF mThicknessRectF = new RectF();

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
        delAnnot(annot, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {

        PointF point = new PointF(e.getX(), e.getY());
//        PointF pageViewPt = new PointF(point.x, point.y);
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        float envX = point.x;
        float envY = point.y;
        int action = e.getAction();
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, e);
            if (annot == null || !(annot instanceof Stamp))
                annot = page.getAnnotAtPos(pointF, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        if (pageIndex == annot.getPage().getIndex()) {
                            RectF pageViewBBox = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            RectF pdfRect = annot.getRect();
                            mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
                            mCurrentCtr = isTouchControlPoint(pageViewBBox, envX, envY);
                            mDownPoint.set(envX, envY);
                            mLastPoint.set(envX, envY);

                            if (mCurrentCtr == CTR_LT) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_LT;
                                return true;
                            } else if (mCurrentCtr == CTR_T) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_T;
                                return true;
                            } else if (mCurrentCtr == CTR_RT) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_RT;
                                return true;
                            } else if (mCurrentCtr == CTR_R) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_R;
                                return true;
                            } else if (mCurrentCtr == CTR_RB) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_RB;
                                return true;
                            } else if (mCurrentCtr == CTR_B) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_B;
                                return true;
                            } else if (mCurrentCtr == CTR_LB) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_LB;
                                return true;
                            } else if (mCurrentCtr == CTR_L) {
                                mTouchCaptured = true;
                                mLastOper = OPER_SCALE_L;
                                return true;
                            } else if (isHitAnnot(annot, point)) {
                                mTouchCaptured = true;
                                mLastOper = OPER_TRANSLATE;
                                return true;
                            }
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                        if (envX != mLastPoint.x && envY != mLastPoint.y) {
                            RectF pageViewBBox = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(envX - mDownPoint.x, envY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(mAnnotMenuRect);
                                    }
                                    mLastPoint.set(envX, envY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(envX, envY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, envY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, envY, envX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, envX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, envX, envY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, envY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_DEFAULT:
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                default:
                                    break;
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = annot.getRect();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth = annot.getBorderInfo().getWidth();
                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                            RectF bboxRect = new RectF();
                            mPdfViewCtrl.convertPageViewRectToPdfRect(viewDrawBox, bboxRect, pageIndex);
                            modifyAnnot(pageIndex, annot, bboxRect, _lineWidth, true, null);

                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);

                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        } else {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth = annot.getBorderInfo().getWidth();
                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        }
                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastOper = OPER_DEFAULT;
                        mCurrentCtr = CTR_NONE;
                        return true;
                    }
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    mTouchCaptured = false;
                    return false;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());//display view

            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
            mPdfPointF.set(point.x, point.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(mPdfPointF, mPdfPointF, pageIndex);

            if (annot == null || !(annot instanceof Stamp)) {
                annot = page.getAnnotAtPos(mPdfPointF, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
            }

            if (annot == null) return false;
            mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            RectF _rect = annot.getRect();
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, point)) {
                    return true;
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    return true;
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p4 = new PointF(mMapBounds.right, (mMapBounds.bottom + mMapBounds.top) / 2);
        PointF p5 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p6 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.bottom);
        PointF p7 = new PointF(mMapBounds.left, mMapBounds.bottom);
        PointF p8 = new PointF(mMapBounds.left, (mMapBounds.bottom + mMapBounds.top) / 2);

        return new PointF[]{p1, p2, p3, p4, p5, p6, p7, p8};
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            int ctl = Color.parseColor("#179CD8");
            mCtlPtPaint.setColor(ctl);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    Path mImaginaryPath = new Path();

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);

    }

    private void drawControlImaginary(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        int selectRect = Color.parseColor("#179CD8");
        mFrmPaint.setColor(selectRect);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x + mCtlPtRadius, ctlPts[1].y, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y);
        pathAddLine(mImaginaryPath, ctlPts[2].x, ctlPts[2].y + mCtlPtRadius, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y + mCtlPtRadius, ctlPts[4].x, ctlPts[4].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[4].x - mCtlPtRadius, ctlPts[4].y, ctlPts[5].x + mCtlPtRadius, ctlPts[5].y);
        pathAddLine(mImaginaryPath, ctlPts[5].x - mCtlPtRadius, ctlPts[5].y, ctlPts[6].x + mCtlPtRadius, ctlPts[6].y);
        pathAddLine(mImaginaryPath, ctlPts[6].x, ctlPts[6].y - mCtlPtRadius, ctlPts[7].x, ctlPts[7].y + mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[7].x, ctlPts[7].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mFrmPaint);

    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof Stamp)) {
            return;
        }

        try {
            int annotPageIndex = annot.getPage().getIndex();

            if (mBitmapAnnot == annot && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                RectF frameRectF = new RectF();
                RectF rect2 = annot.getRect();
                float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, pageIndex);
                rect2.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setStrokeWidth(LineWidth2PageView(pageIndex, 0.6f));
                int color = Color.parseColor("#4EA984");
                mPaint.setColor(color);
                frameRectF.set(rect2.left - mBBoxSpace, rect2.top - mBBoxSpace, rect2.right + mBBoxSpace, rect2.bottom + mBBoxSpace);
                mPaintOut.setColor(color);
                RectF _rect = annot.getRect();
                mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);
                if (mLastOper == OPER_SCALE_LT) {// SCALE
                    mBBoxInOnDraw.set(mLastPoint.x, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_T) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RT) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_R) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RB) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_B) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_LB) {
                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_L) {
                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                }
                mBBoxInOnDraw.inset(-thickness / 2f, -thickness / 2f);
                if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                    mBBoxInOnDraw = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);

                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mBBoxInOnDraw.offset(dx, dy);
                }
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    drawControlPoints(canvas, mBBoxInOnDraw, (int) annot.getBorderColor());
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw, (int) annot.getBorderColor());
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private StampToolHandler mToolHandler;

    public void setToolHandler(StampToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

    private void delAnnot(final Annot annot, final Event.Callback result) {
        // step 1 : set current annot to null
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }
        try {
            final PDFPage page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            RectF viewRect = annot.getRect();
            ((Markup) annot).removeAllReplies();
            page.removeAnnot(annot);

            int pageIndex = page.getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
            }

            if (result != null) {
                result.result(null, true);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private float LineWidth2PageView(int pageIndex, float linewidth) {
        RectF rectF = new RectF(0, 0, linewidth, linewidth);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }
        // must strong to int,In order to solve the conversion error (pageView to Doc)
        if ((int) rectF.left < dxy) {
            adjustx = -rectF.left + dxy;
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            adjusty = -rectF.top + dxy;
            rectF.top = dxy;
        }

        if ((int) rectF.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
            rectF.right = mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
            rectF.bottom = mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
        mAdjustPointF.set(adjustx, adjusty);
        return mAdjustPointF;
    }

    private int isTouchControlPoint(RectF rect, float x, float y) {
        PointF[] ctlPts = calculateControlPoints(rect);
        RectF area = new RectF();
        int ret = -1;
        for (int i = 0; i < ctlPts.length; i++) {
            area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = i + 1;
            }
        }
        return ret;
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);
    protected void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this) {
            try {
                int annotPageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float thickness = thicknessOnPageView(annotPageIndex, curAnnot.getBorderInfo().getWidth());
                    RectF _rect = curAnnot.getRect();
                    mViewDrawRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
                    mViewDrawRect.inset(thickness / 2f, thickness / 2f);
                    if (mLastOper == OPER_SCALE_LT) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_T) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RT) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_R) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RB) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_B) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_LB) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_L) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    }
                    mDocViewerBBox.inset(-thickness / 2f, -thickness / 2f);
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {
                        mDocViewerBBox = curAnnot.getRect();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;
                        mDocViewerBBox.offset(dx, dy);
                    }
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                    mAnnotMenu.update(mDocViewerBBox);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }
}
