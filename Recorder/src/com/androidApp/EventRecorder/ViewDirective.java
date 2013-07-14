package com.androidApp.EventRecorder;

import java.io.BufferedReader;
import java.util.List;

import com.androidApp.Utility.Constants;

/**
 * a view directive is a user-defined view reference, an operation and a value.
 * For example: class_index,TextView,1,copy,my_variable
 * @author matt2
 *
 */
public class ViewDirective {
	public enum ViewOperation {
		COPY_TEXT,				// <view_reference>,copy_text,<when>,variable_name
		PASTE_TEXT,				// <view_reference>,paste_text,<when>,variable_name
		CHECK,					// <view_reference>,check,<when>
		UNCHECK,				// <view_reference>,uncheck,<when>
		MOTION_EVENTS,			// <view_reference>,motion_events,on_activity_start
		IGNORE_EVENTS,			// <view_reference>,ignore_events,on_activity_start
		SELECT_BY_TEXT			// <view_reference>,select_by_text,on_activity_start
	}
	public enum When {
		ON_ACTIVITY_START,		// perform the operation when the activity starts
		ON_VALUE_CHANGE,		// perform the operation when the view value is changed
		ON_ACTIVITY_END,		// perform the operation when the activity exits
	}
	protected UserDefinedViewReference 	mViewReference;
	protected String					mVariable;
	protected ViewOperation				mOperation;
	protected When						mWhen;
	
	public ViewDirective(UserDefinedViewReference 	userDefinedReference,
						 ViewOperation				operation,
						 When						when,
						 String						variable) {
		mViewReference = userDefinedReference;
		mOperation = operation;
		mWhen = when;
		mVariable = variable;
	}
	
	/**
	 * parse a View Directive from a string
	 * @param s
	 * @throws ClassNotFoundException
	 * @throws ReferenceException
	 */
	public ViewDirective(String s) throws ClassNotFoundException, ReferenceException {
		mViewReference = new UserDefinedViewReference(s);
		String[] tokens = s.split("[:,]");
		int startToken = mViewReference.getTokenCount();
		mOperation = ViewDirective.operationFromString(tokens[startToken]);
		mWhen = ViewDirective.whenFromString(tokens[startToken + 1]);
		if (tokens.length > startToken + 2) {
			mVariable = tokens[startToken + 2];
		}
	}
	
	/**
	 * return the view directive in string form.
	 */
	public String toString() {
		try {
			String s = mViewReference.toString() + "," + operationToString(mOperation) + "," + whenToString(mWhen);
			if (mVariable != null) {
				s += "," + mVariable;
			}
			return s;
		} catch (ReferenceException rex) {
			return "bogus view directive";
		}
	}
	
	/**
	 * parse the operation of the directive from a string
	 * @param s
	 * @return
	 * @throws ReferenceException
	 */
	public static ViewOperation operationFromString(String s) throws ReferenceException {
		if (s.equals(Constants.EventTags.COPY_TEXT)) {
			return ViewOperation.COPY_TEXT;
		} else if (s.equals(Constants.EventTags.PASTE_TEXT)) {
			return ViewOperation.PASTE_TEXT;
		} else if (s.equals(Constants.EventTags.CHECK)) {
			return ViewOperation.CHECK;
		} else if (s.equals(Constants.EventTags.UNCHECK)) {
			return ViewOperation.UNCHECK;
		} else if (s.equals(Constants.EventTags.MOTION_EVENTS)) {
			return ViewOperation.MOTION_EVENTS;
		} else if (s.equals(Constants.EventTags.IGNORE_EVENTS)) {
			return ViewOperation.IGNORE_EVENTS;
		} else if (s.equals(Constants.EventTags.SELECT_BY_TEXT)) {
			return ViewOperation.SELECT_BY_TEXT;
		}else {
			throw new ReferenceException("unable to parse operation from " + s);
		}
	}
	
	public static String operationToString(ViewOperation operation) throws ReferenceException {
		switch (operation) {
		case COPY_TEXT:
			return Constants.EventTags.COPY_TEXT;
		case PASTE_TEXT:	
			return Constants.EventTags.PASTE_TEXT;
		case CHECK:	
			return Constants.EventTags.CHECK;
		case UNCHECK:
			return Constants.EventTags.UNCHECK;
		case MOTION_EVENTS:
			return Constants.EventTags.MOTION_EVENTS;
		case IGNORE_EVENTS:
			return Constants.EventTags.IGNORE_EVENTS;
		case SELECT_BY_TEXT:
			return Constants.EventTags.SELECT_BY_TEXT;
		default:
			throw new ReferenceException("view operation " + operation + " unknown");
		}
	}
	
	/**
	 * parse the "When" of the directive from a string
	 * @param s
	 * @return
	 * @throws ReferenceException
	 */
	public static When whenFromString(String s) throws ReferenceException {
		if (s.equals(Constants.When.ON_ACTIVITY_START)) {
			return When.ON_ACTIVITY_START;
		} else if (s.equals(Constants.When.ON_ACTIVITY_END)) {
			return When.ON_ACTIVITY_END;
		} else if (s.equals(Constants.When.ON_VALUE_CHANGE)) {
			return When.ON_VALUE_CHANGE;
		} else {
			throw new ReferenceException("unable to parse when from " + s);
		}
	}
	
	public static String whenToString(When when) throws ReferenceException {
		switch (when) {
		case ON_ACTIVITY_START:
			return Constants.When.ON_ACTIVITY_START;
		case ON_VALUE_CHANGE:	
			return Constants.When.ON_VALUE_CHANGE;
		case ON_ACTIVITY_END:
			return Constants.When.ON_ACTIVITY_END;
		default:
			throw new ReferenceException("when " + when + " unknown");
		}
	}
}
