package com.kernel5.dotvpn;

import com.kernel5.dotvpn.core.ConnectionNotificationManager;
import com.kernel5.dotvpn.core.LaunchVPN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {

	private SharedPreferences mPrefs;

	// Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
	@Override
	public void onReceive(Context context, Intent intent) {

		// read user preferences
		this.mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		final String action = intent.getAction();

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

			boolean auto_connect = this.mPrefs.getBoolean(
					context.getString(R.string.SettingsKeyAutoConnect), false);
			ConnectionNotificationManager.showNotification(context,
					R.drawable.offline,
					context.getString(R.string.state_disconnected));
			if (auto_connect) {
				// start the vpn connection
				Log.v(Constants.TAG, "Starting vpn connection");
				Intent startVpnIntent = new Intent(Intent.ACTION_MAIN);
				startVpnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startVpnIntent.setClass(context, LaunchVPN.class);
				context.startActivity(startVpnIntent);
			}
		}
	}
}