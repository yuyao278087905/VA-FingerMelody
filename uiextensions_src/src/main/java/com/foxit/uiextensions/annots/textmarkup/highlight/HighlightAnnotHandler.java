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
package com.foxit.uiextensions.annots.textmarkup.highlight;

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
import com.foxit.sdk.pdf.annots.Highlight;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;


class HighlightAnnotHandler implements AnnotHandler {
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
    private PropertyBar mAnnotPropertyBar;
    private boolean mIsEditProperty;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    private AppAnnotUtil mAppAnnotUtil;
    private int mPaintBoxOutset;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    public HighlightAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(mAppAnnotUtil.getAnnotBBoxPathEffect());

        mMenuItems = new ArrayList<Integer>();

        mPaintBoxOutset = AppResource.getDimensionPixelSize(mContext, R.dimen.annot_highlisht_paintbox_outset);
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    @Override
    public int getType() {
        return Annot.e_annotHighlight;
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
        return getAnnotBBox(annot).contains(point.x, point.y);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    private Rect mRect = new Rect();
    private RectF mRectF = new RectF();

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (mPdfViewCtrl == null || annot == null || !(annot instanceof Highlight)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
        try {
            int annotPageIndex = annot.getPage().getIndex();

            //update page
            if (pageIndex != annotPageIndex) return;

            if (mLastAnnot == annot) {
                RectF rectF = annot.getRect();
                mRectF.set(rectF.left, rectF.top, rectF.right, rectF.bottom);
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, pageIndex);
                deviceRt.roundOut(mRect);
                mRect.inset(-mPaintBoxOutset, -mPaintBoxOutset);
                canvas.save();
                canvas.drawRect(mRect, mPaintBbox);
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

        Annot annot = null;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null || !(annot instanceof Highlight))
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


    private int mUndoColor;
    private int mUndoOpacity;

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_HIGHLIGHT.length];

    private int getPBCustomColor() {
        int color = PropertyBar.PB_COLORS_HIGHLIGHT[0];
        return color;
    }


    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotPropertyBar;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mUndoColor = (int) annot.getBorderColor();

            mUndoOpacity = (int) (((Highlight) annot).getOpacity() * 255f + 0.5f);

            mPaintBbox.setColor((int) annot.getBorderColor() | 0xFF000000);
            mMenuItems.clear();
            if (DocumentManager.getInstance(mPdfViewCtrl).canCopy()) {
                mMenuItems.add(AnnotMenu.AM_BT_COPY);
            }

            mAnnotPropertyBar.setArrowVisible(false);
            if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            } else {
                mMenuItems.add(AnnotMenu.AM_BT_STYLE);
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }

            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {

                @Override
                public void onAMClick(int type) {
                    try {
                        if (AnnotMenu.AM_BT_COPY == type) {
                            @SuppressWarnings("deprecation")
                            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(annot.getContent());
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        } else if (AnnotMenu.AM_BT_DELETE == type) {
                            deleteAnnot(annot, null);
                        } else if (AnnotMenu.AM_BT_STYLE == type) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            System.arraycopy(PropertyBar.PB_COLORS_HIGHLIGHT, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Highlight) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            RectF annotRectF = annot.getRect();
                            int _pageIndex = annot.getPage().getIndex();

                            RectF deviceRt = new RectF();
                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRt, _pageIndex)) {
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, annotRectF, _pageIndex);
                                }
                            }

                            mAnnotPropertyBar.show(annotRectF, false);
                            mAnnotPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
                        } else if (AnnotMenu.AM_BT_COMMENT == type) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParent, annot);
                        } else if (AnnotMenu.AM_BT_REPLY == type) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, annot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });

            int _pageIndex = annot.getPage().getIndex();
            RectF annotRectF = annot.getRect();

            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRt, _pageIndex);
                Rect rect = rectRoundOut(deviceRt, 0);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, annotRectF, _pageIndex);
                mAnnotMenu.show(annotRectF);
                mPdfViewCtrl.refresh(_pageIndex, rect);
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mAnnotMenu.dismiss();
        mMenuItems.clear();
        if (mIsEditProperty) {
            mIsEditProperty = false;
            mAnnotPropertyBar.dismiss();
        }

        if (mIsAnnotModified && needInvalid) {
            if (mUndoColor != mModifyAnnotColor || mUndoOpacity != mModifyOpacity) {
                modifyAnnot(annot, mModifyColor, mModifyOpacity, null, null);
            }
        } else if (mIsAnnotModified) {
            try {
                annot.setBorderColor(mUndoColor);

                ((Highlight) annot).setOpacity(mUndoOpacity / 255f);
                annot.resetAppearanceStream();
            } catch (PDFException e) {
                if (e.getLastError() == PDFException.e_errOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
                return;
            }
        }
        mIsAnnotModified = false;
        if (needInvalid) {
            try {
                int _pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    RectF rectF = new RectF();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), rectF, _pageIndex);
                    Rect rect = rectRoundOut(rectF, 2);
                    mPdfViewCtrl.refresh(_pageIndex, rect);
                    mLastAnnot = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }
        mLastAnnot = null;
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
        deleteAnnot(annot, result);
    }

    public void modifyAnnotColor(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        mModifyColor = color & 0xFFFFFF;
        try {
            mModifyOpacity = (int) (((Highlight) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Highlight) annot).setOpacity(mModifyOpacity / 255f);
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

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if (((Highlight) annot).getOpacity() * 255f != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Highlight) annot).setOpacity(mModifyOpacity / 255f);
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


    private void modifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final Event.Callback callback) {

        try {
            PDFPage page = annot.getPage();
            if (page == null) {
                if (callback != null) {
                    callback.result(null, false);
                }
                return;
            }

            if (modifyDate == null) {
                modifyDate = new DateTime();
            }
            annot.setModifiedDateTime(modifyDate);

            annot.setBorderColor(color);
            ((Highlight) annot).setOpacity(opacity / 255f);
            annot.resetAppearanceStream();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(page, annot);
            if (mPdfViewCtrl.isPageVisible(page.getIndex())) {
                RectF annotRectF = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, page.getIndex());
                mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(annotRectF));
            }

            if (callback != null) {
                callback.result(null, true);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void deleteAnnot(final Annot annot, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }
        PDFPage page = null;
        int pageIndex = -1;
        RectF annotRectF = null;
        try {
            annotRectF = annot.getRect();
            page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            //remove all replies of the highlight
            ((Markup)annot).removeAllReplies();
            page.removeAnnot(annot);

            pageIndex = page.getIndex();
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }

        RectF deviceRectF = new RectF();
        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, pageIndex);
            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(deviceRectF));
            mLastAnnot = null;
        } else {
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                mLastAnnot = null;
            }
        }

        if (result != null) {
            result.result(null, true);
        }
    }

    private void invalidateForToolModify(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            RectF rectF = annot.getRect();
            RectF pvRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, pvRect, pageIndex);
            Rect rect = rectRoundOut(pvRect, mBBoxSpace);
            rect.inset(-mPaintBoxOutset, -mPaintBoxOutset);
            mPdfViewCtrl.refresh(pageIndex, rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        Rect rect = new Rect();
        rectF.roundOut(rect);
        rect.inset(-roundSize, -roundSize);
        return rect;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, Event.Callback result) {
        if (mToolHandler != null) {
            if (!(contentSupplier instanceof TextMarkupContent)) {
                mToolHandler.setFromSelector(true);
            }
            mToolHandler.addAnnot(pageIndex, contentSupplier, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    private HighlightToolHandler mToolHandler;

    public void setToolHandler(HighlightToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        try {
            mUndoColor = (int) annot.getBorderColor();
            mUndoOpacity = (int) (((Highlight) annot).getOpacity() * 255f);
            annot.setContent(content.getContents());

            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor() | 0xFF000000);
            }
            modifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (mPdfViewCtrl == null || annot == null || !(annot instanceof Highlight)) return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this) return;
        try {
            int annotPageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                mRectF.set(annot.getRect());
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, annotPageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, mRectF, annotPageIndex);
                if (mIsEditProperty) {
                    ((PropertyBarImpl)mAnnotPropertyBar).onConfigurationChanged(mRectF);
                }
                mAnnotMenu.update(mRectF);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}
