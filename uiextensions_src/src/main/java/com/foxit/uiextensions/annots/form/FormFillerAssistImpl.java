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

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.sdk.pdf.form.FormFillerAssist;
import com.foxit.uiextensions.utils.AppDmUtil;

public class FormFillerAssistImpl extends FormFillerAssist {

    private PDFViewCtrl mPDFViewCtrl;
    protected boolean isAllowInput = false;
    protected boolean bWillClose = false;
    protected boolean isScaling = false;
    public FormFillerAssistImpl(PDFViewCtrl pdfViewCtrl) {
        this.mPDFViewCtrl = pdfViewCtrl;
    }

    public void setScaling(boolean scaling) {
        isScaling = scaling;
    }

    @Override
    public void focusGotOnControl(FormControl control, String filedValue) {
        isAllowInput = true;

    }

    @Override
    public void focusLostFromControl(FormControl control, String filedValue) {
         isAllowInput = false;
    }

    @Override
    public void refresh(PDFPage page, RectF pdfRect) {
        try {
            if (bWillClose) return;
            if (isScaling)
                return;
            int pageIndex = page.getIndex();
            int curPageIndex = mPDFViewCtrl.getCurrentPage();
            if (pageIndex != curPageIndex)
                return;
            RectF viewRect = new RectF(0 ,0, mPDFViewCtrl.getDisplayViewWidth(), mPDFViewCtrl.getDisplayViewHeight());
            mPDFViewCtrl.convertPdfRectToPageViewRect(pdfRect, pdfRect, pageIndex);
            RectF rect = new RectF(pdfRect);
            mPDFViewCtrl.convertPageViewRectToDisplayViewRect(pdfRect, pdfRect, pageIndex);
            if(!viewRect.intersect(pdfRect))
                return;
            if (System.currentTimeMillis() - FormFillerAnnotHandler.mLastInputInvalidateTime > 500 && FormFillerAnnotHandler.mIsNeedRefresh) {
                rect.inset(-5, -5);
                mPDFViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rect));
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

}
