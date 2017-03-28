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
package com.foxit.uiextensions.utils;

import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.sdk.pdf.form.FormField;
import com.foxit.sdk.pdf.signature.Signature;
import com.foxit.uiextensions.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppAnnotUtil {
    public static float ANNOT_SELECT_TOLERANCE = 10.0f;
    private static AppAnnotUtil mAppAnnotUtil = null;
    private static Context mContext;
    public static AppAnnotUtil getInstance(Context context) {
        if (mAppAnnotUtil == null) {
            mAppAnnotUtil = new AppAnnotUtil(context);
            mContext = context;
        }
        return mAppAnnotUtil;
    }

    private AppDisplay mDisplay;

    public AppAnnotUtil(Context context) {
        mDisplay = AppDisplay.getInstance(context);
    }

    public static PathEffect getAnnotBBoxPathEffect() {
        return new DashPathEffect(new float[]{6, 2}, 0);
    }

    public static int getAnnotBBoxSpace() {
        return 5;
    }

    public float getAnnotBBoxStrokeWidth() {
        return mDisplay.dp2px(1.0f);
    }

    public static void toastAnnotCopy(Context context) {
        UIToast.getInstance(context).show(R.string.fm_annot_copy);
    }

    private static final List<String> TYPES = Collections.unmodifiableList(Arrays.asList(new String[]{
            "Highlight", "Text", "StrikeOut",
            "Underline", "Squiggly", "Circle", "Square", "FreeTextTypewriter",
            "Stamp", "Caret", "Replace", "Ink", "Line", "LineArrow"}));

    private static final List<Integer> IDS = Collections.unmodifiableList(Arrays.asList(new Integer[]{
            // 1.Highlight
            R.drawable.rv_panel_annot_highlight_type,
            // 2.Text
            R.drawable.rv_panel_annot_text_type,
            // 3.StrikeOut
            R.drawable.rv_panel_annot_strikeout_type,
            // 4.UnderLine
            R.drawable.rv_panel_annot_underline_type,
            // 5.Squiggly
            R.drawable.rv_panel_annot_squiggly_type,
            // 6.Circle
            R.drawable.rv_panel_annot_circle_type,
            // 7.Square
            R.drawable.rv_panel_annot_square_type,
            // 8.Typewriter
            R.drawable.rv_panel_annot_typewriter_type,
			// 9.Stamp
            R.drawable.rv_panel_annot_stamp_type,
            // 10.Insert Text
            R.drawable.rv_panel_annot_caret_type,
            // 11.Replace
            R.drawable.rv_panel_annot_replace_type,
            //12.Ink(pencil)
            R.drawable.rv_panel_annot_ink_type,
            //13.Line
            R.drawable.rv_panel_annot_line_type,
            //14.Arrow
            R.drawable.rv_panel_annot_arrow_type
    }));

    public static boolean isSupportReply(Annot annot) {
        if(annot == null)
            return false;
        try {
            switch (annot.getType()) {
                case Annot.e_annotNote:
                {
                    Note note = (Note)annot;
                    if(note.isStateAnnot())
                        return false;
                }
                case Annot.e_annotHighlight:
                case Annot.e_annotUnderline:
                case Annot.e_annotSquiggly:
                case Annot.e_annotStrikeOut:
                case Annot.e_annotCircle:
                case Annot.e_annotSquare:
                    return !isSupportGroupElement(annot);
                case Annot.e_annotFreeText:
                {
                    String intent = ((Markup)annot).getIntent();
                    if(!"FreeTextTypewriter".equals(intent))
                        return false;
                }
                case Annot.e_annotStamp:
                case Annot.e_annotCaret:
                case Annot.e_annotLine:
                case Annot.e_annotInk:
                    return true;
                default:
                    return false;
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
        return false;
    }

    public static String getTypeString(Annot annot) {
        try {
            switch (annot.getType()) {
                case Annot.e_annotNote:
                    return "Text";
                case Annot.e_annotLink:
                    return "Link";
                case Annot.e_annotFreeText: {
                    String intent = ((FreeText) annot).getIntent();
                    intent = intent == null ? "TextBox" : intent;
                    return intent;
                }
                case Annot.e_annotLine:{
                    String intent = ((Line)annot).getIntent();
                    if("LineArrow".equals(intent))
                        return "LineArrow";
                    return "Line";
                }
                case Annot.e_annotSquare:
                    return "Square";
                case Annot.e_annotCircle:
                    return "Circle";
                case Annot.e_annotPolygon:
                    return "Polygon";
                case Annot.e_annotPolyLine:
                    return "PolyLine";
                case Annot.e_annotHighlight:
                    return "Highlight";
                case Annot.e_annotUnderline:
                    return "Underline";
                case Annot.e_annotSquiggly:
                    return "Squiggly";
                case Annot.e_annotStrikeOut:
                    return "StrikeOut";
                case Annot.e_annotStamp:
                    return "Stamp";
                case Annot.e_annotCaret:
                    return isReplaceCaret(annot) ? "Replace" : "Caret";
                case Annot.e_annotInk:
                    return "Ink";
                case Annot.e_annotPSInk:
                    return "PSInk";
                case Annot.e_annotFileAttachment:
                    return "FileAttachment";
                case Annot.e_annotSound:
                    return "Sound";
                case Annot.e_annotMovie:
                    return "Movie";
                case Annot.e_annotWidget:
                    return "Widget";
                case Annot.e_annotScreen:
                    return "Screen";
                case Annot.e_annotPrinterMark:
                    return "PrinterMark";
                case Annot.e_annotTrapNet:
                    return "TrapNet";
                case Annot.e_annotWatermark:
                    return "Watermark";
                case Annot.e_annot3D:
                    return "3D";
                default:
                    return "Unknown";
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static boolean contentsModifiable(String type) {
        return "Text".equals(type)
                || "Line".equals(type)
                || "LineArrow".equals(type)
                || "Square".equals(type)
                || "Circle".equals(type)
                || "Highlight".equals(type)
                || "Underline".equals(type)
                || "Squiggly".equals(type)
                || "StrikeOut".equals(type)
                || "Stamp".equals(type)
                || "Caret".equals(type)
                || "Replace".equals(type)
                || "Ink".equals(type);
    }

    public static Annot getAnnot(PDFPage page, String UID) {
        Annot annot = null;
        if (page == null) return annot;
        try {
            long nCount = page.getAnnotCount();
            for (int i = 0; i < nCount; i++) {
                try {
                    if (page.getAnnot(i).getUniqueID() != null && page.getAnnot(i).getUniqueID().compareTo(UID) == 0) {
                            annot = page.getAnnot(i);
                            break;
                    }
                } catch (PDFException e) {
                    continue;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return annot;
    }

    public static FormControl getControlAtPos(PDFPage page, PointF point, float tolerance) throws PDFException {
        Annot annot = page.getAnnotAtPos(point, tolerance);
        if(annot!= null && annot.getType() == Annot.e_annotWidget)
        {
            return (FormControl)annot;
        }
        return null;
    }


    public static Signature getSignatureAtPos(PDFPage page, PointF point, float tolerance) throws PDFException {
        Annot annot = page.getAnnotAtPos(point, tolerance);
        if(annot != null && annot.getType() == Annot.e_annotWidget)
        {
            FormControl control = (FormControl)annot;
            FormField field = control.getField();
            if(field!= null && field.getType() == FormField.e_formFieldSignature)
                return (Signature)control;
        }
        return null;
    }

    public static boolean isSameAnnot(Annot annot, Annot comparedAnnot)
    {
        boolean ret = false;
        try {

            long objNumA = 0;
            if(annot!= null)
                objNumA = annot.getDict().getObjNum();
            long objNumB = 0;
            if(comparedAnnot!= null)
                objNumB = comparedAnnot.getDict().getObjNum();
            if(objNumA == objNumB)
                ret = true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    // if the annot in a support group by rdk and the annot is not the header of group  return true, otherwise return false.
    public static boolean isSupportGroupElement(Annot annot){
      if(!isSupportGroup(annot))
          return false;
        try{
            return !isSameAnnot(annot,((Markup)annot).getGroupHeader());
        }catch (PDFException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportGroup(Annot annot){
        if(annot == null)
            return false;
        try{
            if(!annot.isMarkup() || !((Markup)annot).isGrouped())
                return false;
            Markup head = ((Markup)annot).getGroupHeader();

            //now just support replace annot (Caret, StikeOut)
            switch (head.getType()){
                case Annot.e_annotCaret:
                    return isReplaceCaret(head);
                default:
                    return false;
            }

        }catch (PDFException e){
            e.printStackTrace();
        }
        return false;

    }

    public static boolean isReplaceCaret(Annot annot) {
        try {
            if (annot == null || annot.getType() != Annot.e_annotCaret || !((Markup)annot).isGrouped())
                return false;
            Caret caret = (Caret) annot;
            Markup head = caret.getGroupHeader();
            if (head.getType() != Annot.e_annotCaret || head.getGroupElementCount() != 2 || !isSameAnnot(head, caret))
                return false;
            for (int i = 0; i < 2; i++) {
                Markup markup = caret.getGroupElement(i);
                if(markup.getType() == Annot.e_annotStrikeOut){
                    return true;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Annot getReplyToAnnot(Annot annot) {
       if(annot == null)
           return null;
        try{
            if(annot.getType() == Annot.e_annotNote)
                return ((Note)annot).getReplyTo();
        } catch (PDFException e){
            e.printStackTrace();
        }
        return null;

    }


    public static PointF getPageViewPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, MotionEvent motionEvent)
    {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        pdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        return point;
    }

    public static PointF getPdfPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, MotionEvent motionEvent)
    {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pageViewPt = new PointF();
        pdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
        PointF point = new PointF();
        pdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, point, pageIndex);
        return point;
    }

    public static int getIconId(String type) {
        int index = TYPES.indexOf(type);
        if (index != -1) {
            return IDS.get(index);
        }
        return R.drawable.rv_panel_annot_not_edit_type;
    }

    public static boolean isSupportEditAnnot(Annot annot) {
        if(annot == null)
            return false;
        try {
            switch (annot.getType()) {
                case Annot.e_annotNote:
                {
                    Note note = (Note)annot;
                    if(note.isStateAnnot())
                        return false;
                }
                case Annot.e_annotHighlight:
                case Annot.e_annotUnderline:
                case Annot.e_annotSquiggly:
                case Annot.e_annotStrikeOut:
                case Annot.e_annotCircle:
                case Annot.e_annotSquare:
                    return !isSupportGroupElement(annot);
                case Annot.e_annotFreeText:
                {
                    String intent = ((Markup)annot).getIntent();
                    if(!"FreeTextTypewriter".equals(intent))
                        return false;
                }
                case Annot.e_annotStamp:
                case Annot.e_annotCaret:
                case Annot.e_annotLine:
                case Annot.e_annotInk:
                    return !isSupportGroupElement(annot);
                default:
                    return false;
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
        return false;
    }

    private Toast mAnnotToast;
    /**
     * Only for annot continue create toast
     */
    public void showAnnotContinueCreateToast(boolean isContinuousCreate) {
        if (mAnnotToast == null) {
            initAnnotToast();
        }
        if (mAnnotToast == null) {
            return;
        }
        String str;
        if (isContinuousCreate) {
            str = AppResource.getString(mContext, R.string.annot_continue_create);
        } else {
            str = AppResource.getString(mContext, R.string.annot_single_create);
        }
        TextView tv = (TextView) mAnnotToast.getView().findViewById(R.id.annot_continue_create_toast_tv);
        int yOffset;
        if (mDisplay.isPad()) {
            yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16) * (2 + 1);
        } else {
            yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_phone) + mDisplay.dp2px(16) * (2 + 1);
        }
        mAnnotToast.setGravity(Gravity.BOTTOM, 0, yOffset);
        tv.setText(str);
        mAnnotToast.show();
    }

    private void initAnnotToast() {
        try {
            mAnnotToast = new Toast(mContext);
            LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View toastlayout = inflate.inflate(R.layout.annot_continue_create_tips, null);
            TextView tv = (TextView) toastlayout.findViewById(R.id.annot_continue_create_toast_tv);
            mAnnotToast.setView(toastlayout);
            mAnnotToast.setDuration(Toast.LENGTH_SHORT);
            int yOffset;
            if (mDisplay.isPad()) {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16) * (2 + 1);
            } else {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_phone) + mDisplay.dp2px(16) * (2 + 1);
            }
            mAnnotToast.setGravity(Gravity.BOTTOM, 0, yOffset);
        } catch (Exception e) {
            mAnnotToast = null;
        }
    }
}
