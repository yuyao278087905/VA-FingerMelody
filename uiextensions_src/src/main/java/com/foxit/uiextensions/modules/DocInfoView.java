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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIMarqueeTextView;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.io.File;

/**
 * Class <CODE>DocInfoView</CODE> represents the basic information of pdf file.
 * <p/>
 * This class is used for showing the basic information of pdf file. It offers functions to initialize/show/FilePath Foxit PDF file basic information,
 * and also offers functions for global use.<br>
 * Any application should load Foxit PDF SDK by function {@link DocInfoView#init(String)} before calling any other Foxit PDF SDK functions.
 * When there is a need to show the basic information of pdf file, call function {@link DocInfoView#show()}.
 */
public class DocInfoView {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private boolean mIsPad = false;
    private String mFilePath = null;
    private SummaryInfo mSummaryInfo = null;

    DocInfoView(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mIsPad = AppDisplay.getInstance(context).isPad();
    }

    public void init(String filePath) {
        setFilePath(filePath);
        mSummaryInfo = new SummaryInfo();
    }

    public void setFilePath(String path) {
        mFilePath = path;
    }

    public void show() {
        if (mSummaryInfo == null)
            return;
        mSummaryInfo.init();
        mSummaryInfo.show();
    }

    abstract class DocInfo {
        protected View mRootLayout = null;
        protected UIMatchDialog mDialog = null;
        protected String mCaption = null;

        abstract void init();

        abstract void show();
    }

    /**
     * Class <CODE>SummaryInfo</CODE> represents the basic information of pdf file.
     * such as: file name, file path, file size and so on.
     * <p/>
     * This class is used for showing the basic information of pdf file. It offers functions to initialize/show/FilePath Foxit PDF file basic information,
     * and also offers functions for global use.<br>
     * Any application should load Foxit PDF SDK by function {@link SummaryInfo#init()} before calling any other Foxit PDF SDK functions.
     * When there is a need to show the basic information of pdf file, call function {@link SummaryInfo#show()}.
     */
    class SummaryInfo extends DocInfo {
        public class DocumentInfo {
            public String mFilePath = null;
            public String mFileName = null;
            public String mAuthor = null;
            public String mSubject = null;
            public String mCreateTime = null;
            public String mModTime = null;
            public long mFileSize = 0;
        }

        SummaryInfo() {
            mCaption = AppResource.getString(mContext, R.string.rv_doc_info);
        }

        @Override
        void init() {
            String content = null;
            View itemView = null;
            TextView tvContent = null;

            mRootLayout = View.inflate(mContext, R.layout.rv_doc_info, null);
            initPadDimens();

            PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) return;

            DocumentInfo info = getDocumentInfo();
            // file information
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_title);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_fileinfo));

            // filename
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_name_value);
            tvContent.setText(info.mFileName);

            // file path
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_path_value);
            tvContent.setText(AppUtil.getFileFolder(info.mFilePath));

            // file size
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_size_value);
            tvContent.setText(AppUtil.fileSizeToString(info.mFileSize));

            // author
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_author_value);
            tvContent.setText(info.mAuthor);

            // subject
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_subject_value);
            tvContent.setText(info.mSubject);

            // creation date
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_createdate_value);
            tvContent.setText(info.mCreateTime);

            // modify date
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_moddate_value);
            tvContent.setText(info.mModTime);

            // security information
            LinearLayout lySecurity = (LinearLayout) mRootLayout.findViewById(R.id.rv_doc_info_security);
            lySecurity.setVisibility(View.VISIBLE);

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_security_title);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_security));


            itemView = mRootLayout.findViewById(R.id.rv_doc_info_security);
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_security_content);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PermissionInfo permInfo = new PermissionInfo();
                    permInfo.init();
                    permInfo.show();
                }
            });

            try {
                switch (doc.getEncryptionType()) {
                    case PDFDoc.e_encryptPassword:
                        content = AppResource.getString(mContext, R.string.rv_doc_info_security_standard);
                        break;
                    case PDFDoc.e_encryptCertificate:
                        content = AppResource.getString(mContext, R.string.rv_doc_info_security_pubkey);
                        break;
                    case PDFDoc.e_encryptRMS:
                    case PDFDoc.e_encryptFoxitDRM:
                        content = AppResource.getString(mContext, R.string.rv_doc_info_security_rms);
                        break;
                    case PDFDoc.e_encryptCustom:
                        content = AppResource.getString(mContext, R.string.rv_doc_info_security_custom);
                        break;
                    default:
                        content = AppResource.getString(mContext, R.string.rv_doc_info_security_no);
                        break;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            tvContent.setText(content);
        }

        @Override
        void show() {
            mDialog = new UIMatchDialog(mContext);
            mDialog.setTitle(mCaption);
            mDialog.setContentView(mRootLayout);
            if (mIsPad) {
                mDialog.setBackButtonVisible(View.GONE);
            } else {
                mDialog.setBackButtonVisible(View.VISIBLE);
            }
            mDialog.setListener(new MatchDialog.DialogListener() {
                @Override
                public void onResult(long btType) {
                    mDialog.dismiss();
                }

                @Override
                public void onBackClick() {
                }
            });
            mDialog.showDialog(true);
        }

        DocumentInfo getDocumentInfo() {
            DocumentInfo info = new DocumentInfo();
            PDFDoc doc = mPdfViewCtrl.getDoc();
            info.mFilePath = mFilePath;
            if (mFilePath != null) {
                info.mFileName = AppUtil.getFileName(mFilePath);
                File file = new File(mFilePath);
                info.mFileSize = file.length();
            }

            try {
                info.mAuthor = doc.getMetadataValue("Author");
                info.mSubject = doc.getMetadataValue("Subject");
                info.mCreateTime = AppDmUtil.getLocalDateString(doc.getCreationDateTime());
                info.mModTime = AppDmUtil.getLocalDateString(doc.getModifiedDateTime());
            } catch (Exception e) {
            }

            return info;
        }

        void initPadDimens() {
            int leftPadding = 0;
            int rightPadding = 0;
            if (mIsPad) {
                leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
                rightPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            } else {
                leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_phone);
                rightPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_phone);
            }

            UIMarqueeTextView tvContent;
            // file name
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_name_value);
            tvContent.setPadding(0, 0, leftPadding + rightPadding, 0);

            // file path
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_path_value);
            tvContent.setPadding(0, 0, leftPadding + rightPadding, 0);

            if (!mIsPad)
                return;

            int arrIDForPadding[] = {
                R.id.rv_doc_info_fileinfo_title,
                R.id.table_row_file_name,
                R.id.table_row_file_path,
                R.id.table_row_file_size,
                R.id.table_row_file_author,
                R.id.table_row_file_subject,
                R.id.table_row_create_date,
                R.id.table_row_modify_date,
                R.id.rv_doc_info_security,
            };

            for (int i = 0; i < arrIDForPadding.length; i++) {
                View view = mRootLayout.findViewById(arrIDForPadding[i]);
                view.setPadding(leftPadding, 0, rightPadding, 0);
            }

            int arrIDForLayout[] = {
                R.id.rv_doc_info_fileinfo_title,
                R.id.rv_doc_info_fileinfo_name,
                R.id.rv_doc_info_fileinfo_name_value,
                R.id.rv_doc_info_fileinfo_path,
                R.id.rv_doc_info_fileinfo_path_value,
                R.id.rv_doc_info_fileinfo_size,
                R.id.rv_doc_info_fileinfo_size_value,
                R.id.rv_doc_info_fileinfo_author,
                R.id.rv_doc_info_fileinfo_author_value,
                R.id.rv_doc_info_fileinfo_subject,
                R.id.rv_doc_info_fileinfo_subject_value,
                R.id.rv_doc_info_fileinfo_createdate,
                R.id.rv_doc_info_fileinfo_createdate_value,
                R.id.rv_doc_info_fileinfo_moddate,
                R.id.rv_doc_info_fileinfo_moddate_value,
                R.id.rv_doc_info_security_title,
                R.id.rv_doc_info_security_content,
            };

            for (int i = 0; i < arrIDForLayout.length; i++) {
                View view = mRootLayout.findViewById(arrIDForLayout[i]);
                view.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_list_item_height_1l_pad);
            }
        }
    }

    /**
     * Class <CODE>PermissionInfo</CODE> represents the permission information of pdf file.
     * such as: print, modify, fill form, extract and so on.
     * <p/>
     * This class is used for showing the permission information of pdf file. It offers functions to initialize/show Foxit PDF file basic information,
     * and also offers functions for global use.<br>
     * Any application should load Foxit PDF SDK by function {@link PermissionInfo#init()} before calling any other Foxit PDF SDK functions.
     * When there is a need to show the basic information of pdf file, call function {@link PermissionInfo#show()}.
     */
    class PermissionInfo extends DocInfo {

        PermissionInfo() {
            mCaption = AppResource.getString(mContext, R.string.rv_doc_info_permission);
        }

        void init() {
            TextView tvContent = null;
            mRootLayout = View.inflate(mContext, R.layout.rv_doc_info_permissioin, null);

            initPadDimens();

            PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null)
                return;

            // summary
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_title);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_summary));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_print);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_print));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_fillform);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_fillform));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_annotform);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_annotform));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_assemble);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_assemble));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_modify);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_modify));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extractaccess);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_extractaccess));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extract);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_extract));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_signing);
            tvContent.setText(AppResource.getString(mContext, R.string.rv_doc_info_permission_signing));

            // on off switch
            TextView tvPrint = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_print_of);
            TextView tvFillForm = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_fillform_of);
            TextView tvAnnotForm = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_annotform_of);
            TextView tvAssemble = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_assemble_of);
            TextView tvModify = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_modify_of);
            TextView tvExtractAccess = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extractaccess_of);
            TextView tvExtract = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extract_of);
            TextView tvSigning = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_signing_of);

            String allowed = AppResource.getString(mContext, R.string.fx_string_allowed);
            String notAllowed = AppResource.getString(mContext, R.string.fx_string_notallowed);
            long userPermission = 0;
            try {
                userPermission = doc.getUserPermissions();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            tvPrint.setText((userPermission & PDFDoc.e_permPrint) != 0 ? allowed : notAllowed);
            tvFillForm.setText((userPermission & PDFDoc.e_permFillForm) != 0 ? allowed : notAllowed);
            tvAnnotForm.setText((userPermission & PDFDoc.e_permAnnotForm) != 0 ? allowed : notAllowed);
            tvAssemble.setText((userPermission & PDFDoc.e_permAssemble) != 0 ? allowed : notAllowed);
            tvModify.setText((userPermission & PDFDoc.e_permModify) != 0 ? allowed : notAllowed);
            tvExtractAccess.setText((userPermission & PDFDoc.e_permExtractAccess) != 0 ? allowed : notAllowed);
            tvExtract.setText((userPermission & PDFDoc.e_permExtract) != 0 ? allowed : notAllowed);
            boolean canSign = false;
            if ((userPermission & PDFDoc.e_permAnnotForm) != 0 || (userPermission & PDFDoc.e_permFillForm) != 0 ||
                (userPermission & PDFDoc.e_permModify) != 0)
                canSign = true;
            tvSigning.setText(canSign ? allowed : notAllowed);
        }

        void initPadDimens() {
            if (!mIsPad)
                return;

            int idArray[] = {
                R.id.rv_doc_info_permission_title,
                R.id.rv_doc_info_permisson_print_rl,
                R.id.rv_doc_info_permission_fillform_rl,
                R.id.rv_doc_info_permission_annotform_rl,
                R.id.rv_doc_info_permission_assemble_rl,
                R.id.rv_doc_info_permission_modify_rl,
                R.id.rv_doc_info_permission_extractaccess_rl,
                R.id.rv_doc_info_permission_extract_rl,
                R.id.rv_doc_info_permission_signing_rl,
            };

            for (int i = 0; i < idArray.length; i++) {
                View view = mRootLayout.findViewById(idArray[i]);
                int leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
                view.setPadding(leftPadding, 0, leftPadding, 0);
                view.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_list_item_height_1l_pad);
            }
        }

        void show() {
            mDialog = new UIMatchDialog(mContext);
            mDialog.setTitle(mCaption);
            mDialog.setContentView(mRootLayout);
            mDialog.setBackButtonVisible(View.VISIBLE);
            mDialog.setListener(new MatchDialog.DialogListener() {
                @Override
                public void onResult(long btType) {
                }

                @Override
                public void onBackClick() {
                    mDialog.dismiss();
                }
            });
            mDialog.showDialog();
        }
    }
}

