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
package com.foxit.uiextensions.annots.textmarkup.squiggly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Squiggly;
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


class SquigglyAnnotHandler implements AnnotHandler {

    private PropertyBar mAnnotPropertyBar;
    private Context mContext;

    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private AppAnnotUtil mAppAnnotUtil;
    private AnnotMenu mAnnotMenu;
    private Annot mLastAnnot;
    private Paint mPaintBbox;

    private int mBBoxSpace;

    private RectF mDrawLocal_tmpF;

    private int mModifyColor;
    private int mModifyOpacity;
    private int mModifyAnnotColor;

    private ArrayList<Integer> mMenuItems;
    private SquigglyToolHandler mSquigglyToolHandler;

    private boolean mIsEditProperty;
    private boolean mIsAnnotModified;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public SquigglyAnnotHandler(Context context, PDFViewCtrl pdfViewer, ViewGroup parent) {
        mContext = context;

        mPdfViewCtrl = pdfViewer;
        mParent = parent;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);

        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(mAppAnnotUtil.getAnnotBBoxPathEffect());

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

    public void setToolHandler(SquigglyToolHandler toolHandler) {
        mSquigglyToolHandler = toolHandler;
    }

    @Override
    public int getType() {
        return Annot.e_annotSquiggly;
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

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_SQUIGGLY.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_SQUIGGLY[0];
    }

    private int mTmpUndoColor;
    private int mTmpUndoOpacity;

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);
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
                            String text = annot.getContent();
                            clipboard.setText(annot.getContent());
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_DELETE) {
                            DeleteAnnot(annot, null);
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            System.arraycopy(PropertyBar.PB_COLORS_SQUIGGLY, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Squiggly) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            RectF annotRectF = new RectF();
                            int _pageIndex = annot.getPage().getIndex();

                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                            }
                            mAnnotPropertyBar.show(annotRectF, false);
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
            int _pageIndex = annot.getPage().getIndex();

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
        mAnnotMenu.dismiss();
        try {
            if (mIsEditProperty) {
                mIsEditProperty = false;
            }

            if (mIsAnnotModified && needInvalid) {
                if (mTmpUndoColor != mModifyAnnotColor || mTmpUndoOpacity != mModifyOpacity) {
                    ModifyAnnot(annot, mModifyColor, mModifyOpacity, null, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((Squiggly) annot).setOpacity(mTmpUndoOpacity / 255f);
                annot.resetAppearanceStream();
            }
            mIsAnnotModified = false;
            if (needInvalid) {
                int _pageIndex = 0;

                _pageIndex = annot.getPage().getIndex();

                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    RectF rectF = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), rectF, _pageIndex);
                    Rect rect = TextMarkupUtil.rectRoundOut(rectF, 0);
                    mPdfViewCtrl.refresh(_pageIndex, rect);
                    mLastAnnot = null;
                }
                return;
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
        mLastAnnot = null;
    }

    private void ModifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final Event.Callback callback) {
        try {
            PDFPage page = annot.getPage();

            if (null == page) return;
            if (modifyDate == null) {
                modifyDate = new DateTime();
                annot.setBorderColor(mModifyAnnotColor);
            } else {
                annot.setBorderColor(color);
                ((Squiggly) annot).setOpacity(opacity / 255f);
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

    private void DeleteAnnot(final Annot annot, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        RectF annotRectF = null;
        PDFPage page = null;
        try {
            annotRectF = annot.getRect();
            page = annot.getPage();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            //remove all replies of the squiggly
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
    public void addAnnot(int pageIndex, AnnotContent content, Event.Callback result) {
        if (mSquigglyToolHandler != null) {
            if (content instanceof TextMarkupContent) {
                mSquigglyToolHandler.addAnnot(pageIndex, null,
                        content.getBBox(), null, result);
            } else {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(content);
                SquigglyToolHandler.SelectInfo info = mSquigglyToolHandler.mSelectInfo;
                info.clear();
                info.mIsFromTS = true;
                info.mStartChar = tmSelector.getTextSelector().getStart();
                info.mEndChar = tmSelector.getTextSelector().getEnd();
                mSquigglyToolHandler.setFromSelector(true);
                mSquigglyToolHandler.SelectCountRect(pageIndex, info);
                mSquigglyToolHandler.OnSelectRelease(pageIndex, info, result);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        if (content == null) return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);
            annot.setContent(content.getContents());

            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor());
            }
            ModifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), result);
        } catch (PDFException e) {
            e.printStackTrace();
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

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof Squiggly)) return;
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
            PointF pdfPt = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null || !(annot instanceof Squiggly))
                annot = page.getAnnotAtPos(pdfPt, AppAnnotUtil.ANNOT_SELECT_TOLERANCE);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pdfPt)) {
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
        if (curAnnot == null || !(curAnnot instanceof Squiggly)) return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this) return;

        try {
            int annotPageIndex = curAnnot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(curAnnot.getRect(), mDrawLocal_tmpF, annotPageIndex);
                RectF canvasRt = new RectF();
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDrawLocal_tmpF, canvasRt, annotPageIndex);
                if (mIsEditProperty) {
                    ((PropertyBarImpl) mAnnotPropertyBar).onConfigurationChanged(canvasRt);
                }
                mAnnotMenu.update(canvasRt);
            }

        } catch (PDFException e) {
            e.printStackTrace();
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

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), Annot.e_annotSquiggly) == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    public void modifyAnnotColor(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = color & 0xFFFFFF;
            mModifyOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Squiggly) annot).setOpacity(mModifyOpacity / 255f);
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
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if (((Squiggly) annot).getOpacity() * 255f != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Squiggly) annot).setOpacity(mModifyOpacity / 255f);
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

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}
