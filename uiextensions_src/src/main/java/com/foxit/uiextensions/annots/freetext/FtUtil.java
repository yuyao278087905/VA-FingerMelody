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
package com.foxit.uiextensions.annots.freetext;

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;


public class FtUtil {

	public static float widthOnPageView(PDFViewCtrl pdfViewCtrl, int pageIndex, float width) {
		RectF rectF = new RectF(0, 0, width, width);
		pdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
		return Math.abs(rectF.width());
	}

}
