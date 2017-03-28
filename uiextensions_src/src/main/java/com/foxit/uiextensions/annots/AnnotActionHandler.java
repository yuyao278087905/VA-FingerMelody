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

import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.ActionHandler;
import com.foxit.sdk.common.IdentityProperties;

public class AnnotActionHandler extends ActionHandler{
    private PDFViewCtrl mPDFViewCtrl;

    public AnnotActionHandler(PDFViewCtrl pdfViewCtrl) {
        this.mPDFViewCtrl = pdfViewCtrl;
    }

    @Override
    public int alert(String msg, String title, int type, int icon) {
        int ret = 0;
        Toast.makeText(mPDFViewCtrl.getContext(), "alert...." + msg, Toast.LENGTH_SHORT).show();
        return ret;
    }

    @Override
    public IdentityProperties getIdentityProperties() {
        IdentityProperties identityProperties = new IdentityProperties();
        identityProperties.setName("Foxit");

        return identityProperties;
    }
}
