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
package com.foxit.uiextensions.annots.ink;

import android.content.Context;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;


public class InkModule implements Module {
	protected InkAnnotUtil mUtil;
	protected InkAnnotHandler mAnnotHandler;
	protected InkToolHandler mToolHandler;
	private Context mContext;
	private ViewGroup mParent;
	private PDFViewCtrl mPdfViewCtrl;

	public InkModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
		mContext = context;
		mParent = parent;
		mPdfViewCtrl = pdfViewCtrl;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_INK;
	}
	
	@Override
	public boolean loadModule() {
		mUtil = new InkAnnotUtil();

		mToolHandler = new InkToolHandler(mContext, mParent, mPdfViewCtrl, mUtil);
		mAnnotHandler = new InkAnnotHandler(mContext, mParent, mPdfViewCtrl, mToolHandler, mUtil);
		mToolHandler.mAnnotHandler = mAnnotHandler;
		mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mParent));
		mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl, mParent));

		mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
		return true;
	}

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
		mToolHandler.uninitUiElements();
		return true;
	}

	public AnnotHandler getAnnotHandler() {
		return mAnnotHandler;
	}

	public ToolHandler getToolHandler() {
		return mToolHandler;
	}

	PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {
			if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
				mAnnotHandler.getAnnotMenu().dismiss();
			}

			if (mAnnotHandler.getPropertyBar() != null && mAnnotHandler.getPropertyBar().isShowing()) {
				mAnnotHandler.getPropertyBar().dismiss();
			}
		}

		@Override
		public void onRecovered() {
		}
	};
}
