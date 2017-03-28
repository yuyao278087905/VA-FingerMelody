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
package com.foxit.uiextensions.annots.link;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.action.Action;
import com.foxit.sdk.pdf.action.Destination;
import com.foxit.sdk.pdf.action.GotoAction;
import com.foxit.sdk.pdf.action.URIAction;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Link;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

class LinkAnnotHandler implements AnnotHandler {
    protected Context mContext;
    protected boolean isDocClosed = false;
    private Paint mPaint;
    private final int mType;
    private PDFViewCtrl mPdfViewCtrl;
    class LinkInfo {
        PDFPage page;
        ArrayList<Link> links;
    }

    protected SparseArray<LinkInfo> mLinkInfo = new SparseArray<LinkInfo>();

    LinkAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
        mPaint.setARGB(0x16, 0x0, 0x7F, 0xFF);
        mType = Annot.e_annotLink;
    }

    private boolean isLoadLink(int pageIndex) {
        if (mLinkInfo.size() == 0) return false;
        if (mLinkInfo.keyAt(0) == pageIndex) return true;
        return false;
    }

    private void loadLinks(int pageIndex) {
        try {
            if (mPdfViewCtrl.getDoc() == null) return;
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int count = page.getAnnotCount();
            ArrayList<Link> links = new ArrayList<Link>();
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = page.getAnnot(i);
                if (annot == null) continue;
                if (annot.getType() == Annot.e_annotLink) {
                    links.add((Link) annot);
                }
            }

            clear();

            LinkInfo linkInfo = new LinkInfo();
            linkInfo.page = page;
            linkInfo.links = links;

            mLinkInfo.put(page.getIndex(), linkInfo);
        } catch (PDFException e) {
            e.printStackTrace();
        }
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
        return getAnnotBBox(annot).contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {

    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, Event.Callback result) {

    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {

    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    private PointF getDestinationPoint(Destination destination) {
        if (destination == null) {
            return null;
        }

        PointF pt = new PointF(0, 0);
        try {
            switch (destination.getZoomMode()) {
                case CommonDefines.e_zoomXYZ:
                    pt.x = destination.getLeft();
                    pt.y = destination.getTop();
                    break;
                case CommonDefines.e_zoomFitHorz:
                case CommonDefines.e_zoomFitBHorz:
                    pt.y = destination.getTop();
                    break;
                case CommonDefines.e_zoomFitVert:
                case CommonDefines.e_zoomFitBVert:
                    pt.x = destination.getLeft();
                    break;
                case CommonDefines.e_zoomFitRect:
                    pt.x = destination.getLeft();
                    pt.y = destination.getBottom();
                    break;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return pt;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (isDocClosed) return;
        if (!isLoadLink(pageIndex)) {
            loadLinks(pageIndex);
        }

        LinkInfo linkInfo = mLinkInfo.get(pageIndex);
        if (linkInfo.links.size() == 0) return;

        canvas.save();
        Rect clipRect = canvas.getClipBounds();
        Rect rect = new Rect();
        try {
            int count = linkInfo.links.size();
            for (int i = 0; i < count; i++) {
                Annot annot = linkInfo.links.get(i);
                RectF rectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                rectF.round(rect);
                if (rect.intersect(clipRect)) {
                    canvas.drawRect(rect, mPaint);
                }
            }
            canvas.restore();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).isLinksEnabled())
            return false;

        if (pageIndex != mPdfViewCtrl.getCurrentPage()) return false;

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PointF pdfPt = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            Annot annot = page.getAnnotAtPos(pdfPt, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
            if (annot == null || !(annot instanceof Link)) {
                return false;
            }
            Action annotAction = ((Link) annot).getAction();
            if (annotAction == null) {
                return false;
            }
            int type = annotAction.getType();
            switch (type) {
                case Action.e_actionTypeGoto:
                    GotoAction gotoAction = (GotoAction) annotAction;
                    Destination destination = gotoAction.getDestination();
                    if (destination == null) return false;
                    PointF destPt = getDestinationPoint(destination);
                    PointF devicePt = new PointF();
                    if (!mPdfViewCtrl.convertPdfPtToPageViewPt(destPt, devicePt, destination.getPageIndex())) {
                        devicePt.set(0, 0);
                    }
                    mPdfViewCtrl.gotoPage(destination.getPageIndex(), devicePt.x, devicePt.y);
                    break;
                case Action.e_actionTypeURI:
                    URIAction uriAction = (URIAction) annotAction;
                    String uri = uriAction.getURI();
                    if (uri.toLowerCase().startsWith("mailto:")) {
                        AppUtil.mailTo((Activity) mContext, uri);
                    } else {
                        AppUtil.openUrl((Activity) mContext, uri);
                    }
                    break;
                case Action.e_actionTypeUnknown:
                    return false;
            }

            mPdfViewCtrl.getDoc().closePage(pageIndex);
        } catch (PDFException e1) {
            if (e1.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            e1.printStackTrace();
            return true;
        }

        return true;
    }

    protected void clear() {
        synchronized (mLinkInfo) {
            if (mLinkInfo.size() > 0) {
                LinkInfo linkInfo = mLinkInfo.valueAt(0);
                try {
                    mPdfViewCtrl.getDoc().closePage(linkInfo.page.getIndex());
                } catch (PDFException e) {
                    e.printStackTrace();
                }
                linkInfo.page = null;
                linkInfo.links.clear();
                mLinkInfo.clear();
            }
        }
    }
}
