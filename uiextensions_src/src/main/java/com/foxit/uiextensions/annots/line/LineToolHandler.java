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
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;
import java.util.List;

public class LineToolHandler extends AbstractToolHandler {
	protected static final int MIN_LEN = 2;

	protected LineRealAnnotHandler mAnnotHandler;
	protected LineUtil mUtil;
	protected String mIntent;

	protected boolean				mTouchCaptured = false;
	protected int					mCapturedPage = -1;
	protected List<Line> mCache = new ArrayList<Line>();
	protected PointF mStartPt = new PointF();
	protected PointF mStopPt = new PointF();
	protected Paint mPaint;

	PointF tv_start_pt				= new PointF();
	PointF tv_stop_pt				= new PointF();

	public LineToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, LineUtil util, String intent) {
		super(context, parent, pdfViewCtrl, util.getToolName(intent), util.getToolPropertyKey(intent));
		if (intent.equals(LineConstants.INTENT_LINE_ARROW)) {
			mColor = PropertyBar.PB_COLORS_ARROW[0];
			mCustomColor = PropertyBar.PB_COLORS_ARROW[0];
		} else {
			mColor = PropertyBar.PB_COLORS_LINE[0];
			mCustomColor = PropertyBar.PB_COLORS_LINE[0];
		}
		
		mUtil = util;
		mIntent = intent;

		mPaint = new Paint();
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);

		pdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
			@Override
			public void onDocWillOpen() {

			}

			@Override
			public void onDocOpened(PDFDoc document, int errCode) {
				if (mCache != null) {
					mCache.clear();
				}
			}

			@Override
			public void onDocWillClose(PDFDoc document) {

			}

			@Override
			public void onDocClosed(PDFDoc document, int errCode) {
				if (mCache != null) {
					mCache.clear();
				}
			}

			@Override
			public void onDocWillSave(PDFDoc document) {

			}

			@Override
			public void onDocSaved(PDFDoc document, int errCode) {

			}
		});
	}

	protected void initUiElements() {
	}
	
	protected void uninitUiElements() {
		removeToolButton();
	}
	
	protected String getIntent() {
		return mIntent;
	}

	@Override
	public void onActivate() {
		mCapturedPage = -1;
	}

	@Override
	public void onDeactivate() {
		if (mTouchCaptured) {
			if (mPdfViewCtrl.isPageVisible(mCapturedPage)) {
				addAnnot(mCapturedPage);
			}
			mTouchCaptured = false;
			mCapturedPage = -1;
		}
	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent e) {
		PointF pt = new PointF(e.getX(), e.getY());
		mPdfViewCtrl.convertDisplayViewPtToPageViewPt(pt, pt, pageIndex);
		int action = e.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!mTouchCaptured || mCapturedPage == pageIndex) {
				mTouchCaptured = true;
				mStartPt.set(pt);
				mStopPt.set(pt);
				if (mCapturedPage == -1) {
					mCapturedPage = pageIndex;
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mTouchCaptured) {
				PointF point = new PointF(pt.x, pt.y);
				mUtil.correctPvPoint(mPdfViewCtrl, pageIndex, point, mThickness);
				if (mCapturedPage == pageIndex && !point.equals(mStopPt)) {
					float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
					RectF rect1 = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
					RectF rect2 = mUtil.getArrowBBox(mStartPt, point, thickness);
					rect2.union(rect1);
					mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect2, rect2, pageIndex);
					mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect2));
					mStopPt.set(point);
				}
				if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
					addAnnot(pageIndex);
					mTouchCaptured = false;
					mCapturedPage = -1;
					if (!mIsContinuousCreate) {
						((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
					}
				}
			}
			break;
		}
		return true;
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

		if (mCapturedPage == pageIndex) {
			float distance = AppDmUtil.distanceOfTwoPoints(mStartPt, mStopPt);
			float thickness = mThickness;
			thickness = thickness < 1.0f?1.0f:thickness;
			thickness = (thickness + 3)*15.0f/8.0f;
			thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl,pageIndex,thickness);
			if (distance > thickness * LineUtil.ARROW_WIDTH_SCALE / 2) {
				setPaintProperty(mPdfViewCtrl, pageIndex, mPaint);
				Path path = mUtil.getLinePath(getIntent(), mStartPt, mStopPt, thickness);
				canvas.drawPath(path, mPaint);
			}
		}
	}

	@Override
	protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {
		paint.setColor(mColor);
		paint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
		paint.setStrokeWidth(UIAnnotFrame.getPageViewThickness(pdfViewCtrl, pageIndex, mThickness));
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}
	
	@Override
	public long getSupportedProperties() {
		return mUtil.getSupportedProperties();
	}
	
	@Override
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		if (mIntent.equals(LineConstants.INTENT_LINE_ARROW)) {
			int[] colors = new int[PropertyBar.PB_COLORS_ARROW.length];
			System.arraycopy(PropertyBar.PB_COLORS_ARROW, 0, colors, 0, colors.length);
			colors[0] = mCustomColor;
			propertyBar.setColors(colors);
		} else {
			int[] colors = new int[PropertyBar.PB_COLORS_LINE.length];
			System.arraycopy(PropertyBar.PB_COLORS_LINE, 0, colors, 0, colors.length);
			colors[0] = mCustomColor;
			propertyBar.setColors(colors);
		}
		super.setPropertyBarProperties(propertyBar);
	}
	
	void addAnnot(int pageIndex) {
		if (mTouchCaptured && mCapturedPage >= 0) {
			float distance = AppDmUtil.distanceOfTwoPoints(mStartPt, mStopPt);
			float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
			if (distance > thickness * LineUtil.ARROW_WIDTH_SCALE / 2) {
				RectF bbox = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
				PointF startPt = new PointF(mStartPt.x, mStartPt.y);
				PointF stopPt = new PointF(mStopPt.x, mStopPt.y);
				mPdfViewCtrl.convertPageViewRectToPdfRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.convertPageViewPtToPdfPt(startPt, startPt, pageIndex);
				mPdfViewCtrl.convertPageViewPtToPdfPt(stopPt, stopPt, pageIndex);
				Line annot = mAnnotHandler.addAnnot(pageIndex,
						new RectF(bbox),
						mColor,
						AppDmUtil.opacity100To255(mOpacity), 
						mThickness, startPt, stopPt, getIntent(),
						new IAnnotTaskResult<PDFPage, Annot, Void>() {
					public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
						mCache.remove(p2);
					}
				});
				if (annot != null) {
					mCache.add(annot);
				}
			} else {
				RectF bbox = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
				mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(bbox));
			}
		}
	}

}
