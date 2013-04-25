package com.androidApp.Intercept;

import android.view.View;
import android.view.Window;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.EventRecorder.ListenerIntercept;
import com.androidApp.Listeners.RecordWindowCallback;
import com.androidApp.Test.ViewInterceptor;
import com.androidApp.Utility.Constants;

public class InsertRecordWindowCallbackRunnable implements Runnable {
	protected Window			mWindow;
	protected EventRecorder 	mRecorder;
	protected ViewInterceptor	mViewInterceptor;
	
	public InsertRecordWindowCallbackRunnable(Window window, EventRecorder recorder, ViewInterceptor viewInterceptor) {
		mWindow = window;
		mRecorder = recorder;
		mViewInterceptor = viewInterceptor;
	}
	
	public void run() {
		try {
			Window.Callback originalCallback = mWindow.getCallback();
			if (!(originalCallback instanceof RecordWindowCallback)) {
				RecordWindowCallback recordCallback = new RecordWindowCallback(mRecorder, mViewInterceptor, originalCallback);
				mWindow.setCallback(recordCallback);
			}
		} catch (Exception ex) {
			mRecorder.writeRecord(Constants.EventTags.EXCEPTION, "installing window callback recorder");
		}
	}

}
