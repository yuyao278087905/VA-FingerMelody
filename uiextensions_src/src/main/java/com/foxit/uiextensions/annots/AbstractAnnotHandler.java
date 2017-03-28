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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

import static com.foxit.uiextensions.utils.AppAnnotUtil.ANNOT_SELECT_TOLERANCE;

public abstract class AbstractAnnotHandler implements AnnotHandler, PropertyBar.PropertyChangeListener {
    protected Context mContext;
    protected AnnotMenu mAnnotMenu;
    protected PropertyBar mPropertyBar;
    protected int mType;
    protected int mColor;
    protected int mOpacity;
    protected float mThickness;

    protected Paint mPaint;
    protected  RectF mBackRect;
    protected  float mBackThickness;
    protected Annot mSelectedAnnot;
    protected boolean mIsModified;
    protected boolean mTouchCaptured;
    protected int mOp;
    protected int mCtl;
    protected PointF mDownPt;
    protected PointF mLastPt;
    private Rect tv_rect1 = new Rect();

    protected PDFViewCtrl mPdfViewCtrl;
    protected ViewGroup mParent;

    public AbstractAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, int type) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mType = type;

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mDownPt = new PointF();
        mLastPt = new PointF();
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
        if (mSelectedAnnot != null) {
            try {
                mSelectedAnnot.setBorderColor(color);
                mSelectedAnnot.resetAppearanceStream();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mIsModified = true;
            invalidatePageView(mSelectedAnnot, 0, 0);
        }
    }

    public int getOpacity() {
        return mOpacity;
    }

    public void setOpacity(int opacity) {
        mOpacity = opacity;
        if (mSelectedAnnot != null) {
            try {
                ((Markup) mSelectedAnnot).setOpacity(AppDmUtil.opacity100To255(opacity) / 255f);
                mSelectedAnnot.resetAppearanceStream();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mIsModified = true;
            invalidatePageView(mSelectedAnnot, 0, 0);
        }
    }

    public float getThickness() {
        return mThickness;
    }

    public void setThickness(float thickness) {
        mThickness = thickness;
        if (mSelectedAnnot != null) {
            try {
                BorderInfo borderInfo = mSelectedAnnot.getBorderInfo();
                float dt = (thickness - borderInfo.getWidth()) / 2;
                mSelectedAnnot.getRect().inset(-dt, -dt);
                borderInfo.setWidth(thickness);
                mSelectedAnnot.setBorderInfo(borderInfo);
                mSelectedAnnot.resetAppearanceStream();
                mIsModified = true;
                if (dt > 0)
                    invalidatePageView(mSelectedAnnot, 0, 0);
                else
                    invalidatePageView(mSelectedAnnot, -dt + 1, -dt + 1);
            } catch (PDFException e) {

            }

        }
    }

    public String getFontName() {
        return null;
    }

    public void setFontName(String name) {
    }

    public float getFontSize() {
        return 0;
    }

    public void setFontSize(float size) {
    }

    @Override
    public void onValueChanged(long property, int value) {
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            setColor(value);
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            setOpacity(value);
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
        if (property == PropertyBar.PROPERTY_LINEWIDTH) {
            setThickness(value);
        }
    }

    @Override
    public void onValueChanged(long property, String value) {
    }

    @Override
    public int getType() {
        return mType;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        try {
            if (annot.getType() == mType) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
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
        try {
            int pageIndex = annot.getPage().getIndex();
            RectF rectF = getAnnotBBox(annot);
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
            return rectF.contains(point.x, point.y);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent supplier, Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (currentAnnotHandler == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        mIsModified = false;
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) {
                mBackRect = annot.getRect();
                mSelectedAnnot = annot;
            } else {
                RectF docBBox = annot.getRect();
                RectF pvBBox = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(docBBox, pvBBox, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pvBBox));
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mBackRect = annot.getRect();
                    mSelectedAnnot = annot;
                }
            }
            showPopupMenu();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) {
                resetStatus();
            } else {
                RectF bounds = UIAnnotFrame.calculateBounds(mPdfViewCtrl, pageIndex, annot);
                Rect rect = new Rect();
                bounds.roundOut(rect);

                if (!reRender) {
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(AppDmUtil.rectToRectF(rect), AppDmUtil.rectToRectF(rect), pageIndex);
                    mPdfViewCtrl.invalidate(rect);
                    resetStatus();
                } else {
                    mPdfViewCtrl.refresh(pageIndex, rect);
                    if (mSelectedAnnot != DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        resetStatus();
                    }
                }
            }

            dismissPopupMenu();
            hidePropertyBar();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (annot == null) {
                PointF pdfPt = new PointF(point.x, point.y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPt, pdfPt, pageIndex);
                annot = page.getAnnotAtPos(pdfPt, ANNOT_SELECT_TOLERANCE);
            }

            int action = e.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (pageIndex == annot.getPage().getIndex()
                            && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        RectF bounds = UIAnnotFrame.calculateBounds(mPdfViewCtrl, pageIndex, annot);
                        mCtl = UIAnnotFrame.getInstance(mContext).hitControlTest(bounds, point);
                        if (mCtl != UIAnnotFrame.CTL_NONE) {
                            mTouchCaptured = true;
                            mOp = UIAnnotFrame.OP_SCALE;
                            mDownPt.set(point);
                            mLastPt.set(point);
                            return true;
                        } else {
                            if (isHitAnnot(annot, point)) {
                                mTouchCaptured = true;
                                mOp = UIAnnotFrame.OP_TRANSLATE;
                                mDownPt.set(point);
                                mLastPt.set(point);
                                return true;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
                            && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                mTouchCaptured = false;
                                mDownPt.set(0, 0);
                                mLastPt.set(0, 0);
                                mOp = UIAnnotFrame.OP_DEFAULT;
                                mCtl = UIAnnotFrame.CTL_NONE;
                                if (mSelectedAnnot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                                    RectF bbox = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                                    mAnnotMenu.show(bbox);
                                }
                            }
                            return true;
                        } else {
                            if (point.x != mLastPt.x || point.y != mLastPt.y) {
                                if (mAnnotMenu.isShowing()) {
                                    mAnnotMenu.dismiss();
                                }
                                RectF bounds0 = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
                                        mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                                RectF bounds1 = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
                                        point.x - mDownPt.x, point.y - mDownPt.y);
                                PointF adjust = UIAnnotFrame.getInstance(mContext).calculateCorrection(mPdfViewCtrl, pageIndex, bounds1, mOp, mCtl);
                                UIAnnotFrame.adjustBounds(bounds1, mOp, mCtl, adjust);
                                mLastPt.set(point.x + adjust.x, point.y + adjust.y);
                                bounds1.union(bounds0);
                                UIAnnotFrame.getInstance(mContext).extentBoundsToContainControl(bounds1);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bounds1, bounds1, pageIndex);
                                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(bounds1));
                            }
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                if (!mLastPt.equals(mDownPt)) {
                                    RectF bbox = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                                    Matrix matrix = UIAnnotFrame.calculateOperateMatrix(bbox, mOp, mCtl,
                                            mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                                    transformAnnot(mPdfViewCtrl, pageIndex, annot, matrix);
                                    mIsModified = true;
                                }
                                mTouchCaptured = false;
                                mDownPt.set(0, 0);
                                mLastPt.set(0, 0);
                                mOp = UIAnnotFrame.OP_DEFAULT;
                                mCtl = UIAnnotFrame.CTL_NONE;
                                if (mSelectedAnnot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                                    RectF bbox = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                                    mAnnotMenu.show(bbox);
                                }
                            }
                        }

                        return true;
                    }
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    private boolean onSingleTapOrLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot == null) {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                annot = page.getAnnotAtPos(pdfPoint, ANNOT_SELECT_TOLERANCE);
            }
            if (annot != DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
            } else {

                if (pageIndex == annot.getPage().getIndex()
                        && isHitAnnot(annot, point)) {
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    return true;
                }

            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return onSingleTapOrLongPress(pageIndex, motionEvent);
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return onSingleTapOrLongPress(pageIndex, motionEvent);
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot == null || annot.getType() != mType)
                return;
            if (mSelectedAnnot == annot && annot.getPage().getIndex() == pageIndex) {
                RectF bbox = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                Matrix matrix = UIAnnotFrame.calculateOperateMatrix(bbox, mOp, mCtl,
                        mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                RectF mapBounds = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
                        mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);

                ArrayList<Path> paths = generatePathData(mPdfViewCtrl, pageIndex, mSelectedAnnot);
                for (int i = 0; i < paths.size(); i++) {
                    paths.get(i).transform(matrix);
                    setPaintProperty(mPdfViewCtrl, pageIndex, mPaint, mSelectedAnnot);
                    canvas.drawPath(paths.get(i), mPaint);
                }

                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    int color = (int) annot.getBorderColor();
                    int opacity = (int) (((Markup)annot).getOpacity() * 255f);
                    UIAnnotFrame.getInstance(mContext).draw(canvas, mapBounds, color, opacity);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this) {

            try {
                int pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    RectF bbox = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                    mAnnotMenu.update(bbox);
                    if (mPropertyBar.isShowing()) {
                        mPropertyBar.update(bbox);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }

        }
    }

    protected RectF getBBox(PDFViewCtrl pdfViewCtrl, Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            RectF bbox = annot.getRect();
            pdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            return bbox;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void invalidatePageView(Annot annot, float ddx, float ddy) {

        try {
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF bounds = UIAnnotFrame.calculateBounds(mPdfViewCtrl, pageIndex, annot);
                UIAnnotFrame.getInstance(mContext).extentBoundsToContainControl(bounds);
                ddx = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, ddx);
                ddy = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, ddy);
                bounds.inset(-ddx - 5, -ddy - 5);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bounds, bounds, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(bounds));
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public Annot handleAddAnnot(final int pageIndex, final Annot annot, Event event, final IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            final PDFPage page = annot.getPage();
            //step 2: add annot to java
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);

            AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    //stop 3: invalidate page view for modify
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF pvRect = getBBox(mPdfViewCtrl, annot);
                        final Rect tv_rect1 = new Rect();
                        pvRect.roundOut(tv_rect1);
                        mPdfViewCtrl.refresh(pageIndex, tv_rect1);
                    }
                    if (result != null) {
                        result.onResult(true, page, annot, null);
                    }
                }
            });
            new Thread(annotEventTask).start();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return annot;
    }

    protected void handleModifyAnnot(Annot annot, RectF oldBBox, float oldThickness, final boolean reRender,
                                     final IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(page, annot);
            if (!reRender || !mPdfViewCtrl.isPageVisible(pageIndex)) {
                if (result != null) {
                    result.onResult(true, page, annot, null);
                }
            } else {
                RectF oldRect = UIAnnotFrame.calculateBounds(mPdfViewCtrl, pageIndex, oldBBox, oldThickness);
                RectF pvRect = UIAnnotFrame.calculateBounds(mPdfViewCtrl, pageIndex, annot);
                pvRect.union(oldRect);
                pvRect.roundOut(tv_rect1);
                mPdfViewCtrl.refresh(pageIndex, tv_rect1);

                if (result != null) {
                    result.onResult(true, null, null, null);
                }

            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void handleRemoveAnnot(Annot annot, final IAnnotTaskResult<PDFPage, Void, Void> result) {
        try {
            if (annot.getUniqueID() != null && annot.getUniqueID().length() > 0 && DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null
                    && annot.getUniqueID().equals(DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot().getUniqueID())) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            RectF pvRect = getBBox(mPdfViewCtrl, annot);
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            ((Markup)annot).removeAllReplies();
            page.removeAnnot(annot);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                pvRect.roundOut(tv_rect1);
                mPdfViewCtrl.refresh(pageIndex, tv_rect1);
            }

            if (result != null) {
                result.onResult(true, page, null, null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
        try {
            paint.setColor((int) annot.getBorderColor());
            int opacity = (int) (((Markup)annot).getOpacity() * 255f);
            paint.setAlpha(opacity);
            float lineWidth = annot.getBorderInfo().getWidth();
            paint.setStrokeWidth(UIAnnotFrame.getPageViewThickness(pdfViewCtrl, pageIndex, lineWidth));
        } catch (PDFException e) {

        }

    }

    protected void showPropertyBar(long curProperty) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot != null) {
            mPropertyBar.setPropertyChangeListener(this);
            setPropertyBarProperties(mPropertyBar);
            mPropertyBar.reset(getSupportedProperties());

            try {
                RectF bbox = annot.getRect();
                int pageIndex = annot.getPage().getIndex();
                mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                mPropertyBar.show(bbox, false);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    protected void hidePropertyBar() {
        if (mPropertyBar.isShowing()) {
            mPropertyBar.dismiss();
        }
    }

    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
        propertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getOpacity());
        propertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
        propertyBar.setArrowVisible(false);
    }

    protected abstract AbstractToolHandler getToolHandler();

    protected abstract ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot);

    protected abstract void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix);

    protected abstract void resetStatus();

    protected abstract void showPopupMenu();

    protected abstract void dismissPopupMenu();

    protected abstract long getSupportedProperties();
}
