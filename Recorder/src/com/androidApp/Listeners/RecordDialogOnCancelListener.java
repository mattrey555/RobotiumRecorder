package com.androidApp.Listeners;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.EventRecorder.ListenerIntercept;
import com.androidApp.Utility.Constants;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.View;
import android.widget.Spinner;

/**
 * wrapper class to intercept and record dialog cancel events.
 * event tag is so we can write out specific dialog types like autocomplete and spinner
 * @author mattrey
 * Copyright (c) 2013 Visible Automation LLC.  All Rights Reserved.
 *
 */
public class RecordDialogOnCancelListener extends RecordListener implements DialogInterface.OnCancelListener, IOriginalListener {
	protected DialogInterface.OnCancelListener 	mOriginalOnCancelListener;
	
	public RecordDialogOnCancelListener() {
	}
	
	public RecordDialogOnCancelListener(String activityName, EventRecorder eventRecorder, DialogInterface.OnCancelListener originalCancelListener) {
		super(activityName, eventRecorder);
		mOriginalOnCancelListener = originalCancelListener;
	}
	
	
	public Object getOriginalListener() {
		return mOriginalOnCancelListener;
	}
	
	/**
	 * TODO: check how this happens, and see if we should check for the back key first
	 */
	public void onCancel(DialogInterface dialog) {
		boolean fReentryBlock = getReentryBlock();
		if (!RecordListener.getEventBlock()) {
			setEventBlock(true);
			try {
				String description = getDescription(dialog);
				mEventRecorder.writeRecord(mActivityName, Constants.EventTags.CANCEL_DIALOG, description);
			} catch (Exception ex) {
				mEventRecorder.writeException(mActivityName, ex, "on cancel dialog");
			} 
		}
		if (!fReentryBlock) {
			// always call the original on cancel listener.
			if (mOriginalOnCancelListener != null) {
				mOriginalOnCancelListener.onCancel(dialog);
			} 
		}
		setEventBlock(false);
	}
}
