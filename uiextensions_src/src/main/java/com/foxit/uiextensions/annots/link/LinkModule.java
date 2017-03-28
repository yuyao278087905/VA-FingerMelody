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

import android.content.Context;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;

public class LinkModule implements Module {
    private LinkAnnotHandler mAnnotHandler;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    public LinkModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_LINK;
    }

    public LinkAnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public boolean loadModule() {
        mAnnotHandler = new LinkAnnotHandler(mContext, mPdfViewCtrl);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(mRecoveryListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(mRecoveryListener);
        return true;
    }


    PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int i) {
            mAnnotHandler.isDocClosed = false;
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {
            mAnnotHandler.isDocClosed = true;
            mAnnotHandler.clear();
        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {
        }
    };

    PDFViewCtrl.IRecoveryEventListener mRecoveryListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            mAnnotHandler.isDocClosed = true;
            mAnnotHandler.clear();
        }

        @Override
        public void onRecovered() {
        }
    };
}
