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
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AnnotAdapter extends BaseAdapter {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private List<AnnotNode> mNodeList;
    private CheckBoxChangeListener mCheckBoxChangeListener;
    private SparseArray<List<AnnotNode>> mSparseArray;

    private final AppDisplay mDisplay;
    private final ArrayList<Boolean> mItemMoreViewShow;
    private PopupWindow mPopupWindow;

    public interface CheckBoxChangeListener {
        void onChecked(boolean isChecked, AnnotNode node);
    }

    public interface DeleteCallback {
        void result(boolean success, AnnotNode node);
    }

    public void setCheckBoxChangeListener(CheckBoxChangeListener listener) {
        mCheckBoxChangeListener = listener;
    }

    public AnnotAdapter(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent, ArrayList<Boolean> itemMoreViewShow) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mDisplay = new AppDisplay(mContext);
        mItemMoreViewShow = itemMoreViewShow;
        mNodeList = new ArrayList<AnnotNode>();
        mSparseArray = new SparseArray<List<AnnotNode>>();
    }

    public void setPopupWindow(PopupWindow popupWindow) {
        mPopupWindow = popupWindow;
    }

    public void establishNodeList() {
        mNodeList.clear();
        for (int i = 0; i < mSparseArray.size(); i++) {
            List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
            if (nodes != null && nodes.size() > 0) {
                int index = mNodeList.size();
                boolean addPageNode = false;
                int count = 0;
                for (AnnotNode an : nodes) {
                    if (an.isRootNode() && !an.isRedundant()) {
                        addPageNode = true;
                        mNodeList.add(an);
                        count++;
                        establishNodeRoot(an);
                    }
                }
                if (addPageNode) {
                    AnnotNode pageNode = new AnnotNode(mSparseArray.keyAt(i));
                    pageNode.counter = count;
                    mNodeList.add(index, pageNode);
                }
            }
        }
        mItemMoreViewShow.clear();
        for (int i = 0; i < mNodeList.size(); i++) {
            mItemMoreViewShow.add(false);
        }
    }

    private void establishNodeRoot(AnnotNode parent) {
        if (parent.isLeafNode()) return;
        for (AnnotNode child : parent.getChildren()) {
            mNodeList.add(child);
            establishNodeRoot(child);
        }
    }

    public void clearNodes() {
        mNodeList.clear();
        mItemMoreViewShow.clear();
        for (int i = 0; i < mSparseArray.size(); i++) {
            List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
            if (nodes != null) {
                nodes.clear();
            }
        }
        mSparseArray.clear();
        notifyDataSetChanged();
    }

    public void addNode(AnnotNode node) {
        List<AnnotNode> nodes = mSparseArray.get(node.getPageIndex());
        if (nodes == null) {
            nodes = new ArrayList<AnnotNode>();
            mSparseArray.put(node.getPageIndex(), nodes);
        }
        if (nodes.contains(node)) return;
        if (node.getReplyTo().equals("") && node.getUID().equals("")) {
            return;
        }
        boolean needFind = !node.getReplyTo().equals("");
        for (AnnotNode an : nodes) {
            if (needFind) {
                if (an.getUID().equals(node.getReplyTo())) {
                    node.setParent(an);
                    an.addChildNode(node);
                    needFind = false;
                    continue;
                }
            }
            if (!an.getReplyTo().equals("") && an.getReplyTo().equals(node.getUID())) {
                an.setParent(node);
                node.addChildNode(an);
            }
        }
        nodes.add(node);
        Collections.sort(nodes);
    }

    public void updateNode(Annot annot) {
        try {
            if (annot == null || !AppAnnotUtil.isSupportEditAnnot(annot) ||annot.getUniqueID() == null || annot.getUniqueID().equals(""))
                return;
            List<AnnotNode> nodes = mSparseArray.get(annot.getPage().getIndex());
            if (nodes == null) return;
            for (AnnotNode node : nodes) {
                if (node.getUID().equals(annot.getUniqueID())) {
                    if (annot.isMarkup())
                        node.setAuthor(((Markup) annot).getTitle());
                    node.setContent(annot.getContent());
                    String date = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                    if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                        if (annot.isMarkup())
                            date = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                    }
                    node.setDate(date);
                    notifyDataSetChanged();
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removeNode(int pageIndex, String uid) {
        if (uid == null || uid.equals("")) return;
        List<AnnotNode> nodes = mSparseArray.get(pageIndex);
        if (nodes == null) return;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            AnnotNode node = nodes.get(i);
            if (node.getUID().equals(uid)) {
                nodes.remove(node);
                if (node.getChildren() != null) {
                    for (int j = node.getChildren().size() - 1; j >= 0; j--) {
                        node.getChildren().get(j).setParent(null);
                    }
                    node.removeChildren(mPdfViewCtrl);
                }
                if (node.getParent() != null) {
                    node.getParent().removeChild(node);
                }
                break;
            }
        }
    }


    public void removeNode(final AnnotNode node, final DeleteCallback callback) {
        final List<AnnotNode> nodes = mSparseArray.get(node.getPageIndex());
        if (nodes == null || !nodes.contains(node)) {
            if (callback != null) {
                callback.result(true, node);
            }
            return;
        }
        if (node.getChildren() != null) {
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                node.getChildren().get(i).setParent(null);
            }
            node.removeChildren(mPdfViewCtrl);
        }

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
            if (page == null)
                return;
            nodes.remove(node);
            if (node.getParent() != null) {
                node.getParent().removeChild(node);
            }
            establishNodeList();
            notifyDataSetChanged();
            Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
            DocumentManager.getInstance(mPdfViewCtrl).removeAnnot(annot, null);
            callback.result(true, node);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mNodeList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= mNodeList.size()) return null;
        return mNodeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AnnotNode node = mNodeList.get(position);
        if(node == null) return null;
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.panel_annot_item, null);

            holder.mPageLayout = convertView.findViewById(R.id.rv_panel_annot_item_page_layout);
            holder.mPageIndexTextView = (TextView) holder.mPageLayout.findViewById(R.id.rv_panel_annot_item_page_tv);
            holder.mCounterTextView = (TextView) convertView.findViewById(R.id.rv_panel_annot_item_page_count_tv);

            holder.mRlMain = (RelativeLayout) convertView.findViewById(R.id.rd_panel_rl_main);
            holder.mMainLayout = convertView.findViewById(R.id.rv_panel_annot_item_main_layout);
            holder.mAuthorTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_author_tv);
            holder.mContentsTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_contents_tv);

            holder.mDateTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_date_tv);
            holder.mIconImageView = (ImageView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_icon_iv);
            holder.mRedImageView = (ImageView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_icon_red);

            holder.mItemMore = (ImageView) holder.mMainLayout.findViewById(R.id.rd_panel_annot_item_more);
            holder.mItemMoreView = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_moreview);

            holder.mItem_ll_reply = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_reply);
            holder.mItem_ll_comment = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_comment);
            holder.mItem_ll_delete = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_delete);

            LinearLayout.LayoutParams rlMainLayoutParams = (LinearLayout.LayoutParams) holder.mRlMain.getLayoutParams();
            RelativeLayout.LayoutParams mainLayoutParams = (RelativeLayout.LayoutParams) holder.mMainLayout.getLayoutParams();
            LinearLayout.LayoutParams contentLayoutParams = (LinearLayout.LayoutParams) holder.mContentsTextView.getLayoutParams();

            if (mDisplay.isPad()) {
                rlMainLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_pad);

                int marginLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                int marginRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);

                holder.mPageLayout.setPadding(marginLeft, holder.mPageLayout.getPaddingTop(), marginRight, holder.mPageLayout.getPaddingBottom());
                mainLayoutParams.leftMargin = marginLeft;
                contentLayoutParams.rightMargin = marginRight;

                int paddingRightSmall = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                holder.mItemMore.setPadding(holder.mItemMore.getPaddingLeft(), holder.mItemMore.getPaddingTop(), paddingRightSmall, holder.mItemMore.getPaddingBottom());
            } else {
                rlMainLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_phone);

                int marginLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                int marginRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);

                holder.mPageLayout.setPadding(marginLeft, holder.mPageLayout.getPaddingTop(), marginRight, holder.mPageLayout.getPaddingBottom());
                mainLayoutParams.leftMargin = marginLeft;
                contentLayoutParams.rightMargin = marginRight;

                int paddingRightSmall = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                holder.mItemMore.setPadding(holder.mItemMore.getPaddingLeft(), holder.mItemMore.getPaddingTop(), paddingRightSmall, holder.mItemMore.getPaddingBottom());
            }
            holder.mRlMain.setLayoutParams(rlMainLayoutParams);
            holder.mMainLayout.setLayoutParams(mainLayoutParams);
            holder.mContentsTextView.setLayoutParams(contentLayoutParams);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (node.isPageDivider()) {
            holder.mRlMain.setVisibility(View.GONE);
            holder.mItemMoreView.setVisibility(View.GONE);
            holder.mPageLayout.setVisibility(View.VISIBLE);
            holder.mCounterTextView.setText("" + node.counter);
            holder.mPageIndexTextView.setText(AppResource.getString(mContext, R.string.rv_panel_annot_item_pagenum) + " " + (node.getPageIndex() + 1));
            return convertView;
        } else {
            holder.mRlMain.setVisibility(View.VISIBLE);
            holder.mPageLayout.setVisibility(View.GONE);
        }

        int level = node.getLevel();
        holder.mDateTextView.setText(node.getDate());
        holder.mAuthorTextView.setText(node.getAuthor());
        holder.mContentsTextView.setText(node.getContent());

        if (node.isRootNode()) {
            int drawable = AppAnnotUtil.getIconId(node.getType());
            holder.mIconImageView.setImageResource(drawable);
        } else {
            holder.mIconImageView.setImageResource(R.drawable.annot_reply_selector);
        }

        holder.mItemMore.setVisibility(View.VISIBLE);
        if (mPdfViewCtrl.getDoc() != null) {
            if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {

                if (AppAnnotUtil.contentsModifiable(node.getType())) {
                    holder.mItem_ll_comment.setVisibility(View.VISIBLE);
                    holder.mItem_ll_reply.setVisibility(View.GONE);
                    holder.mItem_ll_delete.setVisibility(View.GONE);

                    holder.mItem_ll_comment.setTag(position);
                    View.OnClickListener commentListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;

                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);

                            UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, false, UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {

                                @Override
                                public void result(final String contents) {
                                }

                                @Override
                                public String getContent() {
                                    return (String) node.getContent();
                                }
                            });
                        }
                    };
                    holder.mItem_ll_comment.setOnClickListener(commentListener);
                } else {
                    holder.mItemMore.setVisibility(View.GONE);
                }

            } else {
                if (AppAnnotUtil.contentsModifiable(node.getType())) {
                    holder.mItem_ll_reply.setVisibility(View.VISIBLE);
                    holder.mItem_ll_comment.setVisibility(View.VISIBLE);
                    holder.mItem_ll_delete.setVisibility(View.VISIBLE);
                    holder.mItem_ll_reply.setTag(position);
                    View.OnClickListener replyListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;

                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);

                            UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, true, UIAnnotReply.TITLE_COMMENT_ID, new UIAnnotReply.ReplyCallback() {

                                @Override
                                public void result(final String contents) {
                                    try {
                                        PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
                                        if (page == null) return;
                                        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                                        if (annot == null) return;
                                        final String uid = AppDmUtil.randomUUID(null);
                                        UIAnnotReply.addReplyAnnot(mPdfViewCtrl, annot, page, uid, contents, null);
                                    } catch (PDFException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public String getContent() {
                                    return null;
                                }
                            });
                        }
                    };
                    holder.mItem_ll_reply.setOnClickListener(replyListener);

                    holder.mItem_ll_comment.setTag(position);
                    View.OnClickListener commentListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;
                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);
                            UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, true, UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {
                                @Override
                                public void result(String content) {
                                    try {
                                        PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
                                        if (page == null) return;
                                        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                                        if (annot == null || !AppAnnotUtil.isSupportEditAnnot(annot))
                                            return;
                                        annot.setContent(content);
                                        annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                                        node.setContent(content);
                                        node.setDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                        notifyDataSetInvalidated();
                                    } catch (PDFException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public String getContent() {
                                    return node.getContent().toString();
                                }
                            });
                        }
                    };
                    holder.mItem_ll_comment.setOnClickListener(commentListener);

                } else {
                    holder.mItem_ll_reply.setVisibility(View.GONE);
                    holder.mItem_ll_comment.setVisibility(View.GONE);
                    holder.mItem_ll_delete.setVisibility(View.VISIBLE);
                }

                holder.mItem_ll_delete.setTag(position);
                View.OnClickListener deleteListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (AppUtil.isFastDoubleClick()) return;
                        ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                        int tag = (Integer) v.getTag();
                        mItemMoreViewShow.set(tag, false);
                        removeNode(node, new DeleteCallback() {
                            @Override
                            public void result(boolean success, AnnotNode node) {
                                if (success) {
                                    notifyDataSetChanged();
                                }
                            }
                        });
                    }
                };
                holder.mItem_ll_delete.setOnClickListener(deleteListener);
            }

            holder.mContentsTextView.setBackgroundResource(R.color.ux_color_translucent);
            if (AppAnnotUtil.contentsModifiable(node.getType())) {
                holder.mContentsTextView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (AppUtil.isFastDoubleClick()) return;

                        boolean show = false;
                        for (int i = 0; i < mNodeList.size(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                break;
                            }
                        }
                        if (show) {
                            for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                                mItemMoreViewShow.set(i, false);
                            }
                            notifyDataSetChanged();
                        } else {
                            if (node.canReply()) {
                                UIAnnotReply.replyToAnnot(mContext, mPdfViewCtrl, mParent, DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot(), UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {
                                    @Override
                                    public void result(String content) {
                                        try {
                                            PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
                                            if (page == null) return;
                                            Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                                            if (annot == null || !AppAnnotUtil.isSupportEditAnnot(annot))
                                                return;
                                            annot.setContent(content);
                                            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                                            node.setContent(content);
                                            node.setDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                            notifyDataSetChanged();

                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public String getContent() {
                                        return node.getContent().toString();
                                    }
                                });
                            } else {
                                if (!node.isPageDivider() && node.isRootNode()) {
                                    if (!AppUtil.isEmpty(node.getUID())) {
                                        mPdfViewCtrl.gotoPage(node.getPageIndex(), 0, 0);
                                        if (mPopupWindow != null && mPopupWindow.isShowing()) {
                                            mPopupWindow.dismiss();
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }

        //noinspection deprecation
        holder.mContentsTextView.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
        holder.mRedImageView.setVisibility(View.GONE);

        int dx = mDisplay.dp2px(32 + 5);
        if (level > 0) {
            level = Math.min(level, 2);
            holder.mMainLayout.setPadding(dx * level, 0, 0, 0);
        } else {
            holder.mMainLayout.setPadding(0, 0, 0, 0);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mItemMoreView.getLayoutParams();
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        holder.mItemMoreView.setLayoutParams(params);

        holder.mItemMore.setTag(position);
        holder.mItemMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tag = (Integer) v.getTag();
                for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                    if (i == tag) {
                        mItemMoreViewShow.set(i, true);
                    } else {
                        mItemMoreViewShow.set(i, false);
                    }
                }
                notifyDataSetChanged();
            }
        });
        if (mItemMoreViewShow.get(position)) {
            holder.mItemMoreView.setVisibility(View.VISIBLE);
        } else {
            holder.mItemMoreView.setVisibility(View.GONE);
        }
        return convertView;
    }

    private static final class ViewHolder {
        public RelativeLayout mRlMain;
        public View mMainLayout;
        public ImageView mIconImageView;
        public ImageView mRedImageView;
        public TextView mContentsTextView;
        public TextView mAuthorTextView;
        public TextView mDateTextView;

        public View mPageLayout;
        public TextView mPageIndexTextView;
        public TextView mCounterTextView;

        public ImageView mItemMore;
        public LinearLayout mItemMoreView;

        public LinearLayout mItem_ll_reply;

        public LinearLayout mItem_ll_comment;

        public LinearLayout mItem_ll_delete;
    }

    public boolean selectAll() {
        int countAnnots = getAnnotCount();
        int selectedAnnots = getSelectedCount();
        if (selectedAnnots == countAnnots) {
            for (int i = 0; i < mSparseArray.size(); i++) {
                List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
                if (nodes != null) {
                    for (AnnotNode node : nodes) {
                        if (node.isRedundant()) continue;
                        if (node.isChecked()) {
                            node.setChecked(false);
                            if (mCheckBoxChangeListener != null) {
                                mCheckBoxChangeListener.onChecked(false, node);
                            }
                        }
                    }
                }
            }
            return false;
        }
        for (int i = 0; i < mSparseArray.size(); i++) {
            List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (node.isRedundant()) continue;
                    if (!node.isChecked()) {
                        node.setChecked(true);
                        if (mCheckBoxChangeListener != null) {
                            mCheckBoxChangeListener.onChecked(true, node);
                        }
                    }
                }
            }
        }
        return true;
    }

    private int getSelectedCount() {
        int count = 0;
        for (int i = 0; i < mSparseArray.size(); i++) {
            List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (node.isChecked()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int getAnnotCount() {
        int count = 0;
        for (int i = 0; i < mSparseArray.size(); i++) {
            List<AnnotNode> nodes = mSparseArray.get(mSparseArray.keyAt(i));
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (!node.isRedundant()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public AnnotNode getAnnotNode(int pageIndex, String uid) {
        if (uid == null || uid.equals("")) return null;
        List<AnnotNode> nodes = mSparseArray.get(pageIndex);
        if (nodes == null) return null;
        for (AnnotNode node : nodes) {
            if (node.getUID().equals(uid)) {
                return node;
            }
        }
        return null;
    }
}
