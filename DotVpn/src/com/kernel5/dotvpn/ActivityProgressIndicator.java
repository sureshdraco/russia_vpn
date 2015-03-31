package com.kernel5.dotvpn;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

public class ActivityProgressIndicator extends Dialog {

	public static final int ACTIVITY_PROGRESS_LOADER = 2;

	public ActivityProgressIndicator(Context context, int theme) {
		super(context, theme);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.empty_progress_fragment);
		this.setCancelable(false);
	}
}
