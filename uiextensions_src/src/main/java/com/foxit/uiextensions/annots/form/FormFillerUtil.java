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
package com.foxit.uiextensions.annots.form;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.form.Form;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.uiextensions.utils.AppAnnotUtil;


public class FormFillerUtil {

	protected static int getAnnotFieldType(Form form, Annot annot)
	{
		int type = 0;
		try {

			RectF rect = annot.getRect();
			PointF point = new PointF(rect.left+1, rect.bottom+1);
			FormControl control = AppAnnotUtil.getControlAtPos(annot.getPage(), point, 1);
			if(control != null)
				type = control.getField().getType();
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return type;
	}

	public static  boolean isEmojiCharacter(int codePoint) {
		return (codePoint == 0x0) || (codePoint == 0x9)
				|| (codePoint == 0xa9) || (codePoint == 0xae) || (codePoint == 0x303d)
				|| (codePoint == 0x3030) || (codePoint == 0x2b55) || (codePoint == 0x2b1c) 
				|| (codePoint == 0x2b1b) || (codePoint == 0x2b50)
				|| ((codePoint >= 0x1F0CF) && (codePoint <= 0x1F6B8))
				|| (codePoint == 0xD) || (codePoint == 0xDE0D)
				|| ((codePoint >= 0x2100) && (codePoint <= 0x27FF))
				|| ((codePoint >= 0x2B05) && (codePoint <= 0x2B07))
				|| ((codePoint >= 0x2934) && (codePoint <= 0x2935))
				|| ((codePoint >= 0x203C) && (codePoint <= 0x2049))
				|| ((codePoint >= 0x3297) && (codePoint <= 0x3299))
				|| ((codePoint >= 0x1F600) && (codePoint <= 0x1F64F))
				|| ((codePoint >= 0xDC00) && (codePoint <= 0xE678));

	}
}
