package com.kernel5.dotvpn;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.preference.PreferenceManager;
import android.view.View;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.kernel5.dotvpn.rest.InfoRequest;
import com.kernel5.dotvpn.rest.InfoResponse;
import com.kernel5.dotvpn.rest.JacksonRequests;

public class SettingsActivity extends Activity {

	private static final String TAG = Constants.TAG;

	protected String mPassword = null;
	protected String mUserIp = null;
	protected String mPing = null;

	private LogoutBroadcastReceiver logout_listener;

	protected SharedPreferences mPrefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_view);

		this.logout_listener = new LogoutBroadcastReceiver();

		// read user preferences
		this.mPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		// this.getApplicationContext().getSharedPreferences(Constants.AppName,MODE_WORLD_READABLE);

		// get user infos ( and original ip )
		Runnable mGetUser = new Runnable() {
			@Override
			public void run() {

				try {

					String mUsername = SettingsActivity.this.mPrefs.getString(
							getString(R.string.SettingsKeyAccountName), "...");

					InfoRequest request = new InfoRequest();
					request.token = SettingsActivity.this.mPrefs.getString(
							Constants.OAUTH_TOKEN, "");
					InfoResponse response = JacksonRequests.postForJson(
							Constants.INFO_URL, request, InfoResponse.class);
					if (response.code == 0) {
						SettingsActivity.this.mUserIp = response.ip;
						final String[] pemail = response.email.split("@");
						if (pemail.length != 2) {
							ErrorUtils.showErrorDialog(SettingsActivity.this,
									getString(R.string.error),
									getString(R.string.wrong_email_address),
									getString(R.string.ok));
						}
						String epass = SettingsActivity.this.mPrefs.getString(
								Constants.BAUTH_TOKEN, "");
						String ckey = pemail[0] + "123456789101112";
						ckey = ckey.substring(0, 16);
						DESEncrypt decoder = new DESEncrypt(ckey);
						SettingsActivity.this.mPassword = decoder
								.decrypt(epass);

						if (!mUsername.equals(response.email)) {

							SharedPreferences.Editor prefs_editor = SettingsActivity.this.mPrefs
									.edit();
							prefs_editor.putString(
									getString(R.string.SettingsKeyAccountName),
									response.email);
							prefs_editor.commit();
						}
					}

				} catch (Exception e) {
					Log.v(TAG, "Couldn't get user info", e);
				}
			}
		};
		Thread t = new Thread(mGetUser);
		String mUsername = SettingsActivity.this.mPrefs.getString(
				getString(R.string.SettingsKeyAccountName), "...");
		if (mUsername.equals("...")) {
			t.start();
		}
		Log.v(TAG, "Waiting user info ... ");
		try {
			t.join();
		} catch (Exception e) {
			Log.v(TAG, "Getting user threadus interruptus", e);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				this.logout_listener,
				new IntentFilter("com.kernel5.dotvpn.ACTION_LOGOUT"));
	}

	public void closeActivityAndReturn(View view) {
		// Intent intent = new Intent(SettingsActivity.this,
		// ConnectActivity.class);
		// startActivity( intent );
		finish();
	}

	public void openSigninActivity() {
		Intent intent = new Intent(SettingsActivity.this, SigninActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				this.logout_listener);
		super.onPause();
	}

	protected class LogoutBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(context);

			SharedPreferences.Editor editor = settings.edit();
			editor.putString(Constants.OAUTH_TOKEN, "");
			editor.putString(Constants.BAUTH_TOKEN, "");
			editor.remove(getString(R.string.SettingsKeyAccountName));
			editor.putBoolean(getString(R.string.SettingsKeyAutoConnect), false);
			editor.commit();
			
			openSigninActivity();
		}

	}

}
