package com.androidApp.EventRecorder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Message;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidApp.Utility.Constants;
import com.androidApp.Utility.ReflectionUtils;

/**
 * class which contains static functions used to obtain and set various listeners for android views, like
 * the onClickListener, onItemClickListener, etc).
 * @author Matthew
 *
 */
public class ListenerIntercept {
	protected static final String TAG = "ListenerIntercept";
	
	public static AdapterView.OnItemClickListener getPopupMenuOnItemClickListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Class dropdownListViewClass = Class.forName(Constants.Classes.DROPDOWN_LISTVIEW);
		Object menuPopupHelperObject = ReflectionUtils.getFieldValue(v, AdapterView.class, Constants.Fields.ONITEM_CLICK_LISTENER);
		Class menuPopupHelperClass = Class.forName(Constants.Classes.MENU_POPUP_HELPER);
		Object listPopupWindowObject = ReflectionUtils.getFieldValue(menuPopupHelperObject, menuPopupHelperClass, Constants.Fields.POPUP);
		Object listenerObject = ReflectionUtils.getFieldValue(listPopupWindowObject, ListPopupWindow.class, Constants.Fields.ITEM_CLICK_LISTENER);
		return (AdapterView.OnItemClickListener) listenerObject;
	}
	
	public static void setPopupMenuOnItemClickListener(View v, AdapterView.OnItemClickListener listener) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Class dropdownListViewClass = Class.forName(Constants.Classes.DROPDOWN_LISTVIEW);
		Object menuPopupHelperObject = ReflectionUtils.getFieldValue(v, AdapterView.class, Constants.Fields.ONITEM_CLICK_LISTENER);
		Class menuPopupHelperClass = Class.forName(Constants.Classes.MENU_POPUP_HELPER);
		Object listPopupWindowObject = ReflectionUtils.getFieldValue(menuPopupHelperObject, menuPopupHelperClass, Constants.Fields.POPUP);
		ReflectionUtils.setFieldValue(listPopupWindowObject, ListPopupWindow.class, Constants.Fields.ITEM_CLICK_LISTENER, listener);
	}
	
	public static boolean isPopupMenu(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Class dropdownListViewClass = Class.forName(Constants.Classes.DROPDOWN_LISTVIEW);
		Object clickListenerObject = ReflectionUtils.getFieldValue(v, AdapterView.class, Constants.Fields.ONITEM_CLICK_LISTENER);
		Class menuPopupHelperClass = Class.forName(Constants.Classes.MENU_POPUP_HELPER);
		return clickListenerObject.getClass() == menuPopupHelperClass;
	}
	
	public static PopupMenu.OnMenuItemClickListener getPopupMenuOnMenuItemClickListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object clickListenerObject = ReflectionUtils.getFieldValue(v, AdapterView.class, Constants.Fields.ONITEM_CLICK_LISTENER);
		Class menuPopupHelperClass = Class.forName(Constants.Classes.MENU_POPUP_HELPER);
		if (clickListenerObject.getClass() == menuPopupHelperClass) {
			PopupMenu popupMenu = (PopupMenu) ReflectionUtils.getFieldValue(clickListenerObject, menuPopupHelperClass, Constants.Fields.PRESENTER_CALLBACK);
			PopupMenu.OnMenuItemClickListener listener = (PopupMenu.OnMenuItemClickListener) ReflectionUtils.getFieldValue(popupMenu, PopupMenu.class, Constants.Fields.MENU_ITEM_CLICK_LISTENER);
			return listener;
		}
		return null;
	}
	
	public static void setPopupMenuOnMenuItemClickListener(View v, PopupMenu.OnMenuItemClickListener listener) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Class dropdownListViewClass = Class.forName(Constants.Classes.DROPDOWN_LISTVIEW);
		Object menuPopupHelperObject = ReflectionUtils.getFieldValue(v, AdapterView.class, Constants.Fields.ONITEM_CLICK_LISTENER);
		Class menuPopupHelperClass = Class.forName(Constants.Classes.MENU_POPUP_HELPER);
		PopupMenu popupMenu = (PopupMenu) ReflectionUtils.getFieldValue(menuPopupHelperObject, menuPopupHelperClass, Constants.Fields.PRESENTER_CALLBACK);
		popupMenu.setOnMenuItemClickListener(listener);
	}
	/**
	 * currently unused, because it overlaps onClickListener
	 * @param cb
	 * @return
	 * @throws NoSuchFieldExceptionc
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static CompoundButton.OnCheckedChangeListener getCheckedChangeListener(CompoundButton cb) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		return (CompoundButton.OnCheckedChangeListener) ReflectionUtils.getFieldValue(cb, CompoundButton.class, Constants.Fields.CHECKED_CHANGE_LISTENER);
	}

	/**
	 * retrieve the click listener for a view.
	 * @param v the view (isn't that the name of some tv show or something?)
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static View.OnClickListener getClickListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object listenerInfo = getListenerInfo(v);
		if (listenerInfo != null) {
			return (View.OnClickListener) getListenerInfoField(Constants.Fields.CLICK_LISTENER).get(listenerInfo);
		} else {
			return null;
		}
	}

	/**
	 * get the focus change listener for a view (usually a text view)
	 * @param v
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static View.OnFocusChangeListener getFocusChangeListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object listenerInfo = getListenerInfo(v);
		if (listenerInfo != null) {
			return (View.OnFocusChangeListener) getListenerInfoField(Constants.Fields.FOCUS_CHANGE_LISTENER).get(listenerInfo);
		} else {
			return null;
		}
	}

	/**
	 * get the key listener for a view. Does anyone use keyboards anymore?
	 * @param v
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static View.OnKeyListener getKeyListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object listenerInfo = getListenerInfo(v);
		if (listenerInfo != null) {
			return (View.OnKeyListener) getListenerInfoField(Constants.Fields.KEY_LISTENER).get(listenerInfo);
		} else {
			return null;
		}
	}

	/**
	 * get the mListenerInfo member of a view, which contains the onTouch, onKey and other listeners.
	 * @param v 
	 * @return mListenerInfo as an object (but it can be null if no listeners have been set.
	 * @throws NoSuchFieldException if mListenerInfo doesn't exist.
	 * @throws SecurityException if we can't set mListenerInfo
	 * @throws IllegalAccessException if we can't get mListenerInfo
	 */
	public static Object getListenerInfo(View v) throws NoSuchFieldException, SecurityException, IllegalAccessException {
		return ReflectionUtils.getFieldValue(v, View.class, Constants.Fields.LISTENER_INFO);
	}
	
	public static void setListenerInfo(View v, Object listenerInfoObject) throws NoSuchFieldException, SecurityException, IllegalAccessException {
		ReflectionUtils.setFieldValue(v, View.class, Constants.Fields.LISTENER_INFO, listenerInfoObject);
	}

	/**
	 * retrieve a named field from the mListenerInfo object.
	 * @param fieldName string name to retrieve via reflection.
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 */
	public static Field getListenerInfoField(String fieldName) throws NoSuchFieldException, ClassNotFoundException {
		Class listenerInfoClass = Class.forName(Constants.Classes.LISTENER_INFO);
		Field listenerField = listenerInfoClass.getDeclaredField(fieldName);
		listenerField.setAccessible(true);
		return listenerField;
	}

	/**
	 * get the long click listener for a view
	 * @param v the view with a long click listener
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static View.OnLongClickListener getLongClickListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object listenerInfo = getListenerInfo(v);
		if (listenerInfo != null) {
			return (View.OnLongClickListener) getListenerInfoField(Constants.Fields.LONG_CLICK_LISTENER).get(listenerInfo);
		} else {
			return null;
		}
	}

	/**
	 * get the scroll listener for a list view
	 * @param absListView
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static AbsListView.OnScrollListener getScrollListener(AbsListView absListView) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		return (AbsListView.OnScrollListener) ReflectionUtils.getFieldValue(absListView, AbsListView.class, Constants.Fields.SCROLL_LISTENER);
	}

	/**
	 * get the change listener for a seekbar
	 * @param seekbar
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static SeekBar.OnSeekBarChangeListener getSeekBarChangeListener(SeekBar seekbar) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		return (SeekBar.OnSeekBarChangeListener) ReflectionUtils.getFieldValue(seekbar, SeekBar.class, Constants.Fields.SEEKBAR_CHANGE_LISTENER);
	}
	
	/**
	 * get the onItemSelectedListener for an adapterView
	 * @param adapterView
	 * @return OnItemSelectedListener
	 * @throws NoSuchFieldException thrown if any of the reflection stuff fails
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static AdapterView.OnItemSelectedListener getItemSelectedListener(AdapterView adapterView) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		return (AbsListView.OnItemSelectedListener) ReflectionUtils.getFieldValue(adapterView, AdapterView.class, Constants.Fields.SELECTED_ITEM_LISTENER);
	}
	/**
	 * retrieve the list of text watchers for this view.
	 * @param tv TextView (though it's probably an edit text)
	 * @return list of text watchers or null if none have been set.
	 * @throws NoSuchFieldException if mListeners doesn't exist.
	 * @throws SecurityException if we can't set mListeners
	 * @throws IllegalAccessException if we can't get mListeners
	 */
	public static ArrayList<TextWatcher> getTextWatcherList(TextView tv) throws NoSuchFieldException, SecurityException, IllegalAccessException {
		return (ArrayList<TextWatcher>) ReflectionUtils.getFieldValue(tv, TextView.class, Constants.Fields.TEXT_WATCHER_LIST);
	}

	/**
	 * get the touch listener for a view.  This interferes with scroll listeners, lists, clicks, just
	 * about anything.
	 * @param v
	 * @return
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 */
	public static View.OnTouchListener getTouchListener(View v) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object listenerInfo = getListenerInfo(v);
		if (listenerInfo != null) {
			return (View.OnTouchListener) getListenerInfoField(Constants.Fields.TOUCH_LISTENER).get(listenerInfo);
		} else {
			return null;
		}
	}

	/**
	 * set the list of text watchers for this view (probably with our interceptor in pace
	 * @param tv TextView (though it's probably an edit text)
	 * @param textWatcherList list of text watchers to set.
	 * @return list of text watchers or null if none have been set.
	 * @throws NoSuchFieldException if mListeners doesn't exist.
	 * @throws SecurityException if we can't set mListeners
	 * @throws IllegalAccessException if we can't get mListeners
	 */
	public static void setTextWatcherList(TextView tv, ArrayList<TextWatcher> textWatcherList) throws NoSuchFieldException, SecurityException, IllegalAccessException {
		Field textWatcherListField = TextView.class.getDeclaredField(Constants.Fields.TEXT_WATCHER_LIST);
		textWatcherListField.setAccessible(true);
		textWatcherListField.set(tv, textWatcherList);
	}

	/**
	 * does the text watcher list for this edit text contain the interception text watcher?
	 * @param textWatcherList
	 * @return true if the interception/record text watcher is in the textWatcherList.
	 */
	public static boolean containsTextWatcher(ArrayList<TextWatcher> textWatcherList, Class targetClass) {
		for (TextWatcher tw : textWatcherList) {
			if (tw.getClass() == targetClass) {
				return true;
			}
		}
		return false;
	}

	/**
	 * dialog dismiss listener.
	 * @param dialog
	 * @return the dialog's dismiss listener or null, if none was set
	 * @throws NoSuchFieldException thrown if any of the reflection stuff fails
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 */
	public static DialogInterface.OnDismissListener getOnDismissListener(Dialog dialog) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message dismissMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_DISMISS_MESSAGE);
		if (dismissMessage != null) {
			return (DialogInterface.OnDismissListener) dismissMessage.obj;
		} else {
			return null;
		}
	}
	
	/**
	 * in dialog fragments, we can't call setOnDismissListener(), so we have to do it serruptiously
	 * java.lang.IllegalStateException: OnDismissListener is already taken by DialogFragment and can not be replaced.
	 */
	
	public static void setOnDismissListener(Dialog dialog, DialogInterface.OnDismissListener dismissListener) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message dismissMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_DISMISS_MESSAGE);
		if (dismissMessage != null) {
			dismissMessage.obj = dismissListener;
		} 
	}
	
	public static void setOnCancelListener(Dialog dialog, DialogInterface.OnCancelListener cancelListener) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message cancelMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_CANCEL_MESSAGE);
		if (cancelMessage != null) {
			cancelMessage.obj = cancelListener;
		} 
	}
	
	public static boolean isCancelAndDismissTaken(Dialog dialog) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		String sOwner = (String) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.CANCEL_AND_DISMISS_TAKEN);
		return sOwner != null;
	}

	/**
	 * popupWindow dismiss listener.
	 * @param dialog
	 * @return the dialog's dismiss listener or null, if none was set
	 * @throws NoSuchFieldException thrown if any of the reflection stuff fails
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 */
	public static PopupWindow.OnDismissListener getOnDismissListener(PopupWindow popupWindow) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		PopupWindow.OnDismissListener dismissListener = (PopupWindow.OnDismissListener) ReflectionUtils.getFieldValue(popupWindow, PopupWindow.class, Constants.Fields.POPUP_WINDOW_ON_DISMISS_LISTENER);
		return dismissListener;
	}


	/**
	 * dialog cancel listener.
	 * @param dialog
	 * @return the dialog's cancel listener or null, if none was set
	 * @throws NoSuchFieldException thrown if any of the reflection stuff fails
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 */
	public static DialogInterface.OnCancelListener getOnCancelListener(Dialog dialog) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message cancelMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_CANCEL_MESSAGE);
		if (cancelMessage != null) {
			return (DialogInterface.OnCancelListener) cancelMessage.obj;
		} else {
			return null;
		}
	}
	/**
	 * dialog key listener.
	 * @param dialog
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 */
	public static DialogInterface.OnClickListener getOnClickListener(Dialog dialog) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message clickMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_CLICK_MESSAGE);
		if (clickMessage != null) {
			return (DialogInterface.OnClickListener) clickMessage.obj;
		} else {
			return null;
		}
	}

	public static DialogInterface.OnShowListener getOnShowListener(Dialog dialog) throws NoSuchFieldException, SecurityException, IllegalAccessException  {
		Message showMessage = (Message) ReflectionUtils.getFieldValue(dialog, Dialog.class, Constants.Fields.DIALOG_SHOW_MESSAGE);
		if (showMessage != null) {
			return (DialogInterface.OnShowListener) showMessage.obj;
		} else {
			return null;
		}
	}
	
	/**
	 * given a PhoneWindow$DecorView, get the this$0 reference to its window, and return the Window.Callback so we can intercept it.
	 * @param v actually PhoneWindow$DecorView
	 * @return Window.callback for containing window
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static Window.Callback getWindowCallbackFromDecorView(View v) throws NoSuchFieldException, SecurityException, IllegalAccessException, ClassNotFoundException {
		Class phoneWindowDecorViewClass = Class.forName(Constants.Classes.PHONE_DECOR_VIEW);
		Window phoneWindow = (Window) ReflectionUtils.getFieldValue(v, phoneWindowDecorViewClass, Constants.Fields.ENCLOSING_CLASS);
		Window.Callback callback = phoneWindow.getCallback();
		return callback;
	}
	
	public static void setWindowCallbackToDecorView(View v, Window.Callback callback) throws NoSuchFieldException, SecurityException, IllegalAccessException, ClassNotFoundException {
		Class phoneWindowDecorViewClass = Class.forName(Constants.Classes.PHONE_DECOR_VIEW);
		Window phoneWindow = (Window) ReflectionUtils.getFieldValue(v, phoneWindowDecorViewClass, Constants.Fields.ENCLOSING_CLASS);
		phoneWindow.setCallback(callback);
	}
	
}
