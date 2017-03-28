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
package com.foxit.uiextensions.annots.caret;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

public class CaretToolHandler implements ToolHandler {
    private final Context mContext;
    private final PDFViewCtrl mPdfViewCtrl;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private PropertyBarImpl mAnnotationProperty;

    private int mOpacity;
    private boolean mIsInsertTextModule;

    private int[] mColors;
    private int mColor;

    private Dialog mDialog;
    //private TextView mDlgTitle;
    private EditText mDlgContent;
    /*private Button mDlgCancel;
    private Button mDlgOK;*/

    private PropertyCircleItem mToolCirclItem;
    private final RectF mCharSelectedRectF;
    private final TextSelector mTextSelector;
    private int mSelectedPageIndex;
    private boolean mIsContinuousCreate = false;
    private final Paint mPaint;
    private boolean mRPLCreating = false;
    private boolean mSelecting = false;
    private BaseItem mAnnotButton;
    private int mCaretRotate = 0;

    public CaretToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mTextSelector = new TextSelector(mPdfViewCtrl);
        mCharSelectedRectF = new RectF();
        mAnnotationProperty = new PropertyBarImpl(context, pdfViewCtrl, parent);
        mRPLCreating = false;

    }

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotationProperty = (PropertyBarImpl) propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotationProperty;
    }


    public void init(boolean isInsertTextModule) {
        mIsInsertTextModule = isInsertTextModule;
        mAnnotButton = new CircleItemImpl(mContext);
        mColors = PropertyBar.PB_COLORS_CARET;
        if (mIsInsertTextModule) {
            mAnnotButton.setImageResource(R.drawable.annot_tool_prompt_insert);
            mAnnotButton.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_CARET);

        } else {
            mAnnotButton.setImageResource(R.drawable.annot_tool_prompt_replace);
            mAnnotButton.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_REPLACE);
        }
        mAnnotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextSelector.clear();
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(CaretToolHandler.this);
            }
        });
        if (0 == mColor)
            mColor = mColors[0];
        if (0 == mOpacity)
            mOpacity = 255;
    }

    public void changeCurrentColor(int currentColor) {
        mColor = currentColor;
    }

    public void changeCurrentOpacity(int currentOpacity) {
        mOpacity = currentOpacity;
    }


    @Override
    public String getType() {
        if (mIsInsertTextModule)
            return ToolHandler.TH_TYPR_INSERTTEXT;
        return ToolHandler.TH_TYPE_REPLACE;
    }

    @Override
    public void onActivate() {
        mTextSelector.clear();
        mCharSelectedRectF.setEmpty();
        mAnnotButton.setSelected(true);
        resetPropertyBar();
    }

    @Override
    public void onDeactivate() {
        mTextSelector.clear();
        mCharSelectedRectF.setEmpty();
        mAnnotButton.setSelected(false);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    private int getCharIndexAtPoint(int pageIndex, PointF point) {
        int index = 0;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            index = getCharIndexAtPoint(page, point);

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return index;
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    private void resetPropertyBar() {
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_CARET, 0, mColors, 0, mColors.length);
        mAnnotationProperty.setColors(mColors);
        mAnnotationProperty.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mAnnotationProperty.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(mOpacity));
        mAnnotationProperty.reset(supportProperty);
        mAnnotationProperty.setPropertyChangeListener(mPropertyChangeListener);
    }


    private boolean OnSelectDown(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null) return false;
        int index = getCharIndexAtPoint(pageIndex, point);
        if (index >= 0) {
            selectInfo.setStart(index);
            selectInfo.setEnd(index);

            return true;
        }
        return false;
    }


    private boolean OnSelectMove(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null || selectInfo.getStart() < 0)
            return false;
        if (mSelectedPageIndex != pageIndex)
            return false;

        int index = getCharIndexAtPoint(pageIndex, point);
        if (index >= 0) {
            selectInfo.setEnd(index);
            return true;
        }
        return false;
    }

    private boolean OnSelectRelease(final int pageIndex, final TextSelector selectorInfo) {
        try {
            if (!mIsInsertTextModule && mRPLCreating) {
                if (selectorInfo.getStart() >= 0 && selectorInfo.getEnd() >= 0) {
                    selectorInfo.computeSelected(mPdfViewCtrl.getDoc().getPage(pageIndex), selectorInfo.getStart(), selectorInfo.getEnd());
                    mCharSelectedRectF.set(selectorInfo.getRectFList().get(selectorInfo.getRectFList().size() - 1));
                }
                View.OnClickListener cancelClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                        AppUtil.dismissInputSoft(mDlgContent);
                        selectorInfo.clear();
                    }
                };
                View.OnClickListener okClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        //add Caret Annotation
                        addCaretAnnot(pageIndex, getCaretRectFromSelectRect(selectorInfo, pageIndex, null), mCaretRotate, mTextSelector, null);
                        mDialog.dismiss();
                        AppUtil.dismissInputSoft(mDlgContent);

                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                    }
                };
                initDialog(cancelClickListener, okClickListener);
                mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mSelecting = false;
                        mRPLCreating = false;
                        RectF selectedRectF = new RectF(mCharSelectedRectF);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(selectedRectF, selectedRectF, pageIndex);
                        Rect selectedRect = new Rect();
                        selectedRectF.roundOut(selectedRect);
                        getInvalidateRect(selectedRect);
                        mPdfViewCtrl.invalidate(selectedRect);
                        mCharSelectedRectF.setEmpty();
                    }
                });

                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int getCharIndexAtPoint(PDFPage page, PointF pdfPt) {
        int index = 0;
        try {
            PDFTextSelect textSelect = PDFTextSelect.create(page);
            index = textSelect.getIndexAtPos(pdfPt.x, pdfPt.y, 10);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return index;
    }

    //Non standard dictionary entry, Adobe Acrobat and Foxit RDK support, but not supported by the Foxit Phantom
    private void setCaretRotate(Annot caret, int rotate) {
        if (caret == null || !(caret instanceof Caret))
            return;
        if (rotate < CommonDefines.e_rotation0 || rotate >CommonDefines.e_rotationUnknown)
            rotate = 0;
        try {
            caret.getDict().setAt("Rotate", PDFObject.createFromInteger(360 - rotate * 90));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF getCaretRectFromSelectRect(TextSelector selector, int pageIndex, PointF point) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            PDFTextSelect textSelect = PDFTextSelect.create(page);
            int start = Math.min(selector.getStart(), selector.getEnd());
            int end = Math.max(selector.getStart(), selector.getEnd());
            int nCount = textSelect.getTextRectCount(start, end - start + 1);
            RectF docSelectedRectF = textSelect.getTextRect(nCount - 1);
            if (docSelectedRectF == null) return null;
            mCaretRotate = textSelect.getBaselineRotation(nCount - 1) % CommonDefines.e_rotationUnknown;
            RectF caretRect = new RectF();
            float w, h;
            if (mCaretRotate % 2 != 0) {
                w = (docSelectedRectF.right - docSelectedRectF.left);
                h = w * 2 / 3;
            } else {
                h = (docSelectedRectF.top - docSelectedRectF.bottom);
                w = h * 2 / 3;
            }
            float offsetY = h * 0.9f;
            float offsetX = w * 0.9f;
            switch (mCaretRotate) {
                case CommonDefines.e_rotation0:{
                    if ((point != null && point.x - docSelectedRectF.left <= (docSelectedRectF.right - docSelectedRectF.left) / 2)) {
                        caretRect.set(docSelectedRectF.left - w / 2, docSelectedRectF.bottom + h, docSelectedRectF.left + w / 2, docSelectedRectF.bottom);
                    } else {
                        caretRect.set(docSelectedRectF.right - w / 2, docSelectedRectF.bottom + h, docSelectedRectF.right + w / 2, docSelectedRectF.bottom);
                    }

                    caretRect.offset(0, 0 - offsetY);
                }
                break;
                case CommonDefines.e_rotation90: {
                    if ((point != null && point.y - docSelectedRectF.bottom >= (docSelectedRectF.top - docSelectedRectF.bottom) / 2)) {
                        caretRect.set(docSelectedRectF.left, docSelectedRectF.top + h / 2, docSelectedRectF.left + w, docSelectedRectF.top - h / 2);
                    } else {
                        caretRect.set(docSelectedRectF.left, docSelectedRectF.bottom + h / 2, docSelectedRectF.left + w, docSelectedRectF.bottom - h / 2);
                    }
                    caretRect.offset(0 - offsetX, 0);
                }
                break;
                case CommonDefines.e_rotation180: {
                    if ((point != null && point.x - docSelectedRectF.left >= (docSelectedRectF.right - docSelectedRectF.left) / 2)) {
                        caretRect.set(docSelectedRectF.right - w / 2, docSelectedRectF.top, docSelectedRectF.right + w / 2, docSelectedRectF.top -h);
                    } else {
                        caretRect.set(docSelectedRectF.left - w / 2, docSelectedRectF.top, docSelectedRectF.left + w / 2, docSelectedRectF.top -h);
                    }
                    caretRect.offset(0, offsetY);

                }
                break;
                case CommonDefines.e_rotation270: {
                    if ((point != null && point.y - docSelectedRectF.bottom <= (docSelectedRectF.top - docSelectedRectF.bottom) / 2)) {
                        caretRect.set(docSelectedRectF.right - w, docSelectedRectF.bottom + h / 2, docSelectedRectF.right, docSelectedRectF.bottom - h / 2);
                    } else {
                        caretRect.set(docSelectedRectF.right - w, docSelectedRectF.top + h / 2, docSelectedRectF.right, docSelectedRectF.top - h / 2);
                    }
                    caretRect.offset(offsetX, 0);
                }
                break;
            }

            return caretRect;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent motionEvent) {
        final PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (mIsInsertTextModule) {
                        mTextSelector.clear();
                        mSelectedPageIndex = pageIndex;
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);

                        final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                        int index = getCharIndexAtPoint(page, docPoint);
                        if (index == -1) {
                            return true;
                        }

                        if (index >= 0) {
                            mSelecting = true;
                            mTextSelector.setStart(index);
                            mTextSelector.setEnd(index);

                            mTextSelector.computeSelected(page, mTextSelector.getStart(), mTextSelector.getEnd());
                            mCharSelectedRectF.set(mTextSelector.getBbox());
                            invalidateTouch(pageIndex, mTextSelector);
                            final PointF pointTemp = new PointF(point.x, point.y);
                            View.OnClickListener cancelClickListener = new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    mDialog.dismiss();
                                    AppUtil.dismissInputSoft(mDlgContent);

                                }
                            };
                            View.OnClickListener okClickListener = new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    PointF pdfPoint = new PointF(pointTemp.x, pointTemp.y);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);
                                    addCaretAnnot(pageIndex, getCaretRectFromSelectRect(mTextSelector, pageIndex, pdfPoint), mCaretRotate, mTextSelector, null);
                                    mDialog.dismiss();
                                    AppUtil.dismissInputSoft(mDlgContent);
                                    if (!mIsContinuousCreate) {
                                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                                    }
                                }
                            };
                            initDialog(cancelClickListener, okClickListener);
                            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    mSelecting = false;
                                    RectF selectedRectF = new RectF(mCharSelectedRectF);
                                    clearSelectedRectF();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(selectedRectF, selectedRectF, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(selectedRectF, selectedRectF, pageIndex);
                                    Rect selectedRect = new Rect();
                                    selectedRectF.roundOut(selectedRect);
                                    getInvalidateRect(selectedRect);
                                    mPdfViewCtrl.invalidate(selectedRect);
                                }
                            });

                            return true;
                        }
                    } else {
                        mTextSelector.clear();
                        mSelectedPageIndex = pageIndex;
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);
                        mRPLCreating = OnSelectDown(pageIndex, docPoint, mTextSelector);
                        mSelecting = mRPLCreating;
                        return mRPLCreating;
                    }
                    return false;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (!mIsInsertTextModule && mRPLCreating) {
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);
                        if (OnSelectMove(pageIndex, docPoint, mTextSelector)) {
                            mTextSelector.computeSelected(mPdfViewCtrl.getDoc().getPage(pageIndex), mTextSelector.getStart(), mTextSelector.getEnd());
                            invalidateTouch(pageIndex, mTextSelector);
                            return true;
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_UP:
                    return OnSelectRelease(pageIndex, mTextSelector);
                default:
                    break;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initDialog(View.OnClickListener cancelClickListener, View.OnClickListener okClickListener) {
        Context context = mContext;
        View mView = View.inflate(context, R.layout.rd_note_dialog_edit, null);
        final TextView dialogTitle = (TextView) mView.findViewById(R.id.rd_note_dialog_edit_title);
        mDlgContent = (EditText) mView.findViewById(R.id.rd_note_dialog_edit);
        final Button cancelButton = (Button) mView.findViewById(R.id.rd_note_dialog_edit_cancel);
        final Button applayButton = (Button) mView.findViewById(R.id.rd_note_dialog_edit_ok);

        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mDialog = new Dialog(context, R.style.rv_dialog_style);
        mDialog.setContentView(mView, new ViewGroup.LayoutParams(AppDisplay.getInstance(mContext).getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        mDlgContent.setMaxLines(10);

        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mDialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

        if (mIsInsertTextModule) {
            dialogTitle.setText(mContext.getResources().getString(R.string.fx_string_inserttext));
        } else {
            dialogTitle.setText(mContext.getResources().getString(R.string.fx_string_replacetext));
        }

        applayButton.setEnabled(false);
        //noinspection deprecation
        applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

        mDlgContent.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mDlgContent.getText().length() == 0) {
                    applayButton.setEnabled(false);
                    //noinspection deprecation
                    applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                } else {
                    applayButton.setEnabled(true);
                    //noinspection deprecation
                    applayButton.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        cancelButton.setOnClickListener(cancelClickListener);
        applayButton.setOnClickListener(okClickListener);
        mDialog.show();
        AppUtil.showSoftInput(mDlgContent);
    }


    public void addAnnot(final int pageIndex, final CaretAnnotContent content, final Event.Callback result) {
        try {
            //step 1 add annot to pdf
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            RectF docRect = content.getBBox();
            final Caret caret = (Caret) page.addAnnot(Annot.e_annotCaret, docRect);
            if(caret == null) return;
            caret.setBorderColor(content.getColor());
            caret.setUniqueID(content.getNM());
            caret.setOpacity(content.getOpacity() / 255f);
            caret.setIntent(content.getIntent());
            caret.setSubject(content.getSubject());
            caret.setTitle(content.getAuthor());
            caret.setContent(content.getContents());
            caret.setCreationDateTime(content.getCreatedDate());
            caret.setModifiedDateTime(content.getModifiedDate());
            setCaretRotate(caret, content.getRotate());
            caret.resetAppearanceStream();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, caret);
            //step 2 invalidate page view
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF viewRect = caret.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                Rect rect = new Rect();
                viewRect.roundOut(rect);
                rect.inset(-10, -10);
                mPdfViewCtrl.refresh(pageIndex, rect);
            }

            //step 3 add replace strikeout
            if (!mIsInsertTextModule) {
                TextMarkupContentAbs strikeoutAbs = new TextMarkupContentAbs() {
                    @Override
                    public TextSelector getTextSelector() {
                        return content.getTextSelector();
                    }

                    @Override
                    public int getPageIndex() {
                        return pageIndex;
                    }

                    @Override
                    public int getType() {
                        return Annot.e_annotStrikeOut;
                    }

                    @Override
                    public String getIntent() {
                        return null;
                    }
                };

                AnnotHandler annotHandler = (ToolUtil.getAnnotHandlerByType(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()), Annot.e_annotStrikeOut));
                annotHandler.addAnnot(pageIndex, strikeoutAbs, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            Annot annot = event.annot;
                            if (annot == null || !(annot instanceof StrikeOut))
                                return;
                            StrikeOut strikeOut = (StrikeOut) annot;
                            try {
                                strikeOut.setIntent("StrikeOutTextEdit");
                                Markup[] groups = {caret, strikeOut};
                                page.setAnnotGroup(groups, 0);
                                strikeOut.setBorderColor(content.getColor());
                                strikeOut.resetAppearanceStream();

                            } catch (PDFException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
                if (result != null) {
                    result.result(null, true);
                }
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void addCaretAnnot(final int pageIndex, final RectF annotRect, final int rotate, final TextSelector textSelector, final Event.Callback result) {
        final DateTime dateTime = AppDmUtil.currentDateToDocumentDate();
        CaretAnnotContent caretAnnotContent = new CaretAnnotContent(mIsInsertTextModule) {
            @Override
            public int getRotate() {
                return rotate;
            }

            @Override
            public TextSelector getTextSelector() {
                return textSelector;
            }

            @Override
            public DateTime getCreatedDate() {
                return dateTime;
            }

            @Override
            public RectF getBBox() {
                return annotRect;
            }

            @Override
            public int getColor() {
                return mColor;
            }

            @Override
            public int getOpacity() {
                return AppDmUtil.opacity100To255(mOpacity);
            }


            @Override
            public DateTime getModifiedDate() {
                return dateTime;
            }

            @Override
            public String getContents() {
                return mDlgContent.getText().toString();
            }

        };

        addAnnot(pageIndex, caretAnnotContent, result);

    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {

        if (mSelectedPageIndex != pageIndex) return;
        if (mIsInsertTextModule) {
            if (mSelecting && mTextSelector != null && !(mCharSelectedRectF.left >= mCharSelectedRectF.right || mCharSelectedRectF.top <= mCharSelectedRectF.bottom)) {
                mPaint.setColor(calColorByMultiply(0x73C1E1, 150));
                Rect clipRect = canvas.getClipBounds();
                RectF tmp = new RectF(mTextSelector.getBbox());
                mPdfViewCtrl.convertPdfRectToPageViewRect(tmp, tmp, pageIndex);
                Rect r = new Rect();
                tmp.round(r);
                if (r.intersect(clipRect)) {
                    canvas.save();
                    canvas.drawRect(r, mPaint);

                    if (mTextSelector.getRectFList().size() > 0) {
                        RectF start = new RectF(mTextSelector.getRectFList().get(0));
                        RectF end = new RectF(mTextSelector.getRectFList().get(mTextSelector.getRectFList().size() - 1));
                        mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, pageIndex);

                        mPaint.setARGB(255, 76, 121, 164);
                        canvas.drawLine(start.left, start.top, start.left, start.bottom, mPaint);
                        canvas.drawLine(end.right, end.top, end.right, end.bottom, mPaint);
                    }

                    canvas.restore();
                }
            }
        } else {
            if (mSelecting && mTextSelector != null && mTextSelector.getStart() >= 0) {
                mPaint.setColor(calColorByMultiply(0x73C1E1, 150));
                Rect clipRect = canvas.getClipBounds();
                for (RectF rect : mTextSelector.getRectFList()) {
                    RectF tmp = new RectF(rect);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tmp, tmp, pageIndex);
                    Rect r = new Rect();
                    tmp.round(r);
                    if (r.intersect(clipRect)) {
                        canvas.save();
                        canvas.drawRect(r, mPaint);
                        canvas.restore();
                    }
                }
                if (mTextSelector.getRectFList().size() > 0) {
                    RectF start = new RectF(mTextSelector.getRectFList().get(0));
                    RectF end = new RectF(mTextSelector.getRectFList().get(mTextSelector.getRectFList().size() - 1));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, pageIndex);

                    mPaint.setARGB(255, 76, 121, 164);
                    canvas.drawLine(start.left, start.top, start.left, start.bottom, mPaint);
                    canvas.drawLine(end.right, end.top, end.right, end.bottom, mPaint);
                }
            }
        }

    }

    private final RectF mTmpRect = new RectF();

    private void invalidateTouch(int pageIndex, TextSelector textSelector) {

        if (textSelector == null) return;
        RectF rectF = new RectF();
        rectF.set(textSelector.getBbox());
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    private RectF calculate(RectF desRectF, RectF srcRectF) {
        RectF mTmpDesRect = new RectF();
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        mTmpDesRect.set(desRectF);
        if (count == 2) {
            mTmpDesRect.union(srcRectF);
            RectF rectF = new RectF();
            rectF.set(mTmpDesRect);
            mTmpDesRect.intersect(srcRectF);
            rectF.intersect(mTmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return mTmpDesRect;
        } else {
            mTmpDesRect.union(srcRectF);
            return mTmpDesRect;
        }
    }

    private void getInvalidateRect(Rect rect) {
        rect.top -= 20;
        rect.bottom += 20;
        rect.left -= 20 / 2;
        rect.right += 20 / 2;
        rect.inset(-20, -20);
    }

    private void clearSelectedRectF() {
        mTextSelector.clear();
        mCharSelectedRectF.setEmpty();
    }

    private int calColorByMultiply(int color, int opacity) {
        int rColor = color | 0xFF000000;
        int r = (rColor & 0xFF0000) >> 16;
        int g = (rColor & 0xFF00) >> 8;
        int b = (rColor & 0xFF);
        float rOpacity = opacity / 255.0f;
        r = (int) (r * rOpacity + 255 * (1 - rOpacity));
        g = (int) (g * rOpacity + 255 * (1 - rOpacity));
        b = (int) (b * rOpacity + 255 * (1 - rOpacity));
        rColor = (rColor & 0xFF000000) | (r << 16) | (g << 8) | (b);
        return rColor;
    }

    public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mAnnotButton.setEnable(false);
        } else {
            mAnnotButton.setEnable(true);
        }
    }

    public void onColorValueChanged(int color) {
        this.mColor = color;
        mToolCirclItem.setCentreCircleColor(mColor);
    }

    public void onOpacityValueChanged(int opacity) {
        this.mOpacity = opacity;
    }

}
