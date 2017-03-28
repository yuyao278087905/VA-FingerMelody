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

import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;

public interface ToolHandler extends PDFViewCtrl.IDrawEventListener {
    // Tool handler type
    public static final String TH_TYPE_TEXTSELECT = "TextSelect Tool";

    //Annot tool
    public static final String TH_TYPE_HIGHLIGHT = "Highlight Tool";
    public static final String TH_TYPE_UNDERLINE = "Underline Tool";
    public static final String TH_TYPE_STRIKEOUT = "Strikeout Tool";
    public static final String TH_TYPE_SQUIGGLY = "Squiggly Tool";
    public static final String TH_TYPE_NOTE = "Note Tool";
    public static final String TH_TYPE_CIRCLE = "Circle Tool";
    public static final String TH_TYPE_SQUARE = "Square Tool";
    public static final String TH_TYPE_TYPEWRITER = "Typewriter Tool";
    public static final String TH_TYPR_INSERTTEXT = "InsetText Tool";
    public static final String TH_TYPE_REPLACE = "Replace Tool";
    public static final String TH_TYPE_INK = "Ink Tool";
    public static final String TH_TYPE_ERASER = "Eraser Tool";
    public static final String TH_TYPE_STAMP = "Stamp Tool";
    public static final String TH_TYPE_LINE = "Line Tool";
    public static final String TH_TYPE_ARROW = "Arrow Tool";
    public static final String TH_TYPE_FORMFILLER = "FormFiller Tool";

    String getType();

    void onActivate();

    void onDeactivate();

    boolean onTouchEvent(int pageIndex, MotionEvent motionEvent);

    boolean onLongPress(int pageIndex, MotionEvent motionEvent);

    boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent);
}
