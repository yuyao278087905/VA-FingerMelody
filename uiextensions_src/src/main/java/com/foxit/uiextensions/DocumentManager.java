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
package com.foxit.uiextensions;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.ActionHandler;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.sdk.pdf.form.FormField;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.line.LineConstants;
import com.foxit.uiextensions.textselect.TextSelectToolHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;
import java.util.List;

import static com.foxit.uiextensions.utils.AppAnnotUtil.ANNOT_SELECT_TOLERANCE;

public class DocumentManager {
    protected Annot mCurAnnot = null;
    private PDFViewCtrl mPdfViewCtrl;
    private static DocumentManager mAnnotManager = null;
    ArrayList<AnnotEventListener> mAnnotEventListenerList = new ArrayList<AnnotEventListener>();
    private ActionHandler mActionHandler = null;

    public static DocumentManager getInstance(PDFViewCtrl pdfViewCtrl) {
        if (mAnnotManager == null) {
            mAnnotManager = new DocumentManager(pdfViewCtrl);
        }
        return mAnnotManager;
    }

    public static void release() {
        mAnnotManager = null;
    }

    private DocumentManager(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public void setActionHandler(ActionHandler handler) {
        mActionHandler = handler;
    }

    public ActionHandler getActionHandler() {
        return mActionHandler;
    }

    public void setCurrentAnnot(Annot annot) {
        if (mCurAnnot == annot) return;
        Annot lastAnnot = mCurAnnot;
        try {
            if (mCurAnnot != null) {
                int type = getAnnotHandlerType(lastAnnot);
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type) != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type).onAnnotDeselected(lastAnnot, true);
                }
            }

            mCurAnnot = annot;
            if (annot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(getAnnotHandlerType(annot)) != null) {
                if (annot.getUniqueID() == null) {
                    annot.setUniqueID(AppDmUtil.randomUUID(null));
                }
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(getAnnotHandlerType(annot)).onAnnotSelected(annot, true);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        onAnnotChanged(lastAnnot, mCurAnnot);
    }

    protected static int getAnnotHandlerType(Annot annot) {
        int type = Annot.e_annotUnknownType;
        try {
            type = annot.getType();
            if (type == Annot.e_annotFreeText) {
                if (((FreeText)annot).getIntent() == null) {
                    type = Annot.e_annotUnknownType; // text box;
                } else if(((FreeText)annot).getIntent().equalsIgnoreCase("FreeTextCallout")) {
                    type = type + 100; // FreeTextCallout annot handler type
                }
            }

            if (type == Annot.e_annotWidget) {
                FormField field = ((FormControl)annot).getField();
                if (field != null) {
                    int ft = field.getType();
                    if (ft == FormField.e_formFieldSignature) {
                        type = type + 101;//signature handle type
                    }
                }

            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return type;
    }

    public Annot getCurrentAnnot() {
        return mCurAnnot;
    }

    protected Annot getFocusAnnot() {
        if (mCurAnnot != null) {
            return mCurAnnot;
        }

        if (mEraseAnnotList.size() > 0) {
            return mEraseAnnotList.get(0);
        }

        return null;
    }

    public boolean shouldViewCtrlDraw(Annot annot) {
        try {
            if (mCurAnnot != null && mCurAnnot.getPage().getIndex() == annot.getPage().getIndex()) {
                int type = mCurAnnot.getType();
                if (type == Annot.e_annotFreeText || type == Annot.e_annotSquare
                        || type == Annot.e_annotCircle || type == Annot.e_annotInk || type == Annot.e_annotLine) {

                    if (type == Annot.e_annotLine) {
                        String intent = ((Line)mCurAnnot).getIntent();
                        if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
                            return true;
                        }
                    }
                    if (mCurAnnot.getIndex() == annot.getIndex()) {
                        return false;
                    }
                }
            }

            //for eraser
            if (annot.getType() != Annot.e_annotInk) return true;
            for (int i = 0; i < mEraseAnnotList.size(); i ++) {
                Ink ink =  mEraseAnnotList.get(i);
                if (ink.getPage().getIndex() == annot.getPage().getIndex() &&
                        ink.getIndex() == annot.getIndex()) {
                    return false;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean canCopy() {
        if (isOwner()) return true;
        long permission = 0;
        try {
            permission = mPdfViewCtrl.getDoc().getUserPermissions();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return (permission & PDFDoc.e_permExtract) != 0;
    }

    public boolean canModifyContents() {
        if (isOwner()) return true;
        long permission = 0;
        try {
            permission = mPdfViewCtrl.getDoc().getUserPermissions();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return (permission & PDFDoc.e_permModify) != 0;
    }

    public boolean canAddAnnot() {
        if (mPdfViewCtrl.getDoc() == null)
            return false;
        if (isOwner()) return true;
        long permission = 0;
        try {
            permission = mPdfViewCtrl.getDoc().getUserPermissions();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return (permission & PDFDoc.e_permAnnotForm) != 0;
    }

    public boolean isOwner() {
        try {
            if (PDFDoc.e_pwdOwner == mPdfViewCtrl.getDoc().getPasswordType()) {
                return true;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    //IAnnotEventListener
    public interface AnnotEventListener {
        void onAnnotAdded(PDFPage page, Annot annot);

        void onAnnotDeleted(PDFPage page, Annot annot);

        void onAnnotModified(PDFPage page, Annot annot);

        void onAnnotChanged(Annot lastAnnot, Annot currentAnnot);
    }

    // annot event listener management.
    public void registerAnnotEventListener(AnnotEventListener listener) {
        mAnnotEventListenerList.add(listener);
    }

    public void unregisterAnnotEventListener(AnnotEventListener listener) {
        mAnnotEventListenerList.remove(listener);
    }

    public void onAnnotAdded(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotAdded(page, annot);
        }
    }

    public void onAnnotDeleted(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotDeleted(page, annot);
        }
    }

    public void onAnnotModified(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotModified(page, annot);
        }
    }

    public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotChanged(lastAnnot, currentAnnot);
        }
    }

    protected List<Ink> mEraseAnnotList = new ArrayList<Ink>();
    public void onAnnotStartEraser(Ink annot) {
        mEraseAnnotList.add(annot);
    }

    public void onAnnotEndEraser() {
        mEraseAnnotList.clear();
    }

    public static  boolean intersects(RectF a, RectF b) {
        return a.left < b.right && b.left < a.right
                && a.top > b.bottom && b.top > a.bottom;
    }

    public ArrayList<Annot> getAnnotsInteractRect(PDFPage page, RectF rect, int type) {
        ArrayList<Annot> annotList = new ArrayList<Annot>(4);
        try {
            int count = page.getAnnotCount();
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = page.getAnnot(i);
                if ((annot.getFlags() & Annot.e_annotFlagHidden) != 0)
                    continue;

                int _type = getAnnotHandlerType(annot);
                AnnotHandler annotHandler = ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), _type);
                if (annotHandler != null) {
                    RectF bbox = annotHandler.getAnnotBBox(annot);
                    if (intersects(bbox, rect) && annot.getType() == type) {
                        annotList.add(annot);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return annotList;
    }

    public Annot getAnnot(PDFPage page, String nm) {
        if (page == null) {
            return null;
        }

        try {
            int count = page.getAnnotCount();
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = page.getAnnot(i);
                if (annot.getUniqueID() == null) {
                    continue;
                }

                if (annot.getUniqueID().equals(nm)) {
                    return annot;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void addAnnot(PDFPage page, AnnotContent content, Event.Callback result) {
        if (page == null) {
            return;
        }

        Annot annot = getAnnot(page, content.getNM());
        if (annot != null) {
            modifyAnnot(annot, content, result);
            return;
        }

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(content.getType());
        if (annotHandler != null) {
            try {
                annotHandler.addAnnot(page.getIndex(), content, result);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    public void modifyAnnot(Annot annot, AnnotContent content, Event.Callback result) {
        try {
            if (annot.getModifiedDateTime() != null && content.getModifiedDate() != null
                    && annot.getModifiedDateTime().equals(content.getModifiedDate())) {
                if (result != null) {
                    result.result(null, true);
                }
                return;
            }

            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
            AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot));
            if (annotHandler != null) {
                annotHandler.modifyAnnot(annot, content, result);
            } else {
                if (result != null) {
                    result.result(null, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removeAnnot(final Annot annot, final Event.Callback result) {
        if (annot == getCurrentAnnot()) {
            setCurrentAnnot(null);
        }

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot));

        if (annotHandler != null) {
            annotHandler.removeAnnot(annot, result);
            return;
        }
    }

    //deal with annot
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        int action = motionEvent.getActionMasked();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
                    if (annot != null) {
                        int type = getAnnotHandlerType(annot);
                        annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                        if (annotHandler == null)
                            return false;
                        if (annotHandler.onTouchEvent(pageIndex, motionEvent)) {
                            hideSelectorAnnotMenu(mPdfViewCtrl);
                            return true;
                        }
                    }
                    PointF pdfPoint = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);

                    page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                    if (page != null)
                        annot = page.getAnnotAtPos(pdfPoint, ANNOT_SELECT_TOLERANCE);

                    break;
                }
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
                    break;
                default:
                    return false;
            }
            if (annot != null) {
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                    hideSelectorAnnotMenu(mPdfViewCtrl);
                    return annotHandler.onTouchEvent(pageIndex, motionEvent);
                }
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        try {

            boolean annotCanceled = false;
            annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot != null) {
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.onLongPress(pageIndex, motionEvent)) {
                    hideSelectorAnnotMenu(mPdfViewCtrl);
                    return true;
                }
                if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() == null) {
                    annotCanceled = true;
                }
            }
            PointF pdfPoint = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (page != null)
                annot = page.getAnnotAtPos(pdfPoint, ANNOT_SELECT_TOLERANCE);
            if (annot != null && AppAnnotUtil.isSupportGroup(annot)) {
                annot = ((Markup)annot).getGroupHeader();
            }

            if (annot != null) {
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                    if (annotHandler.onLongPress(pageIndex, motionEvent)) {
                        hideSelectorAnnotMenu(mPdfViewCtrl);
                        return true;
                    }
                }
            }

            if (annotCanceled) return true;
            return false;

        } catch (PDFException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        try {
            boolean annotCanceled = false;
            annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            if (annot != null) {
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.onSingleTapConfirmed(pageIndex, motionEvent)) {
                    hideSelectorAnnotMenu(mPdfViewCtrl);
                    return true;
                }
                if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() == null) {
                    annotCanceled = true;
                }
            }
            PointF pdfPoint = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (page != null)
                annot = page.getAnnotAtPos(pdfPoint, ANNOT_SELECT_TOLERANCE);
            if (annot != null && AppAnnotUtil.isSupportGroup(annot)) {
                annot = ((Markup)annot).getGroupHeader();
            }

            if (annot != null) {
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                    if (annotHandler.onSingleTapConfirmed(pageIndex, motionEvent)) {
                        hideSelectorAnnotMenu(mPdfViewCtrl);
                        return true;
                    }
                }
            }

            if (annotCanceled) {
                return true;
            }

            return false;
        } catch (PDFException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    private void hideSelectorAnnotMenu(PDFViewCtrl pdfViewCtrl) {
        TextSelectToolHandler selectionTool = (TextSelectToolHandler) ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
        if (selectionTool != null) {
            selectionTool.mAnnotationMenu.dismiss();
        }
    }
}
