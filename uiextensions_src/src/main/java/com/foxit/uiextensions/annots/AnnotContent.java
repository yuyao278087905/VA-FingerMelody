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

import android.graphics.RectF;

import com.foxit.sdk.common.DateTime;


public interface AnnotContent {

    public int getPageIndex();

    public int getType();

    public String getNM();

    public RectF getBBox();

    public int getColor();

    public int getOpacity();

    public float getLineWidth();

    public String getSubject();

    public DateTime getModifiedDate();

    public String getContents();

    public String getReplyTo();

    public String getReplyType();

    public String getIntent();
}
