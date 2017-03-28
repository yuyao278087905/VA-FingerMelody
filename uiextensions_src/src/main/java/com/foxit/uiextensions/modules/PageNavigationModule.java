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
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.textselect.TextSelectModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.ToolUtil;


public class PageNavigationModule implements Module {
    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private AppDisplay mDisplay;

    private InputMethodManager mInputMethodMgr = null;

    private boolean mIsClosedState = true;
    private RelativeLayout mClosedRootLayout;
    private LinearLayout mClosedPageLabel;
    private TextView mClosedPageLabel_Total;
    private TextView mClosedPageLabel_Current;
    private ImageView mPreImageView;
    private ImageView mNextImageView;
    private RelativeLayout mOpenedRootLayout;
    private EditText mOpenedPageIndex;
    private ImageView mOpenedClearBtn;
    private TextView mOpenedGoBtn;
    private MyHandler mHandler;
    private MyRunnable mRunnable;
    private OpenJumpPageBackground mOpenJumpPageBackground;

    public PageNavigationModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mDisplay = new AppDisplay(mContext);
    }

    @Override
    public String getName() {
        return MODULE_NAME_PAGENAV;
    }

    @Override
    public boolean loadModule() {
        mInputMethodMgr = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler = new MyHandler();
        mRunnable = new MyRunnable();
        initClosedUI();
        initOpenedUI();
        mPdfViewCtrl.registerDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        mPdfViewCtrl.setOnTouchListener(mOnTouchListener);
        mPdfViewCtrl.setOnKeyListener(mOnKeyKListener);
        Thread thread = new Thread(mRunnable);
        thread.start();
        onUIStatusChanged();
        return true;
    }

    @Override
    public boolean unloadModule() {
        disInitClosedUI();
        disInitOpenedUI();
        mPdfViewCtrl.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        return true;
    }


    private void initClosedUI() {
        mClosedRootLayout = (RelativeLayout) View.inflate(mContext, R.layout.rd_gotopage_close, null);
        mClosedPageLabel = (LinearLayout) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber);
        mClosedPageLabel_Total = (TextView) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber_total);
        mClosedPageLabel_Current = (TextView) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber_current);
        mClosedPageLabel_Current.setText("");
        mClosedPageLabel_Current.setTextColor(Color.WHITE);
        mClosedPageLabel_Total.setText("-");
        mClosedPageLabel_Total.setTextColor(Color.WHITE);
        mClosedPageLabel.setEnabled(false);

        mPreImageView = (ImageView) mClosedRootLayout.findViewById(R.id.rd_jumppage_previous);
        mNextImageView = (ImageView) mClosedRootLayout.findViewById(R.id.rd_jumppage_next);

        setClosedUIClickListener();
        RelativeLayout.LayoutParams closedLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        closedLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mParent.addView(mClosedRootLayout, closedLP);
        if (mDisplay.isPad()) {
            mClosedRootLayout.setPadding((int) (AppResource.getDimension(mContext, R.dimen.ux_horz_left_margin_pad) + mDisplay.dp2px(4)), 0, 0, (int) (AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16)));
        }
        mPreImageView.setVisibility(View.GONE);
        mNextImageView.setVisibility(View.GONE);
    }

    private void initOpenedUI() {
        mOpenedRootLayout = (RelativeLayout) View.inflate(mContext, R.layout.rd_gotopage_open, null);
        mOpenedPageIndex = (EditText) mOpenedRootLayout.findViewById(R.id.rd_gotopage_index_et);
        mOpenedClearBtn = (ImageView) mOpenedRootLayout.findViewById(R.id.rd_gotopage_edit_clear);
        mOpenedGoBtn = (TextView) mOpenedRootLayout.findViewById(R.id.rd_gotopage_togo_iv);

        mOpenJumpPageBackground = new OpenJumpPageBackground(mContext);
        mOpenedClearBtn.setVisibility(View.INVISIBLE);
        mOpenedRootLayout.setVisibility(View.GONE);
        mOpenJumpPageBackground.setVisibility(View.GONE);
        setOpenedClickListener();
    }

    private void addOpenedLayoutToMainFrame() {
        try {
            mOpenedRootLayout.setVisibility(View.VISIBLE);
            mOpenJumpPageBackground.setVisibility(View.VISIBLE);
            mParent.addView(mOpenedRootLayout);
            if (mDisplay.isPad()) {
                mOpenedRootLayout.getLayoutParams().height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_pad);
            } else {
                mOpenedRootLayout.getLayoutParams().height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_phone);
            }
            RelativeLayout.LayoutParams openJumpPageBackgroundLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            openJumpPageBackgroundLP.addRule(RelativeLayout.BELOW, R.id.rd_gotopage_open_root_layout);
            mParent.addView(mOpenJumpPageBackground, openJumpPageBackgroundLP);
        } catch (Exception ignored) {
        }
    }

    private void removeOpenedLayoutFromMainFrame() {
        mParent.removeView(mOpenedRootLayout);
        mParent.removeView(mOpenJumpPageBackground);
    }

    class OpenJumpPageBackground extends RelativeLayout {
        public OpenJumpPageBackground(Context context) {
            super(context);
        }
    }

    private void setOpenedClickListener() {
        mOpenedGoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                onGotoPage();
            }
        });
        mOpenedClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (mOpenedPageIndex != null) {
                    mOpenedPageIndex.setText("");
                }
            }
        });
        mOpenedPageIndex.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                    InputMethodManager inputManager = (InputMethodManager)
                            mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
                    onGotoPage();
                    return true;
                }
                return false;
            }
        });
        mOpenedPageIndex.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOpenedPageIndex == v) {
                    if (!hasFocus) {
                        InputMethodManager inputManager = (InputMethodManager)
                                mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);

                        mIsClosedState = true;
                        onUIStatusChanged();
                    }
                }
            }
        });
        mOpenedPageIndex.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (mOpenedPageIndex.getText() != null) {
                            if (mOpenedPageIndex.getText().length() != 0) {
                                mOpenedClearBtn.setVisibility(View.VISIBLE);
                                Integer number = null;
                                if (!mOpenedPageIndex.getText().toString().trim().equals("")) {
                                    int index = mOpenedPageIndex.getText().toString().indexOf("/");
                                    try {
                                        if (index == -1) {
                                            number = Integer.valueOf(mOpenedPageIndex.getText().toString());
                                        } else {
                                            number = Integer.valueOf(mOpenedPageIndex.getText().subSequence(0, index).toString());
                                        }
                                    } catch (Exception e) {
                                        number = null;
                                    }
                                }
                                if(number == null || 0 > number || number > mPdfViewCtrl.getPageCount()){
                                    mOpenedPageIndex.setText(mOpenedPageIndex.getText().toString().substring(0, mOpenedPageIndex.getText().length() - 1));
                                    mOpenedPageIndex.selectAll();
                                    Toast toast = new Toast(mContext);
                                    int i = mPdfViewCtrl.getPageCount();
                                    String str = AppResource.getString(mContext, R.string.rv_gotopage_error_toast)
                                            + " " + "(1-" + String.valueOf(i) + ")";
                                    LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                    View toastLayout = inflate.inflate(R.layout.rd_gotopage_tips, null);
                                    TextView tv = (TextView) toastLayout.findViewById(R.id.rd_gotopage_toast_tv);
                                    tv.setText(str);
                                    toast.setView(toastLayout);
                                    toast.setDuration(Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                            } else {
                                mOpenedClearBtn.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            mOpenedClearBtn.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }

        );
        mOpenedRootLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                }

        );
        mOpenJumpPageBackground.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsClosedState) {
                            mIsClosedState = true;
                            InputMethodManager inputManager = (InputMethodManager)
                                    mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
                            onUIStatusChanged();
                        }
                    }
                }

        );
    }

    private void disInitClosedUI() {
        mParent.removeView(mClosedRootLayout);
    }

    private void disInitOpenedUI() {
        AppKeyboardUtil.removeKeyboardListener(mOpenedRootLayout);
        mParent.removeView(mOpenedRootLayout);
    }

    private void triggerDismissMenu() {
        TextSelectModule module = (TextSelectModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(MODULE_NAME_SELECTION);
        if (module != null) {
            module.triggerDismissMenu();
        }

        if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }
    }

    private void setClosedUIClickListener() {
        mClosedPageLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                triggerDismissMenu();
                mIsClosedState = false;
                int pageIndex = mPdfViewCtrl.getCurrentPage();
                mPageEventListener.onPageChanged(pageIndex, pageIndex);

                addOpenedLayoutToMainFrame();
                mOpenedPageIndex.selectAll();
                mOpenedPageIndex.requestFocus();
                mInputMethodMgr.showSoftInput(mOpenedPageIndex, 0);
                mOpenedPageIndex.setText("");

                onUIStatusChanged();
            }
        });

        mPreImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                mPdfViewCtrl.gotoPrevView();
                triggerDismissMenu();
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        });
        mNextImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                mPdfViewCtrl.gotoNextView();
                triggerDismissMenu();
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        });

        mClosedRootLayout.findViewById(R.id.rv_gotopage_relativeLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    //The timer
    private static final int SHOW_OVER = 100;
    private static final int SHOW_RESET = 200;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_RESET:
                    mRunnable.reset();
                    break;
                case SHOW_OVER:
                    if (mPreImageView.getVisibility() == View.VISIBLE) {
                        mPreImageView.setVisibility(View.GONE);
                    }
                    if (mNextImageView.getVisibility() == View.VISIBLE) {
                        mNextImageView.setVisibility(View.GONE);
                    }
                    if (mClosedRootLayout.getVisibility() == View.VISIBLE) {
                        mClosedRootLayout.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    }

    class MyRunnable implements Runnable {
        private int mCurTime;
        private boolean mStart;

        public void reset() {
            mCurTime = 0;
            mStart = true;
        }

        @Override
        public void run() {
            while (true) {
                if (mStart) {
                    for (mCurTime = 0; mCurTime < 3000; mCurTime += 100) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mStart) {
                        Message msg = new Message();
                        msg.what = SHOW_OVER;
                        mHandler.sendMessage(msg);
                    }
                    mStart = false;

                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void onUIStatusChanged() {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        if (mIsClosedState
                || uiExtensionsManager.getCurrentToolHandler() != null
                || ToolUtil.getCurrentAnnotHandler(uiExtensionsManager) != null
                || !mOpenedPageIndex.hasWindowFocus()
                ) {
            if (!mIsClosedState) {
                mIsClosedState = true;
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedPageLabel.setEnabled(true);
                mClosedRootLayout.setVisibility(View.VISIBLE);
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
            if (mOpenedRootLayout.getVisibility() != View.GONE) {
                mOpenedRootLayout.setVisibility(View.GONE);
                mOpenJumpPageBackground.setVisibility(View.GONE);
                removeOpenedLayoutFromMainFrame();
            }
        } else {
            if (mClosedRootLayout.getVisibility() != View.GONE) {
                endShow();
                mClosedRootLayout.setVisibility(View.GONE);
            }
            if (mOpenedRootLayout.getVisibility() != View.VISIBLE) {
                mOpenedRootLayout.setVisibility(View.VISIBLE);
                mOpenJumpPageBackground.setVisibility(View.VISIBLE);
                addOpenedLayoutToMainFrame();
            }
        }

    }

    private void onGotoPage() {
        Toast toast = new Toast(mContext);
        Editable text = mOpenedPageIndex.getText();
        Integer number = null;
        if (!text.toString().trim().equals("")) {
            int index = text.toString().indexOf("/");
            try {
                if (index == -1) {//no '/'
                    number = Integer.valueOf(text.toString());
                } else {
                    number = Integer.valueOf(text.subSequence(0, index).toString());
                }
            } catch (Exception e) {
                number = null;
            }
        }

        if (number != null && 0 < number && number <= mPdfViewCtrl.getPageCount()) {
            mPdfViewCtrl.gotoPage(number - 1, 0, 0);
            mIsClosedState = true;
            mInputMethodMgr.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
            mIsClosedState = true;
            onUIStatusChanged();
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
        } else {
            int i = mPdfViewCtrl.getPageCount();
            String str = AppResource.getString(mContext, R.string.rv_gotopage_error_toast)
                    + " " + "(1-" + String.valueOf(i) + ")";
            LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View toastLayout = inflate.inflate(R.layout.rd_gotopage_tips, null);
            TextView tv = (TextView) toastLayout.findViewById(R.id.rd_gotopage_toast_tv);
            tv.setText(str);
            toast.setView(toastLayout);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            mOpenedPageIndex.selectAll();
        }
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int i) {
        }

        @Override
        public void onPageInvisible(int i) {
        }

        @Override
        public void onPageChanged(int old, int current) {
            mClosedPageLabel.setEnabled(true);
            String str = (current + 1) + "/" + mPdfViewCtrl.getPageCount();
            mClosedPageLabel_Current.setText("" + (current + 1));
            mClosedPageLabel_Total.setText("/" + mPdfViewCtrl.getPageCount());
            if (!mIsClosedState) {
                mOpenedPageIndex.setHint(str);
            }
            //reset jumpView
            if (mPdfViewCtrl.hasPrevView()) {
                mPreImageView.setVisibility(View.VISIBLE);
            } else {
                mPreImageView.setVisibility(View.GONE);
            }
            if (mPdfViewCtrl.hasNextView()) {
                mNextImageView.setVisibility(View.VISIBLE);
            } else {
                mNextImageView.setVisibility(View.GONE);
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            if (old != current) {
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onPageJumped() {
            if (mPdfViewCtrl.hasPrevView()) {
                mPreImageView.setVisibility(View.VISIBLE);
            } else {
                mPreImageView.setVisibility(View.GONE);
            }
            if (mPdfViewCtrl.hasNextView()) {
                mNextImageView.setVisibility(View.VISIBLE);
            } else {
                mNextImageView.setVisibility(View.GONE);
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
        }
    };


    public void resetJumpView() {
        if (mPdfViewCtrl.hasPrevView()) {
            mPreImageView.setVisibility(View.VISIBLE);
        } else {
            mPreImageView.setVisibility(View.GONE);
        }
        if (mPdfViewCtrl.hasNextView()) {
            mNextImageView.setVisibility(View.VISIBLE);
        } else {
            mNextImageView.setVisibility(View.GONE);
        }
        if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
            mClosedRootLayout.setVisibility(View.VISIBLE);
        }
        Message msg = new Message();
        msg.what = SHOW_RESET;
        mHandler.sendMessage(msg);
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            onUIStatusChanged();
            return false;
        }
    };

    private void startShow() {
        mClosedRootLayout.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_visible_show));
    }

    private void endShow() {
        mClosedRootLayout.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_visible_hide));
    }

    private View.OnKeyListener mOnKeyKListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if (!mIsClosedState) {
                    mIsClosedState = true;
                    onUIStatusChanged();
                    return true;
                }
            }
            return false;
        }
    };


    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (pdfDoc == null || errCode != PDFException.e_errSuccess) {
                return;
            }
            mPageEventListener.onPageChanged(mPdfViewCtrl.getCurrentPage(), mPdfViewCtrl.getCurrentPage());
            mIsClosedState = true;
            onUIStatusChanged();
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
}
