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
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotActionHandler;
import com.foxit.uiextensions.utils.AppDisplay;


public class FormFillerToolHandler implements ToolHandler {
	private AnnotActionHandler mActionHandler;
	

	public FormFillerToolHandler(PDFViewCtrl pdfViewCtrl) {
		mActionHandler = (AnnotActionHandler) DocumentManager.getInstance(pdfViewCtrl).getActionHandler();
		if (mActionHandler == null) {
			mActionHandler = new AnnotActionHandler(pdfViewCtrl);
			DocumentManager.getInstance(pdfViewCtrl).setActionHandler(mActionHandler);
			try {
				Library.setActionHandler(mActionHandler);
			} catch (PDFException e) {
				e.printStackTrace();
			}
		}

	}
	@Override
	public String getType() {
		return ToolHandler.TH_TYPE_FORMFILLER;
	}

	@Override
	public void onActivate() {

	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public void onDraw(int pageIndex, Canvas canvas) {

	}

}
