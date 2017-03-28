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

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;

public class AnnotMenuImpl implements AnnotMenu {
    private Context mContext;
    private ArrayList<Integer> mMenuItems;
    private LinearLayout mPopView;

    private int mMaxWidth;
    private int mMinWidth;
    private ClickListener mListener;//menu click listener
    private PopupWindow mPopupWindow;
    private boolean mShowing = false;
    private AppDisplay display;
    private ViewGroup mParent;

    public AnnotMenuImpl(Context context, ViewGroup parent) {
        this.mContext = context;
        this.mParent = parent;
        display = AppDisplay.getInstance(context);
        mMaxWidth = display.dp2px(5000.0f);
        mMinWidth = display.dp2px(80.0f);
    }

    @Override
    public void setMenuItems(ArrayList<Integer> menuItems) {
        this.mMenuItems = menuItems;
        initView();
    }

    private void initView() {
        if (mPopView == null) {
            mPopView = new LinearLayout(mContext);
            mPopView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mPopView.setOrientation(LinearLayout.VERTICAL);
            mPopView.setBackgroundResource(R.drawable.am_popup_bg);
        } else {
            mPopView.removeAllViews();
        }

        for (int i = 0; i < mMenuItems.size(); i++) {
            if (i > 0) {
                ImageView separate = new ImageView(mContext);
                separate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, display.dp2px(1.0f)));
                separate.setImageResource(R.color.ux_color_seperator_gray);
                mPopView.addView(separate);
            }
            if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_COPY) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.rd_am_item_copy_text));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_HIGHLIGHT) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_highlight));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_UNDERLINE) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_underline));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_STRIKEOUT) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_strickout));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_SQUIGGLY) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_squiggly));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_EDIT) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_edit));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_STYLE) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_am_style));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_COMMENT) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_open));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_REPLY) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_reply));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_DELETE) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_delete));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_NOTE) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_note));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_SIGN) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_signature));
            } else if (mMenuItems.get(i).intValue() == AnnotMenu.AM_BT_CANCEL) {
                addSubMenu(mMenuItems.get(i).intValue(), mContext.getResources().getString(R.string.fx_string_cancel));
            }
        }

        setMenuWidth();

        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(mPopView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setTouchable(true);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setShowAlways(false);
    }

    @Override
    public void setShowAlways(boolean showAlways) {
        if (showAlways) {
            mPopupWindow.setFocusable(false);
            mPopupWindow.setOutsideTouchable(false);
        } else {
            mPopupWindow.setFocusable(false);
            mPopupWindow.setOutsideTouchable(false);
        }
    }

    private void addSubMenu(int menuTag, String text) {
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, display.dp2px(56.0f)));
        textView.setText(text);
        textView.setTypeface(Typeface.DEFAULT);
        textView.setTextSize(14.0f);
        textView.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        textView.setPadding(display.dp2px(8.0f), display.dp2px(5.0f), display.dp2px(8.0f), display.dp2px(5.0f));
        textView.setBackgroundResource(R.drawable.am_tv_bg_selector);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTag(menuTag);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tag = (Integer) v.getTag();
                if (mListener != null) {
                    mListener.onAMClick(tag);
                }
            }
        });

        mPopView.addView(textView);
    }

    private void setMenuWidth() {
        int width = getMenuWidth();
        ImageView separate;
        LinearLayout.LayoutParams separateParams;
        TextView textView;
        for (int i = 0; i < mMenuItems.size(); i++) {
            if (i > 0) {
                separate = (ImageView) mPopView.getChildAt(2 * i - 1);
                separateParams = (LinearLayout.LayoutParams) separate.getLayoutParams();
                if (separateParams != null && separate != null) {
                    separateParams.width = width;
                    separate.setLayoutParams(separateParams);
                }
            }
            textView = (TextView) mPopView.getChildAt(2 * i);
            if (textView != null) {
                textView.setWidth(width);
                textView.setMaxWidth(mMaxWidth);
            }
        }
    }

    private int getMenuWidth() {
        int realShowWidth = 0;
        TextView textView;
        for (int i = 0; i < mMenuItems.size(); i++) {
            textView = (TextView) mPopView.getChildAt(2 * i);
            if (textView != null) {
                textView.measure(0, 0);
                if (textView.getMeasuredWidth() < mMaxWidth) {
                    if (textView.getMeasuredWidth() > realShowWidth) {
                        realShowWidth = textView.getMeasuredWidth();
                    }
                } else {
                    realShowWidth = mMaxWidth;
                    break;
                }
            }
        }

        if (realShowWidth == 0 || realShowWidth < mMinWidth) {
            realShowWidth = mMinWidth;
        }

        return realShowWidth;
    }


    @Override
    public PopupWindow getPopupWindow() {
        return mPopupWindow;
    }

    @Override
    public void show(RectF rectF) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RectF expandRectF = new RectF(rectF.left - space, rectF.top - space, rectF.right + space, rectF.bottom + space);
            RelativeLayout view = (RelativeLayout) mParent;
            int height = view.getHeight();
            int width = view.getWidth();

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                int top = 0;
                int left = 0;
                int right = mParent.getWidth();
                int bottom = mParent.getHeight();
                top = 0;
                bottom = mParent.getHeight();
                RectF rectFScreen = new RectF(left, top, right, bottom);
                if (RectF.intersects(rectF, rectFScreen)) {
                    mPopupWindow.getContentView().measure(0, 0);
                    if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {//top
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                    } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {//bottom
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.bottom));
                    } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {//right
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                    } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {//left
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));

                    } else {//center
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                    }
                }

                mShowing = true;
            }
        }
    }

    @Override
    public void show(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RectF expandRectF = new RectF(rectF.left - space, rectF.top - space, rectF.right + space, rectF.bottom + space);
            RelativeLayout view = (RelativeLayout) mParent;
            int height = pageHeight;
            int width = pageWidth;

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                int top = 0;
                int left = 0;
                int right = pageWidth;
                int bottom = pageHeight;
                RectF rectFScreen = new RectF(left, top, right, bottom);
                if (RectF.intersects(rectF, rectFScreen) || !autoDismiss) {
                    mPopupWindow.getContentView().measure(0, 0);
                    if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {//top
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                    } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {//bottom
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.bottom));
                    } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {//right
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                    } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {//left
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));

                    } else {//center
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                    }
                }

                mShowing = true;
            }
        }
    }

    @Override
    public void show(RectF rectF, View view) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RectF expandRectF = new RectF(rectF.left - space, rectF.top - space, rectF.right + space, rectF.bottom + space);
            int height = mParent.getHeight();
            int width = mParent.getWidth();

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                mPopupWindow.getContentView().measure(0, 0);
                if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                            (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                            (int) (expandRectF.bottom));
                } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                } else {
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2));
                }

                mShowing = true;
            }
        }
    }

    @Override
    public void update(RectF rectF) {
        if (mMenuItems == null)
            return;
        if (mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RectF expandRectF = new RectF(rectF.left - space, rectF.top - space, rectF.right + space, rectF.bottom + space);
            int height = mParent.getHeight();
            int width = mParent.getWidth();

            if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()), -1, -1);
            } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.bottom), -1, -1);
            } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mPopupWindow.update((int) (expandRectF.right),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mPopupWindow.update((int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            }

            if (mShowing) {
                int top = 0;
                int left = 0;
                int right = mParent.getWidth();
                int bottom = mParent.getHeight();
                top = 0;
                bottom = mParent.getHeight();
                if (isShowing()) {
                    if (rectF.bottom <= top || rectF.right <= left || rectF.left >= right || rectF.top >= bottom) {
                        mPopupWindow.dismiss();
                    }
                } else {
                    if (rectF.top >= top && rectF.left >= left && rectF.right <= right && rectF.bottom <= bottom) {
                        boolean showing = mShowing;
                        show(expandRectF);
                        mShowing = showing;
                    }
                }
            }
        }
    }

    @Override
    public void update(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RectF expandRectF = new RectF(rectF.left - space, rectF.top - space, rectF.right + space, rectF.bottom + space);
            int height = pageHeight;
            int width = pageWidth;

            if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()), -1, -1);
            } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.bottom), -1, -1);
            } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mPopupWindow.update((int) (expandRectF.right),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mPopupWindow.update((int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else {
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2 - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            }
            if (autoDismiss) {
                if (mShowing) {
                    int top = 0;
                    int left = 0;
                    int right = pageHeight;
                    int bottom = pageWidth;
                    if (isShowing()) {
                        if (rectF.bottom <= top || rectF.right <= left || rectF.left >= right || rectF.top >= bottom) {
                            mPopupWindow.dismiss();
                        }
                    } else {
                        if (rectF.top >= top && rectF.left >= left && rectF.right <= right && rectF.bottom <= bottom) {
                            boolean showing = mShowing;
                            show(expandRectF, pageWidth, pageHeight, autoDismiss);
                            mShowing = showing;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isShowing() {
        if (mPopupWindow != null) {
            return mPopupWindow.isShowing();
        } else {
            return false;
        }
    }

    @Override
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            mShowing = false;
        }
    }

    @Override
    public void setListener(ClickListener listener) {
        mListener = listener;
    }
}
