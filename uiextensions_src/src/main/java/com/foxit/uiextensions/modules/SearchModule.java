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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;

public class SearchModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private SearchView mSearchView = null;

    public SearchModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        if (context == null || parent == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean loadModule() {
        mSearchView = new SearchView(mContext, mParent, mPdfViewCtrl);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_SEARCH;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if (mSearchView.mIsCancel) {
                return;
            }

            if (mSearchView.mRect == null || mSearchView.mPageIndex == -1) {
                return;
            }

            if (mSearchView.mPageIndex == pageIndex) {
                if (mSearchView.mRect.size() > 0) {
                    Paint paint = new Paint();
                    paint.setARGB(150, 23, 156, 216);
                    for (int i = 0; i < mSearchView.mRect.size(); i++) {
                        RectF rectF = new RectF(mSearchView.mRect.get(i));
                        RectF deviceRect = new RectF();
                        if (mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, deviceRect, mSearchView.mPageIndex)) {
                            canvas.drawRect(deviceRect, paint);
                        }
                    }
                }
            }
        }
    };

    PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int i) {

        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {
            mSearchView.onDocumentClosed();
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {

        }
    };

    public boolean onKeyBack() {
        if (mSearchView.getView().getVisibility() == View.VISIBLE) {
            mSearchView.cancel();
            return true;

        }
        return false;
    }
}
