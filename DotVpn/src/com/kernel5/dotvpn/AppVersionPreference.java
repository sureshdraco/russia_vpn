package com.kernel5.dotvpn;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.Preference;
import android.util.AttributeSet;

public class AppVersionPreference extends Preference {

	public AppVersionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public CharSequence getSummary() {
		
		Context context = getContext();
		
		String summary = null;
		
		try {
			summary = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			summary = context.getString(R.string.tunknown);
		}
		
		return summary;
	}
}
