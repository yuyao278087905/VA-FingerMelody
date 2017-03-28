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
package com.foxit.uiextensions.controls.panel.impl;

import java.util.ArrayList;
import java.util.List;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.PanelContentViewAdapter;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;


import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class PanelHostImpl implements PanelHost {
    private Context mContext;
    private View mRootView;
    private LinearLayout mTopLayout;
    private LinearLayout mTabLayout;
    private LinearLayout mContentLayout;
    private ArrayList<PanelSpec> mSpecs;
    private PanelSpec mCurSpec;

    private ViewPager mContentViewPager;
    private List<View> mViewPagerList;
    private PanelContentViewAdapter mViewPagerAdapter;
    private AppDisplay mDisplay;

    public PanelHostImpl(Context context) {
        mContext = context;
        mDisplay = new AppDisplay(mContext);
        mRootView = View.inflate(mContext, R.layout.root_panel, null);
        mTopLayout = (LinearLayout) mRootView.findViewById(R.id.panel_topbar_layout);
        mTabLayout = (LinearLayout) mRootView.findViewById(R.id.panel_tabbar_layout);
        mContentLayout = (LinearLayout) mRootView.findViewById(R.id.panel_content_layout);
        mContentViewPager = (ViewPager) mRootView.findViewById(R.id.panel_content_viewpager);

        mSpecs = new ArrayList<PanelSpec>();
        mViewPagerList = new ArrayList<View>();
        mViewPagerAdapter = new PanelContentViewAdapter(mViewPagerList);
        mContentViewPager.setAdapter(mViewPagerAdapter);
        mContentViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mCurSpec != null && mCurSpec.getTag() != mSpecs.get(position).getTag()) {
                    setCurrentSpec(mSpecs.get(position).getTag());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mTopLayout.setBackgroundResource(R.color.ux_text_color_subhead_colour);
        if (mDisplay.isPad()) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTopLayout.getLayoutParams();
            params.height = mDisplay.dp2px(64.0f);
            mTopLayout.setLayoutParams(params);
        }

        mTabLayout.setBackgroundResource(R.color.ux_text_color_subhead_colour);


        if (mDisplay.isPad()) {
            mTopLayout.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad);
        }
    }

    @Override
    public View getContentView() {
        return mRootView;
    }

    @Override
    public PanelSpec getSpec(int tag) {
        for (PanelSpec spec : mSpecs) {
            if (spec.getTag() == tag) {
                return spec;
            }
        }
        return null;
    }

    @Override
    public void addSpec(PanelSpec spec) {
        if (getSpec(spec.getTag()) != null)
            return;
        int index = -1;
        for (int i = 0; i < mSpecs.size(); i++) {
            if (mSpecs.get(i).getTag() > spec.getTag()) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            mSpecs.add(spec);
            mViewPagerList.add(spec.getContentView());
        } else {
            mSpecs.add(index, spec);
            mViewPagerList.add(index, spec.getContentView());
        }
        mViewPagerAdapter.notifyDataSetChanged();

        addTab(spec);
        if (mCurSpec == null) {
            setFocuses(0);
        }
    }

    @Override
    public void removeSpec(PanelSpec spec) {
        int index = mSpecs.indexOf(spec);
        if (index < 0) return;
        mSpecs.remove(index);
        mViewPagerList.remove(index);
        mViewPagerAdapter.notifyDataSetChanged();

        removeTab(spec);
        if (mSpecs.size() > index) {
            setFocuses(index);
        } else {
            setFocuses(mSpecs.size() - 1);
        }
    }

    @Override
    public void setCurrentSpec(int tag) {
        if (mCurSpec != null) {
            if (mCurSpec.getTag() == tag) {
                mCurSpec.onActivated();
                return;
            }
            mCurSpec.onDeactivated();
        }
        for (int i = 0; i < mSpecs.size(); i++) {
            if (mSpecs.get(i).getTag() == tag) {
                setFocuses(i);
                mSpecs.get(i).onActivated();
            }
        }
    }

    @Override
    public PanelSpec getCurrentSpec() {
        return mCurSpec;
    }

    private void addTab(final PanelSpec spec) {
        // icon view
        ImageView iconView = new ImageView(mContext);
        ;
        iconView.setId(R.id.rd_panel_tab_item);
        iconView.setImageResource(spec.getIcon());

        RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        iconLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        iconLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        // selected bright line
        ImageView focusView = new ImageView(mContext);
        focusView.setBackgroundColor(Color.WHITE);
        focusView.setImageResource(R.drawable.toolbar_shadow_top);
        focusView.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams focusLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, mDisplay.dp2px(4.0f));
        focusLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        int topMargin = 0;
        focusLayoutParams.setMargins(0, mDisplay.dp2px(topMargin), 0, 0);

        RelativeLayout tabItemView = new RelativeLayout(mContext);
        tabItemView.addView(iconView, iconLayoutParams);
        tabItemView.addView(focusView, focusLayoutParams);
        tabItemView.setTag(spec.getTag());

        tabItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentSpec(spec.getTag());
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        mTabLayout.addView(tabItemView, lp);
    }

    void removeTab(PanelSpec spec) {
        for (int i = 0, count = mTabLayout.getChildCount(); i < count; i++) {
            if ((Integer) mTabLayout.getChildAt(i).getTag() == spec.getTag()) {
                mTabLayout.removeViewAt(i);
                break;
            }
        }
    }

    private void setFocuses(int index) {
        if (index < 0 || index > mSpecs.size() - 1) {
            index = 0;
        }
        if (mSpecs.size() == 0)
            return;
        mCurSpec = mSpecs.get(index);
        mTopLayout.removeAllViews();
        mTopLayout.addView(mCurSpec.getTopToolbar());

        mContentViewPager.setCurrentItem(index);

        int iconCount = mSpecs.size();
        for (int i = 0; i < iconCount; i++) {
            RelativeLayout iconBox = (RelativeLayout) mTabLayout.getChildAt(i);
            if (i == index) {
                ((ImageView) iconBox.getChildAt(0)).setImageState(new int[]{android.R.attr.state_pressed}, true);
                iconBox.getChildAt(1).setVisibility(View.VISIBLE);
            } else {
                ((ImageView) iconBox.getChildAt(0)).setImageState(new int[]{}, true);
                iconBox.getChildAt(1).setVisibility(View.INVISIBLE);
            }
        }
    }
}
