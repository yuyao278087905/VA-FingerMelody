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
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.form.Form;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.sdk.pdf.form.FormField;
import com.foxit.sdk.pdf.form.FormFiller;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;


public class FormFillerAnnotHandler implements AnnotHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private FormFiller mFormFiller;
    private FormFillerAssistImpl mAssist;
    private static Form mForm;
    private EditText mEditView = null;
    private PointF mLastTouchPoint = new PointF(0, 0);
    protected static boolean mIsNeedRefresh = true;

    private int mPageOffset;
    private boolean mIsBackBtnPush = false; //for some input method, double backspace click
    private boolean mAdjustPosition = false;
    protected static long mLastInputInvalidateTime = 0;
    private String mLastInputText = "";
    private String mChangeText = null;
    private DocumentManager.AnnotEventListener mAnnotListener;
    private Annot mLastAnnotForInputSoft = null;

    public FormFillerAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void init(Form form) {
        mAssist = new FormFillerAssistImpl(mPdfViewCtrl);
        mAssist.bWillClose = false;
        mForm = form;

        try {
            mFormFiller = FormFiller.create(form, mAssist);
            mFormFiller.highlightFormFields(true);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void uninit() {

    }

    protected void clear() {
        if (mAssist != null) {
            mAssist.bWillClose = true;
        }
    }

    public FormFillerAssistImpl getFormFillerAssist() {
        return mAssist;
    }

    @Override
    public int getType() {
        return Annot.e_annotWidget;
    }


    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {

        try {
            return annot.getRect();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {

        try {
            RectF r = annot.getRect();
            RectF rf = new RectF(r.left, r.top, r.right, r.bottom);
            PointF p = new PointF(point.x, point.y);
            int pageIndex = annot.getPage().getIndex();
            FormControl control = AppAnnotUtil.getControlAtPos(annot.getPage(), p, 1);

            mPdfViewCtrl.convertPdfRectToPageViewRect(rf, rf, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(p, p, pageIndex);

            if (rf.contains(p.x, p.y)) {
                return true;
            } else {
                if (AppAnnotUtil.isSameAnnot(annot, control))
                    return true;
                return false;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return true;
    }


    public void onBackspaceBtnDown() {
        try {
            mIsNeedRefresh = true;
            mFormFiller.input((char) 8);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        final int pageIndex = mPdfViewCtrl.getCurrentPage();

        if (shouldShowInputSoft(annot)) {
            mAdjustPosition = true;
            mLastInputText = " ";

            if (mEditView != null) {
                mParent.removeView(mEditView);
            }
            mEditView = new EditText(mContext);
            mEditView.setLayoutParams(new LayoutParams(1, 1));
            mEditView.setSingleLine(false);
            mEditView.setText(" ");
            mEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        try {
                            mIsNeedRefresh = true;
                            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                            PointF point = new PointF(-100, -100);
                            mFormFiller.click(page, point);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        AppUtil.dismissInputSoft(mEditView);
                        mParent.removeView(mEditView);
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        return true;
                    }
                    return false;
                }
            });
            mParent.addView(mEditView);
            AppUtil.showSoftInput(mEditView);


            mEditView.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        onBackspaceBtnDown();
                        mIsBackBtnPush = true;
                    }
                    return false;
                }
            });

            mEditView.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        if (s.length() >= mLastInputText.length()) {
                            String afterchange = s.subSequence(start, start + before).toString();
                            if (mChangeText.equals(afterchange)) {
                                for (int i = 0; i < s.length() - mLastInputText.length(); i++) {
                                    char c = s.charAt(mLastInputText.length() + i);
                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    try {
                                        mIsNeedRefresh = true;
                                        mFormFiller.input(value);
                                        RectF rect = annot.getRect();
                                        mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
                                        rect.inset(-5, -5);
                                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rect));

                                    } catch (PDFException e) {
                                        e.printStackTrace();
                                    }
                                    mLastInputInvalidateTime = System.currentTimeMillis();

                                }
                            } else {
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown();
                                }
                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    try {
                                        mIsNeedRefresh = true;
                                        mFormFiller.input(value);

                                    } catch (PDFException e) {
                                        e.printStackTrace();
                                    }
                                    mLastInputInvalidateTime = System.currentTimeMillis();

                                }
                            }
                        } else if (s.length() < mLastInputText.length()) {

                            if (mIsBackBtnPush == false)
                                onBackspaceBtnDown();
                            mIsBackBtnPush = false;
                        }

                        if (s.toString().length() == 0)
                            mLastInputText = " ";
                        else
                            mLastInputText = s.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                    mChangeText = s.subSequence(start, start + count).toString();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().length() == 0)
                        s.append(" ");
                }
            });

        }

    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {

    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        mIsNeedRefresh = false;
        return false;

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        mLastTouchPoint.set(0, 0);
        boolean ret = false;
        PDFPage page = null;

        try {
            PointF docViewerPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(docViewerPt, point, pageIndex);
            PointF pageViewPt = new PointF(point.x, point.y);
            final PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            Annot annotTmp = page.getAnnotAtPos(pdfPointF, 1);
            if (annot == null)
                annot = annotTmp;
            if (annot == null)
                return false;

            boolean isHit = isHitAnnot(annot, pdfPointF);

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHit) {
                    ret = true;
                } else {
                    if (!isHit) {
                        AppUtil.dismissInputSoft(mEditView);

                        mParent.removeView(mEditView);
                    }

                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    ret = false;
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
                ret = true;
            }


            final PDFPage finalPage = page;
            final RectF rect = annot.getRect();
            if (annotTmp == null || annotTmp == annot || annotTmp.getType() != Annot.e_annotWidget) {

                try {
                    mIsNeedRefresh = true;
                    mFormFiller.click(finalPage, pdfPointF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, finalPage.getIndex());
                    mPdfViewCtrl.refresh(finalPage.getIndex(), AppDmUtil.rectFToRect(rect));
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static PointF getPageViewOrigin(PDFViewCtrl pdfViewCtrl, int pageIndex, float x, float y) {
        PointF pagePt = new PointF(x, y);
        pdfViewCtrl.convertPageViewPtToDisplayViewPt(pagePt, pagePt, pageIndex);
        RectF rect = new RectF(0, 0, pagePt.x, pagePt.y);
        pdfViewCtrl.convertDisplayViewRectToPageViewRect(rect, rect, pageIndex);
        PointF originPt = new PointF(x - rect.width(), y - rect.height());
        return originPt;
    }

    private int getKeyboardHeight() {
        if (mContext == null) return 0;
        Rect r = new Rect();

        mParent.getWindowVisibleDisplayFrame(r);
        int screenHeight = mParent.getRootView().getHeight();
        return screenHeight - (r.bottom - r.top);

    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof FormControl))
            return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this)
            return;
        try {
            RectF rect = annot.getRect();

            PointF viewerpoint = new PointF(rect.left, rect.bottom);
            PointF viewpoint = new PointF(rect.left, rect.bottom);
            mPdfViewCtrl.convertPdfPtToPageViewPt(viewerpoint, viewerpoint, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(viewpoint, viewpoint, pageIndex);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(viewerpoint, viewerpoint, pageIndex);
            int type = FormFillerUtil.getAnnotFieldType(mForm, annot);

            if ((type == FormField.e_formFieldTextField) ||
                    (type == FormField.e_formFieldComboBox && (((FormControl) annot).getField().getFlags() & FormField.e_formFieldFlagComboEdit) != 0)) {
                if (mAdjustPosition && getKeyboardHeight() > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - viewerpoint.y < (getKeyboardHeight() + AppDisplay.getInstance(mContext).dp2px(116))) {
                        int keyheight = getKeyboardHeight();
                        int screenheight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                        mPageOffset = (int) (keyheight - (screenheight - viewerpoint.y));

                        if (mPageOffset != 0 && pageIndex == mPdfViewCtrl.getPageCount() - 1
                                || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) {

                            PointF point1 = new PointF(0, mPdfViewCtrl.getPageViewHeight(pageIndex));
                            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point1, point1, pageIndex);
                            float screenHeight = AppDisplay.getInstance(mContext).getScreenHeight();
                            if (point1.y <= screenHeight) {
                                int offset = -(mPageOffset + AppDisplay.getInstance(mContext).dp2px(116));
                                mPdfViewCtrl.layout(0, offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() + offset);
                            }
                        }
                        mPdfViewCtrl.gotoPage(pageIndex,
                                getPageViewOrigin(mPdfViewCtrl, pageIndex, viewpoint.x, viewpoint.y).x,
                                getPageViewOrigin(mPdfViewCtrl, pageIndex, viewpoint.x, viewpoint.y).y + mPageOffset + AppDisplay.getInstance(mContext).dp2px(116));
                        mAdjustPosition = false;
                    } else {
                        resetDocViewerOffset();
                    }
                }
            }

            if ((pageIndex != mPdfViewCtrl.getPageCount() - 1 && mPdfViewCtrl.getPageLayoutMode() != PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                resetDocViewerOffset();
            }

            if (getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5
                    && (pageIndex == mPdfViewCtrl.getPageCount() - 1 || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                resetDocViewerOffset();
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier,
                         Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {

    }

    public void setAnnotEventListen() {
        mAnnotListener = new DocumentManager.AnnotEventListener() {

            @Override
            public void onAnnotAdded(PDFPage dmPage, Annot annot) {

            }

            @Override
            public void onAnnotDeleted(PDFPage dmPage, Annot annot) {

            }

            @Override
            public void onAnnotModified(PDFPage dmPage, Annot annot) {

            }

            @Override
            public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
                try {
                    if (currentAnnot != null && currentAnnot.getType() != Annot.e_annotWidget) {
                        return;
                    }
                } catch (PDFException e) {
                    return;
                }
                if (currentAnnot != null && shouldShowInputSoft(mLastAnnotForInputSoft) && !shouldShowInputSoft(currentAnnot)) {
                    AppUtil.dismissInputSoft(mEditView);
                    mParent.removeView(mEditView);
                    mLastAnnotForInputSoft = null;
                }

                if (lastAnnot != null && currentAnnot == null) {
                    try {
                        if (lastAnnot.getType() == Annot.e_annotWidget) {
                            mLastAnnotForInputSoft = lastAnnot;
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                }
            }
        };

        DocumentManager.getInstance(mPdfViewCtrl).registerAnnotEventListener(mAnnotListener);
    }


    private boolean shouldShowInputSoft(Annot annot) {
        if (annot == null) return false;
        if (!(annot instanceof FormControl)) return false;
        int type = FormFillerUtil.getAnnotFieldType(mForm, annot);
        try {
            if ((type == FormField.e_formFieldTextField) ||
                    (type == FormField.e_formFieldComboBox && (((FormControl) annot).getField().getFlags() & FormField.e_formFieldFlagComboEdit) != 0))
                return true;

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void resetDocViewerOffset() {
        if (mPageOffset != 0) {
            mPageOffset = 0;
            mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
        }
    }

    protected boolean onKeyBack() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (curAnnot == null) return false;
            if (curAnnot.getType() != Annot.e_annotWidget) return false;
            FormField field =  ((FormControl)curAnnot).getField();
            if (field != null && field.getType() != FormField.e_formFieldSignature &&
                    field.getType() != FormField.e_formFieldUnknownType) {

                mIsNeedRefresh = true;
                PDFPage page = curAnnot.getPage();
                PointF point = new PointF(-100, -100);
                mFormFiller.click(page, point);

                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}
