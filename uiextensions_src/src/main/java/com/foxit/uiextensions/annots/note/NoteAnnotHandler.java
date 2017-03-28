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
package com.foxit.uiextensions.annots.note;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

import static com.foxit.uiextensions.utils.AppAnnotUtil.ANNOT_SELECT_TOLERANCE;

class NoteAnnotHandler implements AnnotHandler {

    private Context mContext;
    private ViewGroup mParentView;
    private PDFViewCtrl mPdfViewCtrl;
    private AppDisplay mDisplay;
    private Paint mPaint;
    private Paint mPaintOut;
    private Annot mBitmapAnnot;

    private PropertyBar mPropertyBar;
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private EditText mET_Content;
    private TextView mDialog_title;
    private Button mCancel;
    private Button mSave;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private int mBBoxSpace;
    private boolean mTouchCaptured = false;
    private boolean mIsEditProperty;
    private boolean mIsModify;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private RectF mDocViewerRectF = new RectF(0, 0, 0, 0);

    NoteAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
        mContext = context;
        mParentView = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mDisplay = new AppDisplay(context);
        AppAnnotUtil annotUtil = new AppAnnotUtil(mContext);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Style.STROKE);
        mPaintOut.setPathEffect(annotUtil.getAnnotBBoxPathEffect());
        mPaintOut.setStrokeWidth(annotUtil.getAnnotBBoxStrokeWidth());

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuItems = new ArrayList<Integer>();

        mPropertyBar = new PropertyBarImpl(context, pdfViewCtrl, mParentView);
        mAnnotMenu = new AnnotMenuImpl(mContext, mParentView);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    @Override
    public int getType() {
        return Annot.e_annotNote;
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
        RectF rectF = getAnnotBBox(annot);

        if (mPdfViewCtrl != null) {
            try {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
            } catch (Exception e) {
                e.printStackTrace();
            }

            rectF.inset(-10, -10);
        }
        return rectF.contains(point.x, point.y);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        PointF pageViewPt = new PointF(point.x, point.y);
        PointF pdfPt = new PointF();
        mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPt, pageIndex);
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            float envX = point.x;
            float envY = point.y;
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null || !(annot instanceof Note))
                annot = page.getAnnotAtPos(pdfPt, ANNOT_SELECT_TOLERANCE);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        try {
                            if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                                mDownPoint.set(envX, envY);
                                mLastPoint.set(envX, envY);
                                mTouchCaptured = true;
                                return true;
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    try {
                        if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                                && pageIndex == annot.getPage().getIndex()
                                && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                            if (envX != mLastPoint.x || envY != mLastPoint.y) {
                                RectF pageViewRectF = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRectF, pageViewRectF, annot.getIndex());
                                RectF rectInv = new RectF(pageViewRectF);
                                RectF rectChanged = new RectF(pageViewRectF);

                                rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);

                                float adjustx = 0;
                                float adjusty = 0;
                                if (rectChanged.left < 0) {
                                    adjustx = -rectChanged.left;
                                }
                                if (rectChanged.top < 0) {
                                    adjusty = -rectChanged.top;
                                }
                                if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                                    adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                                }
                                if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                                    adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                                }
                                rectChanged.offset(adjustx, adjusty);
                                rectInv.union(rectChanged);
                                rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));
                                RectF rectInViewerF = new RectF(rectChanged);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                if (mAnnotMenu.isShowing()) {
                                    mAnnotMenu.dismiss();
                                    mAnnotMenu.update(rectInViewerF);
                                }
                                if (mIsEditProperty) {
                                    mPropertyBar.dismiss();
                                }
                                mLastPoint.set(envX, envY);
                                mLastPoint.offset(adjustx, adjusty);
                            }
                            return true;
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() &&
                            mPdfViewCtrl.getCurrentPage() == pageIndex
                            && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                        RectF pageRectF = annot.getRect();
                        RectF pageViewRectF = new RectF();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageRectF, pageViewRectF, pageIndex);

                        RectF rectInv = new RectF(pageViewRectF);
                        RectF rectChanged = new RectF(pageViewRectF);

                        rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);
                        float adjustx = 0;
                        float adjusty = 0;
                        if (rectChanged.left < 0) {
                            adjustx = -rectChanged.left;
                        }
                        if (rectChanged.top < 0) {
                            adjusty = -rectChanged.top;
                        }
                        if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                        }
                        if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                        }
                        rectChanged.offset(adjustx, adjusty);
                        rectInv.union(rectChanged);
                        rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);

                        Rect invalidateRect = AppDmUtil.rectFToRect(rectInv);
                        mPdfViewCtrl.refresh(pageIndex, invalidateRect);
                        RectF rectInViewerF = new RectF(rectChanged);

                        RectF canvasRectF = new RectF();
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, canvasRectF, pageIndex);
                        if (mIsEditProperty) {
                            if (mPropertyBar.isShowing()) {
                                mPropertyBar.update(canvasRectF);
                            } else {
                                mPropertyBar.show(canvasRectF, false);
                            }
                        } else {
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(canvasRectF);
                            } else {
                                mAnnotMenu.show(canvasRectF);
                            }
                        }

                        RectF pageRect = new RectF();
                        mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, pageRect, pageIndex);
                        if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {

                            int color = (int) annot.getBorderColor();
                            float opacity = ((Note) annot).getOpacity();
                            String iconName = ((Note) annot).getIconName();
                            modifyAnnot(annot, color, opacity, iconName,
                                    pageRect, annot.getContent());
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        return true;
                    }
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
            }
        } catch (PDFException e1) {
            if (e1.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null)
            return;
        try {
            int id = annot.getPage().getIndex();
            if (mBitmapAnnot == annot && id == pageIndex) {
                canvas.save();
                RectF frameRectF = new RectF();
                RectF rect2 = annot.getRect();

                mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, annot.getPage().getIndex());
                rect2.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                frameRectF.set(rect2.left - mBBoxSpace, rect2.top - mBBoxSpace, rect2.right + mBBoxSpace, rect2.bottom + mBBoxSpace);
                int color = (int) (annot.getBorderColor() | 0xFF000000);
                mPaintOut.setColor(color);
                canvas.drawRect(frameRectF, mPaintOut);
                canvas.restore();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        PointF pagePt = new PointF();
        mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pagePt, pageIndex);
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null || !(annot instanceof Note))
                annot = page.getAnnotAtPos(pagePt, ANNOT_SELECT_TOLERANCE);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                    return true;
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);

                    return true;
                }

            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);

            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (AppUtil.isFastDoubleClick()) {
            return true;
        }
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        PointF pagePt = new PointF();
        mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pagePt, pageIndex);
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot == null || !(annot instanceof Note))
                annot = page.getAnnotAtPos(pagePt, ANNOT_SELECT_TOLERANCE);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } else {
                if (annot == null)
                    return false;
                tempUndoColor = (int) annot.getBorderColor();
                tempUndoOpacity = (int) ((Note) annot).getOpacity();
                tempUndoIconType = ((Note) annot).getIconName();
                tempUndoBBox = annot.getRect();
                tempUndoContents = annot.getContent();
                showDialog(annot);

            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return true;
    }

    private int tempUndoColor;
    private int tempUndoOpacity;
    private String tempUndoIconType;
    private RectF tempUndoBBox;
    private String tempUndoContents;

    @Override
    public void onAnnotSelected(final Annot annot, final boolean needInvalid) {
        try {
            tempUndoColor = (int) annot.getBorderColor();

            tempUndoOpacity = (int) ((Note) annot).getOpacity();
            tempUndoIconType = ((Note) annot).getIconName();
            tempUndoBBox = annot.getRect();
            tempUndoContents = annot.getContent();
            mBitmapAnnot = annot;
            tempUndoColor = (int) annot.getBorderColor();
            mAnnotMenu.dismiss();
            mMenuItems.clear();
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
                public void onAMClick(int btType) {

                    mAnnotMenu.dismiss();
                    if (btType == AnnotMenu.AM_BT_COMMENT) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);//, true);

                        UIAnnotReply.showComments(mContext, mPdfViewCtrl, mParentView, annot);
                    } else if (btType == AnnotMenu.AM_BT_REPLY) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);//, true);

                        UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParentView, annot);
                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
                        delAnnot(mPdfViewCtrl, annot, null);

                    } else if (btType == AnnotMenu.AM_BT_STYLE) {
                        mIsEditProperty = true;

                        int[] colors = new int[PropertyBar.PB_COLORS_TEXT.length];
                        System.arraycopy(PropertyBar.PB_COLORS_TEXT, 0, colors, 0, colors.length);
                        colors[0] = PropertyBar.PB_COLORS_TEXT[0];
                        mPropertyBar.setColors(colors);

                        try {
                            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Note) annot).getOpacity() * 255f + 0.5f)));
                            String iconName = ((Note) annot).getIconName();
                            mPropertyBar.setProperty(PropertyBar.PROPERTY_ANNOT_TYPE, iconName);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);

                        mPropertyBar.setArrowVisible(false);
                        long propertys = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY | PropertyBar.PROPERTY_ANNOT_TYPE;
                        mPropertyBar.reset(propertys);

                        mPropertyBar.show(mDocViewerRectF, false);
                    }
                }
            });

            RectF viewRect = new RectF(annot.getRect());

            RectF modifyRectF = new RectF(viewRect);
            int pageIndex = annot.getPage().getIndex();

            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
            mAnnotMenu.show(viewRect);

            // change modify status
            mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                mBitmapAnnot = annot;
            }

            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        // configure annotation menu
        mAnnotMenu.dismiss();
        if (mIsEditProperty) {
            mIsEditProperty = false;
        }
        mPropertyBar.dismiss();

        try {
            PDFPage page = annot.getPage();
            RectF pdfRect = annot.getRect();

            RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
            if (mIsModify && needInvalid) {
                if (tempUndoColor == annot.getBorderColor() && tempUndoOpacity == ((Note) annot).getOpacity()
                        && tempUndoBBox.equals(annot.getRect()) && tempUndoIconType == ((Note) annot).getIconName()) {
                    modifyAnnot(annot, (int) annot.getBorderColor(), ((Note) annot).getOpacity(),
                            ((Note) annot).getIconName(), annot.getRect(), annot.getContent());
                }
            } else if (mIsModify) {
                annot.setBorderColor(tempUndoColor);
                ((Note) annot).setOpacity(tempUndoOpacity);
                ((Note) annot).setIconName(tempUndoIconType);
                annot.move(tempUndoBBox);
                annot.setContent(tempUndoContents);
            }
            mIsModify = false;
            if (needInvalid) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, page.getIndex());
                mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(viewRect));
                mBitmapAnnot = null;
                return;
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
        mBitmapAnnot = null;
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();

        try {
            if (curAnnot != null && ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), curAnnot.getType()) == this) {
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    RectF bboxRect = curAnnot.getRect();

                    mPdfViewCtrl.convertPdfRectToPageViewRect(bboxRect, bboxRect, curAnnot.getPage().getIndex());
                    bboxRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bboxRect, bboxRect, curAnnot.getPage().getIndex());

                    mAnnotMenu.update(bboxRect);
                    mDocViewerRectF.set(bboxRect);
                    if (mIsEditProperty) {
                        ((PropertyBarImpl) mPropertyBar).onConfigurationChanged(mDocViewerRectF);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void delAnnot(final PDFViewCtrl docView, final Annot annot, final Event.Callback result) {
        // set current annot to null
        DocumentManager documentManager = DocumentManager.getInstance(docView);
        if (annot == documentManager.getCurrentAnnot()) {
            documentManager.setCurrentAnnot(null);
        }
        PDFPage page = null;
        try {
            page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            RectF annotRectF = annot.getRect();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            //remove all replies of the note
            ((Markup) annot).removeAllReplies();
            page.removeAnnot(annot);

            docView.convertPdfRectToPageViewRect(annotRectF, annotRectF, page.getIndex());
            docView.refresh(page.getIndex(), AppDmUtil.rectFToRect(annotRectF));

        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    public void modifyAnnot(final Annot annot, int color,
                            float opacity, String iconType,
                            RectF bbox, String content) {

        modifyAnnot(annot, color, opacity, iconType, content, bbox, true, "Note");

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showDialog(final Annot annot) {
        Context context = mContext;
        final Dialog mDialog;
        View mView = View.inflate(context, R.layout.rd_note_dialog_edit, null);
        mDialog_title = (TextView) mView.findViewById(R.id.rd_note_dialog_edit_title);
        mET_Content = (EditText) mView.findViewById(R.id.rd_note_dialog_edit);
        mCancel = (Button) mView.findViewById(R.id.rd_note_dialog_edit_cancel);
        mSave = (Button) mView.findViewById(R.id.rd_note_dialog_edit_ok);


        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mDialog = new Dialog(context, R.style.rv_dialog_style);
        mDialog.setContentView(mView, new ViewGroup.LayoutParams(mDisplay.getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        mET_Content.setMaxLines(10);


        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mDialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

        mDialog_title.setText(mContext.getResources().getString(R.string.fx_string_note));
        mET_Content.setEnabled(true);
        try {
            String content = annot.getContent() != null ? annot.getContent() : "";
            mET_Content.setText(content);
            mET_Content.setSelection(content.length());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        mSave.setEnabled(false);
        mSave.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

        mET_Content.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                try {
                    if (!mET_Content.getText().toString().equals(annot.getContent())) {
                        mSave.setEnabled(true);
                        mSave.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
                    } else {
                        mSave.setEnabled(false);
                        mSave.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {

            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        mSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    if (!mET_Content.getText().toString().equals(annot.getContent())) {
                        modifyAnnot(annot, (int) annot.getBorderColor(), ((Note) annot).getOpacity(),
                                ((Note) annot).getIconName(),
                                annot.getRect(), mET_Content.getText().toString());
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }

                mDialog.dismiss();
            }
        });
        mDialog.show();
        AppUtil.showSoftInput(mET_Content);
    }

    public void onColorValueChanged(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            if (annot != null && color != annot.getBorderColor()) {
                modifyAnnot(annot, (int) color, ((Note) annot).getOpacity(),
                        ((Note) annot).getIconName(), annot.getRect(), ((Note) annot).getContent());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot != null && AppDmUtil.opacity100To255(opacity) != ((Note) annot).getOpacity()) {
                modifyAnnot(annot, (int) annot.getBorderColor(), AppDmUtil.opacity100To255(opacity) / 255f,
                        ((Note) annot).getIconName(), annot.getRect(), ((Note) annot).getContent());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onIconTypeChanged(String iconType) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot != null && iconType != ((Note) annot).getIconName()) {
                modifyAnnot(annot, (int) annot.getBorderColor(), ((Note) annot).getOpacity(), iconType, annot.getRect(),
                        annot.getContent());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, Event.Callback result) {
        if (mToolHandler != null) {
            mToolHandler.addAnnot(pageIndex, (NoteAnnotContent) content, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    private NoteToolHandler mToolHandler;

    public void setToolHandler(NoteToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }


    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        Note lAnnot = (Note) annot;
        try {
            modifyAnnot(lAnnot, content.getColor(), content.getOpacity(), lAnnot.getIconName(), content.getContents(), content.getBBox(), true, "");
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, Event.Callback result) {
        delAnnot(mPdfViewCtrl, annot, result);
    }

    protected void modifyAnnot(Annot annot, int color,
                               float opacity, String iconName, String content, RectF rect,
                               final boolean isModifyJni, final String fromType) {
        int pageIndex = 0;
        try {
            pageIndex = annot.getPage().getIndex();
            if (isModifyJni) {
                RectF tempRectF = annot.getRect();
                if (fromType.equals("")) {
                    Note ta_Annot = (Note) annot;
                    ta_Annot.setBorderColor(color);
                    ta_Annot.setOpacity(opacity);
                    ta_Annot.setIconName(iconName);
                    ta_Annot.setContent(content);
                    ta_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    ta_Annot.move(rect);
                    mIsModify = true;
                }
                RectF annotRectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                annotRectF.union(tempRectF);
                annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
            }
            if (!fromType.equals("")) {
                Note ta_Annot = (Note) annot;
                RectF tempRectF = ta_Annot.getRect();

                ta_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                ta_Annot.setBorderColor(color);
                ta_Annot.setOpacity(opacity);

                ta_Annot.setIconName(iconName);
                if (content != null) {
                    ta_Annot.setContent(content);
                }

                ta_Annot.move(rect);
                ta_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(annot.getPage(), ta_Annot);

                mIsModify = true;
                if (!isModifyJni) {
                    RectF annotRectF = annot.getRect();

                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                    annotRectF.union(tempRectF);
                    annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }
}
