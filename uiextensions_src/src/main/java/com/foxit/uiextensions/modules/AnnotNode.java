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

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnnotNode implements Comparable<AnnotNode> {
    private final int pageIndex;
    private final String uid;
    private final String replyTo;
    int counter;

    private final boolean pageDivider;

    private String type;
    private String author;
    private CharSequence contents;
    private String date;
    private String intent;
    private boolean canDelete;
    private boolean canReply;
    private AnnotNode parent;
    private List<AnnotNode> children;
    private boolean checked;

    public AnnotNode(int pageIndex, String uid, String replyTo) {
        this.pageIndex = pageIndex;
        this.uid = uid;
        this.replyTo = replyTo;//annotation uid
        this.pageDivider = false;
    }

    public AnnotNode(int pageIndex) {
        this.pageIndex = pageIndex;
        this.uid = null;
        this.replyTo = null;
        this.pageDivider = true;
    }

    public boolean isPageDivider() {
        return this.pageDivider;
    }

    public int getPageIndex() {
        return this.pageIndex;
    }

    public String getUID() {
        return this.uid == null ? "" : this.uid;
    }

    public String getReplyTo() {
        return this.replyTo == null ? "" : this.replyTo;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthor() {
        return this.author == null ? "" : this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContent(CharSequence contents) {
        this.contents = contents;
    }

    public CharSequence getContent() {
        return this.contents == null ? "" : this.contents;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return this.date == null ? AppDmUtil.dateOriValue : this.date;
    }

    public void setParent(AnnotNode parent) {
        this.parent = parent;
    }

    public AnnotNode getParent() {
        return this.parent;
    }

    public boolean isRootNode() {
        return this.parent == null;
    }

    public boolean isLeafNode() {
        return this.children == null || this.children.size() == 0;
    }

    public void addChildNode(AnnotNode note) {
        if (this.children == null) {
            this.children = new ArrayList<AnnotNode>();
        }
        if (!this.children.contains(note)) {
            this.children.add(note);
        }
    }

    public void removeChildren(PDFViewCtrl pdfViewCtrl) {
        if (this.children != null) {
            for (int i = 0; i < this.children.size(); i++) {
                this.children.get(i).removeChildren(pdfViewCtrl);
            }
            this.children.clear();
        }
    }

    public void removeChild(AnnotNode node) {
        if (this.children != null) {
            this.children.remove(node);
        }
    }

    public List<AnnotNode> getChildren() {
        return this.children;
    }

    public int getLevel() {
        if (pageDivider) return -1;
        return this.parent == null ? 0 : parent.getLevel() + 1;
    }

    public void setChecked(boolean isChecked) {
        if (!this.pageDivider) {
            this.checked = isChecked;
        }
    }

    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AnnotNode)) return false;
        AnnotNode another = (AnnotNode) o;
        return this.pageDivider == another.pageDivider && this.pageIndex == another.pageIndex && this.getUID().equals(another.getUID());
    }

    @Override
    public int compareTo(AnnotNode another) {
        if (another == null) return 0;
        if (getLevel() != another.getLevel()) {
            return getLevel() - another.getLevel();
        }
        try {
            Date lDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(getDate()));
            Date rDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(another.getDate()));
            if(lDate == null && rDate == null)
                return 0;

            return lDate.before(rDate) ? -1 : (lDate.after(rDate) ? 1 : 0);
        } catch (Exception e) {
        }
        return 0;
    }

    public boolean isRedundant() {
        return !(this.getReplyTo().equals("") || (this.parent != null && !this.parent.isRedundant()));

    }

    public boolean canDelete() {
        return canDelete;
    }

    public void setDeletable(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean canReply() {
        return canReply;
    }

    public void setCanReply(boolean canReply) {
        this.canReply = canReply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
