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
package com.foxit.uiextensions.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtil {
    public static boolean isEmailFormatForRMS(String userId) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?");
        Matcher matcher = emailPattern.matcher(userId);
        return matcher.find();
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

    public static void showSoftInput(final View editText) {
        if (editText == null) return;
        editText.requestFocus();
        editText.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, 0);
            }
        });
    }

    public static void dismissInputSoft(final View editText) {
        if (editText == null) return;
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static void fixBackgroundRepeat(View view) {
        Drawable bg = view.getBackground();
        if (bg != null) {
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate();
                bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            }
        }
    }

    public static void openUrl(final Activity act, String url) {
        final String myurl = url;

        final UITextEditDialog dialog = new UITextEditDialog(act);
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(R.string.rv_url_dialog_title);
        dialog.getPromptTextView().setText(act.getResources().getString(R.string.rv_urldialog_title) +
                url +
                act.getResources().getString(R.string.rv_urldialog_title_ko) +
                "?");
        dialog.getOKButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri;
                if (myurl.toLowerCase().startsWith("http://") || myurl.toLowerCase().startsWith("https://")) {
                    uri = Uri.parse(myurl);
                } else {
                    uri = Uri.parse("http://" + myurl);
                }
                Intent it = new Intent(Intent.ACTION_VIEW, uri);
                act.startActivity(it);
                dialog.dismiss();
            }
        });

        dialog.getCancelButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void mailTo(Activity act, String uri) {
        if (isEmpty(uri) || isFastDoubleClick()) return;
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        if (uri.startsWith("mailto:")) {
            intent.setData(Uri.parse(uri));
        } else {
            intent.setData(Uri.parse("mailto:" + uri));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(Intent.createChooser(intent, ""));
    }

    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    public static String getFileName(String filePath) {
        int index = filePath.lastIndexOf('/');
        return (index < 0) ? filePath : filePath.substring(index + 1, filePath.length());
    }

    public static String fileSizeToString(long size) {
        float fsize = size;
        char unit[] = {'B', 'K', 'M'};
        for (int i = 0; i < unit.length; i++) {
            if (fsize < 1024 || i == unit.length - 1) {
                BigDecimal b = new BigDecimal(fsize);
                fsize = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
                return String.valueOf(fsize) + unit[i];
            }
            fsize /= 1024;
        }
        return "";
    }

    public static String getFileFolder(String filePath) {
        int index = filePath.lastIndexOf('/');
        if (index < 0) return "";
        String folder = filePath.substring(0, index);
        return folder;
    }
}
