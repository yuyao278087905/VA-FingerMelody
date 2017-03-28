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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.ThumbListView;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;


public class ThumbnailModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfView;
    private AppDisplay mDisplay;
    private boolean mSinglePage = true;//true:SINGLE_PAGE,false:CONTINUOUS_PAGE

    private Dialog mThumbnailDialog;
    private LinearLayout mThumbContentView;
    private ThumbListView mThumbnailView;
    private BaseBar mThumbnailTopBar;
    private BaseItem mCloseThumbnailBtn;
    private BaseItem mThumbnailTitle;
    private View mDialogRootView;

    public ThumbnailModule(Context context, PDFViewCtrl pdfView) {
        mContext = context;
        mPdfView = pdfView;
        mDisplay = new AppDisplay(mContext);
    }

    public void show() {
        initApplyValue();
        applyValue();
        if (!mThumbnailDialog.isShowing()) {
            showThumbnailDialog();
            mThumbnailDialog.show();
        }
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_THUMBNAIL;
    }

    @Override
    public boolean loadModule() {
        initDialogUI();
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    private void initApplyValue() {
        if (getViewModePosition() == mPdfView.PAGELAYOUTMODE_SINGLE) {
            mSinglePage = true;
        } else {
            mSinglePage = false;
        }
    }

    private void applyValue() {
        if (mSinglePage) {
            mPdfView.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);

        } else {
            mPdfView.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
        }

        PageNavigationModule pageNumberJump = (PageNavigationModule) ((UIExtensionsManager) mPdfView.getUIExtensionsManager()).getModuleByName(MODULE_NAME_PAGENAV);
        if (pageNumberJump != null)
            pageNumberJump.resetJumpView();
    }


    private int getViewModePosition() {
        switch (mPdfView.getPageLayoutMode()) {
            case PDFViewCtrl.PAGELAYOUTMODE_SINGLE:
                return PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
            case PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS:
                return PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS;
            default:
                return PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
        }
    }

    //--thumbnail Dialog
    private void initDialogUI() {
        mThumbnailDialog = new Dialog((Activity) mContext, R.style.rd_dialog_fullscreen_style);
        mThumbnailDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mThumbnailDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mDialogRootView = View.inflate(mContext, R.layout.rd_thumnail_dialog, null);
        mThumbnailDialog.setContentView(mDialogRootView);
        mThumbContentView = (LinearLayout) mDialogRootView.findViewById(R.id.thumbnailist);
        if (mDisplay.isPad()) {
            ((RelativeLayout.LayoutParams) mThumbContentView.getLayoutParams()).topMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad);
        }
        mThumbnailTopBar = new TopBarImpl(mContext);

        mThumbnailTopBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        mThumbnailTitle = new BaseItemImpl(mContext);
        mThumbnailTitle.setText(AppResource.getString(mContext, R.string.rv_page_present_thumbnail));
        mThumbnailTitle.setTextColorResource(R.color.ux_text_color_title_light);

        mThumbnailTitle.setTextSize(mDisplay.px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        mCloseThumbnailBtn = new BaseItemImpl(mContext);
        mCloseThumbnailBtn.setImageResource(R.drawable.cloud_back);
        mCloseThumbnailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mThumbContentView.removeView(mThumbnailView);
                mThumbnailView = null;
                if (mThumbnailDialog.isShowing()) ;
                mThumbnailDialog.dismiss();
            }
        });

        mThumbnailTopBar.addView(mCloseThumbnailBtn, BaseBar.TB_Position.Position_LT);
        mThumbnailTopBar.addView(mThumbnailTitle, BaseBar.TB_Position.Position_LT);

        RelativeLayout dialogTitle = (RelativeLayout) mDialogRootView.findViewById(R.id.rd_viewmode_dialog_title);
        dialogTitle.removeAllViews();
        dialogTitle.addView(mThumbnailTopBar.getContentView());
    }

    private void showThumbnailDialog() {
        if (mThumbnailView != null) {
            mThumbContentView.removeView(mThumbnailView);
            mThumbnailView = null;
        }
        initDialogUI();
        mThumbnailView = mPdfView.getThumbnailView();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT, (float) 1.0);
        mThumbContentView.addView(mThumbnailView, lp);
        mThumbnailView.setPageClickedListener(new ThumbListView.IPageClickListener() {
            @Override
            public void onClick(int pageIndex) {
                mThumbContentView.removeView(mThumbnailView);
                mThumbnailView = null;
                mThumbnailDialog.dismiss();
            }
        });
    }
}

