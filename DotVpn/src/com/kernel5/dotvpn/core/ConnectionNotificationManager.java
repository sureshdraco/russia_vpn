package com.kernel5.dotvpn.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.kernel5.dotvpn.ConnectActivity;
import com.kernel5.dotvpn.DotVpn;
import com.kernel5.dotvpn.R;
import com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus;

public class ConnectionNotificationManager {
	public static void handleVpnConnectionStatus(Context context, int resId) {
		switch (resId) {
		case R.string.state_connecting:
		case R.string.state_wait:
		case R.string.state_auth:
		case R.string.state_get_config:
		case R.string.state_assign_ip:
		case R.string.state_add_routes:
		case R.string.state_reconnecting:
		case R.string.state_tcp_connect:
		case R.string.state_resolve:
			showNotification(context, R.drawable.connecting,
					context.getString(R.string.state_connecting));
			break;
		case R.string.state_connected:
			showNotification(context, R.drawable.online,
					context.getString(R.string.state_connected));
			break;
		case R.string.state_auth_failed:
		case R.string.state_conn_error:
		case R.string.state_noprocess:
		case R.string.state_nonetwork:
		case R.string.state_exiting:
		case R.string.unknown_state:
			showNotification(context, R.drawable.offline,
					context.getString(R.string.state_disconnected));
			break;
		}
	}

	public static void showNotification(Context context, int icon, String status) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// Sets an ID for the notification, so it can be updated
		int notifyID = 1;
		NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
				context)
				.setContentTitle("DotVPN")
				.setContentText(status)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent.getActivity(context, 0, new Intent(
								context, DotVpn.class), 0)).setSmallIcon(icon);

		// Because the ID remains unchanged, the existing notification is
		// updated.
		mNotificationManager.notify(notifyID, mNotifyBuilder.build());
	}
}
