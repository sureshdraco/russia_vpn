package com.kernel5.dotvpn;

import android.content.Context;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;

public class LogoutPreference extends Preference implements Preference.OnPreferenceClickListener {

	public LogoutPreference(Context context, AttributeSet attrs) {
	
		super(context, attrs);
		
		this.setOnPreferenceClickListener(this);
	}

	@Override
    public boolean onPreferenceClick(Preference preference) {
		
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(getIntent());
		
        return true;
    }
	
}
