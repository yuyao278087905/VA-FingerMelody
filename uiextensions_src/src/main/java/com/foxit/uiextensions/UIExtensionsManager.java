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
package com.foxit.uiextensions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.caret.CaretModule;
import com.foxit.uiextensions.annots.circle.CircleModule;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.annots.freetext.typewriter.TypewriterModule;
import com.foxit.uiextensions.annots.ink.EraserModule;
import com.foxit.uiextensions.annots.ink.InkModule;
import com.foxit.uiextensions.annots.line.LineModule;
import com.foxit.uiextensions.annots.link.LinkModule;
import com.foxit.uiextensions.annots.note.NoteModule;
import com.foxit.uiextensions.annots.square.SquareModule;
import com.foxit.uiextensions.annots.stamp.StampModule;
import com.foxit.uiextensions.annots.textmarkup.highlight.HighlightModule;
import com.foxit.uiextensions.annots.textmarkup.squiggly.SquigglyModule;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutModule;
import com.foxit.uiextensions.annots.textmarkup.underline.UnderlineModule;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.textselect.TextSelectModule;
import com.foxit.uiextensions.textselect.TextSelectToolHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class <CODE>UIExtensionsManager</CODE> represents a UI extensions manager.
 * <p/>
 * The <CODE>UIExtensionsManager</CODE> class is mainly used for manage the UI extensions which implement {@link ToolHandler} interface, it implements the {@link PDFViewCtrl.UIExtensionsManager}
 * interface that is a listener to listen common interaction events and view event, and will dispatch some events to UI extensions, it also defines functions to manage the UI extensions.
 */
public class UIExtensionsManager implements PDFViewCtrl.UIExtensionsManager {

    /**
     * The interface of {@link ToolHandler} change listener.
     */
    public interface ToolHandlerChangedListener {
        /**
         * Called when current {@link ToolHandler} is changed.
         *
         * @param oldToolHandler
         *            The old tool handler.
         *
         * @param newToolHandler
         *            The new tool handler.
         */
        void onToolHandlerChanged(ToolHandler oldToolHandler, ToolHandler newToolHandler);
    }

    private ToolHandler mCurToolHandler = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private final List<Module> mModules = new ArrayList<Module>();
    private HashMap<String, ToolHandler> mToolHandlerList;
    private SparseArray<AnnotHandler> mAnnotHandlerList;
    private ArrayList<ToolHandlerChangedListener> mHandlerChangedListeners;

    private boolean mEnableLinkAnnot = true;
    private int mSelectHighlightColor = 0xFFACDAED;
    /**
     * Instantiates a new UI extensions manager.
     *
     * @param context
     *            A <CODE>Context</CODE> object which species the context.
     * @param parent
     *            An <CODE>ViewGroup</CODE> object which species the parent layout of <CODE>pdfViewCtrl</CODE> object.
     * @param pdfViewCtrl
     *            A <CODE>PDFViewCtrl</CODE> object which species the PDF view control.
     */
    public UIExtensionsManager(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl) {
        if (pdfViewCtrl == null) {
            throw new NullPointerException("PDF view control can't be null");
        }

        mPdfViewCtrl = pdfViewCtrl;
        mToolHandlerList = new HashMap<String, ToolHandler>(8);
        mAnnotHandlerList = new SparseArray<AnnotHandler>(8);
        mHandlerChangedListeners = new ArrayList<ToolHandlerChangedListener>();

        //text select module
        TextSelectModule tsModule = new TextSelectModule(context, parent, pdfViewCtrl);
        registerModule(tsModule);
        tsModule.loadModule();
        registerToolHandler(tsModule.getToolHandler());

        //squiggly annotation module
        SquigglyModule sqgModule = new SquigglyModule(context, parent, pdfViewCtrl);
        registerModule(sqgModule);
        sqgModule.loadModule();
        registerAnnotHandler(sqgModule.getAnnotHandler());
        registerToolHandler(sqgModule.getToolHandler());

        //strikeout annotation module
        StrikeoutModule stoModule = new StrikeoutModule(context, parent, pdfViewCtrl);
        registerModule(stoModule);
        stoModule.loadModule();
        registerAnnotHandler(stoModule.getAnnotHandler());
        registerToolHandler(stoModule.getToolHandler());

        //underline annotation module
        UnderlineModule unlModule = new UnderlineModule(context, parent, pdfViewCtrl);
        registerModule(unlModule);
        unlModule.loadModule();
        registerAnnotHandler(unlModule.getAnnotHandler());
        registerToolHandler(unlModule.getToolHandler());

        //highlight annotation module
        HighlightModule hltModule = new HighlightModule(context, parent, pdfViewCtrl);
        registerModule(hltModule);
        hltModule.loadModule();
        registerAnnotHandler(hltModule.getAnnotHandler());
        registerToolHandler(hltModule.getToolHandler());

        //note annotation module
        NoteModule noteModule = new NoteModule(context, parent, pdfViewCtrl);
        registerModule(noteModule);
        noteModule.loadModule();
        registerAnnotHandler(noteModule.getAnnotHandler());
        registerToolHandler(noteModule.getToolHandler());

        //link module
        LinkModule linkModule = new LinkModule(context, parent, pdfViewCtrl);
        linkModule.loadModule();
        registerAnnotHandler(linkModule.getAnnotHandler());

        //circle module
        CircleModule circleModule = new CircleModule(context, parent, pdfViewCtrl);
        registerModule(circleModule);
        circleModule.loadModule();
        registerAnnotHandler(circleModule.getAnnotHandler());
        registerToolHandler(circleModule.getToolHandler());

        //square module
        SquareModule squareModule = new SquareModule(context, parent, pdfViewCtrl);
        registerModule(squareModule);
        squareModule.loadModule();
        registerAnnotHandler(squareModule.getAnnotHandler());
        registerToolHandler(squareModule.getToolHandler());

        //freetext: typewriter
        TypewriterModule typewriterModule = new TypewriterModule(context, parent, pdfViewCtrl);
        registerModule(typewriterModule);
        typewriterModule.loadModule();
        registerAnnotHandler(typewriterModule.getAnnotHandler());
        registerToolHandler(typewriterModule.getToolHandler());

        //stamp module
        StampModule stampModule = new StampModule(context, parent, pdfViewCtrl);
        registerModule(stampModule);
        stampModule.loadModule();
        registerAnnotHandler(stampModule.getAnnotHandler());
        registerToolHandler(stampModule.getToolHandler());

        //Caret module
        CaretModule caretModule = new CaretModule(context, parent, pdfViewCtrl);
        registerModule(caretModule);
        caretModule.loadModule();
        registerAnnotHandler(caretModule.getAnnotHandler());
        registerToolHandler(caretModule.getISToolHandler());
        registerToolHandler(caretModule.getRPToolHandler());

        //eraser module
        EraserModule eraserModule = new EraserModule(context, parent, pdfViewCtrl);
        registerModule(eraserModule);
        eraserModule.loadModule();
        registerToolHandler(eraserModule.getToolHandler());

        //ink(pencil) module
        InkModule inkModule = new InkModule(context, parent, pdfViewCtrl);
        registerModule(inkModule);
        inkModule.loadModule();
        registerAnnotHandler(inkModule.getAnnotHandler());
        registerToolHandler(inkModule.getToolHandler());

        //Line module
        LineModule lineModule = new LineModule(context, parent, pdfViewCtrl);
        registerModule(lineModule);
        lineModule.loadModule();
        registerAnnotHandler(lineModule.getAnnotHandler());
        registerToolHandler(lineModule.getLineToolHandler());
        registerToolHandler(lineModule.getArrowToolHandler());

        //page navigation module
        PageNavigationModule pageNavigationModule = new PageNavigationModule(context, parent, pdfViewCtrl);
        pageNavigationModule.loadModule();

        //form annotation module
        FormFillerModule formFillerModule = new FormFillerModule(context, parent, pdfViewCtrl);
        registerModule(formFillerModule);
        formFillerModule.loadModule();
        registerToolHandler(formFillerModule.getToolHandler());
        registerAnnotHandler(formFillerModule.getAnnotHandler());

        pdfViewCtrl.registerRecoveryEventListener(new PDFViewCtrl.IRecoveryEventListener() {
            @Override
            public void onWillRecover() {
                DocumentManager.getInstance(pdfViewCtrl).mCurAnnot = null;
            }

            @Override
            public void onRecovered() {

            }
        });

        pdfViewCtrl.registerDoubleTapEventListener(mDoubleTapEventListener);
    }

    /**
     * Get the PDF view control.
     *
     * @return A <CODE>PDFViewCtrl</CODE> object which indicates the current PDF view control.<br>
     */
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Register the {@link ToolHandler} changed listener.
     *
     * @param listener
     *            A <CODE>ToolHandlerChangedListener</CODE> object which specifies the {@link ToolHandler} changed listener.
     */
    public void registerToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.add(listener);
    }

    /**
     * Unregister the {@link ToolHandler} changed listener.
     *
     * @param listener
     *            A <CODE>ToolHandlerChangedListener</CODE> object which specifies the {@link ToolHandler} changed listener.
     */
    public void unregisterToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.remove(listener);
    }

    private void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        for (ToolHandlerChangedListener listener : mHandlerChangedListeners) {
            listener.onToolHandlerChanged(lastTool, currentTool);
        }
    }

    /**
     * Set the current tool handler.
     *
     * @param toolHandler
     *            A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public void setCurrentToolHandler(ToolHandler toolHandler) {
        if (toolHandler == null && mCurToolHandler == null) {
            return;
        }

        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() ||
                (toolHandler != null && mCurToolHandler != null && mCurToolHandler.getType() == toolHandler.getType())) {
            return;
        }
        ToolHandler lastToolHandler = mCurToolHandler;
        if (lastToolHandler != null) {
            lastToolHandler.onDeactivate();
        }

        if (toolHandler != null) {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }
        }

        mCurToolHandler = toolHandler;
        if (mCurToolHandler != null) {
            mCurToolHandler.onActivate();
        }
        onToolHandlerChanged(lastToolHandler, mCurToolHandler);
    }

    /**
     * Get the current tool handler.
     *
     * @return A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public ToolHandler getCurrentToolHandler() {
        return mCurToolHandler;
    }

    /**
     * Register the specified tool handler to current UI extensions manager.
     *
     * @param  handler
     *              A <CODE>ToolHandler</CODE> object to be registered.
     */
    public void registerToolHandler(ToolHandler handler) {
        mToolHandlerList.put(handler.getType(), handler);
    }

    /**
     * Unregister the specified tool handler from current UI extensions manager.
     *
     * @param handler
     *              A <CODE>ToolHandler</CODE> object to be unregistered.
     */
    public void unregisterToolHandler(ToolHandler handler) {
        mToolHandlerList.remove(handler.getType());
    }

    /**
     * get the specified tool handler from current UI extensions manager.
     *
     * @param type The tool handler type, refer to function {@link ToolHandler#getType()}.
     * @return A <CODE>ToolHandler</CODE> object with specified type.
     */
    public ToolHandler getToolHandlerByType(String type) {
        return mToolHandlerList.get(type);
    }

    protected void registerAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.put(handler.getType(), handler);
    }

    protected void unregisterAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.remove(handler.getType());
    }

    protected AnnotHandler getCurrentAnnotHandler() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null) {
            return null;
        }

        return getAnnotHandlerByType(DocumentManager.getAnnotHandlerType(curAnnot));
    }

    protected AnnotHandler getAnnotHandlerByType(int type) {
        return mAnnotHandlerList.get(type);
    }

    /**
     * Register the specified module to current UI extensions manager.
     *
     * @param module
     *              A <CODE>Module</CODE> object to be registered.
     */
    public void registerModule(Module module) {
        mModules.add(module);
    }

    /**
     * Unregister the specified module from current UI extensions manager.
     *
     * @param module
     *              A <CODE>Module</CODE> object to be unregistered.
     */
    public void unregisterModule(Module module) {
        mModules.remove(module);
    }

    /**
     * Get the specified module from current UI extensions manager.
     *
     * @param name
     *              The specified module name, refer to {@link Module#getName()}.
     *
     * @return A <CODE>Module</CODE> object with specified module name.
     */
    public Module getModuleByName(String name) {
        for (Module module : mModules) {
            String moduleName = module.getName();
            if (moduleName != null && moduleName.compareTo(name) == 0)
                return module;
        }
        return null;
    }

    /**
     * Enable link annotation action event.
     *
     * @param enable
     *              True means link annotation action event can be triggered, false for else.
     */
    public void enableLinks(boolean enable) {
        mEnableLinkAnnot = enable;
    }

    /**
     * Check whether link annotation action event can be triggered.
     *
     * @return True means link annotation action event can be triggered, false for else.
     */
    public boolean isLinksEnabled() {
        return mEnableLinkAnnot;
    }

    /**
     * Set highlight color (including alpha) for text select tool handler.
     *
     * @param color
     *              The highlight color to be set.
     */
    public void setSelectionHighlightColor(int color) {
        mSelectHighlightColor = color;
    }

    /**
     * Get highlight color (including alpha) of text select tool handler.
     *
     * @return The highlight color.
     */
    public int getSelectionHighlightColor() {
        return mSelectHighlightColor;
    }

    /**
     * Get current selected text content from text select tool handler.
     *
     * @return The current selected text content.
     */
    public String getCurrentSelectedText() {
        ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
        if (selectionTool != null) {
            return ((TextSelectToolHandler) selectionTool).getCurrentSelectedText();
        }

        return null;
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() > 1) {
            return false;
        }

        if (mCurToolHandler != null) {
            if (mCurToolHandler.onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }
            return false;
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }

            //selection tool
            ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (selectionTool != null && selectionTool.onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return DocumentManager.getInstance(mPdfViewCtrl).shouldViewCtrlDraw(annot);
    }

    @Override
    public Annot getFocusAnnot() {
        return DocumentManager.getInstance(mPdfViewCtrl).getFocusAnnot();
    }

    @SuppressLint("WrongCall")
    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        for (ToolHandler handler : mToolHandlerList.values()) {
            handler.onDraw(pageIndex, canvas);
        }

        for (int i = 0; i < mAnnotHandlerList.size(); i++) {
            int type = mAnnotHandlerList.keyAt(i);
            AnnotHandler handler = mAnnotHandlerList.get(type);
            if (handler != null)
                handler.onDraw(pageIndex, canvas);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() > 1) {
            return false;
        }
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);


        if (mCurToolHandler != null) {
            if (mCurToolHandler.onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }
            return false;
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }

            //selection tool
            ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (selectionTool != null && selectionTool.onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() > 1) {
            return;
        }
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);

        if (mCurToolHandler != null) {
            if (mCurToolHandler.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onLongPress(pageIndex, motionEvent)) {
                return;
            }

            //selection tool
            ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (selectionTool != null && selectionTool.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        }
        return;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

    PDFViewCtrl.IDoubleTapEventListener mDoubleTapEventListener = new PDFViewCtrl.IDoubleTapEventListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return false;
        }
    };

}
