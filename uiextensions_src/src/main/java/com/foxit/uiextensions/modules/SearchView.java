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
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.common.Pause;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFTextSearch;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchView {
    private Context mContext = null;
    private ViewGroup mParent = null;
    private PDFViewCtrl mPdfViewCtrl = null;

    private View mSearchView = null;

    private boolean mIsBlank = true;
    public String mSearch_content;

    public LinearLayout mRd_search_ll_top;
    public EditText mTop_et_content;
    public ImageView mTop_iv_clear;
    private Button mTop_bt_cancel;
    public LinearLayout mTop_ll_shadow;

    public LinearLayout mRd_search_ll_center;
    public View mViewCenterLeft;
    public LinearLayout mViewCenterRight;
    private TextView mCenter_tv_total_number;
    public ListView mCenter_lv_result_list;

    public LinearLayout mRd_search_ll_bottom;
    private ImageView mBottom_iv_prev;
    private ImageView mBottom_iv_next;
    private ImageView mBottom_iv_result;
    public LinearLayout mBottom_ll_shadow;

    protected List<RectF> mRect = new ArrayList<RectF>();
    protected int mPageIndex = -1;//The page index of the search result
    protected boolean mIsCancel = true;
    private DisplayMetrics mMetrics;
    private LayoutInflater mInflater;
    private SearchAdapter mAdapterSearch;
    private AppDisplay mAppDisplay;
    private long mSearchId = 0;

    public SearchView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {

        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mAppDisplay = AppDisplay.getInstance(context);
        mMetrics = context.getResources().getDisplayMetrics();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapterSearch = new SearchAdapter();

        this.mSearchView = LayoutInflater.from(context).inflate(R.layout.search_layout, null, false);
        mSearchView.setVisibility(View.GONE);

        mParent = parent;
        mParent.addView(mSearchView);

        initView();
        bindEvent();
    }

    public void initView() {
        mRd_search_ll_top = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_top);
        mTop_et_content = (EditText) mSearchView.findViewById(R.id.top_et_content);
        mTop_iv_clear = (ImageView) mSearchView.findViewById(R.id.top_iv_clear);
        mTop_bt_cancel = (Button) mSearchView.findViewById(R.id.top_bt_cancel);
        mTop_ll_shadow = (LinearLayout) mSearchView.findViewById(R.id.top_ll_shadow);

        mRd_search_ll_center = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_center);
        mViewCenterLeft = mSearchView.findViewById(R.id.rd_search_center_left);
        mViewCenterRight = (LinearLayout) mSearchView.findViewById(R.id.rd_search_center_right);
        mCenter_tv_total_number = (TextView) mSearchView.findViewById(R.id.center_tv_total_number);
        mCenter_lv_result_list = (ListView) mSearchView.findViewById(R.id.center_lv_result_list);

        mRd_search_ll_bottom = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_bottom);
        mBottom_iv_prev = (ImageView) mSearchView.findViewById(R.id.bottom_iv_prev);
        mBottom_iv_next = (ImageView) mSearchView.findViewById(R.id.bottom_iv_next);
        mBottom_iv_result = (ImageView) mSearchView.findViewById(R.id.bottom_iv_result);
        mBottom_ll_shadow = (LinearLayout) mSearchView.findViewById(R.id.bottom_ll_shadow);

        RelativeLayout.LayoutParams topParams = (RelativeLayout.LayoutParams) mRd_search_ll_top.getLayoutParams();
        RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams) mRd_search_ll_bottom.getLayoutParams();
        if (mAppDisplay.isPad()) {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
        } else {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
        }
        mRd_search_ll_top.setLayoutParams(topParams);
        mRd_search_ll_bottom.setLayoutParams(bottomParams);

        mTop_et_content.setFocusable(true);
        mTop_et_content.requestFocus();

        mRd_search_ll_center.setVisibility(View.VISIBLE);
        mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
        mViewCenterLeft.setVisibility(View.GONE);
        mViewCenterRight.setVisibility(View.GONE);
        mRd_search_ll_bottom.setVisibility(View.GONE);
        mBottom_ll_shadow.setVisibility(View.GONE);

        setSearchResultWidth();
    }

    protected View getView() {
        return  mSearchView;
    }

    public void bindEvent() {
        AppUtil.dismissInputSoft(mTop_et_content);

        mTop_et_content.addTextChangedListener(new myTextWatcher());
        mTop_et_content.setOnKeyListener(mySearchListener);

        mTop_iv_clear.setOnClickListener(searchModelListener);
        mTop_bt_cancel.setOnClickListener(searchModelListener);
        mRd_search_ll_top.setOnClickListener(searchModelListener);

        mViewCenterLeft.setOnClickListener(searchModelListener);
        mViewCenterRight.setOnClickListener(searchModelListener);

        mBottom_iv_result.setOnClickListener(searchModelListener);
        mBottom_iv_prev.setOnClickListener(searchModelListener);
        mBottom_iv_next.setOnClickListener(searchModelListener);
        mBottom_iv_prev.setEnabled(false);
        mBottom_iv_next.setEnabled(false);

        mRd_search_ll_bottom.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mCenter_lv_result_list.setAdapter(mAdapterSearch);
        mCenter_lv_result_list.setOnItemClickListener(mOnItemClickListener);
    }

    protected void setToolbarIcon() {
        mBottom_iv_prev.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_previous));
        mBottom_iv_next.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_next));
        mBottom_iv_prev.setEnabled(true);
        mBottom_iv_next.setEnabled(true);

        if (isFirstSearchResult()) {
            mBottom_iv_prev.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_previous_pressed));
            mBottom_iv_prev.setEnabled(false);
        }

        if (isLastSearchResult()) {
            mBottom_iv_next.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_next_pressed));
            mBottom_iv_next.setEnabled(false);

        }
    }

    protected void setTotalNumber(int count) {
        String totalFind = mContext.getResources().getString(R.string.search_find_number);
        totalFind = String.format(totalFind, count + "");
        mCenter_tv_total_number.setText(totalFind);
    }

    private void setSearchResultWidth() {
        LinearLayout.LayoutParams leftParams = (LinearLayout.LayoutParams) mViewCenterLeft.getLayoutParams();
        LinearLayout.LayoutParams rightParams = (LinearLayout.LayoutParams) mViewCenterRight.getLayoutParams();
        if (mAppDisplay.isPad()) {
            if (mAppDisplay.isLandscape()) {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 2.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 1.0f;

            } else {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 1.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 1.0f;

            }
        } else {
            if (mAppDisplay.isLandscape()) {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 1.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 4.0f;

            } else {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 1.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 4.0f;

            }
        }
        mViewCenterLeft.setLayoutParams(leftParams);
        mViewCenterRight.setLayoutParams(rightParams);
    }

    public void show() {
        if (mSearchView != null) {
            mSearchView.setVisibility(View.VISIBLE);
        }
    }

    public void dismiss() {
        if (mSearchView != null) {
            mSearchView.setVisibility(View.GONE);
        }
    }

    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (isFastDoubleClick()) {
                return;
            }

            OnPreItemClick();

            if (mTagResultList.contains(mShowResultList.get(position))) {
                mCurrentPosition = position + 1;
                setCurrentPageX();
                RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
                RectF canvasRectF = new RectF();
                boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
                int screenWidth = getVisibleWidth().width();
                int screenHeight = getVisibleWidth().height();
                if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
                    int x = (int) (mCurrentPageX - getScreenWidth() / 4);
                    int y = (int) (mCurrentPageY - getScreenHeight() / 4);
                    mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
                }
                mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                mRect = mShowResultList.get(mCurrentPosition).mRects;
                setToolbarIcon();
                mPdfViewCtrl.invalidate();
            } else {
                mCurrentPosition = position;
                setCurrentPageX();
                RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
                RectF canvasRectF = new RectF();
                boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
                int screenWidth = getVisibleWidth().width();
                int screenHeight = getVisibleWidth().height();
                if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
                    int x = (int) (mCurrentPageX - getScreenWidth() / 4);
                    int y = (int) (mCurrentPageY - getScreenHeight() / 4);
                    mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
                }
                mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                mRect = mShowResultList.get(mCurrentPosition).mRects;
                setToolbarIcon();
                mPdfViewCtrl.invalidate();
            }
        }
    };

    protected void OnPreItemClick() {
        AppUtil.dismissInputSoft(mTop_et_content);
        Animation animationL2R = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_hide);
        animationL2R.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mViewCenterRight.setVisibility(View.GONE);
                mViewCenterLeft.setVisibility(View.GONE);
                mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
                mRd_search_ll_bottom.setVisibility(View.VISIBLE);
                mBottom_ll_shadow.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animationL2R.setStartOffset(0);
        mViewCenterRight.startAnimation(animationL2R);
    }

    //search content changed "before" "on" and "after"
    public class myTextWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                mTop_iv_clear.setVisibility(View.VISIBLE);
                mIsBlank = false;
            } else {
                mTop_iv_clear.setVisibility(View.INVISIBLE);
                mIsBlank = true;
            }
        }
    }

    private SearchCancelListener mSearchCancelListener = null;

    public void setSearchCancelListener(SearchCancelListener listener) {
        mSearchCancelListener = listener;
    }

    public interface SearchCancelListener {
        void onSearchCancel();
    }


    private View.OnKeyListener mySearchListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mIsBlank) {
                    mSearch_content = mTop_et_content.getText().toString();
                    AppUtil.dismissInputSoft(mTop_et_content);
                    if (mSearch_content != null && !"".equals(mSearch_content.trim())) {
                        if (mRd_search_ll_bottom.getVisibility() == View.VISIBLE) {
                            mRd_search_ll_bottom.setVisibility(View.GONE);
                            mBottom_ll_shadow.setVisibility(View.GONE);
                        }
                        if (mViewCenterRight.getVisibility() == View.VISIBLE) {
                            mIsCancel = false;
                            searchText(mSearch_content, 0);
                        } else {
                            mRd_search_ll_center.setBackgroundResource(R.color.ux_color_mask_background);
                            mViewCenterLeft.setVisibility(View.VISIBLE);
                            mViewCenterLeft.setClickable(false);
                            mViewCenterRight.setVisibility(View.VISIBLE);
                            Animation animationR2L = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_show);
                            animationR2L.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    mViewCenterLeft.setClickable(true);
                                    mIsCancel = false;
                                    searchText(mSearch_content, 0);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }
                            });
                            animationR2L.setStartOffset(300);
                            mViewCenterRight.startAnimation(animationR2L);
                        }
                    }
                }
                return true;
            }
            return false;
        }
    };

    protected void cancel() {
        mIsCancel = true;
        mIsBlank = true;
        searchCancel();

        mSearchView.setVisibility(View.INVISIBLE);
        cancelSearchText();
        AppUtil.dismissInputSoft(mTop_et_content);
        if (mSearchCancelListener != null)
            mSearchCancelListener.onSearchCancel();
    }

    public void launchSearchView(){
        mIsCancel = false;
        bCancelSearchText = false;
        mRect = null;
        if (mTop_et_content.getText().length() > 0) {
            mTop_et_content.selectAll();
            mTop_iv_clear.setVisibility(View.VISIBLE);
        }

        mTop_et_content.requestFocus();
        mTop_et_content.setFocusable(true);
        AppUtil.showSoftInput(mTop_et_content);
    }

    private View.OnClickListener searchModelListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.top_iv_clear) {
                searchCancel();
            } else if (v.getId() == R.id.top_bt_cancel) {
                cancel();

            } else if (v.getId() == R.id.rd_search_ll_top) {

            } else if (v.getId() == R.id.rd_search_center_right) {

            } else if (v.getId() == R.id.rd_search_center_left) {
                if (isFastDoubleClick()) {
                    return;
                }
                AppUtil.dismissInputSoft(mTop_et_content);
                Animation animationL2R = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_hide);
                animationL2R.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
                        mViewCenterLeft.setVisibility(View.GONE);
                        mViewCenterRight.setVisibility(View.GONE);
                        mRd_search_ll_bottom.setVisibility(View.VISIBLE);
                        mBottom_ll_shadow.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                animationL2R.setStartOffset(0);
                mViewCenterRight.startAnimation(animationL2R);
            } else if (v.getId() == R.id.bottom_iv_result) {
                mRd_search_ll_bottom.setVisibility(View.GONE);
                mBottom_ll_shadow.setVisibility(View.GONE);
                mRd_search_ll_center.setBackgroundResource(R.color.ux_color_mask_background);
                mViewCenterLeft.setVisibility(View.VISIBLE);
                mViewCenterRight.setVisibility(View.VISIBLE);
                Animation animationR2L = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_show);
                animationR2L.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                animationR2L.setStartOffset(0);
                mViewCenterRight.startAnimation(animationR2L);

            } else if (v.getId() == R.id.bottom_iv_prev) {
                searchPre();
            } else if (v.getId() == R.id.bottom_iv_next) {
                searchNext();
            }
        }
    };

    private void searchCancel() {
        mTop_et_content.setText("");
        mTop_iv_clear.setVisibility(View.INVISIBLE);
    }

    private static long sLastTimeMillis;

    public static boolean isFastDoubleClick() {
        long currentTimeMillis = System.currentTimeMillis();
        long delta = currentTimeMillis - sLastTimeMillis;
        if (Math.abs(delta) < 500) {
            return true;
        }
        sLastTimeMillis = currentTimeMillis;
        return false;
    }

    protected int getScreenWidth() {
        return mMetrics.widthPixels;
    }

    protected int getScreenHeight() {
        return mMetrics.heightPixels;
    }

    //search text function
    public interface TaskResult<T1, T2, T3> {
        void onResult(int errCode, T1 p1, T2 p2, T3 p3);
        void setTag(long tag);
        long getTag();
    }

    public static class SearchResult {
        public int mPageIndex;
        public String mSentence;
        public int mPatternStart;
        public ArrayList<RectF> mRects;

        public SearchResult(int pageIndex, String sentence, int patternStart) {
            mPageIndex = pageIndex;
            mSentence = sentence;
            mPatternStart = patternStart;
            mRects = new ArrayList<RectF>();
        }
    }

    class SearchPageTask implements Runnable {
        protected int mPageIndex;
        protected String mPattern; // match text.
        protected int mFlag;
        protected ArrayList<SearchResult> mSearchResults;
        protected PDFViewCtrl mPdfView;
        protected TaskResult mTaskResult;

        public SearchPageTask(PDFViewCtrl pdfView, int pageIndex, String pattern, int flag,
                              TaskResult<Integer, String, ArrayList<SearchResult>> taskResult) {
            mPdfView = pdfView;
            mPageIndex = pageIndex;
            mPattern = pattern;
            mFlag = flag;
            mTaskResult = taskResult;
        }

        @Override
        public void run() {
            if (mSearchResults == null) {
                mSearchResults = new ArrayList<SearchResult>();
            }

            int err = searchPage();
            if (mTaskResult != null) {
                mTaskResult.onResult(err, Integer.valueOf(mPageIndex), mPattern, mSearchResults);
            }
        }

        private int searchPage() {
            int errCode = PDFException.e_errSuccess;
            PDFDoc document = mPdfView.getDoc();
            try {
                Pause pause = new Pause() {
                    @Override
                    public boolean needPauseNow() {
                        return true;
                    }
                };
                PDFTextSearch textSearch = PDFTextSearch.create(document, pause);

                textSearch.setStartPage(mPageIndex);
                textSearch.setKeyWords(mPattern);

                boolean bRet = textSearch.findNext();
                while (bRet) {
                    if (textSearch.getMatchPageIndex() != mPageIndex) {
                        break;
                    }
                    String sentence = textSearch.getMatchSentence();
                    int sentencePos = textSearch.getMatchSentenceStartIndex();
                    SearchResult searchResult = new SearchResult(mPageIndex, sentence, sentencePos);
                    int count = textSearch.getMatchRectCount();
                    for (int i = 0; i < count; i++) {
                        RectF rect = textSearch.getMatchRect(i);// pdf rect
                        searchResult.mRects.add(rect);
                    }

                    mSearchResults.add(searchResult);

                    bRet = textSearch.findNext();
                }
                textSearch.release();
                pause.release();
            } catch (PDFException e) {
                errCode = e.getLastError();
                return errCode;
            }
            return errCode;
        }

    }

    private String mSearchText = null;
    private boolean bCancelSearchText = true;
    private int mCurrentPosition = -1;
    private float mCurrentPageX;
    private float mCurrentPageY;
    private float mCurrentSearchR;
    private float mCurrentSearchB;
    private ArrayList<SearchResult> mTagResultList = new ArrayList<SearchResult>();
    private ArrayList<SearchResult> mValueResultList = new ArrayList<SearchResult>();
    private ArrayList<SearchResult> mShowResultList = new ArrayList<SearchResult>();

    private void searchPage(int pageIndex, String pattern, int flag,
                            final TaskResult<Integer, String, ArrayList<SearchResult>> result) {
        SearchPageTask searchPageTask = new SearchPageTask(mPdfViewCtrl, pageIndex, pattern, flag, result);
        Handler handler = new Handler();
        handler.post(searchPageTask);
    }

    private void _searchText(final String pattern, final int flag, final int pageIndex) {
        this.mSearchText = pattern.trim();
        TaskResult<Integer, String, ArrayList<SearchResult>> taskResult;
        searchPage(pageIndex, pattern.trim(), flag, taskResult = new TaskResult<Integer, String, ArrayList<SearchResult>>() {
            private long mTaskId;
            @Override
            public void onResult(int errCode, Integer p1, String p2, ArrayList<SearchResult> p3) {
                if (errCode == PDFException.e_errOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                    return;
                }

                if (this.mTaskId != mSearchId) {
                    return;
                }

                if (p3 == null) {
                    return;
                }

                if (p3.size() > 0) {
                    SearchResult searchResult = new SearchResult(p1, "tag", p3.size());
                    mTagResultList.add(searchResult);
                    mShowResultList.add(searchResult);

                }
                mValueResultList.addAll(p3);
                mShowResultList.addAll(p3);
                if (p3.size() > 0) {
                    notifyDataSetChangedSearchAdapter();
                }
                setTotalNumber(mValueResultList.size());
                if (pageIndex == mPdfViewCtrl.getPageCount() - 1) {
                    if (mCurrentPosition == -1 && mShowResultList.size() > 0) {
                        mCurrentPosition = mShowResultList.size() - 1;
                        if (mCurrentPosition != -1) {
                            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                            mRect = mShowResultList.get(mCurrentPosition).mRects;
                            setToolbarIcon();
                            mPdfViewCtrl.invalidate();
                        }
                    }

                    return;
                }
                if (p1 >= mPdfViewCtrl.getCurrentPage() && mCurrentPosition == -1 && p3.size() > 0) {
                    mCurrentPosition = mShowResultList.size() - p3.size();
                    mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                    mRect = mShowResultList.get(mCurrentPosition).mRects;
                    setToolbarIcon();
                    mPdfViewCtrl.invalidate();
                }
                setToolbarIcon();
                if (bCancelSearchText) {
                    bCancelSearchText = false;
                } else {
                    _searchText(pattern, flag, pageIndex + 1);
                }
            }

            @Override
            public void setTag(long taskId) {
                mTaskId = taskId;
            }

            @Override
            public long getTag() {
                return mTaskId;
            }
        });
        taskResult.setTag(mSearchId);
    }

    public void searchText(String pattern, int flag) {
        cancelSearchText();
        clearSearchResult();
        mCurrentPosition = -1;
        mSearchId++;
        mRect = null;
        mIsCancel = false;
        mSearchText = null;
        synchronized (this) {
            bCancelSearchText = false;
        }

        _searchText(pattern, flag, 0);
    }

    public void cancelSearchText() {
        synchronized (this) {
            if (!bCancelSearchText) {
                bCancelSearchText = true;

                // do cancel search text
                onCancelSearchText();
            }
        }
    }

    private void notifyDataSetChangedSearchAdapter() {
        if (mAdapterSearch != null) {
            mAdapterSearch.notifyDataSetChanged();
        }
    }

    private void clearSearchResult() {
        if (mShowResultList != null || mTagResultList != null || mValueResultList != null) {
            mTagResultList.clear();
            mValueResultList.clear();
            mShowResultList.clear();
        }
        notifyDataSetChangedSearchAdapter();
    }

    private void onCancelSearchText() {
        mRd_search_ll_bottom.setVisibility(View.GONE);
        mPdfViewCtrl.invalidate();
    }

    private Rect getVisibleWidth() {
        Rect rect = new Rect();
        mPdfViewCtrl.getGlobalVisibleRect(rect);
        return rect;
    }

    public void searchPre() {
        if (mSearchText == null || bCancelSearchText) {
            return;
        }

        if (mCurrentPosition <= 1) {
            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
            mRect = mShowResultList.get(mCurrentPosition).mRects;
            setToolbarIcon();
            mPdfViewCtrl.invalidate();
            return;
        }
        mCurrentPosition--;
        if (mShowResultList.get(mCurrentPosition).mSentence.endsWith("tag")) {
            mCurrentPosition--;
        }
        setCurrentPageX();
        RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
        RectF canvasRectF = new RectF();
        boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
        int screenWidth = getVisibleWidth().width();
        int screenHeight = getVisibleWidth().height();
        if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
            int x = (int) (mCurrentPageX - getScreenWidth() / 4);
            int y = (int) (mCurrentPageY - getScreenHeight() / 4);
            mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
        }
        mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
        mRect = mShowResultList.get(mCurrentPosition).mRects;
        setToolbarIcon();
        mPdfViewCtrl.invalidate();
    }

    public void searchNext() {
        if (mSearchText == null || bCancelSearchText) {
            return;
        }

        if (mCurrentPosition >= mShowResultList.size() - 1) {
            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
            mRect = mShowResultList.get(mCurrentPosition).mRects;
            setToolbarIcon();
            mPdfViewCtrl.invalidate();
            return;
        }
        mCurrentPosition++;
        if (mShowResultList.get(mCurrentPosition).mSentence.endsWith("tag")) {
            mCurrentPosition++;
        }
        setCurrentPageX();
        RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
        RectF canvasRectF = new RectF();
        boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
        int screenWidth = getVisibleWidth().width();
        int screenHeight = getVisibleWidth().height();
        if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
            int x = (int) (mCurrentPageX - getScreenWidth() / 4);
            int y = (int) (mCurrentPageY - getScreenHeight() / 4);
            mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
        }
        mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
        mRect = mShowResultList.get(mCurrentPosition).mRects;
        setToolbarIcon();
        mPdfViewCtrl.invalidate();
    }

    public boolean isFirstSearchResult() {
        return mCurrentPosition <= 1;
    }

    public boolean isLastSearchResult() {
        return mCurrentPosition < 1 || mCurrentPosition >= mShowResultList.size() - 1;
    }

    private void setCurrentPageX() {
        float x = 0, y = 0, r = 0, b = 0;
        for (int i = 0; i < mShowResultList.get(mCurrentPosition).mRects.size(); i++) {
            RectF pageRect = new RectF(mShowResultList.get(mCurrentPosition).mRects.get(i));
            RectF pageViewRect = new RectF();
            if (mPdfViewCtrl.convertPdfRectToPageViewRect(pageRect, pageViewRect, mShowResultList.get(mCurrentPosition).mPageIndex)) {
                    if (i == 0) {
                        x = pageViewRect.left;
                        y = pageViewRect.top;
                        r = pageViewRect.right;
                        b = pageViewRect.bottom;
                    } else {
                        if (pageViewRect.left < x) {
                            x = pageViewRect.left;
                        }
                        if (pageViewRect.top < y) {
                            y = pageViewRect.top;
                        }
                        if (pageViewRect.right > r) {
                            r = pageViewRect.right;
                        }
                        if (pageViewRect.bottom > b) {
                            b = pageViewRect.bottom;
                        }
                    }
            }
        }
        mCurrentPageX = x;
        mCurrentPageY = y;
        mCurrentSearchR = r;
        mCurrentSearchB = b;
    }

    //Search Adapter
    class SearchAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mShowResultList == null ? 0 : mShowResultList.size();
        }

        @Override
        public Object getItem(int position) {
            return mShowResultList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout container = new LinearLayout(mContext);
                SearchItemTag mItemTag = null;
                SearchItemView mItemView = null;
                if (mTagResultList.contains(mShowResultList.get(position))) {
                    View viewTag;
                    viewTag = mInflater.inflate(R.layout.search_item_tag, null);

                    mItemTag = new SearchItemTag();
                    mItemTag.search_pageIndex = (TextView) viewTag.findViewById(R.id.search_page_tv);
                    mItemTag.search_pageCount = (TextView) viewTag.findViewById(R.id.search_curpage_count);
                    String pageNumber = mContext.getResources().getString(R.string.search_page_number);
                    pageNumber = String.format(pageNumber, mShowResultList.get(position).mPageIndex + 1 + "");
                    mItemTag.search_pageIndex.setText(pageNumber);
                    mItemTag.search_pageCount.setText(mShowResultList.get(position).mPatternStart + "");
                    container.addView(viewTag, params);
                } else {
                    mItemView = new SearchItemView();
                    View viewContent = null;
                    viewContent = mInflater.inflate(R.layout.search_item_content, null);

                    mItemView.search_content = (TextView) viewContent.findViewById(R.id.search_content_tv);
                    String mContent = mShowResultList.get(position).mSentence;
                    SpannableString searchContent = new SpannableString(mContent);

                    try {
                        Pattern pattern = Pattern.compile(mSearchText, Pattern.CASE_INSENSITIVE);//ignore the case
                        Matcher matcher = pattern.matcher(searchContent);
                        while (matcher.find()) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + mSearchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);
                        container.addView(viewContent, params);
                    } catch (PatternSyntaxException e) {

                    }
                }
                return container;
            } else {
                LinearLayout container = (LinearLayout) convertView;
                container.removeAllViews();
                SearchItemTag mItemTag = null;
                SearchItemView mItemView = null;
                if (mTagResultList.contains(mShowResultList.get(position))) {
                    View viewTag = null;
                    viewTag = mInflater.inflate(R.layout.search_item_tag, null);

                    mItemTag = new SearchItemTag();
                    mItemTag.search_pageIndex = (TextView) viewTag.findViewById(R.id.search_page_tv);
                    mItemTag.search_pageCount = (TextView) viewTag.findViewById(R.id.search_curpage_count);
                    String pageNumber = mContext.getResources().getString(R.string.search_page_number);
                    pageNumber = String.format(pageNumber, mShowResultList.get(position).mPageIndex + 1 + "");
                    mItemTag.search_pageIndex.setText(pageNumber);
                    mItemTag.search_pageCount.setText(mShowResultList.get(position).mPatternStart + "");
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    container.addView(viewTag, lp);
                } else {
                    mItemView = new SearchItemView();
                    View viewContent = null;
                    viewContent = mInflater.inflate(R.layout.search_item_content, null);

                    mItemView.search_content = (TextView) viewContent.findViewById(R.id.search_content_tv);
                    String mContent = mShowResultList.get(position).mSentence;
                    SpannableString searchContent = new SpannableString(mContent);
                    try {
                        Pattern pattern = Pattern.compile(mSearchText, Pattern.CASE_INSENSITIVE);//ignore the case
                        Matcher matcher = pattern.matcher(searchContent);
                        while (matcher.find()) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + mSearchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);

                        container.addView(viewContent);
                    } catch (PatternSyntaxException e) {

                    }

                }
                return container;
            }
        }
    }

    private class SearchItemTag {
        public TextView search_pageIndex;
        public TextView search_pageCount;
    }

    private class SearchItemView {
        public TextView search_content;
    }

    public void onDocumentClosed() {
        mTop_et_content.setText("");
        clearSearchResult();
        mCenter_tv_total_number.setText("");
    }

}
