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
package com.foxit.uiextensions.annots.line;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;


public class LineModule implements Module {
	protected LineUtil mUtil;
	protected LineToolHandler mLineToolHandler;
	protected LineToolHandler mArrowToolHandler;
	protected LineAnnotHandler mLineAnnotHandler;

	Context mContext;
	ViewGroup mParent;
	PDFViewCtrl mPdfViewCtrl;
	public LineModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
		mContext = context;
		mParent = parent;
		mPdfViewCtrl = pdfViewCtrl;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_LINE;
	}

	@Override
	public boolean loadModule() {
		mUtil = new LineUtil(mContext, this);

		mLineToolHandler = new LineToolHandler(mContext, mParent, mPdfViewCtrl, mUtil, LineConstants.INTENT_LINE_DEFAULT);
		mArrowToolHandler = new LineToolHandler(mContext, mParent, mPdfViewCtrl, mUtil, LineConstants.INTENT_LINE_ARROW);
		mLineAnnotHandler = new LineAnnotHandler(mContext, mParent, mPdfViewCtrl, mUtil);
		mLineToolHandler.mAnnotHandler = mLineAnnotHandler.mRealAnnotHandler;
		mArrowToolHandler.mAnnotHandler = mLineAnnotHandler.mRealAnnotHandler;

		mLineAnnotHandler.setAnnotMenu(LineConstants.INTENT_LINE_DIMENSION, new AnnotMenuImpl(mContext, mParent));
		mLineAnnotHandler.setPropertyBar(LineConstants.INTENT_LINE_DIMENSION, new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));

		mLineAnnotHandler.setAnnotMenu(LineConstants.INTENT_LINE_ARROW, new AnnotMenuImpl(mContext, mParent));
		mLineAnnotHandler.setPropertyBar(LineConstants.INTENT_LINE_ARROW, new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));

		mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);

		initUiElements();
		return true;
	}

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);

		uninitUiElements();
		return true;
	}

	void initUiElements() {
		mArrowToolHandler.initUiElements();
		mLineToolHandler.initUiElements();
	}
	
	void uninitUiElements() {
		mArrowToolHandler.uninitUiElements();
		mLineToolHandler.uninitUiElements();
	}

	public AnnotHandler getAnnotHandler() {
		return mLineAnnotHandler;
	}

	public ToolHandler getLineToolHandler() {
		return mLineToolHandler;
	}

	public ToolHandler getArrowToolHandler() {
		return mArrowToolHandler;
	}

	private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

		@Override
		public void onDraw(int pageIndex, Canvas canvas) {
			mLineAnnotHandler.onDrawForControls(canvas);
		}
	};

	PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {
		}

		@Override
		public void onRecovered() {
		}
	};
}
