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
package com.foxit.uiextensions.textselect;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;

public class TextSelectModule implements Module {

    private TextSelectToolHandler mToolHandler;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    public TextSelectModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_SELECTION;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new TextSelectToolHandler(mContext, mParent, mPdfViewCtrl);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(mRecoveryEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(mRecoveryEventListener);
        mToolHandler.uninit();
        return true;
    }

    public void triggerDismissMenu() {
        if (mToolHandler != null) mToolHandler.dismissMenu();
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mToolHandler.onDrawForAnnotMenu(canvas);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int err) {
            if (err != PDFException.e_errSuccess)
                return;
            mToolHandler.mIsEdit = false;
            mToolHandler.mIsMenuShow = false;
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int err) {
            if (err != PDFException.e_errSuccess)
                return;
            mToolHandler.mSelectInfo.clear();
            mToolHandler.mAnnotationMenu.dismiss();
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {
        }
    };

    PDFViewCtrl.IRecoveryEventListener mRecoveryEventListener = new PDFViewCtrl.IRecoveryEventListener(){

        @Override
        public void onWillRecover() {
            if (mToolHandler.getAnnotationMenu() != null && mToolHandler.getAnnotationMenu().isShowing()) {
                mToolHandler.getAnnotationMenu().dismiss();
            }

            if (mToolHandler.getSelectInfo() != null) {
                mToolHandler.getSelectInfo().clear();
            }
        }

        @Override
        public void onRecovered() {

        }
    };
}
