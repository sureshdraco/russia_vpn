package com.kernel5.dotvpn;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class InformativePreference extends Preference {

	public InformativePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public CharSequence getSummary() {
		return this.getPersistedString("...");
	}

}