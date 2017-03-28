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
package com.foxit.uiextensions.controls.dialog;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UITextEditDialog extends UIDialog {
    TextView mPromptTextView;
    EditText mInputEditText;
    Button mOkButton;
    Button mCancelButton;
    String mEditPattern;
    View mCuttingLine;
    boolean mCheckEditEmail;
    TextWatcher mBaseTextChangeWatcher;

    AppDisplay mAppDisplay;

    public UITextEditDialog(Context context) {
        super(context, R.layout.fx_dialog_tv_edittext, getDialogTheme(), AppDisplay.getInstance(context).getUITextEditDialogWidth());

        mPromptTextView = (TextView) mContentView.findViewById(R.id.fx_dialog_textview);
        mInputEditText = (EditText) mContentView.findViewById(R.id.fx_dialog_edittext);
        mAppDisplay = AppDisplay.getInstance(context);
        if (mAppDisplay.isPad()) {
            usePadDimes();
        }
        mOkButton = (Button) mContentView.findViewById(R.id.fx_dialog_ok);
        mCuttingLine = mContentView.findViewById(R.id.fx_dialog_button_cutting_line);
        mCancelButton = (Button) mContentView.findViewById(R.id.fx_dialog_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mInputEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                    InputMethodManager inputManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        mBaseTextChangeWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text1 = mInputEditText.getText().toString();
                String text2 = stringFilter(text1);
                if (!text1.equals(text2)) {
                    mInputEditText.setText(text2);
                    mInputEditText.setSelection(mInputEditText.length());
                }
                if (mCheckEditEmail) {
                    if (AppUtil.isEmailFormatForRMS(text2)) {
                        mOkButton.setEnabled(true);
                    } else {
                        mOkButton.setEnabled(false);
                    }
                } else {
                    mOkButton.setEnabled((text2.toString().length() == 0) ? false : true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            private String stringFilter(String input) {
                if (mEditPattern == null || mEditPattern.length() == 0)
                    return input;
                Pattern pattern = Pattern.compile(mEditPattern);
                Matcher matcher = pattern.matcher(input);
                return matcher.replaceAll("");
            }
        };

        mInputEditText.addTextChangedListener(mBaseTextChangeWatcher);
        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private void usePadDimes() {
        try {
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).leftMargin = mAppDisplay.dp2px(24);
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).rightMargin = mAppDisplay.dp2px(24);
            ((LinearLayout.LayoutParams) mPromptTextView.getLayoutParams()).leftMargin = mAppDisplay.dp2px(24);
            ((LinearLayout.LayoutParams) mPromptTextView.getLayoutParams()).rightMargin = mAppDisplay.dp2px(24);
            ((LinearLayout.LayoutParams) mInputEditText.getLayoutParams()).leftMargin = mAppDisplay.dp2px(24);
            ((LinearLayout.LayoutParams) mInputEditText.getLayoutParams()).rightMargin = mAppDisplay.dp2px(24);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e("UITextEditDialog", e.getMessage());
            } else {
                Log.e("UITextEditDialog", "usePadDimes_has_an_error");
            }
        }
    }

    public TextView getPromptTextView() {
        return mPromptTextView;
    }

    public EditText getInputEditText() {
        return mInputEditText;
    }

    public Button getOKButton() {
        return mOkButton;
    }

    public Button getCancelButton() {
        return mCancelButton;
    }

    public void setPattern(String pattern) {
        mEditPattern = pattern;
    }

    @Override
    public void show() {
        if (mOkButton.getVisibility() == View.VISIBLE && mCancelButton.getVisibility() == View.VISIBLE) {
            mCuttingLine.setVisibility(View.VISIBLE);
            mOkButton.setBackgroundResource(R.drawable.dialog_right_button_background_selector);
            mCancelButton.setBackgroundResource(R.drawable.dialog_left_button_background_selector);
            if (Build.VERSION.SDK_INT < 11) {
                mOkButton.setBackgroundResource(R.drawable.dialog_left_button_background_selector);
                mCancelButton.setBackgroundResource(R.drawable.dialog_right_button_background_selector);
            }
        } else {
            mCuttingLine.setVisibility(View.GONE);
            mOkButton.setBackgroundResource(R.drawable.dialog_button_background_selector);
            mCancelButton.setBackgroundResource(R.drawable.dialog_button_background_selector);
        }
        super.show();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public static int getDialogTheme() {
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 11) {
            theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
        } else {
            theme = R.style.rv_dialog_style;
        }
        return theme;
    }
}
