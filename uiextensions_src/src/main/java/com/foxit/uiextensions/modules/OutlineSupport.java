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
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.Bookmark;
import com.foxit.sdk.pdf.action.Destination;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.modules.OutlineModule.OutlineItem;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;

public abstract class OutlineSupport {
    private static final int UPDATEUI = 100;

    public static final int STATE_NORMAL = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOAD_FINISH = 2;

    private int mCurrentState = STATE_NORMAL;

    private Context mContext;
    private PDFViewCtrl mPDFViewCtrl;
    private PopupWindow mPanelPopupWindow;
    private MyHandler mHandler;
    private OutlineAdapter mAdapter;
    private OutlineItem mOutlineItem = new OutlineItem();
    private ArrayList<OutlineItem> mOutlineList = new ArrayList<OutlineItem>();
    private ArrayList<OutlineItem> mShowOutlineList = new ArrayList<OutlineItem>();

    private int mLevel = 0;
    private ImageView mBack;
    private ArrayList<OutlineItem> mParents = new ArrayList<OutlineItem>();
    private int mPosition = -1;
    private AppDisplay mDisplay;

    public int getCurrentState() {
        return mCurrentState;
    }

    public OutlineSupport(Context context, PDFViewCtrl pdfViewCtrl, AppDisplay display, PopupWindow popup, ImageView back) {
        mContext = context;
        mDisplay = display;

        mPanelPopupWindow = popup;
        this.mPDFViewCtrl = pdfViewCtrl;
        mBack = back;
        mHandler = new MyHandler();
        mAdapter = new OutlineAdapter();
        mCurrentState = STATE_LOADING;
        updateUI(mLevel, mCurrentState);
        OutlineBindingListView(mAdapter);
        try {
            mOutlineItem.mBookmark = mPDFViewCtrl.getDoc().getFirstBookmark();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        if (mOutlineItem.mBookmark == null)
            return;

        init(mOutlineItem.mBookmark, 0, 0);

        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLevel = mLevel - 1;
                mShowOutlineList.clear();
                mShowOutlineList.addAll(mParents.get(mPosition).mChildren);
                getShowOutline(mShowOutlineList);
                updateUI(mLevel, STATE_NORMAL);
                mAdapter.notifyDataSetChanged();

                mPosition = mShowOutlineList.get(0).mParentPos;
            }
        });
    }

    private void init(Bookmark bookmark, int idx, int level) {
        if (bookmark == null)
            return;
        try {
            OutlineItem outlineItem = new OutlineItem();
            Bookmark current = bookmark.getFirstChild();//

            while (current != null) {
                OutlineItem childItem = new OutlineItem();

                boolean hasChild = current.hasChild();
                childItem.mHaveChild = hasChild;
                childItem.mParentPos = idx;
                childItem.mTitle = current.getTitle();
                childItem.mBookmark = current;
                childItem.mLevel = level;

                Destination dest = current.getDestination();
                if (dest != null)
                    childItem.mPageIndex = dest.getPageIndex();

                current = current.getNextSibling();
                outlineItem.mChildren.add(childItem);
            }
            outlineItem.mLevel = level - 1;

            if (mOutlineList.size() == 0) {
                mOutlineList.addAll(outlineItem.mChildren);
                for (int i = 0; i < outlineItem.mChildren.size(); i++) {
                    mParents.add(outlineItem);
                }
                mShowOutlineList.clear();
                mShowOutlineList.addAll(mOutlineList);
                mCurrentState = STATE_LOAD_FINISH;
                getShowOutline(mShowOutlineList);
                if (mAdapter != null) {
                    Message msg = new Message();
                    msg.arg1 = mCurrentState;
                    msg.what = UPDATEUI;
                    mHandler.sendMessage(msg);
                }
                return;
            }

            if (idx < 0) {
                return;
            }
            mOutlineList.addAll(idx + 1, outlineItem.mChildren);
            for (int i = 0; i < outlineItem.mChildren.size(); i++) {
                mParents.add(idx + 1 + i, outlineItem);
            }

            mShowOutlineList.addAll(outlineItem.mChildren);
            mCurrentState = STATE_NORMAL;
            getShowOutline(mShowOutlineList);
            if (mAdapter != null) {
                Message msg = new Message();
                msg.arg1 = mCurrentState;
                msg.what = UPDATEUI;
                mHandler.sendMessage(msg);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPDFViewCtrl.recoverForOOM();
                return;
            }
        }
    }

    public abstract void OutlineBindingListView(BaseAdapter adapter);

    public abstract void getShowOutline(ArrayList<OutlineItem> mOutlineList);

    public abstract void updateUI(int level, int state);

    //outline item class
    class Outline {
        LinearLayout sd_outline_layout_ll;
        TextView tvChapter = null;//chapter
        ImageView ivMore = null;// opened or closed
        LinearLayout layout;
    }

    private void getOutList(OutlineItem outlineItem, int pos) {
        Bookmark current = null;
        current = outlineItem.mBookmark;
        init(current, pos, mLevel);
    }

    private class OutlineAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mShowOutlineList != null ? mShowOutlineList.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mShowOutlineList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Outline outline;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.rv_panel_outline_item, null, false);

                outline = new Outline();
                outline.sd_outline_layout_ll = (LinearLayout) convertView.findViewById(R.id.sd_outline_layout_ll);
                outline.tvChapter = (TextView) convertView.findViewById(R.id.sd_outline_chapter);
                outline.ivMore = (ImageView) convertView.findViewById(R.id.sd_outline_more);
                outline.layout = (LinearLayout) convertView.findViewById(R.id.sd_outline_layout_more);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) outline.sd_outline_layout_ll.getLayoutParams();
                if (mDisplay.isPad()) {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad);
                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                    outline.ivMore.setPadding(outline.ivMore.getPaddingLeft(), outline.ivMore.getPaddingTop(), paddingRight, outline.ivMore.getPaddingBottom());
                } else {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone);
                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                    outline.ivMore.setPadding(outline.ivMore.getPaddingLeft(), outline.ivMore.getPaddingTop(), paddingRight, outline.ivMore.getPaddingBottom());
                }
                outline.sd_outline_layout_ll.setLayoutParams(layoutParams);

                convertView.setTag(outline);
            } else {
                outline = (Outline) convertView.getTag();
            }

            outline.ivMore.setVisibility(mShowOutlineList.get(position).mHaveChild ? View.VISIBLE : View.INVISIBLE);
            outline.tvChapter.setText(mShowOutlineList.get(position).mTitle);
            outline.ivMore.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    OutlineItem currentNode = mShowOutlineList.get(position);
                    mLevel = currentNode.mLevel + 1;
                    mPosition = mOutlineList.indexOf(currentNode);

                    boolean mIsShowNext = mOutlineList.get(mPosition).mIsExpanded;

                    mOutlineList.get(mPosition).mIsExpanded = !mIsShowNext;
                    mShowOutlineList.clear();
                    mCurrentState = STATE_LOADING;
                    getOutList(currentNode, mPosition);

                }
            });
            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int index = mShowOutlineList.get(position).mPageIndex;
                    float mX = mShowOutlineList.get(position).mX;
                    float mY = mShowOutlineList.get(position).mY;
                    mPDFViewCtrl.gotoPage(index, mX, mY);

                    if (mPanelPopupWindow.isShowing()) {
                        mPanelPopupWindow.dismiss();
                    }
                }
            });
            return convertView;
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATEUI:
                    updateUI(mLevel, msg.arg1);
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
