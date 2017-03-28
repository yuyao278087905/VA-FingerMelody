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
package com.foxit.uiextensions.textselect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.note.NoteAnnotContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

public class TextSelectToolHandler implements ToolHandler {
    private static final int HANDLE_AREA = 10;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    protected final TextSelector mSelectInfo;
    public int mCurrentIndex;
    public RectF mTmpRect;
    private RectF mTmpDesRect;
    private Paint mPaint;
    public Bitmap mHandlerBitmap;
    public boolean mIsEdit;
    public boolean mIsMenuShow;
    public AnnotMenu mAnnotationMenu;

    private ArrayList<Integer> mText;
    private ArrayList<Integer> mBlank;

    private DocumentManager.AnnotEventListener mAnnotListener;
    private PointF mMenuPoint;
    private PointF mMenuPdfPoint;
    private RectF mMenuBox;

    public TextSelectToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mParent = parent;
        mSelectInfo = new TextSelector(pdfViewCtrl);
        mMenuPoint = null;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        mTmpRect = new RectF();
        mTmpDesRect = new RectF();
        mHandlerBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rv_textselect_handler);
        mAnnotationMenu = new AnnotMenuImpl(context, mParent);

        mText = new ArrayList<Integer>();
        mBlank = new ArrayList<Integer>();

        mIsEdit = false;
        mIsMenuShow = false;
        mAnnotListener = new DocumentManager.AnnotEventListener() {
            @Override
            public void onAnnotAdded(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotDeleted(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotModified(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
                if (currentAnnot != null && mIsMenuShow == true) {
                    mIsMenuShow = false;
                    mAnnotationMenu.dismiss();
                    mAnnotationMenu.show(mMenuBox);
                }
                if (currentAnnot != null && mIsEdit == true) {
                    if (mIsEdit == true) {
                        RectF rectF = new RectF(mSelectInfo.getBbox());
                        mSelectInfo.clear();
                        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
                        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                        RectF rF = calculate(rectF, mTmpRect);
                        Rect rect = new Rect();
                        rF.roundOut(rect);
                        getInvalidateRect(rect);
                        mPdfViewCtrl.invalidate(rect);
                        mIsEdit = false;
                        mAnnotationMenu.dismiss();
                        mAnnotationMenu.show(mMenuBox);
                    }
                }
            }
        };

        DocumentManager.getInstance(mPdfViewCtrl).registerAnnotEventListener(mAnnotListener);
    }

    protected AnnotMenu getAnnotationMenu() {
        return mAnnotationMenu;
    }

    private UIExtensionsManager.ToolHandlerChangedListener mHandlerChangedListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
            if (currentTool != null && mIsMenuShow == true) {
                mAnnotationMenu.dismiss();
                mIsMenuShow = false;
            }
            if (currentTool != null && mIsEdit == true) {
                if (mIsEdit == true) {
                    RectF rectF = new RectF(mSelectInfo.getBbox());
                    mSelectInfo.clear();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                    RectF rF = calculate(rectF, mTmpRect);
                    Rect rect = new Rect();
                    rF.roundOut(rect);
                    rect.top -= mHandlerBitmap.getHeight();
                    rect.bottom += mHandlerBitmap.getHeight();
                    rect.left -= mHandlerBitmap.getWidth() / 2;
                    rect.right += mHandlerBitmap.getWidth() / 2;
                    mPdfViewCtrl.invalidate(rect);
                    mIsEdit = false;
                    mAnnotationMenu.dismiss();
                }
            }
        }
    };

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_TEXTSELECT;
    }


    public String getCurrentSelectedText() {
        return mSelectInfo.getContents();
    }


    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {
        RectF rectF = new RectF(mSelectInfo.getBbox());
        mSelectInfo.clear();
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mAnnotationMenu.dismiss();
        mIsEdit = false;
    }

    public void uninit() {
        DocumentManager.getInstance(mPdfViewCtrl).unregisterAnnotEventListener(mAnnotListener);
    }

    public void reloadres() {
        mText.clear();
        mBlank.clear();
        if (DocumentManager.getInstance(mPdfViewCtrl).canCopy()) {
            mText.add(AnnotMenu.AM_BT_COPY);
        }
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotHighlight) != null)
            mText.add(AnnotMenu.AM_BT_HIGHLIGHT);
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotUnderline) != null)
            mText.add(AnnotMenu.AM_BT_UNDERLINE);
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotStrikeOut) != null)
            mText.add(AnnotMenu.AM_BT_STRIKEOUT);
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotSquiggly) != null)
            mText.add(AnnotMenu.AM_BT_SQUIGGLY);

        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotNote) != null) {
            mBlank.add(AnnotMenu.AM_BT_NOTE);
        }
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mBlank.add(AnnotMenu.AM_BT_SIGNATURE);
        }
    }

    int[] EnSeparatorList = {0, 10, 13, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 64};

    private void findEnWord(int pageIndex, TextSelector info, int index) {
        info.setStart(index);
        info.setEnd(index);
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            String charInfo = null;
            for (; info.getStart() >= 0; info.setStart(info.getStart() - 1)) {
                charInfo = textPage.getChars(info.getStart(), 1);
                if (charInfo == null) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
                int i;
                for (i = 0; i < EnSeparatorList.length; i++) {
                    if (EnSeparatorList[i] == charInfo.charAt(0)) {
                        break;
                    }
                }
                if (i != EnSeparatorList.length) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
            }

            if (info.getStart() < 0) {
                info.setStart(0);
            }

            for (; ; info.setEnd(info.getEnd() + 1)) {
                charInfo = textPage.getChars(info.getEnd(), 1);
                if (charInfo == null) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
                int i;
                for (i = 0; i < EnSeparatorList.length; i++) {
                    if (EnSeparatorList[i] == charInfo.charAt(0)) {
                        break;
                    }
                }
                if (i != EnSeparatorList.length) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
            }
            if (charInfo == null) {
                info.setEnd(info.getEnd() - 1);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return;
            }
        }
    }

    int[] ChPassList = {0, 10, 13, 32};

    private void findChWord(int pageIndex, TextSelector info, int index) {
        info.setStart(index);
        info.setEnd(index);
        info.setStart(info.getStart() - 1);
        info.setEnd(info.getEnd() + 1);
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            String charinfo = null;
            charinfo = textPage.getChars(index, 1);

            for (; info.getStart() >= 0; info.setStart(info.getStart() - 1)) {
                charinfo = textPage.getChars(info.getStart(), 1);
                if (charinfo == null) {
                    info.setStart(info.getStart() + 1);
                    break;
                }

                int i;
                for (i = 0; i < ChPassList.length; i++) {
                    if (ChPassList[i] == charinfo.charAt(0)) {
                        break;
                    }
                }
                if (i != ChPassList.length) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
            }
            if (info.getStart() < 0) {
                info.setStart(0);
            }

            for (; info.getEnd() >= 0; info.setEnd(info.getEnd() + 1)) {
                charinfo = textPage.getChars(info.getEnd(), 1);
                if (charinfo == null) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }

                int i;
                for (i = 0; i < ChPassList.length; i++) {
                    if (ChPassList[i] == charinfo.charAt(0)) {
                        break;
                    }
                }
                if (i != ChPassList.length) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
            }
            if (charinfo == null) {
                info.setEnd(info.getEnd() - 1);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return;
            }
        }
    }

    private int ctrlPoint = 0;


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        int action = motionEvent.getActionMasked();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    ctrlPoint = isControlPoint(mCurrentIndex, point);
                    if (ctrlPoint != 0) {
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    mAnnotationMenu.dismiss();
                    if (ctrlPoint != 0) {
                        //mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, mCurrentIndex);
                        OnSelectMove(pageIndex, point, mSelectInfo);
                        mSelectInfo.computeSelected(mPdfViewCtrl.getDoc().getPage(mCurrentIndex), mSelectInfo.getStart(), mSelectInfo.getEnd());
                        invalidateTouch(mCurrentIndex, mSelectInfo);
                        return true;
                    } else {
                        return false;
                    }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (mIsEdit == true) {
                        mText.clear();
                        reloadres();
                        if (mText.size() == 0)
                            return false;
                        mAnnotationMenu.setMenuItems(mText);
                        mMenuBox = new RectF();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), mMenuBox, mCurrentIndex);
                        mMenuBox.inset(-10, -10);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                        mAnnotationMenu.show(mMenuBox);
                        return false;
                    }
                    break;
                default:
                    break;
            }
        } catch (PDFException e1) {
            if (e1.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF pointF = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try{
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return false;
            }
            if (mIsMenuShow == true) {
                mAnnotationMenu.dismiss();
                mIsMenuShow = false;
                return true;
            }
            if (mIsEdit == true) {
                RectF rectF = new RectF(mSelectInfo.getBbox());
                mSelectInfo.clear();
                mCurrentIndex = pageIndex;
                mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                RectF rF = calculate(rectF, mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                getInvalidateRect(rect);
                mPdfViewCtrl.invalidate(rect);
                mIsEdit = false;
                mAnnotationMenu.dismiss();
                return true;
            }
            if (mAnnotationMenu.isShowing()) {
                mAnnotationMenu.dismiss();
            }

            mCurrentIndex = pageIndex;
            PointF pointPdfView = new PointF(pointF.x, pointF.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointPdfView, mCurrentIndex);
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            int index = textPage.getIndexAtPos(pointPdfView.x, pointPdfView.y, 30);
            if (index == -1 && (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() || DocumentManager.getInstance(mPdfViewCtrl).canModifyContents())) {
                mIsMenuShow = true;
                mMenuPoint = new PointF(pointF.x, pointF.y);
                mMenuPdfPoint = new PointF(mMenuPoint.x, mMenuPoint.y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(mMenuPdfPoint, mMenuPdfPoint, mCurrentIndex);

                mMenuBox = new RectF(pointF.x, pointF.y, pointF.x + 1, pointF.y + 1);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                reloadres();
                mAnnotationMenu.setMenuItems(mBlank);
                mAnnotationMenu.show(mMenuBox);
                mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                    @Override
                    public void onAMClick(int btType) {
                        if (btType == AnnotMenu.AM_BT_NOTE) {
                            PDFPage pdfPage = null;
                            try {
                                pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                            } catch (PDFException e1) {
                                e1.printStackTrace();
                            }
                            if (pdfPage == null) return;
                            PointF p = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
                            DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextAnnotContent(p, mCurrentIndex), mAddResult);
                        }

                        mIsMenuShow = false;
                        mAnnotationMenu.dismiss();
                        mMenuPoint = null;
                    }
                });
                return true;
            }
            if (index == -1) {
                return true;
            }
            String info = textPage.getChars(index, 1);
            if (index >= 0) {
                reloadres();
                if (mText.size() == 0)
                    return false;
                if ((info.charAt(0) >= 65 && info.charAt(0) <= 90) || (info.charAt(0) >= 97 && info.charAt(0) <= 122)) {
                    findEnWord(mCurrentIndex, mSelectInfo, index);
                } else {
                    findChWord(mCurrentIndex, mSelectInfo, index);
                }
                mSelectInfo.computeSelected(page, mSelectInfo.getStart(), mSelectInfo.getEnd());
                invalidateTouch(mCurrentIndex, mSelectInfo);
                mIsEdit = true;
            } else {
                mIsEdit = false;
            }
            if (mSelectInfo.getRectFList().size() == 0) {
                mIsEdit = false;
            }
            if (mIsEdit == true) {
                mMenuBox = new RectF(mSelectInfo.getBbox());
                mPdfViewCtrl.convertPdfRectToPageViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                mMenuBox.inset(-10, -10);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                mAnnotationMenu.setMenuItems(mText);
                mMenuPoint = null;
                mAnnotationMenu.show(mMenuBox);

                mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                    @Override
                    public void onAMClick(final int btType) {
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = null;
                            clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(mSelectInfo.getText(page));
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            RectF rectF = new RectF(mSelectInfo.getBbox());
                            mSelectInfo.clear();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                            RectF rF = calculate(rectF, mTmpRect);
                            Rect rect = new Rect();
                            rF.roundOut(rect);
                            getInvalidateRect(rect);
                            mPdfViewCtrl.invalidate(rect);
                            mIsEdit = false;
                            mAnnotationMenu.dismiss();
                            return;
                        }

                        try {
                            if (btType == AnnotMenu.AM_BT_HIGHLIGHT) {
                                PDFPage pdfPage = null;

                                pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);

                                if (pdfPage == null) return;
                                DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextMarkupContentAbs() {
                                    @Override
                                    public TextSelector getTextSelector() {
                                        mSelectInfo.setContents(mSelectInfo.getText(page));
                                        return mSelectInfo;
                                    }

                                    @Override
                                    public int getPageIndex() {
                                        return mCurrentIndex;
                                    }

                                    @Override
                                    public int getType() {
                                        return Annot.e_annotHighlight;
                                    }

                                    @Override
                                    public String getIntent() {
                                        return null;
                                    }

                                }, mAddResult);

                            } else if (btType == AnnotMenu.AM_BT_UNDERLINE) {
                                PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                                if (pdfPage == null) return;
                                DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextMarkupContentAbs() {
                                    @Override
                                    public TextSelector getTextSelector() {
                                        mSelectInfo.setContents(mSelectInfo.getText(page));
                                        return mSelectInfo;
                                    }

                                    @Override
                                    public int getPageIndex() {
                                        return mCurrentIndex;
                                    }

                                    @Override
                                    public int getType() {
                                        return Annot.e_annotUnderline;
                                    }

                                    @Override
                                    public String getIntent() {
                                        return null;
                                    }
                                }, mAddResult);

                            } else if (btType == AnnotMenu.AM_BT_STRIKEOUT) {
                                PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                                if (pdfPage == null) return;
                                DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextMarkupContentAbs() {
                                    @Override
                                    public TextSelector getTextSelector() {
                                        mSelectInfo.setContents(mSelectInfo.getText(page));
                                        return mSelectInfo;
                                    }

                                    @Override
                                    public int getPageIndex() {
                                        return mCurrentIndex;
                                    }

                                    @Override
                                    public int getType() {
                                        return Annot.e_annotStrikeOut;
                                    }

                                    @Override
                                    public String getIntent() {
                                        return null;
                                    }

                                }, mAddResult);

                            } else if (btType == AnnotMenu.AM_BT_SQUIGGLY) {
                                PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                                if (pdfPage == null) return;
                                DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextMarkupContentAbs() {
                                    @Override
                                    public TextSelector getTextSelector() {
                                        mSelectInfo.setContents(mSelectInfo.getText(page));
                                        return mSelectInfo;
                                    }

                                    @Override
                                    public int getPageIndex() {
                                        return mCurrentIndex;
                                    }

                                    @Override
                                    public int getType() {
                                        return Annot.e_annotSquiggly;
                                    }

                                    @Override
                                    public String getIntent() {
                                        return null;
                                    }

                                }, mAddResult);
                            }
                        } catch (PDFException e1) {
                            e1.printStackTrace();
                        }

                        mIsEdit = false;
                        mAnnotationMenu.dismiss();
                    }
                });
            }

        } catch (PDFException exception) {
            if (exception.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (mIsMenuShow == true) {
            mAnnotationMenu.dismiss();
            mIsMenuShow = false;
            return true;
        }
        if (mIsEdit == true) {
            RectF rectF = new RectF(mSelectInfo.getBbox());
            mSelectInfo.clear();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
            RectF rF = calculate(rectF, mTmpRect);
            Rect rect = new Rect();
            rF.roundOut(rect);
            getInvalidateRect(rect);
            mPdfViewCtrl.invalidate(rect);
            mIsEdit = false;
            mAnnotationMenu.dismiss();

            return true;
        } else {
            return false;
        }
    }

    private Event.Callback mAddResult = new Event.Callback() {
        @Override
        public void result(Event event, boolean success) {
            mSelectInfo.clear();
        }
    };

    protected TextSelector getSelectInfo() {
        return mSelectInfo;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCurrentIndex != pageIndex) return;
        if (mSelectInfo != null) {
            mPaint.setColor(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getSelectionHighlightColor());
            Rect clipRect = canvas.getClipBounds();
            for (RectF rect : mSelectInfo.getRectFList()) {
                RectF tmp = new RectF(rect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, tmp, mCurrentIndex);
                Rect r = new Rect();
                tmp.round(r);
                if (r.intersect(clipRect)) {
                    canvas.save();
                    canvas.drawRect(r, mPaint);
                    canvas.restore();
                }
            }
            if (mSelectInfo.getRectFList().size() > 0) {
                RectF start = new RectF(mSelectInfo.getRectFList().get(0));
                RectF end = new RectF(mSelectInfo.getRectFList().get(mSelectInfo.getRectFList().size() - 1));

                mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, mCurrentIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, mCurrentIndex);

                canvas.drawBitmap(mHandlerBitmap, start.left - mHandlerBitmap.getWidth(), start.top - mHandlerBitmap.getHeight(), null);
                canvas.drawBitmap(mHandlerBitmap, end.right, end.bottom, null);

                mPaint.setARGB(255, 76, 121, 164);
                canvas.drawLine(start.left, start.top - 1, start.left, start.bottom + 1, mPaint);
                canvas.drawLine(end.right, end.top - 1, end.right, end.bottom + 1, mPaint);
            }
        }

    }

    private boolean OnSelectMove(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null) return false;

        if (mCurrentIndex != pageIndex) return false;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, mCurrentIndex);
            int index = textPage.getIndexAtPos(point.x, point.y, 30);
            if (index < 0) return false;
            if (ctrlPoint == 1) {
                if (index <= selectInfo.getEnd())
                    selectInfo.setStart(index);
            } else if (ctrlPoint == 2) {
                if (index >= selectInfo.getStart())
                    selectInfo.setEnd(index);
            }
            textPage.release();
            mPdfViewCtrl.getDoc().closePage(mCurrentIndex);
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return false;
            }
        }
        return true;
    }

    private void invalidateTouch(int pageIndex, TextSelector selectInfo) {
        if (selectInfo == null) return;
        RectF rectF = new RectF();
        mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    public RectF calculate(RectF desRectF, RectF srcRectF) {
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        mTmpDesRect.set(desRectF);
        if (count == 2) {
            mTmpDesRect.union(srcRectF);
            RectF rectF = new RectF();
            rectF.set(mTmpDesRect);
            mTmpDesRect.intersect(srcRectF);
            rectF.intersect(mTmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return mTmpDesRect;
        } else {
            mTmpDesRect.union(srcRectF);
            return mTmpDesRect;
        }
    }

    private int isControlPoint(int pageIndex, PointF point) {
        if (mSelectInfo != null && mSelectInfo.getRectFList().size() > 0) {
            RectF mStart = new RectF(mSelectInfo.getRectFList().get(0));
            RectF mEnd = new RectF(mSelectInfo.getRectFList().get(mSelectInfo.getRectFList().size() - 1));
            mPdfViewCtrl.convertPdfRectToPageViewRect(mStart, mStart, pageIndex);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mEnd, mEnd, pageIndex);

            RectF startHandler = new RectF(mStart.left - mHandlerBitmap.getWidth(), mStart.top - mHandlerBitmap.getHeight(),
                    mStart.left, mStart.top);
            RectF endHandler = new RectF(mEnd.right, mEnd.bottom,
                    mEnd.right + mHandlerBitmap.getWidth(), mEnd.bottom + mHandlerBitmap.getHeight());
            startHandler.inset(-HANDLE_AREA, -HANDLE_AREA);
            endHandler.inset(-HANDLE_AREA, -HANDLE_AREA);
            if (mStart != null && startHandler.contains(point.x, point.y))
                return 1;
            if (mEnd != null && endHandler.contains(point.x, point.y))
                return 2;
        }
        return 0;

    }

    public void dismissMenu() {
        if (mIsMenuShow == true) {
            mAnnotationMenu.dismiss();
            mIsMenuShow = false;
        }
        if (mIsEdit == true) {
            RectF rectF = new RectF(mSelectInfo.getBbox());
            mSelectInfo.clear();
            int _pageIndex = mPdfViewCtrl.getCurrentPage();
            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, _pageIndex);
                RectF rF = calculate(rectF, mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                getInvalidateRect(rect);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, _pageIndex);
                mPdfViewCtrl.invalidate(rect);
            }
            mIsEdit = false;
            mAnnotationMenu.dismiss();

        }
    }

    public void getInvalidateRect(Rect rect) {
        rect.top -= mHandlerBitmap.getHeight();
        rect.bottom += mHandlerBitmap.getHeight();
        rect.left -= mHandlerBitmap.getWidth() / 2;
        rect.right += mHandlerBitmap.getWidth() / 2;
        rect.inset(-20, -20);
    }

    public void onDrawForAnnotMenu(Canvas canvas) {
        RectF bboxRect;
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) {
            return;
        }
        if (mIsEdit == false && mIsMenuShow == false)
            return;
        if (mMenuPoint != null) {
            PointF temp = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(mMenuPdfPoint, temp, mCurrentIndex);
            bboxRect = new RectF(temp.x, temp.y, temp.x + 1, temp.y + 1);
        } else {
            bboxRect = new RectF(mSelectInfo.getBbox());
            mPdfViewCtrl.convertPdfRectToPageViewRect(bboxRect, bboxRect, mCurrentIndex);
            bboxRect.inset(-10, -10);
        }

        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bboxRect, bboxRect, mCurrentIndex);
        mAnnotationMenu.update(bboxRect);
    }


}


class TextAnnotContent implements NoteAnnotContent {
    private PointF p = new PointF();
    private int pageIndex;

    public TextAnnotContent(PointF p, int pageIndex) {
        this.p.set(p.x, p.y);
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public int getType() {
        return Annot.e_annotNote;
    }

    @Override
    public String getNM() {
        return null;
    }

    @Override
    public RectF getBBox() {
        return new RectF(p.x, p.y, p.x, p.y);
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
        return null;
    }

    @Override
    public String getContents() {
        return null;
    }

    @Override
    public String getReplyTo() {
        return null;
    }

    @Override
    public String getReplyType() {
        return null;
    }

    @Override
    public String getIntent() {
        return null;
    }

    @Override
    public String getIcon() {
        return "";
    }

    @Override
    public String getFromType() {
        return Module.MODULE_NAME_SELECTION;
    }
}