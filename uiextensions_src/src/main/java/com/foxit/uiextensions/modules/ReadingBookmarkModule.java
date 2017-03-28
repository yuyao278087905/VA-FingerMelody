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

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.ReadingBookmark;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class ReadingBookmarkModule implements Module, PanelSpec {
    private static final float RD_PANEL_WIDTH_SCALE_H = 0.338f;
    private static final float RD_PANEL_WIDTH_SCALE_V = 0.535f;

    private boolean mIsReadingBookmark = false;
    protected PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mClearView;
    private UITextEditDialog mDialog;
    private BaseItem mReadingMarkAddItem;
    protected View mContentView;
    private RelativeLayout mReadingMarkContent;

    private ListView mReadingBookmarkListView;
    private TextView mReadingBookmarkNoInfoTv;
    private ReadingBookmarkSupport mReadingBookmarkSupport;

    private boolean isTouchHold;
    protected ArrayList<Boolean> mItemMoreViewShow;
    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;

    public ReadingBookmarkModule(Context context, PDFViewCtrl pdfViewCtrl) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
    }

    public void setPanelHost(PanelHost panelHost) {
        mPanelHost = panelHost;
    }

    public void setPopupWindow(PopupWindow window) {
        mPanelPopupWindow = window;
    }

    public PopupWindow getPopupWindow() {
        return mPanelPopupWindow;
    }

    public void changeMarkItemState(boolean mark) {
        mIsReadingBookmark = mark;
        mReadingMarkAddItem.setSelected(mark);
    }

    public void setReadingBookmarkButton(BaseItem item) {
        mReadingMarkAddItem = item;
        mReadingMarkAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsReadingBookmark = !mIsReadingBookmark;
                if (mIsReadingBookmark) {
                    mReadingBookmarkSupport.addReadingBookmarkNode(mPdfViewCtrl.getCurrentPage(), String.format("Page %d", mPdfViewCtrl.getCurrentPage() + 1));
                } else {
                    mReadingBookmarkSupport.removeReadingBookmarkNode(mPdfViewCtrl.getCurrentPage());
                }
                changeMarkItemState(mIsReadingBookmark);
            }
        });
    }

    private boolean isReadingBookarkPage(int pageIndex) {
        try {
            int nCount = mPdfViewCtrl.getDoc().getReadingBookmarkCount();
            for (int i = 0; i < nCount; i++) {
                ReadingBookmark bookmark = mPdfViewCtrl.getDoc().getReadingBookmark(i);
                if (bookmark.getPageIndex() == pageIndex)
                    return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private final PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (errCode != PDFException.e_errSuccess) {
                return;
            }
            mReadingBookmarkSupport = new ReadingBookmarkSupport(ReadingBookmarkModule.this);
            mReadingBookmarkListView.setAdapter(mReadingBookmarkSupport.getAdapter());
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {

        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {

        }
    };

    private final PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int i) {
            mIsReadingBookmark = isReadingBookarkPage(i);
            mReadingMarkAddItem.setSelected(mIsReadingBookmark);

        }

        @Override
        public void onPageInvisible(int i) {

        }

        @Override
        public void onPageChanged(int i, int i1) {

        }

        @Override
        public void onPageJumped() {

        }
    };

    public void changeViewState(boolean enable) {
        mClearView.setEnabled(enable);
        if (!enable) {
            mReadingBookmarkNoInfoTv.setVisibility(View.VISIBLE);
        } else {
            mReadingBookmarkNoInfoTv.setVisibility(View.GONE);
        }
    }

    public void show() {
        int width = mPdfViewCtrl.getWidth();
        int height = mPdfViewCtrl.getHeight();
        if (mDisplay.isPad()) {
            float scale = RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int) (mDisplay.getScreenWidth() * scale);
        }
        mPanelPopupWindow.setWidth(width);
        mPanelPopupWindow.setHeight(height);
        mPanelPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        // need this, else lock screen back will show input keyboard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mPanelHost.setCurrentSpec(PANELSPEC_TAG_BOOKMARKS);
        mPanelPopupWindow.showAtLocation(mPdfViewCtrl, Gravity.LEFT | Gravity.TOP, 0, 0);
    }

    @Override
    public boolean loadModule() {
        if (mPanelHost == null)
            mPanelHost = new PanelHostImpl(mContext);
        mTopBarView = View.inflate(mContext, R.layout.panel_bookmark_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_bookmark_close);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.panel_bookmark_title);
        mClearView = mTopBarView.findViewById(R.id.panel_bookmark_clear);
        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelPopupWindow.isShowing())
                        mPanelPopupWindow.dismiss();
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_bookmark_rl_top);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mClearView.setLayoutParams(topClearLayoutParams);
        } else {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) topTitle.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            topTitle.setLayoutParams(topTitleLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            mClearView.setLayoutParams(topClearLayoutParams);
        }

        mClearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = mContext;
                mDialog = new UITextEditDialog(context);
                mDialog.setTitle(mContext.getResources().getString(R.string.hm_clear));
                mDialog.getPromptTextView().setText(mContext.getResources().getString(R.string.rd_panel_clear_readingbookmarks));
                mDialog.getInputEditText().setVisibility(View.GONE);
                mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mReadingBookmarkSupport.clearAllNodes();
                        changeViewState(false);
                        changeMarkItemState(false);
                        mDialog.dismiss();
                    }
                });
                mDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });
                mDialog.show();
            }
        });

        mContentView = LayoutInflater.from(mContext).inflate(R.layout.panel_bookmark_main, null);
        mReadingMarkContent = (RelativeLayout) mContentView.findViewById(R.id.panel_bookmark_content_root);

        mReadingBookmarkListView = (ListView) mReadingMarkContent.findViewById(R.id.panel_bookmark_lv);
        mReadingBookmarkNoInfoTv = (TextView) mReadingMarkContent.findViewById(R.id.panel_nobookmark_tv);

        if (mPanelPopupWindow == null) {
            mPanelPopupWindow = new PopupWindow(mPanelHost.getContentView(),
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
            mPanelPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                }
            });
        }

        mReadingBookmarkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AppUtil.isFastDoubleClick()) return;
                ReadingBookmarkSupport.ReadingBookmarkNode bookmarkNode = (ReadingBookmarkSupport.ReadingBookmarkNode) mReadingBookmarkSupport.getAdapter().getItem(position);
                mPdfViewCtrl.gotoPage(bookmarkNode.getIndex());
                if (mPanelPopupWindow.isShowing())
                    mPanelPopupWindow.dismiss();

            }
        });
        mReadingBookmarkListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        boolean show = false;
                        int position = 0;
                        for (int i = 0; i < mReadingBookmarkSupport.getAdapter().getCount(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                position = i;
                                break;
                            }
                        }
                        if (show) {
                            mItemMoreViewShow.set(position, false);
                            mReadingBookmarkSupport.getAdapter().notifyDataSetChanged();
                            isTouchHold = true;
                            return true;
                        }
                    case MotionEvent.ACTION_UP:

                    case MotionEvent.ACTION_CANCEL:
                        if (isTouchHold) {
                            isTouchHold = false;
                            return true;
                        }

                }
                return false;
            }
        });

        mPanelHost.addSpec(this);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_BOOKMARK;
    }

    @Override
    public void onActivated() {
        changeViewState(mReadingBookmarkSupport.getAdapter().getCount() != 0);
    }

    @Override
    public void onDeactivated() {

    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_readingmark_selector;
    }

    @Override
    public int getTag() {
        return PanelSpec.PANELSPEC_TAG_BOOKMARKS;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }
}
