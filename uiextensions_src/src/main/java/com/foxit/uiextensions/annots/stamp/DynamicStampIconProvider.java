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
package com.foxit.uiextensions.annots.stamp;

import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.AnnotIconProvider;
import com.foxit.sdk.pdf.annots.ShadingColor;

import java.util.HashMap;
import java.util.UUID;

public class DynamicStampIconProvider extends AnnotIconProvider {

    HashMap<String, PDFDoc> mDocMap;
    HashMap<PDFDoc, PDFPage>  mDocPagePair;
    private String id = UUID.randomUUID().toString();
    private String version = "Version 2.0";
    private int pageIndex = 0;

    public DynamicStampIconProvider() {
        mDocMap = new HashMap<String, PDFDoc>();
        mDocPagePair = new HashMap<PDFDoc, PDFPage>();
    }

    public void addDocMap(String key, PDFDoc pdfDoc) {
        if (key == null || key.trim().length() < 1) {
            return;
        }

        if (mDocMap.get(key) == null) {
            mDocMap.put(key, pdfDoc);
        }
    }

    @Override
    public void release() {
        for (PDFDoc pdfDoc: mDocMap.values()) {
            if (mDocPagePair.get(pdfDoc) != null) {
                try {
                    pdfDoc.closePage(mDocPagePair.get(pdfDoc).getIndex());
                    pdfDoc.release();
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }

        mDocPagePair.clear();
        mDocMap.clear();
    }

    @Override
    public String getProviderID() {
        return id;
    }

    @Override
    public String getProviderVersion() {
        return version;
    }

    @Override
    public boolean hasIcon(int annotType, String iconName) {
        return true;
    }

    @Override
    public boolean canChangeColor(int annotType, String iconName) {
        return true;
    }

    @Override
    public PDFPage getIcon(int annotType, String iconName, long color) {
        if (mDocMap == null || mDocMap.get(iconName + annotType) == null || annotType == Annot.e_annotNote) {
            return null;
        }

        try {
            PDFDoc pdfDoc = mDocMap.get(iconName + annotType);
            if (pdfDoc == null) return null;
            if (mDocPagePair.get(pdfDoc) != null) {
                return mDocPagePair.get(pdfDoc);
            }
            PDFPage page = pdfDoc.getPage(pageIndex);
            mDocPagePair.put(pdfDoc, page);
            return page;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ShadingColor getShadingColor(int annotType, String iconName, long refColor, int shadingIndex) {
        return null;
    }

    @Override
    public float getDisplayWidth(int annotType, String iconName) {
        return 0;
    }

    @Override
    public float getDisplayHeight(int annotType, String iconName) {
        return 0;
    }

}
