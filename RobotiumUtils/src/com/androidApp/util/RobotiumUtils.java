package com.androidApp.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.jayway.android.robotium.solo.Solo;

import junit.framework.TestCase;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;

/**
 * utility class for monitoring activities and sending events to views.
 * TODO: we need to change the wait routines so they're instantiated before the events that they depend on are fired.
 * All the wait objects need to be created on a background thread, which will fire a callback when the waitObject is 
 * notified.  The API thread will have an external wait call, which will return immediately if the callback has been 
 * fired, or wait for a notification if it hasn't been fired yet. This will improve the performance of the tests, and their 
 * roboustness.
 * @author mattrey
 * Copyright (c) 2013 Visible Automation LLC.  All Rights Reserved.
 *
 */
public class RobotiumUtils {
	private static final String TAG = "RobotiumUtils";
	protected static int DIALOG_SYNC_TIME = 100;									// interval for dialog polling
	protected static int ACTIVITY_POLL_INTERVAL_MSEC = 1000;						// interval for activity existence polling
	protected static int VIEW_TIMEOUT_MSEC = 5000;									// time to wait for view to be visible
	protected static int VIEW_POLL_INTERVAL_MSEC = 1000;							// poll interval for view existence
	protected static int WAIT_INCREMENT_MSEC = 100;									// poll interval for wait timers.
	protected static int WAIT_SCROLL_MSEC = 2000;									// wait at most this long for a scroll to complete
	protected static int TEXT_FOCUS_TIMEOUT_MSEC = 5000;							// time to wait for text focus (obviously should be much shorter than this
	protected static int DIALOG_GETVIEW_RETRY_COUNT = 10;							// how many times do we try to get a view in dialog?
	protected static int GETVIEW_RETRY_COUNT = 100;									// how many times in view.
	protected static ActivityMonitorRunnable	sActivityMonitorRunnable = null;	// so we can "expect" activity transitions
	protected Instrumentation					mInstrumentation;					// instrumentation handle
	/**
	 * This has to be called before getActivity(), so it can intercept the first activity.
	 * @param instrumentation
	 */
	public RobotiumUtils(Class<? extends ActivityInstrumentationTestCase2> testClass, Instrumentation instrumentation) throws IOException {
		SaveState.restoreDatabases(instrumentation.getTargetContext(), testClass.getSimpleName());
		SaveState.restoreLocalFiles(instrumentation.getTargetContext(), testClass.getSimpleName());
		SaveState.restorePreferences(instrumentation.getTargetContext(), testClass.getSimpleName());
		mInstrumentation = instrumentation;
		sActivityMonitorRunnable = new ActivityMonitorRunnable(instrumentation);
		Thread activityMonitorThread = new Thread(sActivityMonitorRunnable);
		activityMonitorThread.start();
	}
		
	// get a list view item.  
	public static View getAdapterViewItem(AdapterView av, int itemIndex) {
		int firstPosition = av.getFirstVisiblePosition();
		int visibleIndex = itemIndex - firstPosition;
		View itemView = av.getChildAt(visibleIndex);
		if (itemView == null) {
			Log.i(TAG, "getAdapterViewItem failed itemIndex = " + itemIndex + 
					   " first position = " + firstPosition + " childCount = " + av.getChildCount());
		}
		return itemView;
	}
	
	// wrapper for sleep
	static void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException iex) {
			
		}
	}
	
	/**
	 * wait for the adapter view, then wait for the specified view, then click on it.
	 * @param solo robotium handle
	 * @param adapterViewIndex index of the list view in the array of list views in the android layout
	 * @param itemIndex absolute index of the item 
	 * @return list of TextViews in the list
	 */
	public boolean waitAndClickInAdapterByClassIndex(Solo solo, int adapterViewIndex, int itemIndex) {

		boolean foundView = solo.waitForView(android.widget.AbsListView.class, adapterViewIndex + 1, VIEW_TIMEOUT_MSEC);
		if (!foundView) {
			Log.e(TAG, "waitAndClickInAdapterByClassIndex solo.waitForView failed");
			return false;
		}
		android.widget.AbsListView absListView = (android.widget.AbsListView) solo.getView(android.widget.AbsListView.class, adapterViewIndex);
		if (waitForView(absListView, VIEW_TIMEOUT_MSEC)) {
			Log.e(TAG, "waitAndClickInAdapterByClassIndex robotiumUtils.waitForView succeeded");
			return waitAndClickInAdapter(solo, absListView, itemIndex);
		} else {
			Log.i(TAG, "failed to wait for adapter view index = " + adapterViewIndex);
			return false;
		}
	}
	
	/**
	 * wait for the adapter, then wait for the specified view, then click on it.
	 * @param solo robotium handle
	 * @param adapterViewIndex index of the list view in the array of list views in the android layout
	 * @param itemIndex absolute index of the item 
	 * @return true if selected, false if timeout
	 */
	public boolean waitAndClickInAdapterById(Solo solo, int adapterViewId, int itemIndex) {
		android.widget.AbsListView absListView = null;
		int waitMsec = VIEW_TIMEOUT_MSEC;
		while ((absListView == null) && (waitMsec > 0)) {
			absListView = (android.widget.AbsListView) solo.getView(adapterViewId);
			if (absListView != null) {
				TestCase.assertTrue(solo.waitForView(absListView));
				break;
			}
			RobotiumUtils.sleep(VIEW_POLL_INTERVAL_MSEC);
			waitMsec -= VIEW_POLL_INTERVAL_MSEC;
		}
		if (waitForView(absListView, VIEW_TIMEOUT_MSEC)) {
			return waitAndClickInAdapter(solo, absListView, itemIndex);
		} else {
			Log.i(TAG, "failed to wait for adapter id = " + adapterViewId);
			return false;
		}
	}
	
	// click on an adapter item, which isn't as simple as you might think
	// 1. scroll for the view to become visible
	// 2. scroll until the item is visible
	// 3. wait for it to actually become visible
	// 4. click on the item.
	protected boolean waitAndClickInAdapter(Solo solo, AbsListView absListView, int itemIndex) {
		scrollToViewVisible(absListView);
		android.view.View adapterViewItem = null; 
		
		// TODO: for some reason skype scrolls one past.  Need to check if this is consistent
		// between applications
		int scrollIndex = itemIndex;
		if (scrollIndex > 0) {
			scrollIndex--;
		}
		mInstrumentation.runOnMainSync(new ScrollListRunnable(absListView, scrollIndex));
		if (waitForAdapterViewItem(solo, absListView, itemIndex, VIEW_TIMEOUT_MSEC)) {
			adapterViewItem = RobotiumUtils.getAdapterViewItem(absListView, itemIndex);
			solo.clickOnView(adapterViewItem);
			Log.i(TAG, "successfully clicked on list view item " + itemIndex);
			return true;
		} else {
			Log.i(TAG, "failed to click list view item " + itemIndex);
			return false;
		}
	}	
	
	/**
	 * when we scroll a list, the scroll request has to be run on the UI thread.
	 * @author matt2
	 *
	 */
	public class ScrollListRunnable implements Runnable {
		protected AbsListView 	mAbsListView;
		protected int			mItemIndex;
		
		public ScrollListRunnable(AbsListView absListView, int itemIndex) {
			mAbsListView = absListView;
			mItemIndex = itemIndex;
		}
		
		public void run() {
			//mAbsListView.smoothScrollToPosition(mItemIndex);
			mAbsListView.setSelection(mItemIndex);
		}
	}
	
	/**
	 * wait for a specified string to appear in an edit text (usually from setText). We need this because
	 * enter text key-by-key specifies an insert point, and 
	 * @param editText edit text view to wait on
	 * @param stringToWaitFor string to wait for
	 * @param waitMsec timeout to wait
	 * @return true if the text appeared
	 */
	public boolean waitForText(EditText editText, String stringToWaitFor, int waitMsec) {
		while (waitMsec > 0) {
			String text = editText.getText().toString();
			if (text.equals(stringToWaitFor)) {
				return true;
			}
			RobotiumUtils.sleep(VIEW_POLL_INTERVAL_MSEC);
			waitMsec -= VIEW_POLL_INTERVAL_MSEC;
		}
		return false;
	}
	
	/**
	 * wait for a adapter view item to become visible.
	 * @param adpaterView list view containing item
	 * @param itemIndex index of item
	 * @param waitMsec max wait time in milliseconds
	 * @return
	 */
	public boolean waitForAdapterViewItem(Solo solo, AdapterView adapterView, int itemIndex, int waitMsec) {
		android.view.View adapterViewItem = null; 
		while ((adapterViewItem == null) && (waitMsec > 0)) {
			adapterViewItem = RobotiumUtils.getAdapterViewItem(adapterView, itemIndex);
			if (adapterViewItem != null) {
				if (solo.waitForView(adapterViewItem)) {
					return true;
				}
			} 
			RobotiumUtils.sleep(VIEW_POLL_INTERVAL_MSEC);
			waitMsec -= VIEW_POLL_INTERVAL_MSEC;
		}	
		return false;
	}

	/**
	 * sometimes, the application will navigate from one activity to another, but the activities have the same class.
	 * robotium only supports waitForActivity(className), which can fire on the current activity due to a race condition
	 * before the operation which navigates to a new activity, we save the activity, and test against it with the new
	 * activity. This was written to handle a special case in ApiDemos, where new activities had the same class as the
	 * current activity
	 * @param currentActivity activity before the event
	 * @param newActivityClass class of the new activity (which should match currentActivity.class, but we pass it anyway
	 * @param timeoutMsec timeout in millisecond
	 * @return true if a new activity of the specified class has been created
	 */
	public static boolean waitForNewActivity(Activity currentActivity, Class<? extends Activity> newActivityClass, long timeoutMsec) {
		long startTimeMillis = SystemClock.uptimeMillis();
		long currentTimeMillis = startTimeMillis;
		do {
			Activity newActivity = sActivityMonitorRunnable.waitForActivity(newActivityClass, ActivityMonitorRunnable.MINISLEEP);
			Log.d(TAG, "trying " + currentActivity + " against " + newActivity);
			if ((newActivity != currentActivity) || (currentActivity == null)) {
				return true;
			}
			ActivityMonitorRunnable.sleep(ActivityMonitorRunnable.MINISLEEP);
			currentTimeMillis = SystemClock.uptimeMillis();
		} while (currentTimeMillis - startTimeMillis < timeoutMsec);
		Log.e(TAG, "wait for new activity failed " + newActivityClass.getName());
		return false;
	}
	
	/**
	 * public function to wait for an activity from the activity monitor background thread.
	 * @param newActivityClass activity class to match.
	 * @param timeoutMsec timeout in milliseconds
	 * @return true if matching activity was found, false otherwise.
	 */
	public static boolean waitForActivity(Class<? extends Activity> newActivityClass, long timeoutMsec) {
		return sActivityMonitorRunnable.waitForActivity(newActivityClass, timeoutMsec) != null;
	}

	/**
	 * public function to return the current activity from the activity monitor background thread.
	 * @return current activity
	 */
	public static Activity getCurrentActivity() {
		return sActivityMonitorRunnable.getCurrentActivity();
	}
	
	/**
	 * public function to return the previous activity from the activity monitor background thread
	 * @return previos activity
	 */
	public static Activity getPreviousActivity() {
		return sActivityMonitorRunnable.getPreviousActivity();
	}
	
	/**
	 * this is an inadequate test for a scrolling container.  We need to also test whether it can be
	 * scrolled by the content size.
	 * 
	 * @param v
	 * @return
	 */
	public static boolean isScrollView(View v) {
		if ((v instanceof ScrollView) || (v instanceof Gallery) || (v instanceof AdapterView)) {
			return true;
		}
		// HorizontalScrolView may not be defined in earlier android APIs, so we have to extract it via
		// reflection.
		try {
			Class<? extends View> cls = (Class<? extends View>) Class.forName(Constants.Classes.HORIZONTAL_SCROLL_VIEW);
			if (cls.isAssignableFrom(v.getClass())) {
				return true;
			}
		} catch (Exception ex) {		
		}
		
		if (v.isHorizontalScrollBarEnabled() || v.isVerticalScrollBarEnabled()) {
			return true;
		}
		return false;
	}

	/**
	 * get the leastmost scrolling container for this view: NOTE: this may not be the container we're looking for,
	 * since the view may be off the screen horizontally, and the container may only scroll vertically.
	 * @param v
	 * @return
	 */
	public static View getScrollingContainer(View v) {
		ViewParent vp = v.getParent();
		while (vp != null) {
			if (vp instanceof View) {
				if (isScrollView((View) vp)) {
					return (View) vp;
				}
			}
			vp = vp.getParent();
			if ((vp == null) || (vp.getParent() == null)) {
				return null;
			}
		}
		return null;
	}
	
	/** 
	 * return the rectangle of a view relative to its ancestor
	 */
	public static Rect getRelativeRect(View svAncestor, View view) {
		Rect r = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
		ViewParent vp = view.getParent();
		while (vp != svAncestor) {
			r.offset(((View) vp).getLeft(), ((View) vp).getTop());
			vp = vp.getParent();
		}
		
		// the ancestor's scroll position is taken into account for the left and top of the child
		//r.offset(-svAncestor.getScrollX(), -svAncestor.getScrollY());
		return r;
	}
	
	/**
	 * call on the activity ui thread to scroll a view until it's visible, so we can click or do something to it
	 */
	public class ScrollViewRunnable implements Runnable {
		protected View mView;
		protected int mXScroll;
		protected int mYScroll;
		
		public ScrollViewRunnable(View view, int xScroll, int yScroll) {
			mView = view;
			mXScroll = xScroll;
			mYScroll = yScroll;
		}
		public void run() {
			mView.scrollTo(mXScroll, mYScroll);
		}
	}

	/**
	 * scroll the ancestor of the view until the view is fully visible.
	 * NOTE: this doesn't handle embedded scroll views.
	 * @param v view to scroll into view.
	 */
	public void scrollToViewVisible(View v) {
		View scrollingContainer = RobotiumUtils.getScrollingContainer(v);
		
		if (scrollingContainer != null) {
			scrollToViewVisible(scrollingContainer, v);
		}
	}
	
	/**
	 * get the bounds of a view's children, or its bounds if it has no children
	 * @param v
	 * @return
	 */
	public static Rect getChildBounds(View v) {
		Rect rect = new Rect();
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			View vChild = vg.getChildAt(0);
			rect.left = vChild.getLeft();
			rect.top = vChild.getTop();
			rect.right = vChild.getRight();
			rect.bottom = vChild.getBottom();
			for (int i = 1; i < vg.getChildCount(); i++) {
				vChild = vg.getChildAt(i);
				rect.union(vChild.getLeft(), vChild.getTop());
				rect.union(vChild.getRight(), vChild.getBottom());
			}
		} else {
			rect.left = 0;
			rect.top = 0;
			rect.right = v.getWidth();
			rect.top = v.getHeight();
		}
		return rect;
	}
	
	// recursive subfunction
	// TODO: bound the scroll to the bounds.
	protected void scrollToViewVisible(View scrollingContainer, View v) {
		
		// recursively scroll the parent to make it visible.
		View scrollingContainerContainer = RobotiumUtils.getScrollingContainer(scrollingContainer);
		if (scrollingContainerContainer != null) {
			scrollToViewVisible(scrollingContainerContainer, v);
		}
		Rect r = RobotiumUtils.getRelativeRect(scrollingContainer, v);
		int scrollVertical = 0;
		int scrollHorizontal = 0;
		// try to align the child centered on its ancestor, not just top-to-top or bottom-to-bottom.
		int centerVerticalOffset = scrollingContainer.getHeight()/2 - (r.bottom - r.top)/2;
		int centerHorizontalOffset = scrollingContainer.getWidth()/2 - (r.right - r.left)/2;
		if (r.top < 0) {
			scrollVertical = r.top - centerVerticalOffset;
		} else if (r.bottom > scrollingContainer.getHeight()) {
			scrollVertical = r.bottom - scrollingContainer.getHeight() + centerVerticalOffset;
		}
		if (r.left < 0) {
			scrollHorizontal = r.left - centerHorizontalOffset;
		} else if (r.right > scrollingContainer.getWidth()) {
			scrollHorizontal = r.right - scrollingContainer.getWidth() + centerHorizontalOffset;
		}
		// the scrolling container offsets its children's by the scroll amount, but obviously, it cannot
		// scroll past the children's bounds
		Rect childRect = getChildBounds(scrollingContainer);
		if (scrollVertical < childRect.top){
			scrollVertical = 0;
		}
		if (scrollVertical > childRect.bottom - scrollingContainer.getBottom()) {
			scrollVertical = childRect.bottom - scrollingContainer.getBottom();
		}
		if (scrollHorizontal < childRect.left) {
			scrollHorizontal = 0;
		}
		if (scrollHorizontal > childRect.right - scrollingContainer.getRight()) {
			scrollHorizontal = childRect.right - scrollingContainer.getRight();
		}
		
		int originalScrollX = scrollingContainer.getScrollX();
		int originalScrollY = scrollingContainer.getScrollY();
		if ((scrollVertical != originalScrollX) || (scrollHorizontal != originalScrollY)) {
			Runnable runnable = new ScrollViewRunnable(scrollingContainer, scrollHorizontal, scrollVertical);
			mInstrumentation.runOnMainSync(runnable);
			int timeout = WAIT_SCROLL_MSEC;
			while (timeout > 0) {
				int currentScrollX = scrollingContainer.getScrollX();
				int currentScrollY = scrollingContainer.getScrollY();
				if ((currentScrollX == originalScrollX + scrollHorizontal) && (currentScrollY == originalScrollY + scrollVertical)) {
					break;
				}
				sleep(WAIT_INCREMENT_MSEC);
				timeout -= WAIT_INCREMENT_MSEC;
			}
		}
	}
	
	/**
	 * unfortunately, views are often scrolled off the screen, so we often need to scroll them back to 
	 * click on them.
	 * @param solo handle to robotium
	 * @param v view to click on
	 */
	public boolean clickOnViewAndScrollIfNeeded(Solo solo, View v) {
		scrollToViewVisible(v);
		solo.clickOnView(v, true);
		// TEMPORARY
		sleep(WAIT_INCREMENT_MSEC);
		return true;
	}
	
	/**
	 * unfortunately, views are often scrolled off the screen, so we often need to scroll them back to 
	 * click on them.
	 * @param solo handle to robotium
	 * @param v view to click on
	 */
	public boolean longClickOnViewAndScrollIfNeeded(Solo solo, View v) {
		scrollToViewVisible(v);
		solo.clickLongOnView(v);
		// TEMPORARY
		sleep(WAIT_INCREMENT_MSEC);
		return true;
	}
	
	/** 
	 * sending the back key while the IME is up hides the IME, rather than exiting the activity, so
	 * things have a tendency to get out of sync.  We detect show and hide IME events and perform
	 * them explicitly
	 * @param v view that the IME is being shown for.
	 */
	public static void showIME(View v) {
		if (v != null) {
	        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	        imm.showSoftInput(v, 0);
	        // displaying and hiding the input method manager causes a re-layout that we need to wait for, since later events
	        // may get sent with view dimensions before the layout, but after the layout has occurred.
	        sActivityMonitorRunnable.getCurrentLayoutListener().waitForLayout(VIEW_TIMEOUT_MSEC);
		}
	}
	
	/**
	 * see comment above.
	 * @param v
	 */
	public static void hideIME(View v) {
		if (v != null) {
	        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	        sActivityMonitorRunnable.getCurrentLayoutListener().waitForLayout(VIEW_TIMEOUT_MSEC);
		}
	}

	/**
	 * some applications (read SKYPE) listen to the back key and hide the IME explicitly, so we force the IME up
	 * (which does nothing if it's up already), then send the back key.
	 * @param v
	 */
	public  void hideIMEWithBackKey(View v) {
		showIME(v);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);          
	}
	
	/**
	 * wait for a layout
	 * @param timeoutMsec - time to wait for a layout.
	 * 
	 */
	public static void waitForLayout(long timeoutMsec) {
		OnLayoutInterceptListener currentLayoutListener = sActivityMonitorRunnable.getCurrentLayoutListener();
		currentLayoutListener.waitForLayout(timeoutMsec);
	}

	/**
	 * create a runnable which dismisses the auto complete dropdown
	 * @param solo
	 * @param autoCompleteTextView
	 */
	public void dismissAutoCompleteDialog(AutoCompleteTextView autoCompleteTextView) {
		mInstrumentation.runOnMainSync(new DismissAutoCompleteRunnable(autoCompleteTextView));
	}
	
	public class DismissAutoCompleteRunnable implements Runnable {
		public AutoCompleteTextView mAutoCompleteTextView;
		
		public DismissAutoCompleteRunnable(AutoCompleteTextView autoCompleteTextView) {
			mAutoCompleteTextView = autoCompleteTextView;
		}

		@Override
		public void run() {
			mAutoCompleteTextView.dismissDropDown();		
		}		
	}
	
	/**
	 * dismissing a popup has to be done on the UI thread via a runnable
	 * @author Matthew
	 *
	 */
	public class DismissPopupWindowRunnable implements Runnable {
		public PopupWindow mPopupWindow;
		
		public DismissPopupWindowRunnable(PopupWindow popupWindow) {
			mPopupWindow = popupWindow;
		}
		
		@Override
		public void run() {
			mPopupWindow.dismiss();
		}
	}
	
	public void dismissPopupWindow(Activity activity) throws TestException {
		dismissPopupWindow(activity, VIEW_TIMEOUT_MSEC);
	}
	
	public boolean waitForPopupWindow(Activity activity, long waitMsec) {
		while (waitMsec > 0) {
			PopupWindow popupWindow = ViewExtractor.findPopupWindow(activity);
			if (popupWindow != null) {
				return true;
			}
			try {
				Thread.sleep(WAIT_INCREMENT_MSEC);
			} catch (InterruptedException iex) {}
			waitMsec -= WAIT_INCREMENT_MSEC;
		}
		return false;
	}
	/**
	 * dismiss a popup window, like a menu popup, autocomplete dropdown, etc.  I hope there is only one popup.
	 * @param activity
	 */
	public boolean dismissPopupWindow(Activity activity, long waitMsec) throws TestException {
		while (waitMsec > 0) {
			PopupWindow popupWindow = ViewExtractor.findPopupWindow(activity);
			if (popupWindow != null) {
				mInstrumentation.runOnMainSync(new DismissPopupWindowRunnable(popupWindow));
				return true;
			}
			try {
				Thread.sleep(WAIT_INCREMENT_MSEC);
			} catch (InterruptedException iex) {}
			waitMsec -= WAIT_INCREMENT_MSEC;
		}
		return false;
	}
	
	public static boolean verifyPopupWindowDimissed(Activity activity, PopupWindow dismissedPopupWindow) throws TestException {
		return verifyPopupWindowDismissed(activity, dismissedPopupWindow, VIEW_TIMEOUT_MSEC);
	}

	/**
	 * verify that there are no popups displayed.
	 * NOTE: What if another popup is displayed immediately?
	 * @param activity
	 * @param waitMsec
	 * @return
	 * @throws TestException
	 */
	public static boolean verifyPopupWindowDismissed(Activity activity, PopupWindow dismissedPopupWindow, long waitMsec) {
		while (waitMsec > 0) {
			PopupWindow popupWindow = ViewExtractor.findPopupWindow(activity);
			if ((popupWindow == null) || (dismissedPopupWindow != popupWindow)) {
				return true;
			}
			try {
				Thread.sleep(WAIT_INCREMENT_MSEC);
			} catch (InterruptedException iex) {}
			waitMsec -= WAIT_INCREMENT_MSEC;
		}
		return false;
	}
	
	/**
	 * select the indexed tab on the action bar
	 * @param activity activity to get the action bar from
	 * @param tabIndex index of the tab to select
	 * @throws TestException if there is no action bar to select a tab from
	 */
	public void selectActionBarTab(Activity activity, int tabIndex) throws TestException {
		ActionBar actionBar = activity.getActionBar();
		if (actionBar == null) {
			throw new TestException("selectActionBarTab: activity has no action bar");
		}
		mInstrumentation.runOnMainSync(new SetActionBarTabRunnable(actionBar, tabIndex));
	}
	
	/**
	 * runnable to select an action bar tab.
	 * @author Matthew
	 *
	 */
	public class SetActionBarTabRunnable implements Runnable {
		public ActionBar 	mActionBar;
		public int 			mIndex;
		
		public SetActionBarTabRunnable(ActionBar actionBar, int index) {
			mActionBar = actionBar;
			mIndex = index;
		}
		
		public void run() {
			ActionBar.Tab tab = mActionBar.getTabAt(mIndex);
			tab.select();
		}
	}
	
	/**
	 * select the specified tab.
	 * @param tabHost
	 * @param tabId
	 */
	public void selectTab(TabHost tabHost, String tabId) {
		mInstrumentation.runOnMainSync(new SetTabRunnable(tabHost, tabId));
	}
	
	/**
	 * runnable to select an action bar tab.
	 * @author Matthew
	 *
	 */
	public class SetTabRunnable implements Runnable {
		public TabHost 	mTabHost;
		public String 	mTabId;
		
		public SetTabRunnable(TabHost tabHost, String tabId) {
			mTabHost = tabHost;
			mTabId = tabId;
		}
		
		public void run() {
			mTabHost.setCurrentTabByTag(mTabId);
		}
	}

	/**
	 * click a group in an expandable list
	 * @param expandableListView
	 * @param position
	 */
	public void clickGroup(ExpandableListView expandableListView, int position) {
		mInstrumentation.runOnMainSync(new GroupClickRunnable(expandableListView, position));
	}
	
	/**
	 * runnable to select a group in an expandable list
	 */
		
	public class GroupClickRunnable implements Runnable {
		public ExpandableListView 	mExpandableListView;
		public int					mPosition;
		
		public GroupClickRunnable(ExpandableListView expandableListView, int position) {
			mExpandableListView = expandableListView;
			mPosition = position;
		}
		
		public void run() {
			View v = RobotiumUtils.getAdapterViewItem(mExpandableListView, mPosition);	
			Adapter adapter = mExpandableListView.getAdapter();
			long id = adapter.getItemId(mPosition);
			mExpandableListView.performItemClick(v, mPosition, id);
		}
	}

	/**
	 * click a child in an expandable list
	 * @param expandableListView
	 * @param position
	 */
	public void clickChild(ExpandableListView expandableListView, int position) {
		mInstrumentation.runOnMainSync(new ChildClickRunnable(expandableListView, position));
	}
	
	public class ChildClickRunnable implements Runnable {
		public ExpandableListView 	mExpandableListView;
		public int					mPosition;
		
		public ChildClickRunnable(ExpandableListView expandableListView, int position) {
			mExpandableListView = expandableListView;
			mPosition = position;
		}
		
		public void run() {
			View v = RobotiumUtils.getAdapterViewItem(mExpandableListView, mPosition);	
			Adapter adapter = mExpandableListView.getAdapter();
			long id = adapter.getItemId(mPosition);
			mExpandableListView.performItemClick(v, mPosition, id);
		}
	}
	
	
	/**
	 * extract the WebViewClient from WebView.mCallbackProxy.mWebViewClient
	 * @param webView
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	protected static WebViewClient getWebViewClient(WebView webView) throws NoSuchFieldException, SecurityException, IllegalAccessException, ClassNotFoundException {
		Class callbackProxyClass = Class.forName(Constants.Classes.WEBKIT_CALLBACK_PROXY);
		Object callbackProxy = ReflectionUtils.getFieldValue(webView, WebView.class, Constants.Fields.CALLBACK_PROXY);
		WebViewClient webViewClient = (WebViewClient) ReflectionUtils.getFieldValue(callbackProxy, callbackProxyClass, Constants.Fields.WEBVIEW_CLIENT);
		return webViewClient;
	}	
	
	public void waitForPageToLoad(WebView webView, String url, long timeoutMsec) throws TestException {
		try {
			Activity a = (Activity) webView.getContext();
			WaitRunnable waitRunnable = new WaitRunnable(a, new WaitForPageToLoadRunnable(webView, url, timeoutMsec));
			waitRunnable.waitForCompletion(timeoutMsec);
		} catch (Exception ex) {
			throw new TestException(ex.getMessage());
		}
	}
	
	public class WaitForPageToLoadRunnable implements Runnable {
		protected WebView 	mWebView;
		protected String	mUrl;
		protected long		mTimeoutMsec;
		
		public WaitForPageToLoadRunnable(WebView webView, String url, long timeoutMsec) {
			mWebView = webView;
			mUrl = url;
			mTimeoutMsec = timeoutMsec;
		}
		
		public void run() {
			try {
				// need to do the check here, because webviews complain about getting progress unless you get it from the UI thread.
				if (mWebView.getProgress() < 100) {
					WebViewClient originalWebViewClient = RobotiumUtils.getWebViewClient(mWebView);
					if (!(originalWebViewClient instanceof InterceptWebViewClient)) {
						InterceptWebViewClient interceptWebViewClient = new InterceptWebViewClient(originalWebViewClient);
						mWebView.setWebViewClient(interceptWebViewClient);
						interceptWebViewClient.waitForPageLoad(mUrl, mTimeoutMsec);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}			
		}
	}
	
	/** 
	 * wait for a dialog to appear.
	 * @param activity
	 * @param timeoutMsec
	 * @return
	 */
	public static Dialog waitForDialogToOpen(Activity activity, long timeoutMsec) {
		while (timeoutMsec > 0) {
			Dialog dialog = ViewExtractor.findDialog(activity);
			if (dialog != null) {
				// TODO: this is a cheat. We need to wait for the dialog's contents to be
				// rendered properly
				sleep(WAIT_INCREMENT_MSEC);
				return dialog;
			}
			sleep(WAIT_INCREMENT_MSEC);
			timeoutMsec -= WAIT_INCREMENT_MSEC;
		}
		return null;
	}
	
	/**
	 * wait for a dialog to close, by not finding a dialog, because the solo.waitForDialogToClose() (LAME)
	 * just checks to see if the number of windows have changed, and the dialog may already have closed
	 * so we wait for the timeout.
	 * @param activity
	 * @param timeoutMsec
	 * @return
	 */
	public static boolean waitForDialogToClose(Activity activity, long timeoutMsec) {
		while (timeoutMsec > 0) {
			Dialog dialog = ViewExtractor.findDialog(activity);
			if (dialog == null) {
				return true;
			}
			sleep(WAIT_INCREMENT_MSEC);
			timeoutMsec -= WAIT_INCREMENT_MSEC;
		}
		return false;
	}
	
	/**
	 * unfortunatey, KeyCharacterMap returns null for backspace, so we have to improvise.  Chances are
	 * that there are other codes that don't work as well, so we'll have to find them as we go along
	 * TODO: pre-allocate the keyCharactermap for performance
	 * @param s
	 * @return
	 * @throws TestException
	 */
	public int[] getKeyEventCodes(String s) throws TestException {
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
		int[] keyEventCodes = new int[s.length()];
		char[] charvec = new char[1];
		for (int ich = 0; ich < s.length(); ich++) {
			if (s.charAt(ich) == '\b') {
				keyEventCodes[ich] = KeyEvent.KEYCODE_DEL;
			} else {
				charvec[0] = s.charAt(ich);
				KeyEvent keyEventsForKey[] = keyCharacterMap.getEvents(charvec);
				if (keyEventsForKey == null) {
					throw new TestException("failed to find keycod mapping for character" + charvec[0]);
				}
				keyEventCodes[ich] = keyEventsForKey[0].getKeyCode();
			}
		}
		return keyEventCodes;
	}
	
	/**
	 * find an item of the scroll view which contains a text view with a matching string and select it
	 * @param solo
	 * @param absListView
	 * @param text
	 * @return
	 */
	public boolean selectItemByText(Solo solo, AbsListView absListView, String text) {
		for (int i = 0; i < absListView.getChildCount(); i++) {
			View adapterViewItem = absListView.getChildAt(i);
			if (findText(adapterViewItem, text) != null) {
				solo.clickOnView(adapterViewItem);
				return true;
			}			
		}
		return false;
	}
	
	/**
	 * find text in the view hierarchy
	 * @param v view to search against.
	 * @param s string to search for
	 * @return view containing text or null
	 */
	public View findText(View v, String s) {
		if (v instanceof TextView) {
			TextView tv = (TextView) v;
			if (tv.getText().toString().equals(s)) {
				return v;
			}
		} else if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				View vChild = vg.getChildAt(i);
				View vFound = findText(vChild, s);
				if (vFound != null) {
					return vFound;
				}
			}
			
		}
		return null;
	}
	
	
	/**
	 * enter text key by key.  Much more reliable than just setting text directly
	 * TODO: check that \b maps to KEYCODE_DEL
	 * @param editText  edit text to send characters to 
	 * @param insert  insertion point
	 * @param text text to insert
	 */
	public void enterText(EditText editText, int insert, String text) throws TestException {
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
		int[] keyEventCodes = getKeyEventCodes(text);
		// set the focus to the view, then set the requested insert start point
		Activity a = (Activity) editText.getContext();
		WaitRunnable waitRunnable = new WaitRunnable(a, new SetTextInsertionRunnable(editText, insert));
		waitRunnable.waitForCompletion(TEXT_FOCUS_TIMEOUT_MSEC);
		// send the actual key events.
		for (int keyEventCode : keyEventCodes) {
			mInstrumentation.sendKeyDownUpSync(keyEventCode);	
		}
	}
	
	/**
	 * because Robotium selects spinner items RELATIVE to the currently selected item in the spinner,
	 * which is retarded.
	 * @param solo
	 * @param spinnerIndex
	 * @param item
	 */
	public void pressSpinnerItem(Solo solo, int spinnerIndex, int itemIndex) {
		Spinner spinner = solo.getView(Spinner.class, spinnerIndex);
		int currentPosition = spinner.getSelectedItemPosition();
		solo.pressSpinnerItem(spinnerIndex, itemIndex - currentPosition);
	}
	
	/**
	 * workaround for clicking in dialogs because coordinates are reported incorrectly, so we
	 * can't do clickInView
	 * https://code.google.com/p/robotium/issues/detail?id=431
	 * @param v
	 */
	public void performClickWorkaround(View v) {
		mInstrumentation.runOnMainSync(new PerformClickRunnable(v));
	}
	
	protected  void getAllViews(View v, Class<? extends View> viewClass, List<View> viewList) {
		if (viewClass.isAssignableFrom(v.getClass())) {
			viewList.add(v);
		} else if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				View vChild = vg.getChildAt(i);
				getAllViews(vChild, viewClass, viewList);
			}
		}
	}
	
	public List<View> getAllViews(View v, Class<? extends View> viewClass) {
		List<View> arrayList = new ArrayList<View>();
		getAllViews(v, viewClass, arrayList);
		return arrayList;
	}
	
	/**
	 * fortunately, Instrumentation.invokeMenuActionSync() returns a boolean flag on success, so we test
	 * it in a wait timeout loop
	 * TODO: Unfortunately, if the menu item doesn't have a callback, or doesn't invoke a submenu, then this 
	 * fails (i.e. if the menu item does nothing). 
	 * TODO: test with submenus. 
	 * @param menuItemId
	 * @param timeoutMsec
	 * @return
	 */
	public boolean invokeMenuAction(int menuItemId, long timeoutMsec) {
		boolean fSuccess = false;
		while ((timeoutMsec > 0) && !fSuccess) {
			fSuccess = mInstrumentation.invokeMenuActionSync(getCurrentActivity(), menuItemId, 0);
			if (!fSuccess) {
				sleep(WAIT_INCREMENT_MSEC);
				timeoutMsec -= WAIT_INCREMENT_MSEC;
			}
		}
		return fSuccess;
	}
	/**
	 * unlike robotium, which only counts visible views, we like all the views, because the
	 * playback may not have scrolled back to exactly the same position, so we search the view
	 * hierarchy for all views, not just those which are visible on the screen (they filter by offscreen/obscured)
	 * @param viewClass view class to filter by
	 * @param index index to match
	 * @return view or null
	 */
	public View getView(Class<? extends View> viewClass, int index) {
		Activity activity = getCurrentActivity();
		
		// try dialogs first. TODO: wait is wrong.
		View[] viewList = ViewExtractor.getWindowDecorViews();
		for (int iViewRoot = 0; iViewRoot < viewList.length; iViewRoot++) {
			if (DialogListener.isDialogOrPopup(activity, viewList[iViewRoot])) {
				for (int iTry = 0; iTry < DIALOG_GETVIEW_RETRY_COUNT; iTry++) {
					View v = getView(viewList[iViewRoot], viewClass, new ClassCount(index));
					if (v != null) {
						return v;
					}
					sleep(WAIT_INCREMENT_MSEC);
				}
			}
		}
		Window w = activity.getWindow();
		View v = w.getDecorView();
		for (int iTry = 0; iTry < GETVIEW_RETRY_COUNT; iTry++) {
			View vResult = getView(v, viewClass, new ClassCount(index));
			if (vResult != null) {
				return vResult;
			} 
			sleep(WAIT_INCREMENT_MSEC);
		}
		return null;
	}
	
	protected class ClassCount {
		public int mIndex;
		
		public ClassCount(int index) {
			mIndex = index;
		}
	}
	
	// pre-order traversal to search for a matching view with the classCount wrapper for the index.
	protected View getView(View v, Class<? extends View> viewClass, ClassCount classCount) {
		if (viewClass.isAssignableFrom(v.getClass())) {
			if (classCount.mIndex == 0) {
				return v;
			} else {
				classCount.mIndex--;
			}
		}  
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int iChild = 0; iChild < vg.getChildCount(); iChild++) {
				View vChild = vg.getChildAt(iChild);
				View vFound = getView(vChild, viewClass, classCount);
				if (vFound != null) {
					return vFound;
				}
			}
		}
		return null;
	}
	
	/**
	 * a REAL waitForView() that doesn't just wait for it to exist, but also not obscured
	 * @param v
	 * @param timeoutMsec
	 * @return
	 */
	public boolean waitForView(View v, long timeoutMsec) {
		return Waiter.waitForView(v, timeoutMsec);
	}
	
	public void requestFocus(View v, int startInsertion, int endInsertion) {
		mInstrumentation.runOnMainSync(new RequestFocusRunnable(v, startInsertion, endInsertion));
	}
	
		
	protected class RequestFocusRunnable implements Runnable {
		protected View 	mView;
		protected int 	mStartInsertion;
		protected int 	mEndInsertion;
		
		public RequestFocusRunnable(View view, int startInsertion, int endInsertion) {
			mView = view;
			mStartInsertion = startInsertion;
			mEndInsertion = endInsertion;
		}
		public RequestFocusRunnable(View view) {
			mView = view;
			mStartInsertion = -1;
			mEndInsertion = -1;
		}
		
		public void run() {
			mView.requestFocus();
			if ((mStartInsertion != -1) && (mEndInsertion != -1) && (mView instanceof EditText)) {
				EditText et = (EditText) mView;
				et.setSelection(mStartInsertion, mEndInsertion);
			}
		}
	}
	
	/**
	 * set the focus and insertion point of this text control
	 * @author matt2
	 *
	 */
	protected class SetTextInsertionRunnable implements Runnable {
		protected EditText	 mEditText;
		protected int		 mInsertionPt;
		
		public SetTextInsertionRunnable(EditText editText, int insertionPt) {
			mEditText = editText;
			mInsertionPt = insertionPt;
		}
		
		public void run() {
			mEditText.requestFocus();
			mEditText.setSelection(mInsertionPt);
		}
	}
	
	/**
	 * workaround for the fact that clickInView doesn't work for controls in dialogs.
	 * @author matt2
	 *
	 */
	protected class PerformClickRunnable implements Runnable {
		protected View		mView;
		
		public PerformClickRunnable(View view) {
			mView = view;
		}
		
		public void run() {
			mView.performClick();
		}
	}

}
