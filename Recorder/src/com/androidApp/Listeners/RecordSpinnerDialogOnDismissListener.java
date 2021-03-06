package com.androidApp.Listeners;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.EventRecorder.ListenerIntercept;
import com.androidApp.Test.ViewInterceptor;
import com.androidApp.Utility.Constants;
import com.androidApp.Utility.TestUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Spinner;

/**
 * recorder wrapper to intercept dialog dismiss events
 * @author mattrey
 * Copyright (c) 2013 Visible Automation LLC.  All Rights Reserved.
 */

public class RecordSpinnerDialogOnDismissListener extends RecordListener implements DialogInterface.OnDismissListener {
	protected DialogInterface.OnDismissListener mOriginalOnDismissListener;
	protected ViewInterceptor					mViewInterceptor;
	
	public RecordSpinnerDialogOnDismissListener() {
	}
	
	public RecordSpinnerDialogOnDismissListener(String activityName, EventRecorder eventRecorder, ViewInterceptor viewInterceptor, DialogInterface.OnDismissListener originalDismissListener) {
		super(activityName, eventRecorder);
		mOriginalOnDismissListener = originalDismissListener;
		mViewInterceptor = viewInterceptor;
	}
		
	public Object getOriginalListener() {
		return mOriginalOnDismissListener;
	}

	public void onDismiss(DialogInterface dialog) {
		boolean fReentryBlock = getReentryBlock();
		if (!RecordListener.getEventBlock()) {
			setEventBlock(true);
			try {
				
				// clear out the current dialog, so it gets intercepted if it pops up again.
				mViewInterceptor.setCurrentDialog(null);
				String description = getDescription(dialog);
				if (mViewInterceptor.getLastKeyAction() == KeyEvent.KEYCODE_BACK) {
					mEventRecorder.writeRecord(mActivityName, Constants.EventTags.DISMISS_SPINNER_DIALOG_BACK_KEY, description);
					mViewInterceptor.setLastKeyAction(-1);
				} else {
					mEventRecorder.writeRecord(mActivityName, Constants.EventTags.DISMISS_SPINNER_DIALOG, description);
				}
			} catch (Exception ex) {
				mEventRecorder.writeException(mActivityName, ex, "on dismiss dialog");
			}
		}
		if (!fReentryBlock) {
			if (mOriginalOnDismissListener != null) {
				mOriginalOnDismissListener.onDismiss(dialog);
			} 
		}
		setEventBlock(false);
	}
}
