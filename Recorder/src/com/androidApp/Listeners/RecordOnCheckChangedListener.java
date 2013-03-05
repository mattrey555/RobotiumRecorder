package com.androidApp.Listeners;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.EventRecorder.ListenerIntercept;
import com.androidApp.Utility.Constants;

import android.os.SystemClock;
import android.view.View;
import android.widget.CompoundButton;

// recorder for toggle buttons OnCheckedChangeListener (onClick really does this)
public class RecordOnCheckChangedListener extends RecordListener implements CompoundButton.OnCheckedChangeListener {
	protected CompoundButton.OnCheckedChangeListener 	mOriginalOnCheckedChangeListener;
	
	public RecordOnCheckChangedListener(EventRecorder eventRecorder, CompoundButton v) {
		super(eventRecorder);
		try {
			mOriginalOnCheckedChangeListener = ListenerIntercept.getCheckedChangeListener(v);
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
	}
	
	public RecordOnCheckChangedListener(EventRecorder eventRecorder, CompoundButton.OnCheckedChangeListener originalOnCheckedChangeListener) {
		super(eventRecorder);
		mOriginalOnCheckedChangeListener = originalOnCheckedChangeListener;
	}
	
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		long time = SystemClock.uptimeMillis();
		try {
			String logString = Constants.EventTags.CHECKED + ":" + time + "," + isChecked + "," + mEventRecorder.getViewReference().getReference(buttonView);
			mEventRecorder.writeRecord(logString);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (mOriginalOnCheckedChangeListener != null) {
			mOriginalOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
		} 
	}
}
