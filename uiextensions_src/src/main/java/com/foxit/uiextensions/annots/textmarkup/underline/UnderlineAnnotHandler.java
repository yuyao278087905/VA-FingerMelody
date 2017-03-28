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
package com.foxit.uiextensions.annots.textmarkup.underline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Underline;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;


class UnderlineAnnotHandler implements AnnotHandler {

    private Context mContext;
    private Paint mPaintBbox;
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;

    private int mModifyColor;
    private int mModifyOpacity;
    private int mModifyAnnotColor;
    private boolean mIsAnnotModified;
    private Annot mLastAnnot;
    private int mBBoxSpace;
    private UnderlineToolHandler mUnderlineToolHandler;

    private boolean mIsEditProperty;
    private PropertyBar mAnnotPropertyBar;

    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private AppAnnotUtil mAppAnnotUtil;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public UnderlineAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());

        mDrawLocal_tmpF = new RectF();

        mMenuItems = new ArrayList<Integer>();

    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotPropertyBar;
    }

    public void setToolHandler(UnderlineToolHandler toolHandler) {
        mUnderlineToolHandler = toolHandler;
    }

    @Override
    public int getType() {
        return Annot.e_annotUnderline;
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
        RectF bbox = getAnnotBBox(annot);
        return bbox.contains(point.x, point.y);
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_UNDERLINE.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_UNDERLINE[0];
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Markup) annot).getOpacity() * 255f + 0.5f);
            mPaintBbox.setColor(mTmpUndoColor | 0xFF000000);

            mAnnotPropertyBar.setArrowVisible(false);
            resetMenuItems();
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = null;
                            clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(annot.getContent());
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_DELETE) {
                            DeleteAnnot(annot, null);
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            System.arraycopy(PropertyBar.PB_COLORS_UNDERLINE, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Markup) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            RectF annotRectF = new RectF();
                            int _pageIndex = -1;

                            _pageIndex = annot.getPage().getIndex();

                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                            }
                            mAnnotPropertyBar.show(annotRectF, false);//!App.instance().getDisplay().isPad());
                            mAnnotPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
                        } else if (btType == AnnotMenu.AM_BT_COMMENT) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, annot);
                        } else if (btType == AnnotMenu.AM_BT_REPLY) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, annot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });
            RectF annotRectF = annot.getRect();
            int _pageIndex = 0;
            _pageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                Rect rect = TextMarkupUtil.rectRoundOut(annotRectF, 0);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                mPdfViewCtrl.refresh(_pageIndex, rect);
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mLastAnnot = annot;
                }

            } else {
                mLastAnnot = annot;
            }
            mAnnotMenu.show(annotRectF);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        try {
            mAnnotMenu.dismiss();
            if (mIsEditProperty) {
                mIsEditProperty = false;
            }

            if (mIsAnnotModified && needInvalid) {
                if (mTmpUndoColor != mModifyAnnotColor || mTmpUndoOpacity != mModifyOpacity) {
                    ModifyAnnot(annot, mModifyColor, mModifyOpacity, null, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((Markup) annot).setOpacity(mTmpUndoOpacity);
                annot.resetAppearanceStream();
            }
            if (needInvalid) {
                mIsAnnotModified = false;
                int _pageIndex = 0;

                _pageIndex = annot.getPage().getIndex();

                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    RectF rectF = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), rectF, _pageIndex);
                    Rect rect = TextMarkupUtil.rectRoundOut(rectF, 0);
                    mPdfViewCtrl.refresh(_pageIndex, rect);
                }
                return;
            }
            mLastAnnot = null;
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void ModifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final Event.Callback resultCallback) {
        PDFPage page = null;
        try {
            page = annot.getPage();

            if (null == page) return;

            if (modifyDate == null) {
                modifyDate = new DateTime();
                annot.setBorderColor(mModifyAnnotColor);
            } else {
                annot.setBorderColor(color);
                ((Markup) annot).setOpacity(opacity / 255f);
            }

            annot.setModifiedDateTime(modifyDate);

            annot.resetAppearanceStream();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(page, annot);
            int _pageIndex = page.getIndex();
            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                RectF annotRectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(annotRectF));
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void DeleteAnnot(final Annot annot, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }
        PDFPage page = null;
        RectF annotRectF = null;
        try {
            annotRectF = annot.getRect();
            page = annot.getPage();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            //remove all replies of the underline
            ((Markup)annot).removeAllReplies();
            page.removeAnnot(annot);

            int _pageIndex = page.getIndex();
            RectF deviceRectF = new RectF();
            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, _pageIndex);
                mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(deviceRectF));
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, Event.Callback result) {
        if (mUnderlineToolHandler != null) {
            if (contentSupplier instanceof TextMarkupContent) {
                mUnderlineToolHandler.AddAnnot(pageIndex, contentSupplier, null,
                        contentSupplier.getBBox(), null, result);
            } else {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(contentSupplier);
                UnderlineToolHandler.SelectInfo info = mUnderlineToolHandler.mSelectInfo;
                info.clear();
                info.mIsFromTS = true;
                info.mStartChar = tmSelector.getTextSelector().getStart();
                info.mEndChar = tmSelector.getTextSelector().getEnd();
                mUnderlineToolHandler.setFromSelector(true);
                mUnderlineToolHandler.SelectCountRect(contentSupplier.getPageIndex(), info);
                mUnderlineToolHandler.OnSelectRelease(contentSupplier.getPageIndex(), info, result);
            }
        }
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
        DeleteAnnot(annot, result);
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {

        return false;
    }

    private int mTmpUndoColor;
    private int mTmpUndoOpacity;
    private RectF mDrawLocal_tmpF;

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof Markup)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;

        int annotPageIndex = 0;
        try {
            annotPageIndex = annot.getPage().getIndex();

            if (pageIndex != annotPageIndex) return;
            if (mLastAnnot == annot) {
                RectF rectF = annot.getRect();
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, deviceRt, pageIndex);
                Rect rectBBox = TextMarkupUtil.rectRoundOut(deviceRt, mBBoxSpace);
                canvas.save();
                canvas.drawRect(rectBBox, mPaintBbox);
                canvas.restore();

            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        try{
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null  || !(annot instanceof Underline))
                annot = page.getAnnotAtPos(pointF, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pointF)) {
                    return true;
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null || !(curAnnot instanceof Markup)) return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this)
            return;

        try {
            int annotPageIndex = curAnnot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(curAnnot.getRect(), mDrawLocal_tmpF, annotPageIndex);
                RectF canvasRt = new RectF();
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDrawLocal_tmpF, canvasRt, annotPageIndex);
                if (mIsEditProperty) {
                    ((PropertyBarImpl)mAnnotPropertyBar).onConfigurationChanged(canvasRt);
                }
                mAnnotMenu.update(canvasRt);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotColor(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = color & 0xFFFFFF;
            mModifyOpacity = (int) (((Markup) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Markup) annot).setOpacity(mModifyOpacity / 255f);
                annot.resetAppearanceStream();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void invalidateForToolModify(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            RectF rectF = annot.getRect();
            RectF pvRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, pvRect, pageIndex);
            Rect rect = TextMarkupUtil.rectRoundOut(pvRect, mBBoxSpace);
            rect.inset(-1, -1);
            mPdfViewCtrl.refresh(pageIndex, rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if (((Markup) annot).getOpacity() * 255f != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Markup) annot).setOpacity(mModifyOpacity / 255f);
                annot.resetAppearanceStream();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    public void resetMenuItems() {
        mMenuItems.clear();

        if (DocumentManager.getInstance(mPdfViewCtrl).canCopy()) {
            mMenuItems.add(AnnotMenu.AM_BT_COPY);
        }
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_REPLY);
            mMenuItems.add(AnnotMenu.AM_BT_DELETE);
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        if (content == null) return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Markup) annot).getOpacity() * 255f);
            try {
                annot.setContent(content.getContents());
            } catch (PDFException e) {
                e.printStackTrace();
            }
            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor());
            }
            ModifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}