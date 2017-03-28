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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.note.NoteConstants;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyBarImpl extends PopupWindow implements PropertyBar {
    // highlight,1,typewriter,2,squareness,3,text,4,
    // strikeout,5,squiggly,6,underline,7,textinsert,8,strikeoutinsert,9,
    // line,10,circle,11,arrow,12,pencil,13,eraser,,
    // callout,14,sign,15,stamp,,
    private Context mContext;

    private LinearLayout mPopupView;
    private LinearLayout mLl_root;
    private LinearLayout mTopShadow;
    private LinearLayout mLlArrowTop;
    private ImageView mIvArrowTop;
    private LinearLayout mLlArrowLeft;
    private ImageView mIvArrowLeft;
    private LinearLayout mLlArrowRight;
    private ImageView mIvArrowRight;
    private LinearLayout mLlArrowBottom;
    private ImageView mIvArrowBottom;
    private boolean mArrowVisible = false;

    private LinearLayout mLl_PropertyBar;
    private List<String> mTabs;
    private LinearLayout mLl_topTabs;
    private LinearLayout mTopTitleLayout;
    private TextView mTopTitle;
    private LinearLayout mLl_titles;
    private LinearLayout mLl_title_checked;
    private ImageView mIv_title_shadow;
    private LinearLayout mLl_tabContents;
    private long mSupportProperty = 0;
    private long mCustomProperty = 0;
    private int mCurrentTab = 0;
    private String[] mSupportTabNames;
    private List<Map<String, Object>> mCustomTabList;
    private List<Map<String, Object>> mCustomItemList;

    private int[] mColors;
    private int[] mOpacitys = PB_OPACITYS;
    private int mColor;
    private int mOpacity = mOpacitys[mOpacitys.length - 1];
    private String mFontname = "Courier";
    private float mFontsize = 24.0f;
    private float mLinewith = 6.0f;
    private int[] mLinestyles = new int[]{1, 2, 3, 4, 5};
    private int mLinestyle = mLinestyles[0];

    private int[] mOpacityIds = new int[]{R.drawable.pb_opacity25, R.drawable.pb_opacity50, R.drawable.pb_opacity75, R.drawable.pb_opacity100};
    private int[] mOpacityIdsChecked = new int[]{R.drawable.pb_opacity25_pressed, R.drawable.pb_opacity50_pressed, R.drawable.pb_opacity75_pressed, R.drawable.pb_opacity100_pressed};
    private int[] mTypePicIds = new int[]{R.drawable.pb_note_type_comment, R.drawable.pb_note_type_key, R.drawable.pb_note_type_note,
            R.drawable.pb_note_type_help, R.drawable.pb_note_type_new_paragraph, R.drawable.pb_note_type_paragraph, R.drawable.pb_note_type_insert};
    private String[] mTypeNames;
    private int mNoteIconType = NoteConstants.TA_ICON_COMMENT;

    private ViewPager mColorViewPager;
    private LinearLayout mLlColorDots;
    private LinearLayout mPBLlColors;// ViewPager view1
    private LinearLayout mColorsPickerRoot;// ViewPager view2
    private int[] mColorDotPics = new int[]{R.drawable.pb_ll_colors_dot_selected, R.drawable.pb_ll_colors_dot};
    private int mCurrentColorIndex = 0;

    private String[] mFontNames = PB_FONTNAMES;
    private boolean[] mFontChecked;
    private float[] mFontSizes = PB_FONTSIZES;
    private boolean[] mFontSizeChecked;
    private final int mShowFontSize = 2;
    private final int mShowFont = 1;

    private EditText mScaleEdt;
    private int mScalePercent = 20;
    private int mScaleSwitch = 0;

    private FontAdapter      mFontAdapter;
    private FontSizeAdapter  mFontSizeAdapter;
    private TypeAdapter mTypeAdapter;

    private PropertyChangeListener mPropertyChangeListener;
    private PropertyBar.DismissListener mDismissListener;
    private int mPadWidth;
    private int mCurrentWidth = 0;
    private boolean mShowMask = false;

    private RectF mRectF;
    private float offset = 0;
    private boolean mOrientationed = false;
    private int mCurrentRotation;

    private AppDisplay display;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent;

    public PropertyBarImpl(Context context, PDFViewCtrl pdfViewer, ViewGroup parent) {
        this(context, null, pdfViewer, parent);
    }

    public PropertyBarImpl(Context context, AttributeSet attrs, PDFViewCtrl pdfViewer, ViewGroup parent) {
        this(context, attrs, 0, pdfViewer, parent);
    }

    public PropertyBarImpl(Context context, AttributeSet attrs, int defStyleAttr, PDFViewCtrl pdfViewCtrl, ViewGroup parent) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;
        this.mParent = parent;
        display = AppDisplay.getInstance(context);
        mCurrentRotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        initVariable();
        initView();

        if (display.isPad()) {
            setWidth(mPadWidth);
        } else {
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(mPopupView);

        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (!display.isPad()) {
            setAnimationStyle(R.style.PB_PopupAnimation);
        }
        setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mDismissListener != null) {
                    mDismissListener.onDismiss();
                }

                if (mShowMask) {
                    mShowMask = false;
                }
                if (!display.isPad()) {
                    setPhoneFullScreen(false);
                }
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
                // move full screen to original position if it's moved to somewhere in selected annotation model.
                if ((!display.isPad()) && currentAnnotHandler != null && uiExtensionsManager.getCurrentToolHandler() == null) {
                    if (offset > 0) {
                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                        offset = 0;
                    }
                }
                if(DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()!=null){
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            }
        });
    }

    private void initVariable() {
        mPadWidth = display.dp2px(320.0f);

        int[] colors = new int[PropertyBar.PB_COLORS_TEXT.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXT[0];
        mColors = colors;
        mColor = mColors[0];
        mRectF = new RectF();
        mSupportTabNames = new String[]{mContext.getResources().getString(R.string.pb_type_tab),
                mContext.getResources().getString(R.string.pb_fill_tab),
                mContext.getResources().getString(R.string.fx_string_border),
                mContext.getResources().getString(R.string.fx_string_fontname),
                "Watermark"};
        mTabs = new ArrayList<String>();
        mCustomTabList = new ArrayList<Map<String, Object>>();
        mCustomItemList = new ArrayList<Map<String, Object>>();
        mFontChecked = new boolean[mFontNames.length];
        for (int i = 0; i < mFontNames.length; i++) {
            if (i == 0) {
                mFontChecked[i] = true;
            } else {
                mFontChecked[i] = false;
            }
        }
        mFontSizeChecked = new boolean[mFontSizes.length];
        for (int i = 0; i < mFontSizes.length; i++) {
            if (i == 0) {
                mFontSizeChecked[i] = true;
            } else {
                mFontSizeChecked[i] = false;
            }
        }
        mTypeNames = new String[]{mContext.getResources().getString(R.string.annot_text_comment),
                mContext.getResources().getString(R.string.annot_text_key),
                mContext.getResources().getString(R.string.annot_text_note),
                mContext.getResources().getString(R.string.annot_text_help),
                mContext.getResources().getString(R.string.annot_text_newparagraph),
                mContext.getResources().getString(R.string.annot_text_paragraph),
                mContext.getResources().getString(R.string.annot_text_insert)};

    }

    private void initView() {
        mPopupView = new LinearLayout(mContext);
        mPopupView.setOrientation(LinearLayout.VERTICAL);
        mLl_root = new LinearLayout(mContext);
        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLl_root.setOrientation(LinearLayout.VERTICAL);
        mPopupView.addView(mLl_root);

        // ---phone top shadow
        if (!display.isPad()) {
            mTopShadow = new LinearLayout(mContext);
            mTopShadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mTopShadow.setOrientation(LinearLayout.VERTICAL);
            mLl_root.addView(mTopShadow);

            ImageView shadow = new ImageView(mContext);
            shadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_shadow_height)));
            shadow.setImageResource(R.drawable.search_shadow_bg270);
            mTopShadow.addView(shadow);

            ImageView shadowLine = new ImageView(mContext);
            shadowLine.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            shadowLine.setImageResource(R.color.ux_color_shadow_solid_line);
            mTopShadow.addView(shadowLine);
        }

        // ---top
        mLlArrowTop = new LinearLayout(mContext);
        mLlArrowTop.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLlArrowTop.setOrientation(LinearLayout.VERTICAL);
        mLl_root.addView(mLlArrowTop);

        mIvArrowTop = new ImageView(mContext);
        mIvArrowTop.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowTop.setImageResource(R.drawable.pb_arrow_top);
        mLlArrowTop.addView(mIvArrowTop);
        mLlArrowTop.setVisibility(View.GONE);

        // ---left center and right
        LinearLayout mLlArrowCenter = new LinearLayout(mContext);
        mLlArrowCenter.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        mLlArrowCenter.setOrientation(LinearLayout.HORIZONTAL);
        mLl_root.addView(mLlArrowCenter);

        //---left
        mLlArrowLeft = new LinearLayout(mContext);
        mLlArrowLeft.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mLlArrowLeft.setOrientation(LinearLayout.VERTICAL);
        mLlArrowCenter.addView(mLlArrowLeft);

        mIvArrowLeft = new ImageView(mContext);
        mIvArrowLeft.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowLeft.setImageResource(R.drawable.pb_arrow_left);
        mLlArrowLeft.addView(mIvArrowLeft);
        mLlArrowLeft.setVisibility(View.GONE);

        // ---center
        mLl_PropertyBar = new LinearLayout(mContext);
        mLl_PropertyBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        mLl_PropertyBar.setOrientation(LinearLayout.VERTICAL);
        if (display.isPad()) {
            mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
            mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f));
        } else {
            mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
        }
        mLlArrowCenter.addView(mLl_PropertyBar);

        // ---right
        mLlArrowRight = new LinearLayout(mContext);
        mLlArrowRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mLlArrowRight.setOrientation(LinearLayout.VERTICAL);
        mLlArrowCenter.addView(mLlArrowRight);

        mIvArrowRight = new ImageView(mContext);
        mIvArrowRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowRight.setImageResource(R.drawable.pb_arrow_right);
        mLlArrowRight.addView(mIvArrowRight);
        mLlArrowRight.setVisibility(View.GONE);

        // ---bottom
        mLlArrowBottom = new LinearLayout(mContext);
        mLlArrowBottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLlArrowBottom.setOrientation(LinearLayout.VERTICAL);
        mLl_root.addView(mLlArrowBottom);

        mIvArrowBottom = new ImageView(mContext);
        mIvArrowBottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowBottom.setImageResource(R.drawable.pb_arrow_bottom);
        mLlArrowBottom.addView(mIvArrowBottom);
        mLlArrowBottom.setVisibility(View.GONE);

        addPbAll();
    }

    private void addPbAll() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_rl_propertybar, null, false);

        mLl_topTabs = (LinearLayout) view.findViewById(R.id.pb_ll_top);
        if (display.isPad()) {
            mLl_topTabs.setBackgroundResource(R.drawable.pb_tabs_bg);
        } else {
            mLl_topTabs.setBackgroundResource(R.color.ux_text_color_subhead_colour);
        }

        mTopTitleLayout = (LinearLayout) view.findViewById(R.id.pb_topTitle_ll);
        mTopTitleLayout.setVisibility(View.GONE);
        mTopTitleLayout.setTag(0);
        if (display.isPad()) {
            mTopTitle = new TextView(mContext);
            mTopTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            mTopTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_title));
            mTopTitle.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            mTopTitle.setTypeface(Typeface.DEFAULT);
            mTopTitle.setGravity(Gravity.CENTER);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);

            mTopTitleLayout.addView(mTopTitle);
        } else {
            RelativeLayout relativeLayout = new RelativeLayout(mContext);
            relativeLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
            mTopTitleLayout.addView(relativeLayout);

            mTopTitle = new TextView(mContext);
            RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            titleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            titleLayoutParams.leftMargin = display.dp2px(70.0f);
            mTopTitle.setLayoutParams(titleLayoutParams);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);

            mTopTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_title));
            mTopTitle.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            mTopTitle.setTypeface(Typeface.DEFAULT);
            mTopTitle.setGravity(Gravity.CENTER_VERTICAL);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);
            relativeLayout.addView(mTopTitle);

            ImageView img = new ImageView(mContext);
            RelativeLayout.LayoutParams imgLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            imgLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            imgLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            imgLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            img.setLayoutParams(imgLayoutParams);
            img.setImageResource(R.drawable.panel_topbar_close_selector);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            relativeLayout.addView(img);

        }

        mLl_titles = (LinearLayout) view.findViewById(R.id.pb_ll_titles);
        mLl_title_checked = (LinearLayout) view.findViewById(R.id.pb_ll_title_checks);
        mIv_title_shadow = (ImageView) view.findViewById(R.id.pb_iv_title_shadow);
        mLl_tabContents = (LinearLayout) view.findViewById(R.id.pb_ll_tabContents);

        mLl_PropertyBar.addView(view);
    }

    @Override
    public void setPhoneFullScreen(boolean fullScreen) {
        if (!display.isPad()) {
            LinearLayout tabLayout = (LinearLayout) mLl_tabContents.getParent();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tabLayout.getLayoutParams();

            LinearLayout.LayoutParams tabContentsLayoutParams = (LinearLayout.LayoutParams) mLl_tabContents.getLayoutParams();
            if (fullScreen) {
                setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            } else {
                setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            tabLayout.setLayoutParams(layoutParams);
            mLl_tabContents.setLayoutParams(tabContentsLayoutParams);
        }
    }

    private View getScaleView() {
        View scaleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_scale, null, false);
        mScaleEdt = (EditText) scaleItem.findViewById(R.id.pb_scale_percent);
        LinearLayout switchLayout = (LinearLayout) scaleItem.findViewById(R.id.pb_scale_switch_ll);
        ImageView switchImg = (ImageView) scaleItem.findViewById(R.id.pb_scale_switch);

        mScaleEdt.setText(String.valueOf(mScalePercent));
        mScaleEdt.setSelection(String.valueOf(mScalePercent).length());
        mScaleEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mScaleSwitch == 1) {
                    if (s.toString().length() == 0) {
                        return;
                    }
                    int percent = Integer.valueOf(s.toString());
                    if (percent < 1) {
                        mScaleEdt.setText(String.valueOf(mScalePercent));
                        mScaleEdt.selectAll();
                    } else if (percent > 100) {
                        mScaleEdt.setText(s.toString().substring(0, s.toString().length() - 1));
                        mScaleEdt.selectAll();
                    } else {
                        mScalePercent = percent;
                        if (mPropertyChangeListener != null) {
                            mPropertyChangeListener.onValueChanged(PROPERTY_SCALE_PERCENT, mScalePercent);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        if (mScaleSwitch == 1) {
            switchImg.setImageResource(R.drawable.setting_on);
            mScaleEdt.setEnabled(true);
        } else {
            switchImg.setImageResource(R.drawable.setting_off);
            mScaleEdt.setEnabled(false);
        }
        switchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView switchImage = (ImageView) ((LinearLayout) v).getChildAt(0);
                EditText scaleEdit = (EditText) ((LinearLayout) v.getParent()).getChildAt(0);

                if (mScaleSwitch == 1) {
                    mScaleSwitch = 0;
                    switchImage.setImageResource(R.drawable.setting_off);
                    scaleEdit.setEnabled(false);
                } else {
                    mScaleSwitch = 1;
                    switchImage.setImageResource(R.drawable.setting_on);
                    scaleEdit.setEnabled(true);
                }

                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PROPERTY_SCALE_SWITCH, mScaleSwitch);
                }
            }
        });

        return scaleItem;
    }

    private View getIconTypeView() {
        LinearLayout typeItem = new LinearLayout(mContext);
        typeItem.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        typeItem.setGravity(Gravity.CENTER);
        typeItem.setOrientation(LinearLayout.HORIZONTAL);

        ListView lv_type = new ListView(mContext);
        lv_type.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        lv_type.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));

        lv_type.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        lv_type.setDividerHeight(1);
        typeItem.addView(lv_type);

        mTypeAdapter = new TypeAdapter(mContext, mTypePicIds, mTypeNames);
        mTypeAdapter.setNoteIconType(mNoteIconType);
        lv_type.setAdapter(mTypeAdapter);

        lv_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mNoteIconType = ICONTYPES[position];
                mTypeAdapter.setNoteIconType(mNoteIconType);
                mTypeAdapter.notifyDataSetChanged();
                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_ANNOT_TYPE, mNoteIconType);
                }
            }
        });

        return typeItem;
    }

    private View getLineWidthView() {
        View lineWidthItem = LayoutInflater.from(mContext).inflate(R.layout.pb_linewidth, null, false);
        ThicknessImage thicknessImage = (ThicknessImage) lineWidthItem.findViewById(R.id.pb_img_lineWidth_mypic);
        TextView tv_width = (TextView) lineWidthItem.findViewById(R.id.pb_tv_lineWidth_size);
        tv_width.setText((int) (mLinewith + 0.5f) + "px");
        SeekBar sb_lineWidth = (SeekBar) lineWidthItem.findViewById(R.id.sb_lineWidth);
        sb_lineWidth.setProgress((int) (mLinewith - 1 + 0.5f));
        sb_lineWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                LinearLayout linearLayout = (LinearLayout) (seekBar.getParent());
                ThicknessImage thicknessImage = (ThicknessImage) linearLayout.getChildAt(0);
                TextView tv_width = (TextView) linearLayout.getChildAt(1);
                if (progress >= 0 && progress < 12) {
                    mLinewith = progress + 1;
                    thicknessImage.setBorderThickness(progress + 1);
                    tv_width.setText((progress + 1) + "px");
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_LINEWIDTH, (float) (progress + 1));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        thicknessImage.setBorderThickness(mLinewith);
        thicknessImage.setColor(mColor);

        return lineWidthItem;
    }

    private View getLineStyleView() {
        View lineStyleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_linestyle, null, false);
        LinearLayout pb_ll_borderStyle = (LinearLayout) lineStyleItem.findViewById(R.id.pb_ll_borderStyle);
        for (int i = 0; i < mLinestyles.length; i++) {
            if (i + 1 == mLinestyle) {
                pb_ll_borderStyle.getChildAt(i).setBackgroundResource(R.drawable.pb_border_style_checked);
            } else {
                pb_ll_borderStyle.getChildAt(i).setBackgroundResource(0);
            }
        }
        for (int i = 0; i < mLinestyles.length; i++) {
            ImageView imageView = (ImageView) pb_ll_borderStyle.getChildAt(i);
            imageView.setTag(i);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout linearLayout = (LinearLayout) (v.getParent());
                    int tag = Integer.valueOf(v.getTag().toString());

                    for (int i = 0; i < mLinestyles.length; i++) {
                        if (i == tag) {
                            linearLayout.getChildAt(i).setBackgroundResource(R.drawable.pb_border_style_checked);
                        } else {
                            linearLayout.getChildAt(i).setBackgroundResource(0);
                        }
                    }
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_LINE_STYLE, mLinestyles[tag]);
                    }
                }
            });
        }

        return lineStyleItem;
    }

    private View getFontView() {
        View fontStyleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle, null, false);
        TextView pb_tv_font = (TextView) fontStyleItem.findViewById(R.id.pb_tv_font);
        pb_tv_font.setText(mFontname);
        pb_tv_font.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
                mLl_tabContents.removeAllViews();
                mLl_tabContents.addView(getFontSelectedView(mShowFont));// 1 means show Font page, 2 means show FontSize page
            }
        });
        TextView pb_tv_fontSize = (TextView) fontStyleItem.findViewById(R.id.pb_tv_fontSize);
        pb_tv_fontSize.setText(((int) mFontsize) + "px");
        pb_tv_fontSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
                mLl_tabContents.removeAllViews();
                mLl_tabContents.addView(getFontSelectedView(mShowFontSize));// 1 means show Font page, 2 means show FontSize page
            }
        });

        return fontStyleItem;
    }

    private View getFontSelectedView(final int type) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle_set, null, false);
        ImageView pb_iv_fontstyle_back = (ImageView) view.findViewById(R.id.pb_iv_fontstyle_back);
        pb_iv_fontstyle_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset(mSupportProperty);
            }
        });

        TextView pb_font_select_title  = (TextView) view.findViewById(R.id.pb_font_select_title);
        ListView pb_lv_font = (ListView) view.findViewById(R.id.pb_lv_font);
        if (type == mShowFont) {
            pb_font_select_title.setText(mContext.getResources().getString(R.string.fx_string_font));
            for (int i = 0; i < mFontNames.length; i++) {
                if (mFontNames[i].equals(mFontname)) {
                    mFontChecked[i] = true;
                } else {
                    mFontChecked[i] = false;
                }
            }
            mFontAdapter = new FontAdapter(mContext, mFontNames, mFontChecked);
            pb_lv_font.setAdapter(mFontAdapter);
        } else if (type == mShowFontSize) {
            pb_font_select_title.setText(mContext.getResources().getString(R.string.fx_string_fontsize));
            for (int i = 0; i < mFontSizes.length; i++) {
                if (mFontSizes[i] == mFontsize) {
                    mFontSizeChecked[i] = true;
                } else {
                    mFontSizeChecked[i] = false;
                }
            }
            mFontSizeAdapter = new FontSizeAdapter(mContext, mFontSizes, mFontSizeChecked);
            pb_lv_font.setAdapter(mFontSizeAdapter);
        }

        pb_lv_font.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (type == mShowFont) {
                    for (int i = 0; i < mFontChecked.length; i++) {
                        if (i == position) {
                            mFontChecked[i] = true;
                        } else {
                            mFontChecked[i] = false;
                        }
                    }
                    mFontAdapter.notifyDataSetChanged();
                    mFontname = mFontNames[position];
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_FONTNAME, mFontNames[position]);
                    }
                } else if (type == mShowFontSize) {
                    for (int i = 0; i < mFontSizeChecked.length; i++) {
                        if (i == position) {
                            mFontSizeChecked[i] = true;
                        } else {
                            mFontSizeChecked[i] = false;
                        }
                    }
                    mFontSizeAdapter.notifyDataSetChanged();
                    mFontsize = mFontSizes[position];
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_FONTSIZE, mFontSizes[position]);
                    }
                }
            }
        });

        return view;
    }

    private View getOpacityView() {
        View opacityItem = LayoutInflater.from(mContext).inflate(R.layout.pb_opacity, null, false);
        final LinearLayout pb_ll_opacity = (LinearLayout) opacityItem.findViewById(R.id.pb_ll_opacity);
        for (int i = 0; i < pb_ll_opacity.getChildCount(); i++) {
            if (i % 2 == 0) {
                ImageView iv_opacity_item = (ImageView) (((LinearLayout) pb_ll_opacity.getChildAt(i)).getChildAt(0));
                TextView tv_opacity_item = (TextView) (((LinearLayout) pb_ll_opacity.getChildAt(i)).getChildAt(1));
                iv_opacity_item.setTag(i);
                iv_opacity_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int tag = Integer.valueOf(v.getTag().toString());

                        for (int j = 0; j < pb_ll_opacity.getChildCount(); j++) {
                            if (j % 2 == 0) {
                                ImageView iv_opacity = (ImageView) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((ImageView) v).getParent()).getParent()).getChildAt(j)).getChildAt(0);
                                TextView tv_opacity = (TextView) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((ImageView) v).getParent()).getParent()).getChildAt(j)).getChildAt(1);
                                if (tag == j) {
                                    ((ImageView) v).setImageResource(mOpacityIdsChecked[j / 2]);
                                    tv_opacity.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_button_colour));
                                } else {
                                    iv_opacity.setImageResource(mOpacityIds[j / 2]);
                                    tv_opacity.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));
                                }
                            }
                        }
                        if (mPropertyChangeListener != null) {
                            mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_OPACITY, mOpacitys[tag / 2]);
                            mOpacity = mOpacitys[tag/2];
                        }
                    }
                });

                if (mOpacity == mOpacitys[i / 2]) {
                    iv_opacity_item.setImageResource(mOpacityIdsChecked[i / 2]);
                    tv_opacity_item.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_button_colour));
                } else {
                    iv_opacity_item.setImageResource(mOpacityIds[i / 2]);
                    tv_opacity_item.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));
                }
            }
        }

        return opacityItem;
    }

    private View getColorView() {
        View colorItemView = LayoutInflater.from(mContext).inflate(R.layout.pb_color, null, false);
        TextView pb_tv_colorTitle = (TextView) colorItemView.findViewById(R.id.pb_tv_colorTitle);
        pb_tv_colorTitle.setText(mContext.getResources().getString(R.string.fx_string_color));
        mColorViewPager = (ViewPager) colorItemView.findViewById(R.id.pb_ll_colors_viewpager);
        mLlColorDots = (LinearLayout) colorItemView.findViewById(R.id.pb_ll_colors_dots);
        LinearLayout.LayoutParams vpParams = (LinearLayout.LayoutParams) mColorViewPager.getLayoutParams();
        if (display.isPad()) {
            vpParams.height = display.dp2px(90.0f);
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                vpParams.height = display.dp2px(42.0f);
            } else {
                vpParams.height = display.dp2px(90.0f);
            }
        }
        mColorViewPager.setLayoutParams(vpParams);

        // ViewPager view1
        mPBLlColors = new LinearLayout(mContext);
        mPBLlColors.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mPBLlColors.setOrientation(LinearLayout.HORIZONTAL);
        initColorOne(mPBLlColors);

        // ViewPager view2
        mColorsPickerRoot = new LinearLayout(mContext);
        mColorsPickerRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mColorsPickerRoot.setOrientation(LinearLayout.HORIZONTAL);
        mColorsPickerRoot.setGravity(Gravity.CENTER);
        ColorPicker colorPicker = new ColorPicker(mContext, mParent);
        mColorsPickerRoot.addView(colorPicker);

        ImageView selfColor = new ImageView(mContext);
        LinearLayout.LayoutParams selfColorParams = new LinearLayout.LayoutParams(display.dp2px(30.0f), display.dp2px(90.0f));
        if (display.isPad()) {
            selfColorParams.height = display.dp2px(90.0f);
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                selfColorParams.height = display.dp2px(42.0f);
            } else {
                selfColorParams.height = display.dp2px(90.0f);
            }
        }
        selfColorParams.leftMargin = display.dp2px(10.0f);
        selfColor.setLayoutParams(selfColorParams);
        selfColor.setImageDrawable(new ColorDrawable(mColors[0]));
        mColorsPickerRoot.addView(selfColor);

        colorPicker.setOnUpdateViewListener(new UpdateViewListener() {
            @Override
            public void onUpdate(long property, int value) {
                mColors[0] = value;
                mColor = value;
                ((ImageView) mColorsPickerRoot.getChildAt(1)).setImageDrawable(new ColorDrawable(value));
                initColorOne(mPBLlColors);
                if (mTabs.contains(mSupportTabNames[2])) {
                    ThicknessImage thicknessImage = (ThicknessImage) ((LinearLayout) ((LinearLayout) ((LinearLayout) mLl_tabContents.getChildAt(mTabs.indexOf(mSupportTabNames[2]))).getChildAt(1)).getChildAt(1)).getChildAt(0);
                    thicknessImage.setColor(mColor);
                }
                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_SELF_COLOR, value);
                }
            }
        });

        List<View> colorViewList = new ArrayList<View>();
        colorViewList.add(mPBLlColors);
        colorViewList.add(mColorsPickerRoot);

        ColorVPAdapter colorVPAdapter = new ColorVPAdapter(colorViewList);
        mColorViewPager.setAdapter(colorVPAdapter);
        mColorViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentColorIndex = position;

                for (int i = 0; i < mLlColorDots.getChildCount(); i++) {
                    ImageView imageView = (ImageView) mLlColorDots.getChildAt(i);
                    if (i == position) {
                        imageView.setImageResource(mColorDotPics[0]);
                    } else {
                        imageView.setImageResource(mColorDotPics[1]);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        for (int i = 0; i < mLlColorDots.getChildCount(); i++) {
            ImageView imageView = (ImageView) mLlColorDots.getChildAt(i);
            imageView.setTag(i);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (Integer) v.getTag();

                    if (mCurrentColorIndex != index) {
                        for (int j = 0; j < mLlColorDots.getChildCount(); j++) {
                            ImageView iv = (ImageView) mLlColorDots.getChildAt(j);
                            if (j == index) {
                                iv.setImageResource(mColorDotPics[0]);
                            } else {
                                iv.setImageResource(mColorDotPics[1]);
                            }
                        }

                        mColorViewPager.setCurrentItem(index);
                    }
                }
            });

            if (i == 0) {
                imageView.setImageResource(mColorDotPics[0]);
            } else {
                imageView.setImageResource(mColorDotPics[1]);
            }
        }
        mColorViewPager.setCurrentItem(mCurrentColorIndex);

        return colorItemView;
    }

    private void initColorOne(LinearLayout pb_ll_colors) {
        pb_ll_colors.removeAllViews();
        int colorWidth = display.dp2px(30.0f);
        final int padding = display.dp2px(6.0f);
        int space = display.dp2px(5.0f);

        int tempWidth = mParent.getWidth();
        int tempHeight = mParent.getHeight();

        if (display.isPad()) {
            mCurrentWidth = mPadWidth;
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mCurrentWidth = tempHeight > tempWidth ? tempHeight : tempWidth;
            } else {
                mCurrentWidth = tempWidth < tempHeight ? tempWidth : tempHeight;
            }
        }

        int length;
        if (display.isPad()) {
            if (mLlArrowLeft.getVisibility() == View.VISIBLE) {
                mLlArrowLeft.measure(0, 0);
                length = mCurrentWidth - display.dp2px(16.0f) * 2 - mLlArrowLeft.getMeasuredWidth();
            } else if (mLlArrowRight.getVisibility() == View.VISIBLE) {
                mLlArrowRight.measure(0, 0);
                length = mCurrentWidth - display.dp2px(16.0f) * 2 - mLlArrowRight.getMeasuredWidth();
            } else if (mLlArrowLeft.getVisibility() == View.GONE && mLlArrowRight.getVisibility() == View.GONE
                    && mLlArrowTop.getVisibility() == View.GONE && mLlArrowBottom.getVisibility() == View.GONE) {
                length = mCurrentWidth - display.dp2px(16.0f + 4.0f) * 2;
            } else {
                length = mCurrentWidth - display.dp2px(16.0f + 4.0f) * 2;
            }
        } else {
            length = mCurrentWidth - display.dp2px(16.0f) * 2;
        }
        if ((colorWidth + padding * 2) * mColors.length > length) {
            if (mColors.length > 1) {
                if (mColors.length % 2 == 0) {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 2));
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 2 - 1));
                    } else {
                        space = 0;
                    }
                } else {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 2) + 1);
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 2));
                    } else {
                        space = 0;
                    }
                }
            } else {
                space = 0;
            }

            // 2 rows
            pb_ll_colors.setOrientation(LinearLayout.VERTICAL);
            pb_ll_colors.setGravity(Gravity.CENTER);

            LinearLayout ll_ColorRow1 = new LinearLayout(mContext);
            ll_ColorRow1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow1.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow1.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow1);

            LinearLayout ll_ColorRow2 = new LinearLayout(mContext);
            ll_ColorRow2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow2.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow2.setPadding(0, display.dp2px(5.0f), 0, 0);
            ll_ColorRow2.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow2);

            for (int i = 0; i < mColors.length; i++) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(colorWidth + padding * 2, colorWidth + padding * 2);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER);
                if (mColors.length % 2 == 0) {
                    if ((i > 0 && i < mColors.length / 2) || (i > mColors.length / 2)) {
                        linearLayoutParams.leftMargin = space;
                    }
                } else {
                    if ((i > 0 && i < mColors.length / 2 + 1) || (i > mColors.length / 2 + 1)) {
                        linearLayoutParams.leftMargin = space;
                    }
                }
                linearLayout.setLayoutParams(linearLayoutParams);
                linearLayout.setTag(i);

                LinearLayout oneColor = new LinearLayout(mContext);
                LinearLayout.LayoutParams oneColorParams = new LinearLayout.LayoutParams(colorWidth, colorWidth);
                oneColor.setOrientation(LinearLayout.HORIZONTAL);
                oneColor.setGravity(Gravity.CENTER);
                oneColor.setLayoutParams(oneColorParams);
                oneColor.setBackgroundResource(R.drawable.pb_color_bg_border);
                linearLayout.addView(oneColor);

                ImageView color = new ImageView(mContext);
                color.setLayoutParams(new LinearLayout.LayoutParams(colorWidth - 2, colorWidth - 2));
                color.setImageDrawable(new ColorDrawable(mColors[i]));
                oneColor.addView(color);

                if (mColors.length % 2 == 0) {
                    if (i < mColors.length / 2) {
                        ll_ColorRow1.addView(linearLayout);
                    } else {
                        ll_ColorRow2.addView(linearLayout);
                    }
                } else {
                    if (i < mColors.length / 2 + 1) {
                        ll_ColorRow1.addView(linearLayout);
                    } else {
                        ll_ColorRow2.addView(linearLayout);
                    }
                }

                if (mColor == mColors[i]) {
                    linearLayout.setBackgroundResource(R.drawable.pb_color_bg);
                } else {
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }

                linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v instanceof LinearLayout) {
                            int tag = ((Integer) v.getTag()).intValue();
                            mColor = mColors[tag];

                            for (int j = 0; j < mColors.length; j++) {
                                if (j == tag) {
                                    v.setBackgroundResource(R.drawable.pb_color_bg);
                                } else {
                                    LinearLayout otherColor;
                                    if (mColors.length % 2 == 0) {
                                        if (j < mColors.length / 2) {
                                            otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(0)).getChildAt(j);

                                        } else {
                                            otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(1)).getChildAt(j - mColors.length / 2);
                                        }
                                    } else {
                                        if (j < mColors.length / 2 + 1) {
                                            otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(0)).getChildAt(j);

                                        } else {
                                            otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(1)).getChildAt(j - (mColors.length / 2 + 1));
                                        }
                                    }
                                    otherColor.setBackgroundColor(Color.TRANSPARENT);
                                }
                            }
                            if (mPropertyChangeListener != null) {
                                mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_COLOR, mColor);
                            }

                            if (mTabs.contains(mSupportTabNames[2])) {
                                ThicknessImage thicknessImage = (ThicknessImage) ((LinearLayout) ((LinearLayout) ((LinearLayout) mLl_tabContents.getChildAt(mTabs.indexOf(mSupportTabNames[2]))).getChildAt(1)).getChildAt(1)).getChildAt(0);
                                thicknessImage.setColor(mColor);
                            }
                        }
                    }
                });
            }
        } else {
            if (mColors.length > 1) {
                space = (length - colorWidth * mColors.length - padding * 2 * mColors.length) / (mColors.length - 1);
            } else {
                space = 0;
            }
            // 1 rows
            pb_ll_colors.setOrientation(LinearLayout.HORIZONTAL);
            pb_ll_colors.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

            for (int i = 0; i < mColors.length; i++) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(colorWidth + padding * 2, colorWidth + padding * 2);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER);
                if (i > 0) {
                    linearLayoutParams.leftMargin = space;
                } else if (i == 0) {
                    linearLayoutParams.leftMargin = 0;
                }
                linearLayout.setLayoutParams(linearLayoutParams);
                linearLayout.setTag(i);

                LinearLayout oneColor = new LinearLayout(mContext);
                LinearLayout.LayoutParams oneColorParams = new LinearLayout.LayoutParams(colorWidth, colorWidth);
                oneColor.setOrientation(LinearLayout.HORIZONTAL);
                oneColor.setGravity(Gravity.CENTER);
                oneColor.setLayoutParams(oneColorParams);
                oneColor.setBackgroundResource(R.drawable.pb_color_bg_border);
                linearLayout.addView(oneColor);

                ImageView color = new ImageView(mContext);
                color.setLayoutParams(new LinearLayout.LayoutParams(colorWidth - 2, colorWidth - 2));
                color.setImageDrawable(new ColorDrawable(mColors[i]));
                oneColor.addView(color);

                pb_ll_colors.addView(linearLayout);

                if (mColor == mColors[i]) {
                    linearLayout.setBackgroundResource(R.drawable.pb_color_bg);
                } else {
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }

                linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v instanceof LinearLayout) {
                            int tag = ((Integer) v.getTag()).intValue();
                            mColor = mColors[tag];
                            for (int j = 0; j < mColors.length; j++) {
                                if (j == tag) {
                                    v.setBackgroundResource(R.drawable.pb_color_bg);
                                } else {
                                    LinearLayout colorRow = (LinearLayout) ((LinearLayout) v).getParent();
                                    LinearLayout otherColor = (LinearLayout) colorRow.getChildAt(j);
                                    otherColor.setBackgroundColor(Color.TRANSPARENT);
                                }
                            }
                            if (mPropertyChangeListener != null) {
                                mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_COLOR, mColor);
                            }

                            if (mTabs.contains(mSupportTabNames[2])) {
                                ThicknessImage thicknessImage = (ThicknessImage) ((LinearLayout) ((LinearLayout) ((LinearLayout) mLl_tabContents.getChildAt(mTabs.indexOf(mSupportTabNames[2]))).getChildAt(1)).getChildAt(1)).getChildAt(0);
                                thicknessImage.setColor(mColor);
                            }
                        }
                    }
                });
            }
        }
    }

    public void onConfigurationChanged(RectF rectF) {
        int currentRotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        if (mCurrentRotation == currentRotation&&rectF != null)
            update(rectF);
        else {
            mCurrentRotation = currentRotation;
            if ((mSupportProperty != 0 || mCustomProperty != 0) && mPdfViewCtrl.getDoc() != null) {
                mOrientationed = true;
                reset(mSupportProperty);
            }
        }
    }

    @Override
    public void reset(long items) {
        mSupportProperty = items;
        if (mOrientationed) {
        } else {
            mCustomProperty = 0;
            mCurrentTab = 0;
            mCustomTabList.clear();
            mCustomItemList.clear();
        }

        mTabs.clear();
        mLl_titles.removeAllViews();
        mLl_title_checked.removeAllViews();
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            ((LinearLayout) mLl_tabContents.getChildAt(i)).removeAllViews();
        }
        mLl_tabContents.removeAllViews();

        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (items == PropertyBar.PROPERTY_UNKNOWN) {
            mLl_topTabs.setVisibility(View.GONE);
            mIv_title_shadow.setVisibility(View.GONE);
        } else {
            mLl_topTabs.setVisibility(View.VISIBLE);
            mIv_title_shadow.setVisibility(View.VISIBLE);

            resetSupportedView();
        }

        if (mOrientationed) {
            if (mCustomProperty != 0) {
                resetCustomView();
            }
            mOrientationed = false;
        }
    }

    private void resetSupportedView() {
        if ((mSupportProperty & PropertyBar.PROPERTY_ANNOT_TYPE) == PropertyBar.PROPERTY_ANNOT_TYPE) {
            String iconTabTitle = mSupportTabNames[0];
            int iconTabIndex = 0;
            if (mTabs.size() > 0) {
                if (mTabs.contains(iconTabTitle)) {
                    iconTabIndex = mTabs.indexOf(iconTabTitle);
                    if (iconTabIndex < 0) {
                        iconTabIndex = 0;
                    }
                } else {
                    iconTabIndex = 0;
                }
            } else {
                iconTabIndex = 0;
            }
            mTopTitleLayout.setVisibility(View.GONE);
            addTab(iconTabTitle, iconTabIndex);
            addCustomItem(0, getIconTypeView(), iconTabIndex, -1);
        }

        if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE
                || (mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH
                || (mSupportProperty & PropertyBar.PROPERTY_COLOR) == PropertyBar.PROPERTY_COLOR
                || (mSupportProperty & PropertyBar.PROPERTY_OPACITY) == PropertyBar.PROPERTY_OPACITY
                || (mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH) {
            String propertyTabTitle = "";
            if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                    || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE) {
                propertyTabTitle = mSupportTabNames[3];
            } else if ((mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH
                    || (mSupportProperty & PropertyBar.PROPERTY_LINE_STYLE) == PropertyBar.PROPERTY_LINE_STYLE) {
                propertyTabTitle = mSupportTabNames[2];
            } else if ((mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                    || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH) {
                propertyTabTitle = mSupportTabNames[4];
            } else {
                propertyTabTitle = mSupportTabNames[1];
            }

            int propertyTabIndex = mTabs.size();
            mTopTitleLayout.setVisibility(View.GONE);
            addTab(propertyTabTitle, propertyTabIndex);

            if ((mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                    || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH) {
                addCustomItem(0, getScaleView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                    || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE) {
                addCustomItem(0, getFontView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_COLOR) == PropertyBar.PROPERTY_COLOR) {
                addCustomItem(0, getColorView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH) {
                addCustomItem(0, getLineWidthView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_LINE_STYLE) == PropertyBar.PROPERTY_LINE_STYLE) {
                addCustomItem(0, getLineStyleView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_OPACITY) == PropertyBar.PROPERTY_OPACITY) {
                addCustomItem(0, getOpacityView(), propertyTabIndex, -1);
            }
        }
    }

    private void resetCustomView() {
        for (int i = 0; i < mCustomTabList.size(); i++) {
            addTab(mCustomTabList.get(i).get("topTitle").toString(), (Integer) mCustomTabList.get(i).get("resid_img"),
                    mCustomTabList.get(i).get("title").toString(), (Integer) mCustomTabList.get(i).get("tabIndex"));
        }
        for (int i = 0; i < mCustomItemList.size(); i++) {
            long item = (Long) mCustomItemList.get(i).get("item");
            if ((item & mCustomProperty) == item) {
                addCustomItem(item, (View) mCustomItemList.get(i).get("itemView"), (Integer) mCustomItemList.get(i).get("tabIndex"),
                        (Integer) mCustomItemList.get(i).get("index"));
            }
        }
    }

    private void doAfterAddContentItem() {
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            LinearLayout tabContentTemp = (LinearLayout) mLl_tabContents.getChildAt(i);
            if (tabContentTemp != null && tabContentTemp.getChildCount() > 0) {
                for (int j = 0; j < tabContentTemp.getChildCount(); j++) {
                    View viewItem = tabContentTemp.getChildAt(j);
                    if (viewItem != null) {
                        View separator = viewItem.findViewById(R.id.pb_separator_iv);
                        if (separator != null) {
                            if (j == tabContentTemp.getChildCount() - 1) {
                                separator.setVisibility(View.GONE);
                            } else {
                                separator.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        }

        resetContentHeight();

        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            if (i == mCurrentTab) {
                mLl_tabContents.getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                mLl_tabContents.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    private void resetContentHeight() {
        int maxTabContentHeight = 0;
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int iconTabIndex = -1;
        if (mTabs.contains(mSupportTabNames[0])) {
            iconTabIndex = mTabs.indexOf(mSupportTabNames[0]);
        }
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) mLl_tabContents.getChildAt(i);
            child.measure(w, h);
            int childHeight = child.getMeasuredHeight();
            if (i == iconTabIndex) {
                childHeight = 0;
            }
            if (childHeight > maxTabContentHeight) {
                maxTabContentHeight = childHeight;
            }
        }

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mLl_tabContents.getLayoutParams();
        if (!(!display.isPad() && layoutParams.height == LinearLayout.LayoutParams.MATCH_PARENT)) {
            layoutParams.height = maxTabContentHeight;
            mLl_tabContents.setLayoutParams(layoutParams);
        }
    }

    private void checkContained() {
        boolean colorContained = false;
        for (int i = 0; i < mColors.length; i++) {
            if (mColor == mColors[i]) {
                colorContained = true;
                break;
            }
        }
        if (!colorContained) {
            mColor = mColors[0];
        }

        boolean colorOpacity = false;
        for (int i = 0; i < mOpacitys.length; i++) {
            if (mOpacity == mOpacitys[i]) {
                colorOpacity = true;
                break;
            }
        }
        if (!colorOpacity) {
            mOpacity = mOpacitys[mOpacitys.length - 1];
        }
    }

    @Override
    public void addTab(String title, int tabIndex) {
        if (tabIndex > mTabs.size() || tabIndex < 0)
            return;
        if (title.length() == 0) {
            mTabs.add(tabIndex, "");
        } else {
            mTabs.add(tabIndex, title);
        }

        TextView tv_title = new TextView(mContext);
        tv_title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tv_title.setText(title);
        tv_title.setTextSize(16.0f);
        tv_title.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
        tv_title.setTypeface(Typeface.DEFAULT);
        tv_title.setGravity(Gravity.CENTER);
        tv_title.setSingleLine(true);
        tv_title.setEllipsize(TextUtils.TruncateAt.END);
        tv_title.setPadding(0, display.dp2px(5.0f), 0, display.dp2px(10.0f));

        tv_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickTagIndex = 0;
                for (int i = 0; i < mLl_titles.getChildCount(); i++) {
                    if (v == mLl_titles.getChildAt(i)) {
                        clickTagIndex = i;
                    }
                }

                if (mCurrentTab != clickTagIndex) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mCurrentTab = clickTagIndex;
                    setCurrentTab(mCurrentTab);
                }
            }
        });
        mLl_titles.addView(tv_title, tabIndex);

        ImageView iv_title_checked = new ImageView(mContext);
        iv_title_checked.setLayoutParams(new LinearLayout.LayoutParams(0, (int) mContext.getResources()
                .getDimension(R.dimen.ux_tab_selection_height), 1));
        mLl_title_checked.addView(iv_title_checked);

        LinearLayout ll_content = new LinearLayout(mContext);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ll_content.setOrientation(LinearLayout.VERTICAL);
        mLl_tabContents.addView(ll_content, tabIndex);

        if (mTabs.size() + mCustomTabList.size() > 0) {
            if (mTabs.size() + mCustomTabList.size() == 1) {
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
            } else {
                mLl_topTabs.setVisibility(View.VISIBLE);
                mIv_title_shadow.setVisibility(View.VISIBLE);

                setCurrentTab(mCurrentTab);
            }
        }
    }

    @Override
    public void setTopTitleVisible(boolean visible) {
        if (visible) {
            mTopTitleLayout.setVisibility(View.VISIBLE);
            mTopTitleLayout.setTag(1);
        } else {
            mTopTitleLayout.setVisibility(View.GONE);
            mTopTitleLayout.setTag(0);
        }
    }

    @Override
    public void addTab(String topTitle, int resid_img, String title, int tabIndex) {

        if (tabIndex > mTabs.size() + mCustomTabList.size() || tabIndex < 0) {
            return;
        }

        if (!mOrientationed) {
            Map<String, Object> map = new HashMap<String, Object>();
            if (title.length() == 0) {
                map.put("title", "");
            } else {
                map.put("title", title);
            }
            if (topTitle.length() == 0) {
                map.put("topTitle", "");
            } else {
                map.put("topTitle", topTitle);
            }
            map.put("resid_img", resid_img);
            map.put("tabIndex", tabIndex);
            mCustomTabList.add(map);
        }

        LinearLayout titleLayout = new LinearLayout(mContext);
        titleLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleLayout.setGravity(Gravity.CENTER);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setPadding(0, display.dp2px(5.0f), 0, display.dp2px(10.0f));

        titleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickTagIndex = 0;
                for (int i = 0; i < mLl_titles.getChildCount(); i++) {
                    if (v == mLl_titles.getChildAt(i)) {
                        clickTagIndex = i;
                    }
                }

                if (mCurrentTab != clickTagIndex) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mCurrentTab = clickTagIndex;
                    setCurrentTab(mCurrentTab);
                }
            }
        });

        if (resid_img != 0 && resid_img > 0) {
            ImageView img = new ImageView(mContext);
            img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            img.setImageResource(resid_img);
            titleLayout.addView(img);
        }

        if (title != null && !"".equals(title)) {
            TextView tv_title = new TextView(mContext);
            tv_title.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tv_title.setText(title);
            tv_title.setTextSize(16.0f);
            tv_title.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            tv_title.setGravity(Gravity.CENTER);
            tv_title.setSingleLine(true);
            tv_title.setEllipsize(TextUtils.TruncateAt.END);
            titleLayout.addView(tv_title);
        }
        mLl_titles.addView(titleLayout, tabIndex);

        ImageView iv_title_checked = new ImageView(mContext);
        iv_title_checked.setLayoutParams(new LinearLayout.LayoutParams(0, (int) mContext.getResources()
                .getDimension(R.dimen.ux_tab_selection_height), 1));
        mLl_title_checked.addView(iv_title_checked);

        LinearLayout ll_content = new LinearLayout(mContext);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ll_content.setOrientation(LinearLayout.VERTICAL);
        mLl_tabContents.addView(ll_content, tabIndex);

        if (mTabs.size() + mCustomTabList.size() > 0) {
            if (mTabs.size() + mCustomTabList.size() == 1) {
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
            } else {
                mLl_topTabs.setVisibility(View.VISIBLE);
                mIv_title_shadow.setVisibility(View.VISIBLE);

                setCurrentTab(mCurrentTab);
            }
        }
    }

    @Override
    public int getCurrentTabIndex() {
        return mCurrentTab;
    }

    @Override
    public void setCurrentTab(int currentTab) {
        mCurrentTab = currentTab;
        for (int i = 0; i < mLl_titles.getChildCount(); i++) {
            if (i == currentTab) {
                View viewTab = mLl_titles.getChildAt(i);
                if (viewTab instanceof TextView) {
                    mTopTitle.setText("");
                    mTopTitleLayout.setVisibility(View.GONE);
                } else if (viewTab instanceof LinearLayout) {
                    if ((Integer) mTopTitleLayout.getTag() == 1) {
                        mTopTitleLayout.setVisibility(View.VISIBLE);
                        if (mTopTitleLayout.getVisibility() == View.VISIBLE) {
                            String topTitle = "";
                            for (int j = 0; j < mCustomTabList.size(); j++) {
                                if (currentTab == (Integer) mCustomTabList.get(j).get("tabIndex")) {
                                    topTitle = mCustomTabList.get(j).get("topTitle").toString();
                                    break;
                                }
                            }
                            mTopTitle.setText(topTitle);
                        }
                    }

                    View view = ((LinearLayout) viewTab).getChildAt(0);
                    if (view != null && view instanceof ImageView) {
                        ((ImageView) view).setImageState(new int[]{android.R.attr.state_selected}, true);
                        ((ImageView) view).setSelected(true);
                    }
                }

                ((ImageView) mLl_title_checked.getChildAt(i)).setImageDrawable(new ColorDrawable(Color.WHITE));
                mLl_tabContents.getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                View viewTab = mLl_titles.getChildAt(i);
                if (viewTab instanceof LinearLayout) {
                    View view = ((LinearLayout) mLl_titles.getChildAt(i)).getChildAt(0);
                    if (view != null && view instanceof ImageView) {
                        ((ImageView) view).setImageState(new int[]{}, true);
                        ((ImageView) view).setSelected(false);
                    }
                } else if (viewTab instanceof TextView) {

                }

                ((ImageView) mLl_title_checked.getChildAt(i)).setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                mLl_tabContents.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemIndex(long item) {
        int indexItemInTab = -1;
        if ((mSupportProperty & item) == item) {
            if (item == PropertyBar.PROPERTY_ANNOT_TYPE) {
                indexItemInTab = mTabs.indexOf(mSupportTabNames[0]);
            } else {
                if (mTabs.contains(mSupportTabNames[1])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[1]);
                }
                if (mTabs.contains(mSupportTabNames[2])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[2]);
                }
                if (mTabs.contains(mSupportTabNames[3])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[3]);
                }
                if (mTabs.contains(mSupportTabNames[4])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[4]);
                }
            }
        } else {
            if ((mCustomProperty & item) == item) {
                for (int i = 0; i < mCustomItemList.size(); i++) {
                    if (item == (Long) mCustomItemList.get(i).get("item")) {
                        indexItemInTab = (Integer) mCustomItemList.get(i).get("tabIndex");
                        break;
                    }
                }
            }
        }

        return indexItemInTab;
    }

    @Override
    public void addCustomItem(long item, View itemView, int tabIndex, int index) {
        if (itemView == null) {
            return;
        }
        if (tabIndex < 0 || tabIndex > mLl_tabContents.getChildCount() - 1) {
            return;
        }
        View view = mLl_tabContents.getChildAt(tabIndex);
        if (view != null) {
            LinearLayout ll_content = (LinearLayout) view;
            if (index != -1 && (index < 0 || index > ll_content.getChildCount())) {
                return;
            }

            if (item > 0 && !mOrientationed) {
                mCustomProperty = mCustomProperty | item;
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("item", item);
                map.put("itemView", itemView);
                map.put("tabIndex", tabIndex);
                map.put("index", index);
                mCustomItemList.add(map);
            }

            if (index == -1) {
                ll_content.addView(itemView);
            } else {
                if (index < 0 || index > ll_content.getChildCount()) {
                    return;
                }
                ll_content.addView(itemView, index);
            }
        }
        doAfterAddContentItem();
    }

    @Override
    public void addContentView(View contentView) {
        mLl_tabContents.addView(contentView);
    }

    @Override
    public View getContentView() {
        return super.getContentView();
    }

    @Override
    public void update(RectF rectF) {
        mRectF.set(rectF);

        int height = mParent.getHeight();
        int width = mParent.getWidth();
        if (display.isPad()) {
            int w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            int arrowPosition;
            if (rectF.top >= mLl_root.getMeasuredHeight()) {
                arrowPosition = ARROW_BOTTOM;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.VISIBLE);
            } else if (height - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                arrowPosition = ARROW_TOP;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.VISIBLE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else if (width - rectF.right >= mPadWidth) {
                arrowPosition = ARROW_LEFT;
                mLlArrowLeft.setVisibility(View.VISIBLE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else if (rectF.left >= mPadWidth) {
                arrowPosition = ARROW_RIGHT;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.VISIBLE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else {
                arrowPosition = ARROW_CENTER;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            }
            if (mArrowVisible) {
                mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg);
                mLl_PropertyBar.setPadding(0, 0, 0, display.dp2px(5.0f));
            } else {
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
                mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
                mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                        display.dp2px(4.0f), display.dp2px(4.0f));
            }

            mLl_root.measure(w1, h1);
            if (arrowPosition == ARROW_BOTTOM) {
                mIvArrowBottom.measure(0, 0);

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowBottom.setPadding((int) (mPadWidth / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        }
                    } else {
                        toLeft = width - mPadWidth;
                        if (mArrowVisible) {
                            if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                mLlArrowBottom.setPadding(0, 0, (int) (width - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0);
                            } else {
                                mLlArrowBottom.setPadding(mPadWidth - mIvArrowBottom.getMeasuredWidth(), 0, 0, 0);
                            }
                        }
                    }
                } else {
                    toLeft = 0;
                    if (mArrowVisible) {
                        if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                            mLlArrowBottom.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        } else {
                            mLlArrowBottom.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update(toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()), -1, -1);
            } else if (arrowPosition == ARROW_TOP) {
                mIvArrowTop.measure(0, 0);

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowTop.setPadding((int) (mPadWidth / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        }
                    } else {
                        toLeft = width - mPadWidth;
                        if (mArrowVisible) {
                            if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                mLlArrowTop.setPadding(0, 0, (int) (width - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0);
                            } else {
                                mLlArrowTop.setPadding(mPadWidth - mIvArrowTop.getMeasuredWidth(), 0, 0, 0);
                            }
                        }
                    }
                } else {
                    toLeft = 0;
                    if (mArrowVisible) {
                        if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                            mLlArrowTop.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        } else {
                            mLlArrowTop.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update(toLeft, (int) rectF.bottom, -1, -1);

            } else if (arrowPosition == ARROW_LEFT) {
                mIvArrowLeft.measure(0, 0);

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowLeft.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                        }
                    } else {
                        toTop = height - mLl_root.getMeasuredHeight();
                        if (mArrowVisible) {
                            if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                mLlArrowLeft.setPadding(0, 0, 0, (int) (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f));
                            } else {
                                mLlArrowLeft.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowLeft.getMeasuredHeight(), 0, 0);
                            }
                        }
                    }
                } else {
                    toTop = 0;
                    if (mArrowVisible) {
                        if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                            mLlArrowLeft.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                        } else {
                            mLlArrowLeft.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update((int) (rectF.right), toTop, -1, -1);
            } else if (arrowPosition == ARROW_RIGHT) {
                mIvArrowRight.measure(0, 0);

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowRight.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                        }
                    } else {
                        toTop = height - mLl_root.getMeasuredHeight();
                        if (mArrowVisible) {
                            if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                mLlArrowRight.setPadding(0, 0, 0, (int) (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f));
                            } else {
                                mLlArrowRight.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowRight.getMeasuredHeight(), 0, 0);
                            }
                        }
                    }
                } else {
                    toTop = 0;
                    if (mArrowVisible) {
                        if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                            mLlArrowRight.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                        } else {
                            mLlArrowRight.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update((int) (rectF.left - mPadWidth), toTop, -1, -1);
            } else if (arrowPosition == ARROW_CENTER) {
                update((int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f), -1, -1);
            }
        } else {
            mArrowVisible = false;
            mLlArrowLeft.setVisibility(View.GONE);
            mLlArrowTop.setVisibility(View.GONE);
            mLlArrowRight.setVisibility(View.GONE);
            mLlArrowBottom.setVisibility(View.GONE);
            mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            update(0, 0, -1, -1);
        }
    }

    @Override
    public boolean isShowing() {
        if (this != null) {
            return super.isShowing();
        } else {
            return false;
        }
    }


    @Override
    public void show(RectF rectF, boolean showMask) {
        mRectF.set(rectF);
        if (this != null && !this.isShowing()) {
            setFocusable(true);

            int height = mParent.getHeight();
            int width = mParent.getWidth();

            int w1;
            if (display.isPad()) {
                w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            } else {
                w1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            }
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            if (display.isPad()) {
                int arrowPosition;
                if (rectF.top >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = ARROW_BOTTOM;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.VISIBLE);
                } else if (height - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = ARROW_TOP;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.VISIBLE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else if (width - rectF.right >= mPadWidth) {
                    arrowPosition = ARROW_LEFT;
                    mLlArrowLeft.setVisibility(View.VISIBLE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else if (rectF.left >= mPadWidth) {
                    arrowPosition = ARROW_RIGHT;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.VISIBLE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else {
                    arrowPosition = ARROW_CENTER;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                }
                if (mArrowVisible) {
                    mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg);
                    mLl_PropertyBar.setPadding(0, 0, 0, display.dp2px(5.0f));
                } else {
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                    mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
                    mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                            display.dp2px(4.0f), display.dp2px(4.0f));
                }

                int w2 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
                int h2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mLl_root.measure(w2, h2);

                if (arrowPosition == ARROW_BOTTOM) {
                    mIvArrowBottom.measure(0, 0);

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowBottom.setPadding((int) (mPadWidth / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            }
                        } else {
                            toLeft = width - mPadWidth;
                            if (mArrowVisible) {
                                if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                    mLlArrowBottom.setPadding(0, 0, (int) (width - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0);
                                } else {
                                    mLlArrowBottom.setPadding(mPadWidth - mIvArrowBottom.getMeasuredWidth(), 0, 0, 0);
                                }
                            }
                        }
                    } else {
                        toLeft = 0;
                        if (mArrowVisible) {
                            if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                mLlArrowBottom.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            } else {
                                mLlArrowBottom.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP, toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()));
                } else if (arrowPosition == ARROW_TOP) {
                    mIvArrowTop.measure(0, 0);

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowTop.setPadding((int) (mPadWidth / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            }
                        } else {
                            toLeft = width - mPadWidth;
                            if (mArrowVisible) {
                                if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                    mLlArrowTop.setPadding(0, 0, (int) (width - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0);
                                } else {
                                    mLlArrowTop.setPadding(mPadWidth - mIvArrowTop.getMeasuredWidth(), 0, 0, 0);
                                }
                            }
                        }
                    } else {
                        toLeft = 0;
                        if (mArrowVisible) {
                            if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                mLlArrowTop.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            } else {
                                mLlArrowTop.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP, toLeft, (int) rectF.bottom);
                } else if (arrowPosition == ARROW_LEFT) {
                    mIvArrowLeft.measure(0, 0);

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowLeft.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                            }
                        } else {
                            toTop = height - mLl_root.getMeasuredHeight();
                            if (mArrowVisible) {
                                if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                    mLlArrowLeft.setPadding(0, 0, 0, (int) (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f));
                                } else {
                                    mLlArrowLeft.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowLeft.getMeasuredHeight(), 0, 0);
                                }
                            }
                        }
                    } else {
                        toTop = 0;
                        if (mArrowVisible) {
                            if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                mLlArrowLeft.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                            } else {
                                mLlArrowLeft.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP, (int) (rectF.right), toTop);
                } else if (arrowPosition == ARROW_RIGHT) {
                    mIvArrowRight.measure(0, 0);

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowRight.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                            }
                        } else {
                            toTop = height - mLl_root.getMeasuredHeight();
                            if (mArrowVisible) {
                                if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                    mLlArrowRight.setPadding(0, 0, 0, (int) (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f));
                                } else {
                                    mLlArrowRight.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowRight.getMeasuredHeight(), 0, 0);
                                }
                            }
                        }
                    } else {
                        toTop = 0;
                        if (mArrowVisible) {
                            if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                mLlArrowRight.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                            } else {
                                mLlArrowRight.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left - mPadWidth), toTop);
                } else if (arrowPosition == ARROW_CENTER) {
                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f));
                }

            } else {
                if (showMask) {
                    mTopShadow.setVisibility(View.GONE);
                } else {
                    mTopShadow.setVisibility(View.VISIBLE);
                }

                mArrowVisible = false;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
                mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
                showAtLocation(mParent, Gravity.BOTTOM, 0, 0);

                // move full screen to somewhere in selected annotation model.
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
                if (currentAnnotHandler != null && uiExtensionsManager.getCurrentToolHandler() == null) {

                    mLl_root.measure(w1, h1);
                    if (rectF.bottom > 0 && rectF.bottom <= height) {
                        if (rectF.bottom > height - mLl_root.getMeasuredHeight()) {
                            offset = mLl_root.getMeasuredHeight() - (height - rectF.bottom);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                                }
                            }, 300);
                        }

                    } else if (rectF.top >= 0 && rectF.top <= height && rectF.bottom > height) {
                        if (rectF.top > height - mLl_root.getMeasuredHeight()) {
                            offset = mLl_root.getMeasuredHeight() - (height - rectF.top) + 10;

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                                }
                            }, 300);
                        }
                    }
                }
            }

            mShowMask = showMask;
        }
    }

    @Override
    public void dismiss() {
        if (this != null && this.isShowing()) {
            setFocusable(false);
            super.dismiss();
        }
    }

    @Override
    public void setArrowVisible(boolean visible) {
        mArrowVisible = visible;
    }

    @Override
    public void setColors(int[] colors) {
        this.mColors = colors;
    }

    @Override
    public void setProperty(long property, int value) {
        if (property == PropertyBar.PROPERTY_COLOR) {
            mColor = value;
            int r = Color.red(mColor);
            int g = Color.green(mColor);
            int b = Color.blue(mColor);
            for (int i = 0; i < mColors.length; i++) {
                int r2 = Color.red(mColors[i]);
                int g2 = Color.green(mColors[i]);
                int b2 = Color.blue(mColors[i]);
                if (Math.abs(r2 - r) <= 3 && Math.abs(g2 - g) <= 3 && Math.abs(b2 - b) <= 3) {
                    mColor = mColors[i];
                    break;
                }
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            mOpacity = value;
        } else if (property == PropertyBar.PROPERTY_ANNOT_TYPE) {
            mNoteIconType = value;
        } else if (property == PropertyBar.PROPERTY_SCALE_PERCENT) {
            mScalePercent = value;
        } else if (property == PropertyBar.PROPERTY_SCALE_SWITCH) {
            mScaleSwitch = value;
        }
    }

    @Override
    public void setProperty(long property, float value) {
        if (property == PropertyBar.PROPERTY_LINEWIDTH) {
            mLinewith = value;
        } else if (property == PropertyBar.PROPERTY_FONTSIZE) {
            mFontsize = value;
        }
    }

    @Override
    public void setProperty(long property, String value) {
        if (property == PropertyBar.PROPERTY_FONTNAME) {
            mFontname = value;
        } else if (property == PropertyBar.PROPERTY_ANNOT_TYPE) {
            for (int i = 0; i < ICONNAMES.length; i++) {
                if (ICONNAMES[i].compareTo(value) != 0)
                    continue;
                mNoteIconType = ICONTYPES[i];
                break;
            }
        }
    }

    @Override
    public PropertyChangeListener getPropertyChangeListener() {
        return this.mPropertyChangeListener;
    }

    @Override
    public void setPropertyChangeListener(PropertyChangeListener listener) {
        this.mPropertyChangeListener = listener;
    }

    @Override
    public void setDismissListener(PropertyBar.DismissListener dismissListener) {
        this.mDismissListener = dismissListener;
    }
}
