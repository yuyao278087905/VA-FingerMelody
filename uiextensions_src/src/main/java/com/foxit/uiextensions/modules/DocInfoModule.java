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
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;

public class DocInfoModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private String mFilePath = null;
    private ViewGroup mParent = null;
    private DocInfoView mDocInfo = null;

    public DocInfoModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, String filePath) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mFilePath = filePath;
        mParent = parent;
    }

    public DocInfoView getView() {
        return mDocInfo;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_DOCINFO;
    }

    @Override
    public boolean loadModule() {
        mDocInfo = new DocInfoView(mContext, mPdfViewCtrl);
        if (mDocInfo == null)
            return false;
        mDocInfo.init(mFilePath);
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

}
