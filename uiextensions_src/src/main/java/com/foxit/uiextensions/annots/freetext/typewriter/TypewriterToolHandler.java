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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DefaultAppearance;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TypewriterToolHandler implements ToolHandler {

    private Context mContext;
    private int mColor;
    private int mOpacity;
    private String mFont;
    private float mFontSize;
    private EditText mEditView;
    private boolean mCreating;
    private int mCreateIndex;
    private PointF mStartPoint = new PointF(0, 0);
    private PointF mStartPdfPoint = new PointF(0, 0);
    private PointF mEditPoint = new PointF(0, 0);
    public int mLastPageIndex = -1;
    private String mAnnotText;
    private FtTextUtil mTextUtil;
    private float mPageViewWidth;
    private float mPageViewHeigh;
    private float mBBoxWidth;
    private float mBBoxHeight;
    private boolean mIsContinue;
    private CreateAnnotResult mListener;
    private boolean mCreateAlive = true;
    private boolean mIsCreated;
    private boolean mIsSelcetEndText = false;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    public interface CreateAnnotResult {
        public void callBack();
    }

    public TypewriterToolHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;

        pdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {

            }

            @Override
            public void onDocWillClose(PDFDoc document) {

            }

            @Override
            public void onDocClosed(PDFDoc document, int errCode) {
                mAnnotText = "";
                mStartPoint.set(0, 0);
                mEditPoint.set(0, 0);
                mLastPageIndex = -1;
                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                if (mTextUtil != null) {
                    mTextUtil.setKeyboardOffset(0);
                }
                mEditView = null;
                mBBoxHeight = 0;
                mBBoxWidth = 0;
                mCreating = false;
                if (mTextUtil != null) {
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                }
                mIsContinue = false;
            }

            @Override
            public void onDocWillSave(PDFDoc document) {

            }

            @Override
            public void onDocSaved(PDFDoc document, int errCode) {

            }
        });
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    public String getFontName() {
        return mFont;
    }

    public float getFontSize() {
        return mFontSize;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_TYPEWRITER;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mCreateAlive = true;
        mIsCreated = false;
        AppKeyboardUtil.setKeyboardListener(mParent, mParent, new AppKeyboardUtil.IKeyboardListener() {
            @Override
            public void onKeyboardOpened(int keyboardHeight) {

            }

            @Override
            public void onKeyboardClosed() {
                mCreateAlive = false;
            }
        });
    }

    @Override
    public void onDeactivate() {
        if (mEditView != null) {
            mIsContinue = false;
            if (!mIsCreated) {
                createFTAnnot();
            }
        }
        AppKeyboardUtil.removeKeyboardListener(mParent);
        mCreateAlive = true;
    }

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent e) {

        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);

        float x = point.x;
        float y = point.y;

        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (uiExtensionsManager.getCurrentToolHandler() == this) {
                    if (!mCreating) {
                        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
                        mPageViewWidth = mPdfViewCtrl.getPageViewWidth(pageIndex);
                        mPageViewHeigh = mPdfViewCtrl.getPageViewHeight(pageIndex);
                        mStartPoint.set(x, y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mStartPoint);
                        mStartPdfPoint.set(mStartPoint.x, mStartPoint.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mStartPdfPoint, mStartPdfPoint, pageIndex);
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
                        mCreateIndex = pageIndex;
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                if (uiExtensionsManager.getCurrentToolHandler() == this && mEditView == null) {
                    mEditView = new EditText(mContext);
                    mEditView.setLayoutParams(new LayoutParams(1, 1));
                    mEditView.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            mAnnotText = FtTextUtil.filterEmoji(String.valueOf(s));
                            mPdfViewCtrl.invalidate();
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count,
                                                      int after) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    mTextUtil.setOnWidthChanged(new FtTextUtil.OnTextValuesChangedListener() {

                        @Override
                        public void onMaxWidthChanged(float maxWidth) {
                            mBBoxWidth = maxWidth;
                        }

                        @Override
                        public void onMaxHeightChanged(float maxHeight) {
                            mBBoxHeight = maxHeight;
                        }

                        @Override
                        public void onCurrentSelectIndex(int selectIndex) {
                            if (selectIndex >= mEditView.getText().length()) {
                                selectIndex = mEditView.getText().length();
                                mIsSelcetEndText = true;
                            } else {
                                mIsSelcetEndText = false;
                            }
                            mEditView.setSelection(selectIndex);
                        }

                        @Override
                        public void onEditPointChanged(float editPointX,
                                                       float editPointY) {
                            PointF point = new PointF(editPointX, editPointY);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, pageIndex);
                            mEditPoint.set(point.x, point.y);
                        }
                    });
                    mParent.addView(mEditView);
                    mPdfViewCtrl.invalidate();

                    AppUtil.showSoftInput(mEditView);
                    mTextUtil.getBlink().postDelayed((Runnable) mTextUtil.getBlink(), 500);
                    mCreating = true;
                }
                mCreateAlive = true;
                return false;
            case MotionEvent.ACTION_CANCEL:
                mStartPoint.set(0, 0);
                mEditPoint.set(0, 0);
                mCreateAlive = true;
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point);
    }

    public boolean onSingleTapOrLongPress(final int pageIndex, final PointF point) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);

        float x = point.x;
        float y = point.y;
        if (uiExtensionsManager.getCurrentToolHandler() == this && mEditView != null) {
            RectF rectF = new RectF(mStartPoint.x, mStartPoint.y,
                    mStartPoint.x + mBBoxWidth, mStartPoint.y + mBBoxHeight);
            if (rectF.contains(x, y)) {
                PointF pointF = new PointF(x, y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointF, pageIndex);
                mEditPoint.set(pointF.x, pointF.y);
                mTextUtil.resetEditState();
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                AppUtil.showSoftInput(mEditView);
                return true;
            } else {
                if (!mIsContinuousCreate) {
                    uiExtensionsManager.setCurrentToolHandler(null);
                }
                if (mCreateAlive) {
                    mCreateAlive = false;
                    if (uiExtensionsManager.getCurrentToolHandler() == TypewriterToolHandler.this) {
                        createFTAnnot();
                    }

                    return true;
                } else {
                    mCreateAlive = true;
                }
                mIsContinue = true;
                setCreateAnnotListener(new CreateAnnotResult() {
                    @Override
                    public void callBack() {
                        mStartPoint.set(point.x, point.y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mStartPoint);
                        mStartPdfPoint.set(mStartPoint.x, mStartPoint.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mStartPdfPoint, mStartPdfPoint, pageIndex);
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
                        mCreateIndex = pageIndex;
                        if (mEditView != null) {
                            AppUtil.showSoftInput(mEditView);
                        }
                    }
                });
                createFTAnnot();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        canvas.save();
        if (mLastPageIndex == pageIndex && mEditView != null) {
            PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, pageIndex);
            PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
            if (editPoint.x != 0 || editPoint.y != 0) {
                mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
            }
            mTextUtil.setTextString(pageIndex, mAnnotText, true);
            mTextUtil.setStartPoint(startPoint);
            mTextUtil.setEditPoint(editPoint);
            mTextUtil.setMaxRect(mPdfViewCtrl.getPageViewWidth(pageIndex) - startPoint.x, mPdfViewCtrl.getPageViewHeight(pageIndex) - startPoint.y);
            mTextUtil.setTextColor(mColor, AppDmUtil.opacity100To255(mOpacity));
            mTextUtil.setFont(mFont, mFontSize);
            if (mIsSelcetEndText) {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
            } else {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd());
            }
            mTextUtil.loadText();
            mTextUtil.DrawText(canvas);
        }

        canvas.restore();
    }


    private void createFTAnnot() {
        if (mAnnotText != null && mAnnotText.length() > 0) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mCreateIndex);
            final RectF rect = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + mBBoxWidth, pdfPointF.y
                    + mBBoxHeight);
            final RectF pdfRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + mBBoxWidth, pdfPointF.y
                    + mBBoxHeight);
            mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, mCreateIndex);

            RectF _rect = new RectF(pdfRectF);
            mPdfViewCtrl.convertPdfRectToPageViewRect(_rect, _rect, mCreateIndex);
            String content = "";
            try {
                content = new String(mAnnotText.getBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, mCreateIndex, _rect, content, mFont, mFontSize);

            String annotContent = "";
            for (int i = 0; i < composeText.size(); i ++) {
                annotContent += composeText.get(i);
                if (i != composeText.size() - 1 && annotContent.charAt(annotContent.length() - 1) != '\n') {
                    annotContent += "\r";
                }
            }

            try {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(mCreateIndex);
                final Annot annot = page.addAnnot(Annot.e_annotFreeText, new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom));
                annot.setBorderColor(mColor);
                DefaultAppearance da = new DefaultAppearance();
                Font font = mTextUtil.getSupportFont(mFont);
                da.setFlags(DefaultAppearance.e_defaultAPFont | DefaultAppearance.e_defaultAPTextColor|DefaultAppearance.e_defaultAPFontSize);
                da.setFont(font);
                da.setFontSize(mFontSize);
                da.setTextColor(mColor);
                ((FreeText)annot).setDefaultAppearance(da);
                ((FreeText)annot).setIntent("FreeTextTypewriter");
                ((FreeText)annot).setOpacity(AppDmUtil.opacity100To255(mOpacity) / 255f);
                ((FreeText)annot).setTitle(AppDmUtil.getAnnotAuthor());
                ((FreeText)annot).setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.setContent(annotContent);
                annot.setUniqueID(AppDmUtil.randomUUID(null));
                annot.setFlags(Annot.e_annotFlagPrint);

                annot.resetAppearanceStream();

                DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                AnnotEventTask eventTask = new AnnotEventTask(new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (mPdfViewCtrl.isPageVisible(mCreateIndex)) {
                            Rect mRect = new Rect((int) rect.left, (int) rect.top, (int) rect.right,
                                    (int) rect.bottom);
                            mPdfViewCtrl.refresh(mCreateIndex, mRect);

                            if (mIsContinue && mCreateAlive) {
                                mEditView.setText("");
                            } else {
                                AppUtil.dismissInputSoft(mEditView);
                                mParent.removeView(mEditView);
                                mEditView = null;
                                mCreating = false;
                                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                if (mPdfViewCtrl.isPageVisible(mCreateIndex)
                                        && (mCreateIndex == mPdfViewCtrl.getPageCount() - 1
                                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)
                                        && mCreateIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));

                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);

                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);

                                        mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
                                        mPdfViewCtrl.gotoPage(mCreateIndex,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).x,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).y);
                                    }
                                }
                            }
                            mAnnotText = "";
                            mStartPoint.set(0, 0);
                            mEditPoint.set(0, 0);
                            mLastPageIndex = -1;
                            if (mIsContinue) {
                                mListener.callBack();
                            }
                        } else {
                            mAnnotText = "";
                            mStartPoint.set(0, 0);
                            mEditPoint.set(0, 0);
                            mLastPageIndex = -1;
                            AppUtil.dismissInputSoft(mEditView);
                            mParent.removeView(mEditView);
                            mEditView = null;
                            mBBoxHeight = 0;
                            mBBoxWidth = 0;
                            mCreating = false;
                            mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                        }
                    }
                });
                new Thread(eventTask).start();
            } catch (PDFException e) {
                if (e.getLastError() == PDFException.e_errOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
            }
        } else {
            if (mIsContinue && mCreateAlive && mListener != null) {
                mLastPageIndex = -1;
                mListener.callBack();
            } else {
                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                mEditView = null;
                mCreating = false;
                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
            }

            mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
            if (mPdfViewCtrl.isPageVisible(mCreateIndex) && (mCreateIndex == mPdfViewCtrl.getPageCount() - 1
                    || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));
                mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);
                if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                    mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                    mTextUtil.setKeyboardOffset(0);
                    PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
                    mPdfViewCtrl.gotoPage(mCreateIndex,
                            mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).x,
                            mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).y);
                }
            }
        }
    }

    public void onColorValueChanged(int color) {
        mColor = color;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    public void onOpacityValueChanged(int opacity) {
        mOpacity = opacity;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    public void onFontValueChanged(String font) {
        mFont = font;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    public void onFontSizeValueChanged(float fontSize) {
        mFontSize = fontSize;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            adjustStartPt(mPdfViewCtrl, mLastPageIndex, pdfPointF);
            PointF pdfPtChanged = new PointF(pdfPointF.x, pdfPointF.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPtChanged, pdfPtChanged, mLastPageIndex);
            mStartPdfPoint.x = pdfPtChanged.x;
            mStartPdfPoint.y = pdfPtChanged.y;
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    private void adjustStartPt(PDFViewCtrl pdfViewCtrl, int pageIndex, PointF point) {
        if (pdfViewCtrl.getPageViewWidth(pageIndex) - point.x < mTextUtil.getFontWidth(pdfViewCtrl, pageIndex, mFont, mFontSize)) {
            point.x = mPageViewWidth - mTextUtil.getFontWidth(pdfViewCtrl, pageIndex, mFont, mFontSize);
        }
        if (pdfViewCtrl.getPageViewHeight(pageIndex) - point.y < mTextUtil.getFontHeight(pdfViewCtrl, pageIndex, mFont, mFontSize)) {
            point.y = mPageViewHeigh - mTextUtil.getFontHeight(pdfViewCtrl, pageIndex, mFont, mFontSize);
        }
    }

    private void setCreateAnnotListener(CreateAnnotResult listener) {
        mListener = listener;
    }

    private boolean mIsContinuousCreate = false;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }
}
