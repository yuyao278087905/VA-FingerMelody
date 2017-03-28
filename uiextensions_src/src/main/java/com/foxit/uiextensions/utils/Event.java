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

import com.foxit.sdk.pdf.annots.Annot;

public class Event {
    public int mType;
    public Annot annot;

    public Event() {
    }

    public Event(int type) {
        mType = type;
    }

    public interface Callback {
        void result(Event event, boolean success);
    }
}
