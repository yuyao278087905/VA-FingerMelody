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
package com.foxit.uiextensions.annots.textmarkup;

import android.graphics.RectF;

import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.utils.AppDmUtil;

public abstract class TextMarkupContentAbs implements AnnotContent {

    public abstract TextSelector getTextSelector();

    @Override
    public String getNM() {
        return AppDmUtil.randomUUID(null);
    }

    @Override
    public RectF getBBox() {
        return getTextSelector().getBbox();
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public float getLineWidth() {
        return 0;
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public DateTime getModifiedDate() {
        try {
            return new DateTime();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getContents() {
        return getTextSelector().getContents();
    }

    @Override
    public String getReplyTo() {
        return null;
    }

    @Override
    public String getReplyType() {
        return null;
    }
}
