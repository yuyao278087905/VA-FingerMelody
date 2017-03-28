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
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.ReadingBookmark;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ReadingBookmarkSupport {

    interface IReadingBookmarkListener {
        void onMoreClick(int position);

        void onRename(int position);

        void onDelete(int position);
    }

    private final ReadingBookmarkModule mReadingBookmarkModule;
    private final ReadingBookmarkAdapter mAdapter;

    public ReadingBookmarkSupport(ReadingBookmarkModule panelModule) {
        mReadingBookmarkModule = panelModule;
        mAdapter = new ReadingBookmarkAdapter();
        mAdapter.initBookmarkList();
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void clearAllNodes() {
        mAdapter.clearAllNodes();
    }

    public void addReadingBookmarkNode(int index, String title) {
        mAdapter.addBookmarkNode(index, title);
    }

    public void removeReadingBookmarkNode(int index) {
        mAdapter.removeBookmarkNode(index);
    }


    public class ReadingBookmarkNode {
        private String mTitle;
        private final int mIndex;
        private DateTime mDateTime;

        public ReadingBookmarkNode(int index, String title, DateTime dateTime) {
            mTitle = title;
            mIndex = index;
            mDateTime = dateTime;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void setDateTime(DateTime dateTime){
            mDateTime = dateTime;
        }

        public String getTitle() {
            return mTitle;
        }

        public int getIndex() {
            return mIndex;
        }

        public DateTime getDateTime(){
            return mDateTime;
        }
    }

    private class ReadingBookmarkAdapter extends BaseAdapter {

        private final Context mContext;
        private final PDFViewCtrl mPdfViewCtrl;

        private final ArrayList<ReadingBookmarkNode> mNodeList;
        private PDFDoc mPdfDoc;
        private final ArrayList<Boolean> mItemMoreViewShow;
        private final IReadingBookmarkListener mBookmarkListener;

        class RKViewHolder {
            public TextView mRMContent;
            public TextView mRMCreateTime;

            public ImageView mRMMore;
            public LinearLayout mRMMoreView;

            public LinearLayout mLlRename;
            public ImageView mRMRename;
            public TextView mRMTvRename;

            public LinearLayout mLlDelete;
            public ImageView mRMDelete;
            public TextView mRMTvDelete;
        }

        public ReadingBookmarkAdapter() {
            mContext = mReadingBookmarkModule.mContentView.getContext();
            mPdfViewCtrl = mReadingBookmarkModule.mPdfViewCtrl;
            mNodeList = new ArrayList<ReadingBookmarkNode>();
            mItemMoreViewShow = mReadingBookmarkModule.mItemMoreViewShow;
            mBookmarkListener = new IReadingBookmarkListener() {
                @Override
                public void onMoreClick(int position) {
                    for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                        if (i == position) {

                            mItemMoreViewShow.set(i, true);
                        } else {
                            mItemMoreViewShow.set(i, false);
                        }
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onRename(final int position) {
                    if (!AppUtil.isFastDoubleClick()) {
                        final UITextEditDialog renameDlg = new UITextEditDialog(mContext);
                        renameDlg.getPromptTextView().setVisibility(View.GONE);
                        renameDlg.setTitle(mContext.getResources().getString(R.string.fx_string_reanme));
                        renameDlg.getDialog().setCanceledOnTouchOutside(false);
                        final InputMethodManager mInputManager;
                        final EditText renameDlgEt = renameDlg.getInputEditText();
                        final Button renameDlgOk = renameDlg.getOKButton();
                        final Button renameDlgCancel = renameDlg.getCancelButton();

                        renameDlgEt.setTextSize(17.3f);
                        renameDlgEt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
                        renameDlgEt.setTextColor(Color.BLACK);
                        mInputManager = (InputMethodManager) renameDlgEt.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        ReadingBookmarkNode node = mNodeList.get(position);
                        renameDlgEt.setText(node.getTitle());
                        renameDlgEt.selectAll();
                        renameDlgOk.setEnabled(false);
                        renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                        renameDlgEt.addTextChangedListener(new TextWatcher() {

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                if (renameDlgEt.getText().toString().trim().length() > 199) {
                                    final Toast toast = Toast.makeText(mContext, R.string.rv_panel_readingbookmark_tips_limited, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    final Timer timer = new Timer();
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            toast.show();
                                        }
                                    }, 0, 3000);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            toast.cancel();
                                            timer.cancel();
                                        }
                                    }, 5000 );

                                } else if (renameDlgEt.getText().toString().trim().length() == 0) {
                                    renameDlgOk.setEnabled(false);
                                    renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

                                } else {
                                    renameDlgOk.setEnabled(true);
                                    renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));

                                }
                            }

                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                for (int i = s.length(); i > 0; i--) {
                                    if (s.subSequence(i - 1, i).toString().equals("\n"))
                                        s.replace(i - 1, i, "");
                                }
                            }
                        });

                        renameDlgEt.setOnKeyListener(new View.OnKeyListener() {

                            @Override
                            public boolean onKey(View v, int keyCode, KeyEvent event) {
                                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                                    mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                    return true;
                                }
                                return false;
                            }
                        });

                        renameDlgOk.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                String newContent = renameDlgEt.getText().toString().trim();
                                updateBookmarkNode(position, newContent, AppDmUtil.currentDateToDocumentDate());
                                notifyDataSetChanged();
                                mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                renameDlg.dismiss();
                            }
                        });

                        renameDlgCancel.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                renameDlg.dismiss();
                            }
                        });
                        renameDlg.show();
                        renameDlgEt.setFocusable(true);
                        renameDlgEt.setFocusableInTouchMode(true);
                        renameDlgEt.requestFocus();
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                mInputManager.showSoftInput(renameDlgEt, 0);
                            }
                        }, 500);
                    }
                }

                @Override
                public void onDelete(int position) {
                    ReadingBookmarkNode node = mNodeList.get(position);
                    mAdapter.removeBookmarkNode(node.getIndex());
                    if (node.getIndex() == mPdfViewCtrl.getCurrentPage()) {
                        mReadingBookmarkModule.changeMarkItemState(false);
                    }
                    mReadingBookmarkModule.changeViewState(mNodeList.size() != 0);
                }
            };
        }

        public void initBookmarkList() {
            try {
                mPdfDoc = mPdfViewCtrl.getDoc();
                if (mPdfDoc == null) {
                    return;
                }
                mNodeList.clear();
                int nCount = mPdfDoc.getReadingBookmarkCount();
                for (int i = 0; i < nCount; i++) {
                    ReadingBookmark readingBookmark = mPdfDoc.getReadingBookmark(i);
                    if (readingBookmark == null)
                        continue;
                    DateTime dateTime = readingBookmark.getDateTime(false);
                    if(dateTime == null)
                        dateTime = readingBookmark.getDateTime(true);
                    mNodeList.add(new ReadingBookmarkNode(readingBookmark.getPageIndex(), readingBookmark.getTitle(),dateTime));
                    mItemMoreViewShow.add(false);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public void addBookmarkNode(int pageIndex, String title) {
            try {
                ReadingBookmark readingBookmark = mPdfDoc.insertReadingBookmark(0, title, pageIndex);
                DateTime dateTime = AppDmUtil.currentDateToDocumentDate();
                readingBookmark.setDateTime(dateTime,true);
                readingBookmark.setDateTime(dateTime,false);
                mNodeList.add(0,new ReadingBookmarkNode(pageIndex, title,dateTime));
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mItemMoreViewShow.add(0,false);
            notifyDataSetChanged();
        }

        public void removeBookmarkNode(int pageIndex) {
            for(int position = 0 ;position < mNodeList.size();position++){
                if(mNodeList.get(position).getIndex() == pageIndex){
                    mNodeList.remove(position);
                    mItemMoreViewShow.remove(position);
                    break;
                }
            }
            try {
                int nCount = mPdfDoc.getReadingBookmarkCount();
                for (int i = 0; i < nCount; i++) {
                    ReadingBookmark readingMark = mPdfDoc.getReadingBookmark(i);
                    if (readingMark.getPageIndex() == pageIndex) {
                        mPdfDoc.removeReadingBookmark(readingMark);
                        break;
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            notifyDataSetChanged();
        }

        public void updateBookmarkNode(int position, String title, DateTime dateTime) {
            int pageIndex = mNodeList.get(position).getIndex();
            mNodeList.get(position).setTitle(title);
            mNodeList.get(position).setDateTime(dateTime);
            try {
                int nCount = mPdfDoc.getReadingBookmarkCount();
                for (int i = 0; i < nCount; i++) {
                    ReadingBookmark readingBookmark = mPdfDoc.getReadingBookmark(i);
                    if (readingBookmark.getPageIndex() == pageIndex) {
                        readingBookmark.setTitle(title);
                        readingBookmark.setDateTime(dateTime,false);
                        break;
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public void clearAllNodes() {
            try {
                ArrayList<ReadingBookmark> mTmpReadingBookmark = new ArrayList<ReadingBookmark>();
                for (int i = 0; i < mPdfDoc.getReadingBookmarkCount(); i++) {
                    mTmpReadingBookmark.add(mPdfDoc.getReadingBookmark(i));
                }

                for (ReadingBookmark readingBookmark : mTmpReadingBookmark) {
                    mPdfDoc.removeReadingBookmark(readingBookmark);
                }
                mTmpReadingBookmark.clear();
                mNodeList.clear();
                notifyDataSetChanged();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCount() {
            return mNodeList.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            RKViewHolder bkHolder;
            if (null == convertView) {
                bkHolder = new RKViewHolder();
                convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.rd_readingmark_item, null);

                bkHolder.mRMContent = (TextView) convertView.findViewById(R.id.rd_bookmark_item_content);
                bkHolder.mRMCreateTime = (TextView) convertView.findViewById(R.id.rd_bookmark_item_date);

                bkHolder.mRMMore = (ImageView) convertView.findViewById(R.id.rd_panel_item_more);
                bkHolder.mRMMoreView = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_moreView);

                bkHolder.mLlRename = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_ll_rename);
                bkHolder.mRMRename = (ImageView) convertView.findViewById(R.id.rd_bookmark_item_rename);
                bkHolder.mRMTvRename = (TextView) convertView.findViewById(R.id.rd_bookmark_item_tv_rename);

                bkHolder.mLlDelete = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_ll_delete);
                bkHolder.mRMDelete = (ImageView) convertView.findViewById(R.id.rd_bookmark_item_delete);
                bkHolder.mRMTvDelete = (TextView) convertView.findViewById(R.id.rd_bookmark_item_tv_delete);

                if (AppDisplay.getInstance(mContext).isPad()) {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_pad)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                    bkHolder.mRMMore.setPadding(bkHolder.mRMMore.getPaddingLeft(), bkHolder.mRMMore.getPaddingTop(), paddingRight, bkHolder.mRMMore.getPaddingBottom());
                } else {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_phone)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                    bkHolder.mRMMore.setPadding(bkHolder.mRMMore.getPaddingLeft(), bkHolder.mRMMore.getPaddingTop(), paddingRight, bkHolder.mRMMore.getPaddingBottom());
                }

                convertView.setTag(bkHolder);
            } else {
                bkHolder = (RKViewHolder) convertView.getTag();
            }

            final ReadingBookmarkNode node = (ReadingBookmarkNode) getItem(position);
            bkHolder.mRMContent.setText(node.getTitle());
            String time = AppDmUtil.dateOriValue;
            if(node.getDateTime()!=null){
                time = AppDmUtil.getLocalDateString(node.getDateTime());
            }
            bkHolder.mRMCreateTime.setText(time);

            if (mItemMoreViewShow.get(position)) {
                bkHolder.mRMMoreView.setVisibility(View.VISIBLE);
            } else {
                bkHolder.mRMMoreView.setVisibility(View.GONE);
            }

            bkHolder.mRMMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBookmarkListener.onMoreClick(position);
                }
            });

            View.OnClickListener renameListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((LinearLayout) (v.getParent()).getParent()).setVisibility(View.GONE);
                    mItemMoreViewShow.set(position, false);
                    mBookmarkListener.onRename(position);
                }
            };
            bkHolder.mRMRename.setOnClickListener(renameListener);
            bkHolder.mRMTvRename.setOnClickListener(renameListener);

            View.OnClickListener deleteListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((LinearLayout) (v.getParent()).getParent()).setVisibility(View.GONE);
                    mItemMoreViewShow.set(position, false);
                    mBookmarkListener.onDelete(position);
                }
            };
            bkHolder.mRMDelete.setOnClickListener(deleteListener);
            bkHolder.mRMTvDelete.setOnClickListener(deleteListener);

            View.OnTouchListener listener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.setPressed(true);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.setPressed(false);
                    }
                    return false;
                }
            };
            bkHolder.mLlRename.setOnTouchListener(listener);
            bkHolder.mLlDelete.setOnTouchListener(listener);

            RelativeLayout.LayoutParams paramsMoreView = (RelativeLayout.LayoutParams) bkHolder.mRMMoreView.getLayoutParams();
            paramsMoreView.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            bkHolder.mRMMoreView.setLayoutParams(paramsMoreView);

            return convertView;
        }

        @Override
        public Object getItem(int position) {
            return mNodeList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

}
