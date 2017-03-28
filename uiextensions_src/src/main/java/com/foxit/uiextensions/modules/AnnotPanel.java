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
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.AnnotEventTask;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AnnotPanel implements View.OnClickListener, AnnotAdapter.CheckBoxChangeListener{

    public static final int STATUS_LOADING = 1;
    public static final int STATUS_CANCEL = 2;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_DONE = 4;
    public static final int STATUS_FAILED = 5;
    public static final int STATUS_DELETING = 6;

    private static final int DELETE_CAN = 0;
    private static final int DELETE_SRCAN = 1;
    private static final int DELETE_UNCAN = 2;

    private final PDFViewCtrl mPdfViewCtrl;
    private final Context mContext;
    private final ViewGroup mParent;
    private final AnnotAdapter mAdapter;
    private final List<AnnotNode> mCheckedNodes;
    private final List<AnnotNode> mDeleteTemps;

    private View mMainLayout;
    private TextView mChangedTextView;

    private int mLoadedState;
    private int mLoadedIndex;

    private AnnotPanelModule mPanel;

    AnnotPanel(AnnotPanelModule panelModule, Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent, View layout, ArrayList<Boolean> itemMoreViewShow) {
        mPanel = panelModule;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
        mContext = context;
        mMainLayout = layout;
        mCheckedNodes = new ArrayList<AnnotNode>();
        mDeleteTemps = new ArrayList<AnnotNode>();
        mAdapter = new AnnotAdapter(layout.getContext(), mPdfViewCtrl, mParent, itemMoreViewShow);
        mAdapter.setPopupWindow(mPanel.getPopupWindow());
        mAdapter.setCheckBoxChangeListener(this);
        init();

    }

    void clearAllNodes() {
        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        if (mAdapter.selectAll()) {
            final Activity context = (Activity) mContext;
            resetDeleteDialog();
            Collections.sort(mCheckedNodes);
            if (checkDeleteStatus() == DELETE_SRCAN) {
                if (mSRDeleteDialog == null || mSRDeleteDialog.getDialog().getOwnerActivity() == null) {
                    mSRDeleteDialog = new UITextEditDialog(context);
                    mSRDeleteDialog.getPromptTextView().setText(R.string.rv_panel_annot_delete_tips);
                    mSRDeleteDialog.setTitle(R.string.cloud_delete_tv);
                    mSRDeleteDialog.getInputEditText().setVisibility(View.GONE);
                }
                mSRDeleteDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mSRDeleteDialog.dismiss();
                        mDeleteDialog = new ProgressDialog(context);
                        mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mDeleteDialog.setCancelable(false);
                        mDeleteDialog.setIndeterminate(false);
                        mDeleteDialog.setMessage(context.getString(R.string.rv_panel_annot_deleting));
                        mDeleteDialog.show();
                        mDeleteTemps.clear();
                        mDeleteTemps.addAll(mCheckedNodes);
                        deleteItems();
                    }
                });
                mSRDeleteDialog.show();
            } else if (checkDeleteStatus() == DELETE_UNCAN) {
                UIToast.getInstance(mContext).show("Failed...");
            } else {
                mDeleteDialog = new ProgressDialog(context);
                mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mDeleteDialog.setCancelable(false);
                mDeleteDialog.setIndeterminate(false);
                mDeleteDialog.setMessage(context.getString(R.string.rv_panel_annot_deleting));
                mDeleteDialog.show();
                mDeleteTemps.clear();
                mDeleteTemps.addAll(mCheckedNodes);
                deleteItems();
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private boolean isPageLoaded(PDFPage page) {
        if (page == null) return false;
        try {
            int pageIndex = page.getIndex();
            return pageIndex < mLoadedIndex || mLoadedState == STATUS_DONE;

        } catch (PDFException e) {
            return false;
        }
    }


    private boolean mPause;

    public void setStatusPause(boolean status) {
        mPause = status;
    }

    public DocumentManager.AnnotEventListener getAnnotEventListener() {
        return mAnnotEventListener;
    }

    private DocumentManager.AnnotEventListener mAnnotEventListener = new DocumentManager.AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            //After adding an annotation, add a node that corresponds to the annotation in the Annotation list.
            if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
            try {
                if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_annotFlagHidden) != 0 || !isPageLoaded(page)) return;
                AnnotNode node = mAdapter.getAnnotNode(page.getIndex(), annot.getUniqueID());
                if (node == null) {
                    String replyTo = "";
                    Annot replyToAnnot = AppAnnotUtil.getReplyToAnnot(annot);
                    if (replyToAnnot != null) {
                        replyTo = replyToAnnot.getUniqueID();
                    }

                    node = new AnnotNode(page.getIndex(), annot.getUniqueID(), replyTo);
                    if (annot.isMarkup())
                        node.setAuthor(((Markup) annot).getTitle());
                    node.setType(AppAnnotUtil.getTypeString(annot));
                    node.setContent(annot.getContent());
                    String date = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                    if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                        if (annot.isMarkup())
                            date = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                    }
                    node.setDate(date);
                    node.setDeletable(true);
                    node.setCanReply(AppAnnotUtil.isSupportReply(annot));
                    if (annot.isMarkup())
                        node.setIntent(((Markup) annot).getIntent());
                    mAdapter.addNode(node);
                }
            } catch (PDFException e) {
                e.printStackTrace();
                return;
            }
            mAdapter.establishNodeList();
            mAdapter.notifyDataSetChanged();
            mPanel.hideNoAnnotsView();
        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {
            //After removing an annotation, remove the node that corresponds to the annotation in the Annotation list.
            if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
            try {
                if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_annotFlagHidden) != 0 || !isPageLoaded(page)) return;
                for (int i = mCheckedNodes.size() - 1; i > -1; i--) {
                    AnnotNode node = mCheckedNodes.get(i);
                    if (node.getUID().equals(annot.getUniqueID())) {
                        node.setChecked(false);
                        onChecked(false, node);
                    }
                    AnnotNode parent = node.getParent();
                    while (parent != null) {
                        if (parent.getUID().equals(annot.getUniqueID())) {
                            node.setChecked(false);
                            onChecked(false, node);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
                mAdapter.removeNode(page.getIndex(), annot.getUniqueID());
                mAdapter.establishNodeList();
                mAdapter.notifyDataSetChanged();
                if (mLoadedState == STATUS_DONE && mAdapter.getCount() == 0) {
                    mPanel.showNoAnnotsView();
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {
            //After modifying an annotation, modify information of the node that corresponds to the annotation in the Annotation list.
            if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
            try {
                if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_annotFlagHidden) != 0 || !isPageLoaded(page)) return;
                mAdapter.updateNode(annot);
                mAdapter.notifyDataSetChanged();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

        }
    };

    private void init() {
        mMainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mChangedTextView = (TextView) mMainLayout.findViewById(R.id.rv_panel_annot_notify);
        mChangedTextView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {

    }

    private ProgressDialog mDeleteDialog;
    private UITextEditDialog mSRDeleteDialog;

    private void resetDeleteDialog() {
        if (mDeleteDialog != null) {
            mDeleteDialog.dismiss();
            mDeleteDialog = null;
        }
    }

    private void deleteItems() {
        final int size = mDeleteTemps.size();
        if (size == 0) {
            if (mLoadedState == STATUS_DELETING) {
                startSearch(mLoadedIndex);
            }
            resetDeleteDialog();
            if (mAdapter.getAnnotCount() == 0) {
                mPanel.showNoAnnotsView();
            }
            mAdapter.notifyDataSetChanged();
            return;
        }
        if (mLoadedState != STATUS_DONE) {
            mLoadedState = STATUS_DELETING;
        }
        if (mPdfViewCtrl.getDoc() == null) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
        }

        final AnnotNode node = mDeleteTemps.get(size - 1);
        if (node == null || node.isPageDivider()) {
            mDeleteTemps.remove(node);
            deleteItems();
            return;
        }
        if (!node.canDelete()) {
            node.setChecked(false);
            onChecked(false, node);
            mDeleteTemps.remove(node);
            deleteItems();
            return;
        }
        mAdapter.removeNode(node, new AnnotAdapter.DeleteCallback() {

            @Override
            public void result(boolean success, AnnotNode n) {
                if (success) {
                    mDeleteTemps.remove(n);
                    onChecked(false, n);
                    deleteItems();
                } else {
                    resetDeleteDialog();
                }
            }
        });
    }

    private int checkDeleteStatus() {
        int status = DELETE_CAN;
        for (AnnotNode node : mCheckedNodes) {
            if (!node.canDelete()) {
                status = DELETE_UNCAN;
                AnnotNode parent = node.getParent();
                while (parent != null) {
                    if (parent.isChecked() && parent.canDelete()) {
                        status = DELETE_SRCAN;
                        break;
                    }
                    parent = parent.getParent();
                }
                if (status == DELETE_UNCAN) break;
            }
        }
        return status;
    }

    @Override
    public void onChecked(boolean isChecked, AnnotNode node) {
        if (isChecked) {
            if (!mCheckedNodes.contains(node)) {
                mCheckedNodes.add(node);
            }
        } else {
            mCheckedNodes.remove(node);
        }
    }

    public boolean jumpToPage(int position) {
        final AnnotNode node = (AnnotNode) mAdapter.getItem(position);
        if (node != null && !node.isPageDivider() && node.isRootNode() && !AppUtil.isEmpty(node.getUID())) {
            AnnotEventTask annotEventTask = new AnnotEventTask(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    try {
                        PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
                        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                        RectF rect = annot.getRect();
                        RectF rectPageView = new RectF();

                        //Covert rect from the PDF coordinate system to the page view coordinate system,
                        // and show the annotation to the middle of the screen as possible.
                        if(mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rectPageView, node.getPageIndex())) {
                            float devX = rectPageView.left - (mPdfViewCtrl.getWidth() - rectPageView.width()) / 2;
                            float devY = rectPageView.top - (mPdfViewCtrl.getHeight() - rectPageView.height()) / 2;
                            mPdfViewCtrl.gotoPage(node.getPageIndex(), devX, devY);
                        } else{
                            mPdfViewCtrl.gotoPage(node.getPageIndex(), new PointF(rect.left, rect.top));
                        }
                        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
                        //return true;
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });
            new Thread(annotEventTask).start();
            return true;
        }
        return false;
    }

    public int getCount() {
        return mAdapter.getCount();
    }

    public int getCurrentStatus() {
        return mLoadedState;
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void onDocOpened() {
        reset();
    }

    public void onDocWillClose() {
        reset();
        resetDeleteDialog();
        mSRDeleteDialog = null;
    }

    private void reset() {
        mChangedTextView.setVisibility(View.GONE);
        mPanel.hideNoAnnotsView();
        mLoadedState = STATUS_CANCEL;
        mPause = false;
        mLoadedIndex = 0;
        mCheckedNodes.clear();
        mAdapter.clearNodes();
        mAdapter.notifyDataSetChanged();
    }

    public void startSearch(final int pageIndex) {
        mLoadedState = STATUS_LOADING;
        mLoadedIndex = pageIndex;
        searchPage(pageIndex, new TaskResult<ArrayList<AnnotNode>>() {
            @Override
            public void onResult(boolean success, ArrayList<AnnotNode> nodeList) {
                int pageCount = mPdfViewCtrl.getPageCount();
                //If search the current page  failed, then all subsequent pages will no longer continue to search.
                if (!success) {
                    mLoadedState = STATUS_FAILED;
                    mPanel.updateLoadedPage(pageIndex + 1, pageCount);
                    return;
                }

                if (mPause) {
                    mPanel.pauseSearch(pageIndex);
                    mLoadedState = STATUS_PAUSED;
                    return;
                }

                mPanel.updateLoadedPage(pageIndex + 1, pageCount);

                int nCount = nodeList.size();
                for (int i = 0; i < nCount; i++) {
                    mAdapter.addNode(nodeList.get(i));
                }
                mAdapter.establishNodeList();
                mAdapter.notifyDataSetChanged();
                if (pageIndex < pageCount - 1) {
                    if (mLoadedState != STATUS_CANCEL) {
                        startSearch(pageIndex + 1);
                    }
                } else {
                    mLoadedState = STATUS_DONE;
                    mPanel.updateLoadedPage(0, 0);
                }
            }
        });
    }

    public interface TaskResult<T1> {
        void onResult(boolean success, T1 p1);
    }

    private void searchPage(int pageIndex, final TaskResult<ArrayList<AnnotNode>> result) {
        SearchPageTask searchPageTask = new SearchPageTask(mPdfViewCtrl, pageIndex, result);
        Handler handler = new Handler();
        handler.post(searchPageTask);
    }

    class SearchPageTask implements Runnable {
        private int mPageIndex;
        private PDFViewCtrl mPdfView;
        private ArrayList<AnnotNode> mSearchResults;
        private TaskResult mTaskResult;

        public SearchPageTask(PDFViewCtrl pdfView, int pageIndex, TaskResult<ArrayList<AnnotNode>> taskResult) {
            mPdfView = pdfView;
            mPageIndex = pageIndex;
            mTaskResult = taskResult;
        }

        @Override
        public void run() {
            if (mSearchResults == null) {
                mSearchResults = new ArrayList<AnnotNode>();
            }
            boolean bRet = searchPage();
            if (mTaskResult != null) {
                //noinspection unchecked
                mTaskResult.onResult(bRet, mSearchResults);
            }
        }

        private boolean searchPage() {
            try {
                PDFPage page = mPdfView.getDoc().getPage(mPageIndex);
                if (page == null)
                    return false;

                int nCount = page.getAnnotCount();
                if (nCount > 0) {
                    for (int i = 0; i < nCount; i++) {
                        Annot annot = page.getAnnot(i);
                        if (annot == null || (annot.getFlags() & Annot.e_annotFlagHidden) != 0 ||!AppAnnotUtil.isSupportEditAnnot(annot))
                            continue;
                        String replyTo = "";
                        Annot replyToAnnot = AppAnnotUtil.getReplyToAnnot(annot);

                        if (replyToAnnot != null) {
                            if (replyToAnnot.getUniqueID() == null)
                                replyToAnnot.setUniqueID(AppDmUtil.randomUUID(null));
                            replyTo = replyToAnnot.getUniqueID();
                        }

                        if (annot.getUniqueID() == null)
                            annot.setUniqueID(AppDmUtil.randomUUID(null));
                        AnnotNode node = new AnnotNode(mLoadedIndex, annot.getUniqueID(), replyTo);
                        if (annot.isMarkup())
                            node.setAuthor(((Markup) annot).getTitle());
                        node.setType(AppAnnotUtil.getTypeString(annot));
                        node.setContent(annot.getContent());
                        String date = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                        if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                            if (annot.isMarkup())
                                date = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                        }
                        node.setDate(date);
                        node.setDeletable(true);
                        node.setCanReply(AppAnnotUtil.isSupportReply(annot));
                        if (annot.isMarkup())
                            node.setIntent(((Markup) annot).getIntent());
                        mSearchResults.add(node);
                    }
                    page.getDocument().closePage(mPageIndex);
                }

            } catch (PDFException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
