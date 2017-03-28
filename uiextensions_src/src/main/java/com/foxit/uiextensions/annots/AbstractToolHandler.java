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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.CircleItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;

public abstract class AbstractToolHandler implements ToolHandler, PropertyBar.PropertyChangeListener {
	protected Context mContext;

	protected PropertyBar mPropertyBar;
	protected String mToolName;
	protected String mPropertyKey;
	
	protected int					mBtnTag;
	protected CircleItem            mMoreToolsBtn;
	protected CircleItem			mContinuousBtn;
	protected CircleItem			mOkBtn;
	protected PropertyCircleItem    mPropertyBtn;
	protected boolean				mIsContinuousCreate;
    
	protected int					mColor;
	protected int					mCustomColor;
	protected int					mOpacity;
	protected float					mThickness;
	protected String mDisplayName;
	protected ArrayList<Integer> mDisplayIcons;

	protected ViewGroup mParent;
	protected PDFViewCtrl mPdfViewCtrl;
	
	public AbstractToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, String name, String propKey) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mParent = parent;
//		mPropertyBar = mRead.getMainFrame().getPropertyBar();
		mToolName = name;
		mPropertyKey = propKey;
		mColor = Color.RED;
		mCustomColor = Color.RED;
		mOpacity = 100;
		mThickness = 5.0f;
		mDisplayIcons = new ArrayList<Integer>();
		
//		// more annotation tools button
//		mMoreToolsBtn = new CircleItemImpl(mContext) {
//			public void onItemLayout(int l, int t, int r, int b) {
//				updatePopupWindowPosition();
//			}
//		};
//		mMoreToolsBtn.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
//		mMoreToolsBtn.setImageResource(R.drawable.mt_more_selector);
//		mMoreToolsBtn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//                Rect rect = new Rect();
//                mMoreToolsBtn.getContentView().getGlobalVisibleRect(rect);
////                mRead.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
//			}
//		});
//
//        // preperty button
//        mPropertyBtn = new PropertyCircleItemImp(mContext) {
//        	@Override
//        	public void onItemLayout(int l, int t, int r, int b) {
//        		updatePopupWindowPosition();
//        	}
//        };
//        mPropertyBtn.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
//        mPropertyBtn.setCentreCircleColor(mColor);
//		mPropertyBtn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				showPropertyBar(PropertyBar.PROPERTY_COLOR);
//			}
//		});
//
//		// continuous create annot button
//        mContinuousBtn = new CircleItemImpl(mContext);
//        mContinuousBtn.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
//        mIsContinuousCreate = App.instance().getSetting().getIsAnnotContinuousCreate(getName(), false);
//        if (mIsContinuousCreate) {
//            mContinuousBtn.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
//        } else {
//            mContinuousBtn.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
//        }
//        mContinuousBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (AppUtil.isFastDoubleClick()) {
//                    return;
//                }
//                if (mIsContinuousCreate) {
//                    mIsContinuousCreate = false;
//                    App.instance().getSetting().setAnnotContinuousCreate(getName(), mIsContinuousCreate);
//                    mContinuousBtn.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
//                } else {
//                    mIsContinuousCreate = true;
//                    App.instance().getSetting().setAnnotContinuousCreate(getName(), mIsContinuousCreate);
//                    mContinuousBtn.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
//                }
////        		AppAnnotUtil.showAnnotContinueCreateToast(mIsContinuousCreate);
//            }
//        });
//
//        // end create annot button
//        mOkBtn = new CircleItemImpl(mContext);
//        mOkBtn.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
//        mOkBtn.setImageResource(R.drawable.rd_annot_create_ok_selector);
//        mOkBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mRead.changeState(ReadStateConfig.STATE_EDIT);
//                mRead.getHandlerMgr().setCurrentToolHandler(null);
//            }
//        });
	}


	public void setPropertyBar(PropertyBar propertyBar) {
		mPropertyBar = propertyBar;
	}

	public PropertyBar getPropertyBar() {
		return mPropertyBar;
	}

//	IMT_MoreClickListener mMoreClickListener = new IMT_MoreClickListener() {
//		@Override
//		public void onMTClick(int type) {
//			if (type == mBtnTag) {
//				if (mRead.getHandlerMgr().getCurrentToolHandler() != AbstractToolHandler.this) {
//					mRead.getHandlerMgr().setCurrentToolHandler(AbstractToolHandler.this);
//				} else {
//					//mRead.getHandlerMgr().setCurrentToolHandler(null);
//				}
//			}
//		}
//
//		@Override
//		public int getType() {
//			return mBtnTag;
//		}
//	};
	
	protected void addToolButton(final int tag) {
		mBtnTag = tag;
//		mRead.getMainFrame().getMoreToolsBar().registerListener(mMoreClickListener);
	}

	protected void removeToolButton() {
//		mRead.getMainFrame().getMoreToolsBar().unRegisterListener(mMoreClickListener);
	}
	
	public void updateToolButtonStatus() {
//		if (mDocViewer.getDocument() != null 
//				&& mDocViewer.getDocument().canAddAnnot()) {
//			mToolBtn.setEnable(true);
//		} else {
//			mToolBtn.setEnable(false);
//		}
//		if (mRead.getHandlerMgr().getCurrentToolHandler() == this) {
//			mToolBtn.setSelected(true);
//		} else {
//			mToolBtn.setSelected(false);
//		}
	}

	public int getCustomColor() {
		return mCustomColor;
	}

	public void setCustomColor(int color) {
		mCustomColor = color;
	}

	public int getColor() {
		return mColor;
	}

	public void setColor(int color) {
		if (mColor == color) return;
		mColor = color;
//		mPropertyBtn.setCentreCircleColor(mColor);
	}

	public int getOpacity() {
		return mOpacity;
	}

	public void setOpacity(int opacity) {
		if (mOpacity == opacity) return;
		mOpacity = opacity;
	}

	public float getThickness() {
		return mThickness;
	}

	public void setThickness(float thickness) {
		if (mThickness == thickness) return;
		mThickness = thickness;
	}

	public String getFontName() {
		return null;
	}

	public void setFontName(String name) {
	}

	public float getFontSize() {
		return 0;
	}

	public void setFontSize(float size) {
	}

	private ColorChangeListener mColorChangeListener = null;

	public void setColorChangeListener(ColorChangeListener listener) {
		mColorChangeListener = listener;
	}

	public interface ColorChangeListener {
		void onColorChange(int color);
	}

	@Override
    public void onValueChanged(long property, int value) {
		if (property == PropertyBar.PROPERTY_COLOR) {
			setColor(value);
			if (mColorChangeListener != null) {
				mColorChangeListener.onColorChange(value);
			}
		} else if (property == PropertyBar.PROPERTY_SELF_COLOR) {
			setCustomColor(value);
			setColor(value);
			if (mColorChangeListener != null) {
				mColorChangeListener.onColorChange(value);
			}
		} else if (property == PropertyBar.PROPERTY_OPACITY) {
			setOpacity(value);
		}
	}
	
	@Override
    public void onValueChanged(long property, float value) {
		if (property == PropertyBar.PROPERTY_LINEWIDTH) {
			setThickness(value);
		}
	}
	
	@Override
    public void onValueChanged(long property, String value) {
	}

	@Override
	public String getType() {
		return mToolName;
	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
		return false;
	}
	
//	void updatePopupWindowPosition() {
//		UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
//		if (uiExtensionsManager.getCurrentToolHandler() == AbstractToolHandler.this) {
//			if(App.instance().getRead().getMainFrame().getMoreToolsBar().isShowing()) {
//				Rect rect = new Rect();
//				mMoreToolsBtn.getContentView().getGlobalVisibleRect(rect);
//				App.instance().getRead().getMainFrame().getMoreToolsBar().update(new RectF(rect));
//			}
//		}
//		if (uiExtensionsManager.getCurrentToolHandler() == AbstractToolHandler.this) {
//			if (mPropertyBar.isShowing()) {
//				Rect rect = new Rect();
//				mPropertyBtn.getContentView().getGlobalVisibleRect(rect);
//				mPropertyBar.update(new RectF(rect));
//			}
//		}
//	}

//	@Override
//	public void onDraw(IDV_DocViewer docViewer, Canvas canvas) {
//
//	}
	
	public boolean onPrepareOptionsMenu() {
		if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
			return false;
		}
		return true;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
				return true;
			}
		}
		return false;
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		//if (mRead.getHandlerMgr().getCurrentToolHandler() == this) {
		//	mRead.getHandlerMgr().setCurrentToolHandler(null);
		//}
	}
	
	public void onStatusChanged(int oldState, int newState) {
		//if (mdStatus == mMdStatus || oldMdStatus == mMdStatus) {
			updateToolButtonStatus();
		//}
	}

	protected void showPropertyBar(long curProperty) {
		setPropertyBarProperties(mPropertyBar);
		mPropertyBar.setPropertyChangeListener(this);
		mPropertyBar.reset(getSupportedProperties());
		Rect rect = new Rect();
        mPropertyBtn.getContentView().getGlobalVisibleRect(rect);
        if (AppDisplay.getInstance(mContext).isPad()) {
        	mPropertyBar.show(new RectF(rect), true);
        } else {
        	mPropertyBar.show(new RectF(rect), true);
        }
	}
	
	protected void hidePropertyBar() {
		//if (mPropertyBar.getListener == this)
			mPropertyBar.setPropertyChangeListener(null);
		if (mPropertyBar.isShowing())
			mPropertyBar.dismiss();
	}
	
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
		propertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getOpacity());
		propertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
		if (AppDisplay.getInstance(mContext).isPad()) {
			propertyBar.setArrowVisible(true);
		} else {
			propertyBar.setArrowVisible(false);
		}
	}

	public boolean getIsContinuousCreate() {
		return mIsContinuousCreate;
	}

	public void setIsContinuousCreate(boolean isContinuousCreate) {
		this.mIsContinuousCreate = isContinuousCreate;
	}

	protected abstract void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint);
	public abstract long getSupportedProperties();

}
