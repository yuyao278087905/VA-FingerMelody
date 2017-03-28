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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.common.PDFPath;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AbstractAnnotHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


class InkAnnotHandler extends AbstractAnnotHandler {
    protected InkToolHandler mToolHandler;
    protected InkAnnotUtil mUtil;
    protected ArrayList<Integer> mMenuText;
    protected String mSubject = "Pencil";

    public InkAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, InkToolHandler toolHandler, InkAnnotUtil util) {
        super(context, parent, pdfViewCtrl, Annot.e_annotInk);
        mToolHandler = toolHandler;
        mColor = mToolHandler.getColor();
        mOpacity = mToolHandler.getOpacity();
        mThickness = mToolHandler.getThickness();
        mUtil = util;
        mMenuText = new ArrayList<Integer>();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected AbstractToolHandler getToolHandler() {
        return mToolHandler;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            mColor = (int) annot.getBorderColor();
            mOpacity = AppDmUtil.opacity255To100((int) (((Ink) annot).getOpacity() * 255f + 0.5f));
            mThickness = annot.getBorderInfo().getWidth();
            super.onAnnotSelected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (!mIsModified) {
            super.onAnnotDeselected(annot, reRender);
        } else {
            modifyAnnot(mSelectedAnnot, mBackRect, mThickness, reRender, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mSelectedAnnot != DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        resetStatus();
                    }
                }
            });
            dismissPopupMenu();
            hidePropertyBar();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, final Event.Callback result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            InkAnnotContent inkAnnotContent = (InkAnnotContent) content;
            final Ink annot = (Ink) page.addAnnot(Annot.e_annotInk, inkAnnotContent.getBBox());
            annot.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setUniqueID(inkAnnotContent.getNM());
            annot.setBorderColor(inkAnnotContent.getColor());
            annot.setOpacity(inkAnnotContent.getOpacity() / 255f);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(inkAnnotContent.getLineWidth());
            annot.setBorderInfo(borderInfo);
            annot.setTitle(AppDmUtil.getAnnotAuthor());

            ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
            if (lines != null) {
                PDFPath path = PDFPath.create();
                for (int i = 0; i < lines.size(); i++) {
                    ArrayList<PointF> line = lines.get(i);
                    for (int j = 0; j < line.size(); j++) {
                        if (j == 0) {
                            path.moveTo(line.get(j));
                        } else {
                            path.lineTo(line.get(j));
                        }
                    }
                }
                annot.setInkList(path);
            }
            annot.resetAppearanceStream();
            addAnnot(pageIndex, annot, new IAnnotTaskResult<PDFPage, Annot, Void>() {
                        public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                            if (result != null) {
                                result.result(null, true);
                            }
                        }
                    });
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected Annot addAnnot(int pageIndex, RectF bbox, int color, int opacity, float thickness,
                             ArrayList<ArrayList<PointF>> lines, IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Ink annot = (Ink) page.addAnnot(Annot.e_annotInk, bbox);

            annot.setBorderColor(color);
            annot.setOpacity(opacity / 255f);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(thickness);
            annot.setBorderInfo(borderInfo);
            annot.setFlags(Annot.e_annotFlagPrint);
            annot.setSubject(mSubject);
            annot.setUniqueID(AppDmUtil.randomUUID(null));
            annot.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setTitle(AppDmUtil.getAnnotAuthor());

            PDFPath path = PDFPath.create();
            for (int li = 0; li < lines.size(); li++) { //li: line index
                ArrayList<PointF> line = lines.get(li);
                for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                    if (pi == 0) {
                        path.moveTo(line.get(pi));
                    } else {
                        path.lineTo(line.get(pi));
                    }
                }
            }
            annot.setInkList(path);
            annot.resetAppearanceStream();
            addAnnot(pageIndex, annot, result);
            return annot;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addAnnot(int pageIndex, Annot annot, IAnnotTaskResult<PDFPage, Annot, Void> result) {

        // event
        Event event = new Event() {
            public boolean isModifyDocument() {
                return true;
            }
        };

        handleAddAnnot(pageIndex, annot, event, result);
    }

    @Override
    public Annot handleAddAnnot(final int pageIndex, final Annot annot, Event event,
                                final IAnnotTaskResult<PDFPage, Annot, Void> result) {
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

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, final Event.Callback result) {
        try {
            RectF oldBBox = annot.getRect();
            float oldThickness = annot.getBorderInfo().getWidth();

            annot.setUniqueID(content.getNM());
            annot.setBorderColor(content.getColor());
            BorderInfo borderInfo = annot.getBorderInfo();
            borderInfo.setWidth(content.getLineWidth());
            annot.setBorderInfo(borderInfo);
            ((Ink) annot).setOpacity(content.getOpacity() / 255f);
            annot.setModifiedDateTime(content.getModifiedDate());
            ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
            if (lines != null) {
                PDFPath path = PDFPath.create();

                for (int i = 0; i < lines.size(); i++) {
                    ArrayList<PointF> line = lines.get(i);
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

            handleModifyAnnot(annot, oldBBox, oldThickness, true,
                    new IAnnotTaskResult<PDFPage, Annot, Void>() {
                        @Override
                        public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                            if (result != null) {
                                result.result(null, success);
                            }
                        }
                    });
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void modifyAnnot(Annot annot, RectF oldBBox, float oldThickness, boolean reRender,
                               final Event.Callback result) {
        handleModifyAnnot(annot, oldBBox, oldThickness, reRender,
                new IAnnotTaskResult<PDFPage, Annot, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    public void removeAnnot(Annot annot, final Event.Callback result) {
        handleRemoveAnnot(annot,
                new IAnnotTaskResult<PDFPage, Void, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Void p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
        return InkAnnotUtil.generatePathData(mPdfViewCtrl, pageIndex, (Ink) annot);
    }

    @Override
    protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
        RectF bbox = getBBox(pdfViewCtrl, annot);
        matrix.mapRect(bbox);
        pdfViewCtrl.convertPageViewRectToPdfRect(bbox, bbox, pageIndex);
        try {
            annot.move(bbox);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void resetStatus() {
        mBackRect = null;
        mBackThickness = 0.0f;
        mSelectedAnnot = null;
        mIsModified = false;
    }

    @Override
    protected void showPopupMenu() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null) return;
        try {
            if (curAnnot.getType() != Annot.e_annotInk)
                return;

            reloadPopupMenuString();
            mAnnotMenu.setMenuItems(mMenuText);
            RectF bbox = curAnnot.getRect();
            int pageIndex = curAnnot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            mAnnotMenu.show(bbox);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int flag) {
                    if (mSelectedAnnot == null) return;
                    if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_DELETE) { // delete
                        if (mSelectedAnnot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                            removeAnnot(mSelectedAnnot, null);
                        }
                    } else if (flag == AnnotMenu.AM_BT_STYLE) { // line color
                        dismissPopupMenu();
                        showPropertyBar(PropertyBar.PROPERTY_COLOR);
                    }
                }
            });
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void dismissPopupMenu() {
        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
    }

    @Override
    protected void showPropertyBar(long curProperty) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        if (!(annot instanceof Ink)) return;
        long properties = getSupportedProperties();

        mPropertyBar.setPropertyChangeListener(this);
        setPropertyBarProperties(mPropertyBar);
        mPropertyBar.reset(properties);

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

    @Override
    public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
        super.setPaintProperty(pdfViewCtrl, pageIndex, paint, annot);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Style.STROKE);

    }

    @Override
    protected long getSupportedProperties() {
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        super.setPropertyBarProperties(propertyBar);
    }

    protected void reloadPopupMenuString() {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
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

}