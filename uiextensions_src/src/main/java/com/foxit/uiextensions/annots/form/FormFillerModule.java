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
package com.foxit.uiextensions.annots.form;


import android.content.Context;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.form.Form;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;


public class FormFillerModule implements Module, PropertyBar.PropertyChangeListener {

    private FormFillerToolHandler mToolHandler;
    private FormFillerAnnotHandler mAnnotHandler;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private Form mForm = null;

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {

        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != PDFException.e_errSuccess) {
                return;
            }
            try {
                if (mPdfViewCtrl.getDoc() != null) {
                    boolean hasForm = mPdfViewCtrl.getDoc().hasForm();
                    if (!hasForm)
                        return ;
                    mForm = mPdfViewCtrl.getDoc().getForm();
                    mAnnotHandler.init(mForm);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mAnnotHandler.clear();
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {

        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private PDFViewCtrl.IScaleGestureEventListener mScaleGestureEventListener = new PDFViewCtrl.IScaleGestureEventListener(){

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(true);
            }

            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(false);
            }
        }
    };


    public FormFillerModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mParent = parent;
        this.mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_FORMFILLER;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    public boolean resetForm()
    {
        try {
            return mForm.reset();
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportFormToXML(String path)
    {
        try {
            return mForm.exportToXML(path);
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean importFormFromXML(String path)
    {
        try {
            return mForm.importFromXML(path);
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean loadModule() {
        mPdfViewCtrl.registerDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.registerScaleGestureEventListener(mScaleGestureEventListener);
        mToolHandler = new FormFillerToolHandler(mPdfViewCtrl);
        mAnnotHandler = new FormFillerAnnotHandler(mContext, mParent, mPdfViewCtrl);
        mAnnotHandler.setAnnotEventListen();
        return true;
    }

    @Override
    public boolean unloadModule() {
        mAnnotHandler.uninit();
        return true;
    }

    @Override
    public void onValueChanged(long property, int value) {

    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    public boolean onKeyBack() {
        return mAnnotHandler.onKeyBack();
    }
}
