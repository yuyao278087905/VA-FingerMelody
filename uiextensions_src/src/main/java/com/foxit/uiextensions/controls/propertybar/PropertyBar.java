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
package com.foxit.uiextensions.controls.propertybar;

import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;

import com.foxit.uiextensions.annots.note.NoteConstants;


public interface PropertyBar {
    public interface PropertyChangeListener {
        public void onValueChanged(long property, int value);

        public void onValueChanged(long property, float value);

        public void onValueChanged(long property, String value);
    }

    public interface DismissListener {
        public void onDismiss();
    }

    public interface UpdateViewListener {
        public void onUpdate(long property, int value);
    }

    public static final long PROPERTY_UNKNOWN = 0x00000000L;
    public static final long PROPERTY_COLOR = 0x00000001L;
    public static final long PROPERTY_OPACITY = 0x00000002L;
    public static final long PROPERTY_LINEWIDTH = 0x00000004L;
    public static final long PROPERTY_FONTNAME = 0x00000008L;
    public static final long PROPERTY_FONTSIZE = 0x00000010L;
    public static final long PROPERTY_LINE_STYLE = 0x00000020L;
    public static final long PROPERTY_ANNOT_TYPE = 0x00000040L;
    public static final long PROPERTY_SELF_COLOR = 0x00000080L;
    public static final long PROPERTY_SCALE_PERCENT = 0x00000100L;
    public static final long PROPERTY_SCALE_SWITCH = 0x00000200L;
    public static final long PROPERTY_ALL = 0x000003FFL;

    public static final int ARROW_NONE = 0;
    public static final int ARROW_LEFT = 1;
    public static final int ARROW_TOP = 2;
    public static final int ARROW_RIGHT = 3;
    public static final int ARROW_BOTTOM = 4;
    public static final int ARROW_CENTER = 5;

    public static final int[] PB_COLORS_HIGHLIGHT = new int[]{Color.argb(255, 116, 128, 252), Color.argb(255, 255, 255, 0), Color.argb(255, 204, 255, 102), Color.argb(255, 0, 255, 255),
            Color.argb(255, 153, 204, 255), Color.argb(255, 204, 153, 255), Color.argb(255, 255, 153, 153),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_COLORS_UNDERLINE = new int[]{Color.argb(255, 0, 153, 204), Color.argb(255, 51, 204, 0), Color.argb(255, 204, 204, 0), Color.argb(255, 255, 153, 51),
            Color.argb(255, 255, 0, 0), Color.argb(255, 51, 102, 255), Color.argb(255, 204, 51, 255),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_COLORS_SQUIGGLY = PB_COLORS_UNDERLINE;
    public static final int[] PB_COLORS_STRIKEOUT = new int[]{Color.argb(255, 153, 102, 102), Color.argb(255, 255, 51, 51), Color.argb(255, 255, 0, 255), Color.argb(255, 153, 102, 255),
            Color.argb(255, 102, 204, 51), Color.argb(255, 0, 204, 255), Color.argb(255, 255, 153, 0),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_COLORS_CARET = PB_COLORS_STRIKEOUT;
    public static final int[] PB_COLORS_TYPEWRITER = new int[]{Color.argb(255, 51, 102, 204), Color.argb(255, 102, 153, 51), Color.argb(255, 204, 102, 0), Color.argb(255, 204, 153, 0),
            Color.argb(255, 163, 163, 5), Color.argb(255, 204, 0, 0), Color.argb(255, 51, 102, 102),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_COLORS_CALLOUT = PB_COLORS_TYPEWRITER;
    public static final int[] PB_COLORS_LINE = new int[]{Color.argb(255, 255, 159, 64), Color.argb(255, 128, 128, 255), Color.argb(255, 186, 233, 76), Color.argb(255, 255, 241, 96),
            Color.argb(255, 153, 102, 102), Color.argb(255, 255, 76, 76), Color.argb(255, 102, 153, 153),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_COLORS_SQUARENESS = PB_COLORS_LINE;
    public static final int[] PB_COLORS_CIRCLE = PB_COLORS_LINE;
    public static final int[] PB_COLORS_SQUARE = PB_COLORS_LINE;
    public static final int[] PB_COLORS_ARROW = PB_COLORS_LINE;
    public static final int[] PB_COLORS_PENCIL = PB_COLORS_LINE;
    public static final int[] PB_COLORS_TEXT = PB_COLORS_LINE;
    public static final int[] PB_COLORS_SIGN = new int[]{Color.argb(255, 0, 0, 51), Color.argb(255, 0, 0, 102), Color.argb(255, 50, 50, 50), Color.argb(255, 50, 102, 0),
            Color.argb(255, 102, 0, 0), Color.argb(255, 0, 50, 50), Color.argb(255, 50, 0, 153),
            Color.argb(255, 255, 255, 255), Color.argb(255, 195, 195, 195), Color.argb(255, 0, 0, 0)};

    public static final int[] PB_OPACITYS = new int[]{25, 50, 75, 100};
    public static final String[] PB_FONTNAMES = new String[]{"Courier", "Helvetica", "Times"};
    public static final float PB_FONTSIZE_DEFAULT = 24.0f;
    public static final float[] PB_FONTSIZES = new float[]{6.0f, 8.0f, 10.0f, 12.0f, 18.0f, PB_FONTSIZE_DEFAULT, 36.0f, 48.0f, 64.0f, 72.0f};

    public static final int[] ICONTYPES = new int[]{NoteConstants.TA_ICON_COMMENT, NoteConstants.TA_ICON_KEY, NoteConstants.TA_ICON_NOTE, NoteConstants.TA_ICON_HELP, NoteConstants.TA_ICON_NEWPARAGRAPH, NoteConstants.TA_ICON_PARAGRAPH, NoteConstants.TA_ICON_INSERT};
    public static final String[] ICONNAMES = new String[]{"Comment", "Key", "Note", "Help", "NewParagraph", "Paragraph", "Insert"};

    public void setColors(int[] colors);

    public void setProperty(long property, int value);

    public void setProperty(long property, float value);

    public void setProperty(long property, String value);

    public void setArrowVisible(boolean visible);

    public void setPhoneFullScreen(boolean fullScreen);

    public void reset(long items);

    public void setTopTitleVisible(boolean visible);

    public void addTab(String title, int index);

    public void addTab(String topTitle, int resid_img, String title, int index);

    public int getItemIndex(long item);

    public void addCustomItem(long item, View itemView, int tabIndex, int index);

    public void addContentView(View contentView);

    public View getContentView();
    public int      getCurrentTabIndex();
    public void     setCurrentTab(int currentTab);

    public void show(RectF rectF, boolean showMask);

    public void update(RectF rectF);

    public void dismiss();

    public boolean isShowing();

    public PropertyChangeListener getPropertyChangeListener();

    public void setPropertyChangeListener(PropertyChangeListener listener);

    public void setDismissListener(DismissListener dismissListener);
}
