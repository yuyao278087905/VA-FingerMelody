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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class DefaultAnnotHandler extends AbstractAnnotHandler {
	protected ArrayList<Integer> mMenuText;

	public DefaultAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
		super(context, parent, pdfViewCtrl, Annot.e_annotUnknownType);
		mMenuText = new ArrayList<Integer>();
	}

	@Override
	protected AbstractToolHandler getToolHandler() {
		return null;
	}

	@Override
	public boolean annotCanAnswer(Annot annot) {
		return AppAnnotUtil.isSupportEditAnnot(annot);
	}

	@Override
	public void onAnnotSelected(final Annot annot, boolean reRender) {
		super.onAnnotSelected(annot, reRender);
	}

	@Override
	public void onAnnotDeselected(Annot annot, boolean reRender) {
		if (!mIsModified) {
			super.onAnnotDeselected(annot, reRender);
		}
	}
	
	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent e) {
		int action = e.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			return super.onTouchEvent(pageIndex, e);
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			PointF point = new PointF(e.getX(), e.getY());
			mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
			Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
			try {
				PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
				if (annot == null) {
					PointF pdfPt = new PointF(point.x, point.y);
					mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPt, pdfPt, pageIndex);
					annot = page.getAnnotAtPos(pdfPt, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
				}

				if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
						&& annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
					if (action == MotionEvent.ACTION_UP|| action == MotionEvent.ACTION_CANCEL) {
						mTouchCaptured = false;
						mDownPt.set(0, 0);
						mLastPt.set(0, 0);
						mOp = UIAnnotFrame.OP_DEFAULT;
						mCtl = UIAnnotFrame.CTL_NONE;
					}
					return true;
				}
			} catch (PDFException e1) {
				e1.printStackTrace();
			}

			break;
		}
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
		return super.onSingleTapConfirmed(pageIndex, motionEvent);
	}

	@Override
	public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
		return super.onLongPress(pageIndex, motionEvent);
	}

	@Override
	public void onDraw(int pageIndex, Canvas canvas) {
		Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
		if (annot == null) return;
		try {
			if (mSelectedAnnot == annot && annot.getPage().getIndex() == pageIndex) {
				RectF bbox = annot.getRect();
				mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
				RectF mapBounds = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
						mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);

				if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
					int color = (int)annot.getBorderColor() | 0xFF000000;
					int opacity = (int)(((Markup)annot).getOpacity() * 255f);
					UIAnnotFrame.getInstance(mContext).drawFrame(canvas, mapBounds, color, opacity);
				}
			}
		} catch (PDFException e) {

		}

	}

	@Override
	public void addAnnot(int pageIndex, AnnotContent content, final Event.Callback result) {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
			final Annot annot = page.addAnnot(content.getType(), content.getBBox());
			((Markup)annot).setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
			annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
			annot.setUniqueID(content.getNM());
			annot.setBorderColor(content.getColor());
			((Markup) annot).setOpacity(content.getOpacity() / 255f);
			BorderInfo borderInfo = new BorderInfo();
			borderInfo.setWidth(content.getLineWidth());
			annot.setBorderInfo(borderInfo);
			((Markup)annot).setTitle(AppDmUtil.getAnnotAuthor());
			addAnnot(pageIndex, annot,
					new IAnnotTaskResult<PDFPage, Annot, Void>() {
						public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
							if (result != null) {
								result.result(null, true);
							}
						}
					});
		} catch (PDFException e) {
			e.printStackTrace();
		}
	}

	protected void addAnnot(int pageIndex, Annot annot, IAnnotTaskResult<PDFPage, Annot, Void> result) {

		Event event =  new Event();

		handleAddAnnot(pageIndex, annot, event, result);
	}

	@Override
	public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {

		try {
			RectF oldBBox = annot.getRect();
			float oldThickness = annot.getBorderInfo().getWidth();

			annot.setUniqueID(content.getNM());
			annot.setBorderColor(content.getColor());
			BorderInfo borderInfo = annot.getBorderInfo();
			borderInfo.setWidth(content.getLineWidth());
			annot.setBorderInfo(borderInfo);
			((Markup) annot).setOpacity(content.getOpacity() / 255f);
			annot.setModifiedDateTime(content.getModifiedDate());

			annot.resetAppearanceStream();

			modifyAnnot(annot, oldBBox, oldThickness, true, result);
		} catch (PDFException e) {
			e.printStackTrace();
		}
		

	}


	protected void modifyAnnot(Annot annot, RectF oldBBox, float oldThickness, boolean reRender, final Event.Callback result) {
		handleModifyAnnot(annot, oldBBox, oldThickness, reRender,
				new IAnnotTaskResult<PDFPage, Annot, Void>() {
			@Override
			public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
				if (result != null) {
					result.result(null, success);
				}
			}
		});
	}

	@Override
	public void removeAnnot(Annot annot, final Event.Callback result) {
		handleRemoveAnnot(annot,
				new IAnnotTaskResult<PDFPage, Void, Void>() {
					@Override
					public void onResult(boolean success, PDFPage p1, Void p2, Void p3) {
						if (result != null) {
							result.result(null, success);
						}
					}
				});
	}

	@Override
	protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
		return new ArrayList<Path>();
	}

	@Override
	protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
	}

	@Override
	protected void resetStatus() {
		mBackRect = null;
		mSelectedAnnot = null;
		mIsModified = false;
	}

	@Override
	protected void showPopupMenu() {
		Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
		if (curAnnot == null) return;
		try {
			if (!AppAnnotUtil.isSupportReply(curAnnot))
                return;

			reloadPopupMenuString();
			mAnnotMenu.setMenuItems(mMenuText);
			RectF bbox = curAnnot.getRect();
			int pageIndex = curAnnot.getPage().getIndex();
			mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
			mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
			mAnnotMenu.show(bbox);
			mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
				@Override
				public void onAMClick(int flag) {
					if (mSelectedAnnot == null) return;
					if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
						DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
						UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, mSelectedAnnot);
					} else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
						DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
						UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, mSelectedAnnot);
					}
				}
			});
		} catch (PDFException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void dismissPopupMenu() {
		mAnnotMenu.dismiss();
	}
	
	@Override
	protected void showPropertyBar(long curProperty) {
		Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
		if (annot != null) {
			mPropertyBar.setPropertyChangeListener(this);
			mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
			mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getColor());
			mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
			mPropertyBar.reset(getSupportedProperties());

			try {
				RectF bbox = annot.getRect();
				int pageIndex = annot.getPage().getIndex();
				mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
				mPropertyBar.show(bbox, false);
			} catch (PDFException e) {
				e.printStackTrace();
			}


		}
	}
	
	@Override
	protected void hidePropertyBar() {
		if (mPropertyBar.isShowing()) {
			mPropertyBar.dismiss();
		}
	}
	
	@Override
	protected long getSupportedProperties() {
		return PropertyBar.PROPERTY_COLOR;
	}
	
	@Override
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
		if (AppDisplay.getInstance(mContext).isPad())
			propertyBar.setArrowVisible(true);
	}

	@Override
	public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
		super.setPaintProperty(pdfViewCtrl, pageIndex, paint, annot);
	}

	protected void onLanguageChanged() {
		reloadPopupMenuString();
	}

	protected void reloadPopupMenuString() {
		mMenuText.clear();
		mMenuText.add(AnnotMenu.AM_BT_COMMENT);
		if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
			mMenuText.add(AnnotMenu.AM_BT_REPLY);
		}
	}
}