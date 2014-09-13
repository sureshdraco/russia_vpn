package com.kernel5.dotvpn.core;

import android.Manifest.permission;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.ApplicationInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.*;
import android.os.Handler.Callback;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kernel5.dotvpn.ConnectActivity;
import com.kernel5.dotvpn.R;
import com.kernel5.dotvpn.Constants;
import com.kernel5.dotvpn.TrafficSource;
import com.kernel5.dotvpn.VpnProfile;
import com.kernel5.dotvpn.core.OpenVPN.ByteCountListener;
import com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus;
import com.kernel5.dotvpn.core.OpenVPN.StateListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import static com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus.*;

public class OpenVpnService extends VpnService implements StateListener,
		Callback, ByteCountListener {

	private final String TAG = Constants.TAG;
	public static final String START_SERVICE = "com.kernel5.dotvpn.START_SERVICE";
	public static final String START_SERVICE_STICKY = "com.kernel5.dotvpn.START_SERVICE_STICKY";
	public static final String ALWAYS_SHOW_NOTIFICATION = "com.kernel5.dotvpn.NOTIFICATION_ALWAYS_VISIBLE";

	public static final String DISCONNECT_VPN = "com.kernel5.dotvpn.DISCONNECT_VPN";
	private static final String PAUSE_VPN = "com.kernel5.dotvpn.PAUSE_VPN";
	private static final String RESUME_VPN = "com.kernel5.dotvpn.RESUME_VPN";
	private Thread mProcessThread = null;
	private final Vector<String> mDnslist = new Vector<String>();
	private String mDomain = null;
	private final Vector<CIDRIP> mRoutes = new Vector<CIDRIP>();
	private final Vector<String> mRoutesv6 = new Vector<String>();

	private CIDRIP mLocalIP = null;

	private int mMtu;
	private String mLocalIPv6 = null;
	private DeviceStateReceiver mDeviceStateReceiver;

	private boolean mDisplayBytecount = false;
	private boolean mStarting = false;
	private long mConnecttime;
	private static final int OPENVPN_STATUS = 1;
	private static boolean mNotificationAlwaysVisible = false;
	private final IBinder mBinder = new LocalBinder();
	private boolean mOvpn3 = false;
	private TrafficSource mTrafficSource = null;

	private OpenVPNManagement mManagement;

	public class LocalBinder extends Binder {
		public OpenVpnService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return OpenVpnService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
		if (action != null && action.equals(START_SERVICE))
			return this.mBinder;
		else
			return super.onBind(intent);
	}

	@Override
	public void onRevoke() {
		this.mManagement.stopVPN();
		endVpnService(0);
	}

	// Similar to revoke but do not try to stop process
	public void processDied(int errorCode) {
		endVpnService(errorCode);
	}

	private void endVpnService(int errorCode) {
		this.mProcessThread = null;
		OpenVPN.removeByteCountListener(this);
		unregisterDeviceStateReceiver();
		ProfileManager.setConnectedVpnProfileDisconnected(this);
		if (!this.mStarting) {
			stopForeground(!mNotificationAlwaysVisible);

			if (!mNotificationAlwaysVisible) {
				stopSelf();
				OpenVPN.removeStateListener(this);
			}
		}
	}

	synchronized void registerDeviceStateReceiver(OpenVPNManagement magnagement) {
		// Registers BroadcastReceiver to track network connection changes.
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		this.mDeviceStateReceiver = new DeviceStateReceiver(magnagement);
		registerReceiver(this.mDeviceStateReceiver, filter);
		OpenVPN.addByteCountListener(this.mDeviceStateReceiver);
	}

	synchronized void unregisterDeviceStateReceiver() {
		if (this.mDeviceStateReceiver != null)
			try {
				OpenVPN.removeByteCountListener(this.mDeviceStateReceiver);
				this.unregisterReceiver(this.mDeviceStateReceiver);
			} catch (IllegalArgumentException iae) {
				// I don't know why this happens:
				// java.lang.IllegalArgumentException: Receiver not registered:
				// com.kernel5.dotvpn.NetworkSateReceiver@41a61a10
				// Ignore for now ...
				iae.printStackTrace();
			}
		this.mDeviceStateReceiver = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// retrieving server information from the settings //
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		String server_url = preferences.getString(
				getString(R.string.SettingsKeyServerUrl),
				Constants.SERVER_NAME_DE);
		String server_config_file = preferences.getString(
				getString(R.string.SettingsKeyServerConfigFileName),
				Constants.VPN_CONFIG_DE);
		// ///////////////////////////////////////////////////

		String[] argv = new String[3];
		argv[0] = getCacheDir().getAbsolutePath() + "/" + VpnProfile.MINIVPN;
		argv[1] = "--config";
		argv[2] = getCacheDir().getAbsolutePath() + "/" + server_config_file;

		ApplicationInfo info = this.getApplicationInfo();
		String nativelibdir = info.nativeLibraryDir;

		// open database connection
		if (this.mTrafficSource == null) {
			try {
				this.mTrafficSource = new TrafficSource(this);
				this.mTrafficSource.open();
				Log.v(this.TAG, "Created traffic database connection");
			} catch (Exception e) {
				Log.e(this.TAG, "Couldn't open the traffic database", e);
			}
		}

		OpenVPN.addStateListener(this);
		OpenVPN.addByteCountListener(this);

		if (intent != null && PAUSE_VPN.equals(intent.getAction())) {
			if (this.mDeviceStateReceiver != null)
				this.mDeviceStateReceiver.userPause(true);
			return START_NOT_STICKY;
		}

		if (intent != null && RESUME_VPN.equals(intent.getAction())) {
			if (this.mDeviceStateReceiver != null)
				this.mDeviceStateReceiver.userPause(false);
			return START_NOT_STICKY;
		}

		if (intent != null && START_SERVICE.equals(intent.getAction()))
			return START_NOT_STICKY;
		if (intent != null && START_SERVICE_STICKY.equals(intent.getAction())) {
			return START_REDELIVER_INTENT;
		}

		assert (intent != null);

		// Extract information from the intent.
		// String prefix = getPackageName();

		// Set a flag that we are starting a new VPN
		this.mStarting = true;
		// Stop the previous session by interrupting the thread.
		if (this.mManagement != null && this.mManagement.stopVPN())
			// an old was asked to exit, wait 1s
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		if (this.mProcessThread != null) {
			this.mProcessThread.interrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// An old running VPN should now be exited
		this.mStarting = false;

		// start a Thread that handles incoming messages of the management
		// socket
		OpenVpnManagementThread ovpnManagementThread = new OpenVpnManagementThread(
				this, server_url);
		if (ovpnManagementThread.openManagementInterface(this)) {
			Thread mSocketManagerThread = new Thread(ovpnManagementThread,
					"OpenVPNManagementThread");
			mSocketManagerThread.start();
			this.mManagement = ovpnManagementThread;
			Log.v(this.TAG, "Started management thread");
		}

		// Start a new session by creating a new thread.
		Runnable processThread;
		HashMap<String, String> env = new HashMap<String, String>();
		processThread = new OpenVPNThread(this, argv, env, nativelibdir);
		Log.v(this.TAG, "Started openVPN thread");

		this.mProcessThread = new Thread(processThread, "OpenVPNProcessThread");
		this.mProcessThread.start();

		if (this.mDeviceStateReceiver != null)
			unregisterDeviceStateReceiver();
		registerDeviceStateReceiver(this.mManagement);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		ConnectionNotificationManager.handleVpnConnectionStatus(
				getApplicationContext(), R.string.state_disconnected);
		if (this.mProcessThread != null) {
			this.mManagement.stopVPN();
			this.mProcessThread.interrupt();
		}
		if (this.mDeviceStateReceiver != null) {
			this.unregisterReceiver(this.mDeviceStateReceiver);
		}
		if (this.mTrafficSource != null) {
			this.mTrafficSource.close();
		}
		// Just in case unregister for state
		OpenVPN.removeStateListener(this);
	}

	public ParcelFileDescriptor openTun() {
		Builder builder = new Builder();

		if (this.mLocalIP == null && this.mLocalIPv6 == null) {
			Log.e(this.TAG, getString(R.string.opentun_no_ipaddr));
			return null;
		}

		if (this.mLocalIP != null) {
			builder.addAddress(this.mLocalIP.mIp, this.mLocalIP.len);
		}

		if (this.mLocalIPv6 != null) {
			String[] ipv6parts = this.mLocalIPv6.split("/");
			builder.addAddress(ipv6parts[0], Integer.parseInt(ipv6parts[1]));
		}

		for (String dns : this.mDnslist) {
			try {
				builder.addDnsServer(dns);
			} catch (IllegalArgumentException iae) {
				Log.e(this.TAG, getString(R.string.dns_add_error, dns), iae);
			}
		}
		builder.setMtu(this.mMtu);

		for (CIDRIP route : this.mRoutes) {
			try {
				builder.addRoute(route.mIp, route.len);
			} catch (IllegalArgumentException ia) {
				Log.v(this.TAG, getString(R.string.route_rejected) + route
						+ " " + ia.getLocalizedMessage());
			}
		}

		for (String v6route : this.mRoutesv6) {
			try {
				String[] v6parts = v6route.split("/");
				builder.addRoute(v6parts[0], Integer.parseInt(v6parts[1]));
			} catch (IllegalArgumentException ia) {
				Log.e(this.TAG, getString(R.string.route_rejected) + v6route
						+ " " + ia.getLocalizedMessage());
			}
		}

		if (this.mDomain != null)
			builder.addSearchDomain(this.mDomain);

		Log.v(this.TAG, getString(R.string.last_openvpn_tun_config));
		Log.v(this.TAG,
				getString(R.string.local_ip_info, this.mLocalIP.mIp,
						this.mLocalIP.len, this.mLocalIPv6, this.mMtu));
		Log.v(this.TAG,
				getString(R.string.dns_server_info, joinString(this.mDnslist),
						this.mDomain));
		Log.v(this.TAG,
				getString(R.string.routes_info, joinString(this.mRoutes)));
		Log.v(this.TAG,
				getString(R.string.routes_info6, joinString(this.mRoutesv6)));

		String session = "Dotvpn";
		if (this.mLocalIP != null && this.mLocalIPv6 != null)
			session = getString(R.string.session_ipv6string, session,
					this.mLocalIP, this.mLocalIPv6);
		else if (this.mLocalIP != null)
			session = getString(R.string.session_ipv4string, session,
					this.mLocalIP);
		builder.setSession(session);

		// No DNS Server, log a warning
		if (this.mDnslist.size() == 0)
			Log.v(this.TAG, getString(R.string.warn_no_dns));

		// Reset information
		this.mDnslist.clear();
		this.mRoutes.clear();
		this.mRoutesv6.clear();
		this.mLocalIP = null;
		this.mLocalIPv6 = null;
		this.mDomain = null;
		builder.setConfigureIntent(PendingIntent.getActivity(
				getApplicationContext(), 0, new Intent(getApplicationContext(),
						ConnectActivity.class), 0));

		try {
			return builder.establish();
		} catch (Exception e) {
			Log.v(this.TAG, getString(R.string.tun_open_error));
			Log.v(this.TAG, getString(R.string.error) + e.getLocalizedMessage());
			Log.v(this.TAG, getString(R.string.tun_error_helpful));
			return null;
		}

	}

	// Ugly, but java has no such method
	private <T> String joinString(Vector<T> vec) {
		String ret = "";
		if (vec.size() > 0) {
			ret = vec.get(0).toString();
			for (int i = 1; i < vec.size(); i++) {
				ret = ret + ", " + vec.get(i).toString();
			}
		}
		return ret;
	}

	public void addDNS(String dns) {
		this.mDnslist.add(dns);
	}

	public void setDomain(String domain) {
		if (this.mDomain == null) {
			this.mDomain = domain;
		}
	}

	public void addRoute(String dest, String mask) {
		CIDRIP route = new CIDRIP(dest, mask);
		if (route.len == 32 && !mask.equals("255.255.255.255")) {
			Log.v(this.TAG, getString(R.string.route_not_cidr, dest, mask));
		}

		if (route.normalise())
			Log.v(this.TAG,
					getString(R.string.route_not_netip, dest, route.len,
							route.mIp));

		Log.v(this.TAG, "adding route : " + dest);
		this.mRoutes.add(route);
	}

	public void addRoutev6(String extra) {
		this.mRoutesv6.add(extra);
	}

	public void setMtu(int mtu) {
		this.mMtu = mtu;
	}

	public void setLocalIP(CIDRIP cdrip) {
		this.mLocalIP = cdrip;
	}

	public void setLocalIP(String local, String netmask, int mtu, String mode) {
		this.mLocalIP = new CIDRIP(local, netmask);
		this.mMtu = mtu;

		if (this.mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
			// get the netmask as IP
			long netint = CIDRIP.getInt(netmask);
			if (Math.abs(netint - this.mLocalIP.getInt()) == 1) {
				if ("net30".equals(mode))
					this.mLocalIP.len = 30;
				else
					this.mLocalIP.len = 31;
			} else {
				Log.v(this.TAG,
						getString(R.string.ip_not_cidr, local, netmask, mode));
			}
		}
	}

	public void setLocalIPv6(String ipv6addr) {
		this.mLocalIPv6 = ipv6addr;
	}

	@Override
	public void updateState(String state, String logmessage, int resid,
			ConnectionStatus level) {
		ConnectionNotificationManager.handleVpnConnectionStatus(
				getApplicationContext(), resid);
		// If the process is not running, ignore any state,
		// Notification should be invisible in this state
		doSendBroadcast(state, level);
		// Display byte count only after being connected
		{
			if (level == LEVEL_WAITING_FOR_USER_INPUT) {
				// The user is presented a dialog of some kind, no need to
				// inform the user
				// with a notifcation
				return;
			} else if (level == LEVEL_CONNECTED) {
				this.mDisplayBytecount = true;
				this.mConnecttime = System.currentTimeMillis();
			} else {
				this.mDisplayBytecount = false;
			}
		}
	}

	private void doSendBroadcast(String state, ConnectionStatus level) {
		Intent vpnstatus = new Intent();
		vpnstatus.setAction("com.kernel5.dotvpn.VPN_STATUS");
		vpnstatus.putExtra("status", level.toString());
		vpnstatus.putExtra("detailstatus", state);
		Log.v(this.TAG, "Sending broadcast : " + state);
		sendBroadcast(vpnstatus, permission.ACCESS_NETWORK_STATE);
	}

	@Override
	public void updateByteCount(long in, long out, long diffin, long diffout) {
		if (this.mDisplayBytecount) {
			String netstat = String.format(
					getString(R.string.statusline_bytecount),
					humanReadableByteCount(in, false),
					humanReadableByteCount(diffin
							/ OpenVPNManagement.mBytecountInterval, true),
					humanReadableByteCount(out, false),
					humanReadableByteCount(diffout
							/ OpenVPNManagement.mBytecountInterval, true));

		}
		if (this.mTrafficSource != null) {
			try {
				if (in == 0 || diffin < 0 || diffout < 0) // initial message or
															// junk
					this.mTrafficSource.clearTraffic();
				else
					this.mTrafficSource.recordTraffic(in, out, diffin, diffout);
			} catch (Exception e) {
				Log.e(this.TAG, "could not record traffic", e);
			}
		}
	}

	public static String humanReadableByteCount(long bytes, boolean mbit) {
		if (mbit)
			bytes = bytes * 8;
		int unit = mbit ? 1000 : 1024;
		if (bytes < unit)
			return bytes + (mbit ? " bit" : " B");

		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (mbit ? "" : "");
		if (mbit)
			return String.format(Locale.getDefault(), "%.1f %sbit", bytes
					/ Math.pow(unit, exp), pre);
		else
			return String.format(Locale.getDefault(), "%.1f %sB",
					bytes / Math.pow(unit, exp), pre);
	}

	@Override
	public boolean handleMessage(Message msg) {
		Runnable r = msg.getCallback();
		if (r != null) {
			r.run();
			return true;
		} else {
			return false;
		}
	}

	public OpenVPNManagement getManagement() {
		return this.mManagement;
	}

}
