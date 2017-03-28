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
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Square;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

//@SuppressLint("WrongCall")
public class SquareAnnotHandler implements AnnotHandler {

	/*
     *  LT     T     RT
	 *   1-----2-----3
	 *   |	         |
	 *   |	         |
	 * L 8           4 R
	 *   |           |
	 *   |           |
	 *   7-----6-----5
	 *   LB    B     RB
	 *   */

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

    static private float mCtlPtLineWidth = 2;
    static private float mCtlPtRadius = 5;
    static private float mCtlPtTouchExt = 20;
    static private float mCtlPtDeltyXY = 20;// Additional refresh range

    private Paint mPathPaint;
    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;

    private ArrayList<Integer> mMenuText;
    private AnnotMenu mAnnotationMenu;

    private Annot mBitmapAnnot;

    private PropertyBar mAnnotationProperty;
    private boolean mIsEditProperty;
    private boolean mIsModify;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    public SquareAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPathPaint = new Paint();
        mPathPaint.setStyle(Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Style.STROKE);
        mFrmPaint.setAntiAlias(true);

        mCtlPtPaint = new Paint();

        mMenuText = new ArrayList<Integer>();
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotationMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotationMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotationProperty = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotationProperty;
    }

    public void onColorValueChanged(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        try {
            if (annot != null && ToolUtil.getCurrentAnnotHandler(uiExtensionsManager) == this && color != annot.getBorderColor()) {
                modifyAnnot(annot.getPage().getIndex(), annot, annot.getRect(), color, (int) (((Square) annot).getOpacity() * 255f), annot.getBorderInfo().getWidth(), annot.getContent(), false, null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        try {
            if (annot != null && ToolUtil.getCurrentAnnotHandler(uiExtensionsManager) == this && opacity != (int) (((Square) annot).getOpacity() * 255f)) {
                modifyAnnot(annot.getPage().getIndex(), annot, annot.getRect(), (int) annot.getBorderColor(), AppDmUtil.opacity100To255(opacity), annot.getBorderInfo().getWidth(), annot.getContent(), false, null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onLineWidthValueChanged(float lineWidth) {

        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        try {
            if (annot != null && ToolUtil.getCurrentAnnotHandler(uiExtensionsManager) == this && lineWidth != annot.getBorderInfo().getWidth()) {
                RectF bboxRect = annot.getRect();
                float deltLineWidth = annot.getBorderInfo().getWidth() - lineWidth;
                modifyAnnot(annot.getPage().getIndex(), annot, bboxRect, (int) annot.getBorderColor(), (int) (((Square) annot).getOpacity() * 255f), lineWidth, annot.getContent(), false, null);

                if (mAnnotationMenu.isShowing()) {
                    RectF pageViewBBox = annot.getRect();
                    pageViewBBox.inset(deltLineWidth * 0.5f, deltLineWidth * 0.5f);

                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, annot.getPage().getIndex());
                    mAnnotationMenu.update(pageViewBBox);
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

    /**
     * reset mAnnotationMenu text
     */
    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();

        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
            mMenuText.add(AnnotMenu.AM_BT_REPLY);
            mMenuText.add(AnnotMenu.AM_BT_DELETE);
        } else {
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
        }
    }

    private void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, int color, int opacity, float lineWidth, String contents, boolean isModifyJni, final Event.Callback result) {// to be continued
        // step 1: modify annot in pdf
        if (isModifyJni) {
            RectF tempRectF = mTempLastBBox;
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF annotRectF = null;
                try {
                    annotRectF = annot.getRect();
                } catch (PDFException e) {
                    e.printStackTrace();
                }

                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                annotRectF.union(tempRectF);
                annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
            }
            if (result != null) {
                result.result(null, false);
            }
        }

        // step 2: modify annot in java
        try {
            annot.setBorderColor(color);
            ((Square) annot).setOpacity(opacity / 255f);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(lineWidth);
            annot.setBorderInfo(borderInfo);
            if (contents != null) {
                annot.setContent(contents);
            }

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
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void deleteAnnot(final Annot annot, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        try {
            final PDFPage page = annot.getPage();
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

    @Override
    public int getType() {
        return Annot.e_annotSquare;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        RectF rectF = null;
        try {
            rectF = annot.getRect();
        } catch (PDFException e) {

        }
        return rectF;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF rectF = null;
        try {
            rectF = annot.getRect();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
        } catch (PDFException e) {
            return false;
        }
        return rectF.contains(point.x, point.y);
    }

    private int mTempLastColor;
    private int mTempLastOpacity;
    private float mTempLastLineWidth;
    private RectF mTempLastBBox = new RectF();

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);
        // configure annotation menu

        try {
            mTempLastColor = (int) annot.getBorderColor();
            mTempLastOpacity = (int) (((Square) annot).getOpacity() * 255f + 0.5f);
            mTempLastBBox = annot.getRect();
            mTempLastLineWidth = annot.getBorderInfo().getWidth();
            RectF _rect = annot.getRect();
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            prepareAnnotMenu(annot);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotationMenu.show(menuRect);

            preparePropertyBar();

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }
            } else {
                mBitmapAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;
        // configure annotation menu
        mAnnotationMenu.setListener(null);
        mAnnotationMenu.dismiss();
        if (mIsEditProperty) {
            mIsEditProperty = false;
            mAnnotationProperty.dismiss();
        }
        if (mAnnotationProperty.isShowing()) {
            mAnnotationProperty.dismiss();
        }

        PDFPage page = null;
        try {
            page = annot.getPage();

            if (needInvalid && mIsModify) {
                // must calculate BBox again
                if (mTempLastColor == annot.getBorderColor() && mTempLastLineWidth == annot.getBorderInfo().getWidth() && mTempLastBBox.equals(annot.getRect()) && mTempLastOpacity == (int) (((Square) annot).getOpacity() * 255f)) {
                    modifyAnnot(page.getIndex(), annot, annot.getRect(), (int) annot.getBorderColor(), (int) (((Square) annot).getOpacity() * 255f), annot.getBorderInfo().getWidth(), annot.getContent(), false, null);
                } else {
                    modifyAnnot(page.getIndex(), annot, annot.getRect(), (int) annot.getBorderColor(), (int) (((Square) annot).getOpacity() * 255f), annot.getBorderInfo().getWidth(), annot.getContent(), true, null);
                }
            } else if (mIsModify) {
                annot.setBorderColor(mTempLastColor);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mTempLastLineWidth);
                annot.setBorderInfo(borderInfo);
                ((Square) annot).setOpacity(mTempLastOpacity / 255f);
                annot.move(mTempLastBBox);
            }

            RectF pdfRect = annot.getRect();
            RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);

            if (mPdfViewCtrl.isPageVisible(page.getIndex()) && needInvalid) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, page.getIndex());
                mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(viewRect));
            }

            mBitmapAnnot = null;
            mIsModify = false;

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void prepareAnnotMenu(final Annot annot) {
        resetAnnotationMenuResource(annot);

        mAnnotationMenu.setMenuItems(mMenuText);

        mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        deleteAnnot(annot, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_COMMENT) {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, annot);
                } else if (btType == AnnotMenu.AM_BT_REPLY) {

                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, annot);
                } else if (btType == AnnotMenu.AM_BT_STYLE) {

                    mAnnotationProperty.show(mDocViewerBBox, false);
                    mAnnotationMenu.dismiss();
                }
            }
        });
    }

    private void preparePropertyBar() {

        int[] colors = new int[PropertyBar.PB_COLORS_SQUARE.length];
        System.arraycopy(PropertyBar.PB_COLORS_SQUARE, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_SQUARE[0];
        mAnnotationProperty.setColors(colors);

        try {
            mAnnotationProperty.setProperty(PropertyBar.PROPERTY_COLOR, (int) DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot().getBorderColor());
            int opacity = AppDmUtil.opacity255To100((int) (((Square) DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()).getOpacity() * 255f + 0.5f));
            mAnnotationProperty.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
            mAnnotationProperty.setProperty(PropertyBar.PROPERTY_LINEWIDTH, DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot().getBorderInfo().getWidth());
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mAnnotationProperty.setArrowVisible(false);
        mAnnotationProperty.reset(getSupportedProperties());

        mAnnotationProperty.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, final Event.Callback result) {

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = page.addAnnot(Annot.e_annotSquare, contentSupplier.getBBox());
            annot.setUniqueID(contentSupplier.getNM());
            annot.setBorderColor(contentSupplier.getColor());
            ((Square) annot).setOpacity(contentSupplier.getOpacity() / 255f);
            if (contentSupplier.getLineWidth() != 0) {
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(contentSupplier.getLineWidth());
                annot.setBorderInfo(borderInfo);

            }
            ((Square) annot).setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

            annot.resetAppearanceStream();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF viewRect = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                Rect rect = new Rect();
                viewRect.roundOut(rect);
                rect.inset(-10, -10);
                mPdfViewCtrl.refresh(pageIndex, rect);
            }
            if (result != null)
                result.result(null, true);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
        deleteAnnot(annot, result);
    }

    /**
     * onTouchEvent pageView point to pdfPoint
     */
    private PointF mPdfPointF = new PointF(0, 0);
    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);

    private float mThickness = 0f;

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {
        // in pageView evX and evY

        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        int action = e.getAction();
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, e);
            if (annot == null || !(annot instanceof Square))
                annot = page.getAnnotAtPos(pointF, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);

            switch (action) {
                case MotionEvent.ACTION_DOWN:

                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                        RectF pageViewBBox = annot.getRect();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = annot.getRect();
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mDocViewerPt.set(e.getX(), e.getY());

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
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (pageIndex == annot.getPage().getIndex() && mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            RectF pageViewBBox = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mInvalidateRect.union(mAnnotMenuRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }
                                    if (mIsEditProperty) {
                                        mAnnotationProperty.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mAnnotationProperty.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;

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
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, (int) annot.getBorderColor(), (int) (((Square) annot).getOpacity() * 255f), _lineWidth, annot.getContent(), false, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (!mIsEditProperty) {
                                if (mAnnotationMenu.isShowing()) {
                                    mAnnotationMenu.update(viewDrawBox);
                                } else {
                                    mAnnotationMenu.show(viewDrawBox);
                                }
                            }
                        } else {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth = annot.getBorderInfo().getWidth();
                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotationMenu.isShowing()) {
                                mAnnotationMenu.update(viewDrawBox);
                            } else {
                                mAnnotationMenu.show(viewDrawBox);
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
            return false;

        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return onSingleTapOrLongPress(pageIndex, motionEvent);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return onSingleTapOrLongPress(pageIndex, motionEvent);
    }

    private boolean onSingleTapOrLongPress(int pageIndex, MotionEvent motionEvent) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();

            mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());//display view
            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
            mPdfPointF.set(point.x, point.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(mPdfPointF, mPdfPointF, pageIndex);

            if (annot == null || !(annot instanceof Square)) {
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

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof Square)) {
            return;
        }

        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (mBitmapAnnot == annot && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                mPathPaint.setColor((int) annot.getBorderColor());
                mPathPaint.setAlpha((int) (((Square) annot).getOpacity() * 255f));
                mPathPaint.setStrokeWidth(thickness);
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
                mBBoxInOnDraw.inset(thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth()) / 2f, thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth()) / 2f);// draw Square
                canvas.drawRect(mBBoxInOnDraw, mPathPaint);
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    public void onDrawForControls(Canvas canvas) {

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
                    mAnnotationMenu.update(mDocViewerBBox);
                    if (mAnnotationProperty.isShowing()) {
                        mAnnotationProperty.update(mDocViewerBBox);
                    }

                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

    }

    Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        mFrmPaint.setColor(color);
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

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);

    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setStyle(Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    /*
     *   1-----2-----3
     *   |	         |
     *   |	         |
     *   8           4
     *   |           |
     *   |           |
     *   7-----6-----5
     *   */
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

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }

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

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            RectF bbox = annot.getRect();
            int color = (int) annot.getBorderColor();
            float lineWidth = annot.getBorderInfo().getWidth();
            int opacity = (int) (((Square) annot).getOpacity() * 255f);
            String contents = annot.getContent();

            mTempLastColor = (int) annot.getBorderColor();
            mTempLastOpacity = (int) (((Square) annot).getOpacity() * 255f);
            mTempLastLineWidth = annot.getBorderInfo().getWidth();
            mTempLastBBox = annot.getRect();

            if (content.getBBox() != null)
                bbox = content.getBBox();
            if (content.getColor() != 0)
                color = content.getColor();
            if (content.getLineWidth() != 0)
                lineWidth = content.getLineWidth();
            if (content.getOpacity() != 0)
                opacity = content.getOpacity();
            if (content.getContents() != null)
                contents = content.getContents();

            modifyAnnot(pageIndex, annot, bbox, color, opacity, lineWidth, contents, true, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removePropertyListener() {
        mPropertyChangeListener = null;
    }
}
