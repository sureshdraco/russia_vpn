package com.kernel5.dotvpn;

import android.app.Activity;

import com.kernel5.dotvpn.core.*;
import com.kernel5.dotvpn.rest.*;
import com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus;
import com.kernel5.dotvpn.core.OpenVPN.StateListener;
import com.kernel5.dotvpn.core.OpenVPN.ByteCountListener;
import com.kernel5.dotvpn.core.OpenVpnService.LocalBinder;
import com.kernel5.dotvpn.core.OpenVpnService;

import android.os.Bundle;
import android.content.*;
import android.os.IBinder;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.util.Log;
import android.net.VpnService;
import android.widget.AdapterView;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

public class ConnectActivity extends Activity implements StateListener,
		ByteCountListener, AdapterView.OnItemSelectedListener,
		View.OnClickListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	private final static boolean DEBUG = false;

	private static final String TAG = Constants.TAG;
	private static final int UPDATE_PERIOD = 1000; // update state period (in
													// ms)
	private static final int UPDATE_UI_PERIOD = 500; // update UI period (in ms)
	private static final String PA_URL = "http://what-is-my-ip.net/?json";
	public static final int START_VPN_PROFILE = 70;

	// protected boolean mConnectionPending = false;
	private Context mContext;
	protected TextView mStatus;
	private ImageButton connect_button;
	protected ImageView is;
	protected ImageView connection_dots;
	private String mPrivateIp = "";
	protected String mPublicIp = "";
	protected OpenVpnService mService = null;
	protected Handler mHandler = null;
	protected int mCounter = -1;
	protected float[] mTrafIn;
	protected float[] mTrafOut;
	protected String[] mLHor;
	protected String[] mLVer;
	protected GraphView mTrafficView;
	protected TrafficSource mTrafficSource = null;
	protected String mUsername = null;
	protected String mPassword = null;
	protected String mUserIp = null;
	protected String mPing = null;
	protected SharedPreferences mPrefs;

	protected TextView public_ip_view;
	private TextView private_ip_view;
	private Spinner location_select_view;

	protected String selected_server_url;
	protected String selected_server_config;

	/** Flag to reconnect after current connection becomes stopped. */
	protected boolean reconnect;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Acquire a reference to the application context
		this.mContext = this.getApplicationContext();

		// retrieve user preferences
		this.mPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		if (this.mPrefs.getString(Constants.OAUTH_TOKEN, "").equals("")) {
			// go to connection screen
			Intent i = new Intent(ConnectActivity.this, DotVpn.class);
			startActivity(i);
			finish();
			return;
		}

		// retrieve stored values pertinent for the UI
		this.selected_server_url = this.mPrefs.getString(
				getString(R.string.SettingsKeyServerUrl),
				Constants.SERVER_NAME_DE);
		this.selected_server_config = this.mPrefs.getString(
				getString(R.string.SettingsKeyServerConfigFileName),
				Constants.VPN_CONFIG_DE);
		// setup listener
		this.mPrefs.registerOnSharedPreferenceChangeListener(this);

		// allocate graph data
		this.mTrafIn = new float[Constants.NB_GRAPH_POINTS];
		this.mTrafOut = new float[Constants.NB_GRAPH_POINTS];
		this.mLHor = new String[Constants.NB_GRAPH_LABELS];
		this.mLVer = new String[Constants.NB_GRAPH_LABELS];

		// open database connection
		try {
			this.mTrafficSource = new TrafficSource(this.mContext);
			this.mTrafficSource.open();
			if (DEBUG)
				Log.v(TAG, "Created traffic database connection");
		} catch (Exception e) {
			if (DEBUG)
				Log.e(TAG, "Couldn't open the traffic database", e);
		}

		// get user infos ( and original ip )
		Runnable mGetUser = new Runnable() {
			@Override
			public void run() {

				try {
					InfoRequest request = new InfoRequest();
					request.token = ConnectActivity.this.mPrefs.getString(
							Constants.OAUTH_TOKEN, "");
					InfoResponse response = JacksonRequests.postForJson(
							Constants.INFO_URL, request, InfoResponse.class);
					if (response.code == 0) {
						ConnectActivity.this.mUsername = response.email;
						ConnectActivity.this.mUserIp = response.ip;
						final String[] pemail = ConnectActivity.this.mUsername
								.split("@");
						if (pemail.length != 2) {
							ErrorUtils.showErrorDialog(ConnectActivity.this,
									getString(R.string.error),
									getString(R.string.wrong_email_address),
									getString(R.string.ok));
						}
						String epass = ConnectActivity.this.mPrefs.getString(
								Constants.BAUTH_TOKEN, "");
						String ckey = pemail[0] + "123456789101112";
						ckey = ckey.substring(0, 16);
						DESEncrypt decoder = new DESEncrypt(ckey);
						ConnectActivity.this.mPassword = decoder.decrypt(epass);
					}

				} catch (Exception e) {
					if (DEBUG)
						Log.v(TAG, "Couldn't get user info", e);
				}
			}
		};
		Thread t = new Thread(mGetUser);
		t.start();
		if (DEBUG)
			Log.v(TAG, "Waiting user info ... ");
		try {
			t.join();
		} catch (Exception e) {
			if (DEBUG)
				Log.v(TAG, "Getting user threadus interruptus", e);
		}

		showConnectScreen(true);

		// start updating state
		this.mHandler = new Handler();
		this.mHandler.postDelayed(this.mUpdateState, UPDATE_PERIOD);
		this.mHandler.postDelayed(this.updateUI, UPDATE_UI_PERIOD);
	}

	// show connect screen
	public void showConnectScreen(boolean force) {
		setContentView(R.layout.main);

		ImageView sm = (ImageView) findViewById(R.id.settings);

		sm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(ConnectActivity.this,
						SettingsActivity.class);
				startActivity(intent);
			}
		});

		this.is = (ImageView) findViewById(R.id.bgstatus);

		this.connection_dots = (ImageView) findViewById(R.id.dotsconnect);

		this.connect_button = (ImageButton) findViewById(R.id.connect);
		this.connect_button.setOnClickListener(this);

		this.mStatus = (TextView) findViewById(R.id.status);
		this.mStatus.setTypeface(Constants.rrFont, Typeface.BOLD);
		this.mStatus.setText(getString(R.string.sdisconnected).toUpperCase());

		this.public_ip_view = (TextView) findViewById(R.id.pa);
		this.private_ip_view = (TextView) findViewById(R.id.pr);

		this.location_select_view = (Spinner) findViewById(R.id.location_select_view);
		LocationSpinnerAdapter adapter = new LocationSpinnerAdapter(this,
				R.layout.country_spinner_layout, new String[] { "Germany",
						"France", "Japan", "Netherlands", "Russia", "Sweden",
						"Singapore", "UK", "United States" }, new int[] {
						R.drawable.de_flag, R.drawable.fr_flag,
						R.drawable.jp_flag, R.drawable.nl_flag,
						R.drawable.ru_flag, R.drawable.se_flag,
						R.drawable.sg_flag, R.drawable.uk_flag,
						R.drawable.us_flag });
		this.location_select_view.setAdapter(adapter);

		int selected_country = 0;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_DE))
			selected_country = 0;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_FR))
			selected_country = 1;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_JP))
			selected_country = 2;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_NL))
			selected_country = 3;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_RU))
			selected_country = 4;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_SE))
			selected_country = 5;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_SG))
			selected_country = 6;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_UK))
			selected_country = 7;
		if (this.selected_server_url.equals(Constants.SERVER_NAME_US))
			selected_country = 8;
		this.location_select_view.setSelection(selected_country);

		this.location_select_view.setOnItemSelectedListener(this);

		// fake data
		for (int it = 0; it < Constants.NB_GRAPH_POINTS; it++) {
			this.mTrafIn[it] = 0.0f;
			this.mTrafOut[it] = 0.0f;
		}
		for (int it = 0; it < Constants.NB_GRAPH_LABELS; it++) {
			this.mLHor[it] = "";
			this.mLVer[it] = "";
		}

		this.mTrafficView = (GraphView) findViewById(R.id.trafic_view);
		this.mTrafficView.setup(this, this.mTrafIn, this.mTrafOut,
				getString(R.string.traffic_title_in),
				getString(R.string.traffic_title_out), this.mLHor, this.mLVer);

	}

	/** Called when the activity is resumed. */
	@Override
	public void onResume() {

		super.onResume();
		if (this.mPrefs.getString(Constants.OAUTH_TOKEN, "").equals("")) {
			return;
		}
		OpenVPN.addStateListener(this);
		OpenVPN.addByteCountListener(this);
		Intent intent = new Intent(this, OpenVpnService.class);
		intent.setAction(OpenVpnService.START_SERVICE);
		bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
	}

	private void blockConnectUI() {
		this.connect_button.setEnabled(false);
		this.location_select_view.setEnabled(false);
	}

	protected void unblockConnectUI() {
		this.connect_button.setEnabled(true);
		this.location_select_view.setEnabled(true);
	}

	@Override
	public void onClick(View view) {

		if (view == this.connect_button) {

			// if ( this.mConnectionPending ) return;

			if (OpenVPN.mLastLevel == ConnectionStatus.LEVEL_NOTCONNECTED) {
				blockConnectUI();

				startVpnService();
			}

			if (OpenVPN.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {
				stopVpnService();
			}
		}

	}

	// COUNTRY SELECT NOTIFIER //

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {

		String country = (String) parent.getItemAtPosition(pos);

		switch (pos) {
		case 0: // DE
			if (this.selected_server_url.equals(Constants.SERVER_NAME_DE)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_DE;
			this.selected_server_config = Constants.VPN_CONFIG_DE;
			break;
		case 1: // FR
			if (this.selected_server_url.equals(Constants.SERVER_NAME_FR)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_FR;
			this.selected_server_config = Constants.VPN_CONFIG_FR;
			break;
		case 2: // JP
			if (this.selected_server_url.equals(Constants.SERVER_NAME_JP)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_JP;
			this.selected_server_config = Constants.VPN_CONFIG_JP;
			break;
		case 3: // NL
			if (this.selected_server_url.equals(Constants.SERVER_NAME_NL)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_NL;
			this.selected_server_config = Constants.VPN_CONFIG_NL;
			break;
		case 4: // RU
			if (this.selected_server_url.equals(Constants.SERVER_NAME_RU)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_RU;
			this.selected_server_config = Constants.VPN_CONFIG_RU;
			break;
		case 5: // SE
			if (this.selected_server_url.equals(Constants.SERVER_NAME_SE)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_SE;
			this.selected_server_config = Constants.VPN_CONFIG_SE;
			break;
		case 6: // SG
			if (this.selected_server_url.equals(Constants.SERVER_NAME_SG)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_SG;
			this.selected_server_config = Constants.VPN_CONFIG_SG;
			break;
		case 7: // UK
			if (this.selected_server_url.equals(Constants.SERVER_NAME_UK)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_UK;
			this.selected_server_config = Constants.VPN_CONFIG_UK;
			break;
		case 8: // US
			if (this.selected_server_url.equals(Constants.SERVER_NAME_US)) {
				// no change
				return;
			}
			this.selected_server_url = Constants.SERVER_NAME_US;
			this.selected_server_config = Constants.VPN_CONFIG_US;
			break;
		}

		blockConnectUI();

		// save selected country server parameter to user preferences //
		SharedPreferences.Editor preferences_editor = this.mPrefs.edit();
		preferences_editor.putString(getString(R.string.SettingsKeyServerUrl),
				this.selected_server_url);
		preferences_editor.putString(
				getString(R.string.SettingsKeyServerConfigFileName),
				this.selected_server_config);
		preferences_editor.commit();
		// //////////////////////////////////////////////////////////////

		if (DEBUG)
			Log.i(TAG, "Connect to " + country + " (" + pos + ")");

		if (OpenVPN.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {
			// reconnect to selected country //

			// set reconnect flag
			this.reconnect = true;

			stopVpnService();
		} else {
			// start connection to selected country //

			startVpnService();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// do nothing //
	}

	// ///////////////////////////

	public void updatePrivateIp() {
		boolean private_ip_found = false;

		try {
			one: for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {

				NetworkInterface intf = en.nextElement();

				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {

					InetAddress inetAddress = enumIpAddr.nextElement();

					// we only want to have IPv4 addresses
					if (!inetAddress.isLoopbackAddress()
							&& inetAddress.getHostAddress().length() <= 15) {
						this.mPrivateIp = inetAddress.getHostAddress();
						this.private_ip_view.setText(this.mPrivateIp);
						private_ip_found = true;
						break one;
					}
				}
			}
		} catch (Exception e) {
			if (DEBUG)
				Log.e(TAG, "Could not find private IP", e);
		}

		if (!private_ip_found) {
			this.private_ip_view.setText("no connection");
		}
	}

	public void updatePublicIp() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					String PostURL = PA_URL;
					HttpParams httpParameters = new BasicHttpParams();
					int timeoutConnection = 3000;
					HttpConnectionParams.setConnectionTimeout(httpParameters,
							timeoutConnection);
					int timeoutSocket = 10000; // 10 seconds
					HttpConnectionParams.setSoTimeout(httpParameters,
							timeoutSocket);
					DefaultHttpClient httpclient = new DefaultHttpClient(
							httpParameters);

					HttpPost httpPost = new HttpPost(PostURL);
					httpPost.setHeader("Accept", "application/json");
					httpPost.setHeader("Content-type", "application/json");
					HttpResponse response = httpclient.execute(httpPost);
					HttpEntity entity = response.getEntity();
					ConnectActivity.this.mPublicIp = EntityUtils.toString(
							entity).replaceAll("\"", "");

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ConnectActivity.this.public_ip_view
									.setText(ConnectActivity.this.mPublicIp);
						}
					});
				} catch (Exception e) {
					if (DEBUG)
						Log.w(TAG,
								"Could not update public IP - "
										+ e.getMessage());
					// apparently we can reach the service behind the url ->
					// show "no connection"
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ConnectActivity.this.public_ip_view
									.setText("no connection");
						}
					});
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
	}

	public void updateServerInfo() {
		Runnable r = new Runnable() {
			@Override
			public void run() {

				try {

					if (OpenVPN.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {
						if (ConnectActivity.this.mPublicIp.contains(".")) {
							if (ConnectActivity.this.mPing == null || connectedUpdatePing) {
								connectedUpdatePing = false;
								long startTimeMs = System.currentTimeMillis();
								long endTimeMs = startTimeMs;

								try {
									String GetURL = "https://google.com";
									if (DEBUG)
										Log.v(TAG, "trying to reach : "
												+ GetURL);
									HttpParams httpParameters = new BasicHttpParams();
									int timeoutConnection = 3000;
									HttpConnectionParams.setConnectionTimeout(
											httpParameters, timeoutConnection);
									int timeoutSocket = 10000; // 10 seconds
									HttpConnectionParams.setSoTimeout(
											httpParameters, timeoutSocket);
									DefaultHttpClient httpclient = new DefaultHttpClient(
											httpParameters);

									HttpGet httpGet = new HttpGet(GetURL);
									httpclient.execute(httpGet);

								} catch (Exception e) {
									// even if certificate is wrong, we don't
									// care here
									if (DEBUG)
										Log.w(TAG, "Couldn't reach server", e);
								} finally {
									endTimeMs = System.currentTimeMillis();
								}
								Log.d("ping", "ping info : " + (endTimeMs - startTimeMs));
								ConnectActivity.this.mPing = ((endTimeMs - startTimeMs) > 0) ? ""
										+ (endTimeMs - startTimeMs)
										+ " "
										+ getString(R.string.millis)
										: getString(R.string.unknown);
							}

							// getting distance to the server
							String mDistance = getString(R.string.unknown);
							try {
								IpRequest request = new IpRequest();
								request.token = ConnectActivity.this.mPrefs
										.getString(Constants.OAUTH_TOKEN, "");
								request.srcip = ConnectActivity.this.mUserIp;
								IpResponse response = JacksonRequests
										.postForJson(Constants.IP_URL, request,
												IpResponse.class);
								if (response.code == 0) {
									mDistance = "" + response.distance;
								}
							} catch (Exception e) {
								if (DEBUG)
									Log.v(TAG,
											"Couldn't get distance to server",
											e);
							}
							final String pDistance = mDistance + " "
									+ getString(R.string.kilometers);

							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// TextView sl = (TextView) findViewById(
									// R.id.sl );
									// ImageView sli = (ImageView) findViewById(
									// R.id.sli );
									// if ( sl != null )
									// {
									// sl.setText( sCountryCode );
									// if ( !sCountryCode.equals( getString(
									// R.string.unknown ) ) )
									// {
									// lp = new RelativeLayout.LayoutParams( 40,
									// 25);
									// lp.leftMargin=(int)((mMetrics.widthPixels)/2);
									// lp.topMargin=300;
									// rl.updateViewLayout(sli, lp);
									// sli.setVisibility( View.VISIBLE );
									// sl.setPadding( 50, 0, 0, 0 );
									// if ( sCountryCode.equals( "Germany" ) )
									// {
									// sli.setImageResource( R.drawable.de );
									// }
									// }
									// else
									// {
									// sli.setVisibility( View.GONE );
									// sl.setPadding( 0, 0, 0, 0 );
									// }
									// }
									TextView pi = (TextView) findViewById(R.id.pi);
									if (pi != null)
										pi.setText(ConnectActivity.this.mPing);
									TextView ds = (TextView) findViewById(R.id.ds);
									if (ds != null)
										ds.setText(pDistance);
									TextView pt = (TextView) findViewById(R.id.pt);
									if (pt != null)
										pt.setText(R.string.openvpn);
									TextView fw = (TextView) findViewById(R.id.fw);
									if (fw != null) {
										fw.setText(getString(R.string.active));
										fw.setCompoundDrawablesRelativeWithIntrinsicBounds(
												R.drawable.check_active, 0, 0,
												0);
									}
								}
							});
						}
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TextView sl = (TextView) findViewById(
								// R.id.sl );
								// if ( sl != null ) sl.setText(
								// R.string.unknown ) );
								// sl.setPadding( 0, 0, 0, 0 );
								// ImageView sli = (ImageView) findViewById(
								// R.id.sli );
								// sli.setVisibility( View.GONE );
								TextView pi = (TextView) findViewById(R.id.pi);
								if (pi != null)
									pi.setText(R.string.unknown);
								TextView ds = (TextView) findViewById(R.id.ds);
								if (ds != null)
									ds.setText(R.string.unknown);
								TextView pt = (TextView) findViewById(R.id.pt);
								if (pt != null)
									pt.setText(R.string.unknown);
								TextView fw = (TextView) findViewById(R.id.fw);
								if (fw != null) {
									fw.setText(R.string.inactive);
									fw.setCompoundDrawablesRelativeWithIntrinsicBounds(
											0, 0, 0, 0);
								}
							}
						});
					}
				} catch (Exception e) {
					if (DEBUG)
						Log.e(TAG, "Could not update server info", e);
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
	}

	public void updateTraffic() {
		if (this.mTrafficSource != null) {
			try {
				final long[] totalIn = new long[1];
				final long[] totalOut = new long[1];
				final long[] max = new long[1];
				max[0] = 0;

				// reading from the database
				this.mTrafficSource.getTraffic(this.mTrafIn, this.mTrafOut,
						totalIn, totalOut, max);

				// no traffic data, fake scale
				if (max[0] == 0)
					max[0] = 64 * 1024;

				// set scale
				long step = max[0] / (Constants.NB_GRAPH_LABELS - 1);
				for (int li = 0; li < Constants.NB_GRAPH_LABELS; li++) {
					this.mLVer[li] = OpenVpnService.humanReadableByteCount(
							(max[0] - step * li), false);
				}
				this.mLVer[Constants.NB_GRAPH_LABELS - 1] = "0";

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (DEBUG)
							Log.v(TAG, "Updating traffic");
						ConnectActivity.this.mTrafficView.setData(
								ConnectActivity.this.mTrafIn,
								ConnectActivity.this.mTrafOut,
								getString(R.string.traffic_title_in)
										+ " : "
										+ OpenVpnService
												.humanReadableByteCount(
														totalIn[0], false),
								getString(R.string.traffic_title_out)
										+ " : "
										+ OpenVpnService
												.humanReadableByteCount(
														totalOut[0], false),
								ConnectActivity.this.mLHor,
								ConnectActivity.this.mLVer);
					}
				});

			} catch (Exception e) {
				if (DEBUG)
					Log.e(TAG, "Could not update traffic", e);
			}
		}
	}

	protected static int getRId(int counter) {
		switch (counter) {
		case 0:
			return R.drawable.dots_connect_0;
		case 1:
			return R.drawable.dots_connect_1;
		case 2:
			return R.drawable.dots_connect_2;
		case 3:
			return R.drawable.dots_connect_3;
		case 4:
			return R.drawable.dots_connect_4;
		case 5:
			return R.drawable.dots_connect_5;
		case 6:
			return R.drawable.dots_connect_6;
		case 7:
			return R.drawable.dots_connect_7;
		case 8:
			return R.drawable.dots_connect_8;
		case 9:
			return R.drawable.dots_connect_9;
		case 10:
			return R.drawable.dots_connect_10;
		case 11:
			return R.drawable.dots_connect_11;
		case 12:
			return R.drawable.dots_connect_10;
		case 13:
			return R.drawable.dots_connect_9;
		case 14:
			return R.drawable.dots_connect_8;
		case 15:
			return R.drawable.dots_connect_7;
		case 16:
			return R.drawable.dots_connect_6;
		case 17:
			return R.drawable.dots_connect_5;
		case 18:
			return R.drawable.dots_connect_4;
		case 19:
			return R.drawable.dots_connect_3;
		case 20:
			return R.drawable.dots_connect_2;
		case 21:
			return R.drawable.dots_connect_1;
		default:
			return R.drawable.dots_connect_0;
		}
	}

	// function to update data ( except byte count )
	private Runnable mUpdateState = new Runnable() {

		@Override
		public void run() {
			updatePrivateIp();
			updatePublicIp();
			updateServerInfo();
			if (ConnectActivity.this.mHandler != null) {
				ConnectActivity.this.mHandler.postDelayed(this, UPDATE_PERIOD);
			}
		}

	};

	private Runnable updateUI = new Runnable() {

		@Override
		public void run() {
			if (ConnectActivity.this.connection_dots.getVisibility() == View.VISIBLE) {
				ConnectActivity.this.mCounter = (ConnectActivity.this.mCounter + 1) % 22;
				ConnectActivity.this.connection_dots
						.setImageResource(getRId(ConnectActivity.this.mCounter));
			}
			if (ConnectActivity.this.mHandler != null) {
				ConnectActivity.this.mHandler.postDelayed(this,
						UPDATE_UI_PERIOD);
			}
		}

	};

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			ConnectActivity.this.mService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			ConnectActivity.this.mService = null;
		}
	};

	protected boolean connectedUpdatePing;

	public void startVpnService() {
		this.mStatus.setText(getString(R.string.sconnecting).toUpperCase());
		// this.mConnectionPending=true;

		try {

			Intent intent = VpnService.prepare(this);

			try {
				if (DEBUG)
					Log.v(TAG, "Inserting tun module");
				executeSUcmd("insmod /system/lib/modules/tun.ko");
			} catch (Exception e) {
				if (DEBUG)
					Log.e(TAG, "Could not insert tun module", e);
			}

			try {
				if (DEBUG)
					Log.v(TAG, "Changing tun permission");
				executeSUcmd("chown system /dev/tun");
			} catch (Exception e) {
				if (DEBUG)
					Log.e(TAG, "Could not change tun permissions", e);
			}

			if (intent != null) {
				// Start the query
				try {
					if (DEBUG)
						Log.v(TAG, "starting android vpn service");
					startActivityForResult(intent, START_VPN_PROFILE);
				} catch (ActivityNotFoundException ane) {
					// Shame on you Sony! At least one user reported that
					// an official Sony Xperia Arc S image triggers this
					// exception
					if (DEBUG)
						Log.e(TAG, "Could not start VPN service", ane);
				}
			} else { // service is already running
				onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
			}

		} catch (Exception e) {
			ErrorUtils.showErrorDialog(ConnectActivity.this,
					getString(R.string.error), getString(R.string.exception)
							+ " : " + e.getMessage(), getString(R.string.ok));
		}
	}

	public void stopVpnService() {
		try {
			if (this.mService != null && this.mService.getManagement() != null)
				this.mService.getManagement().stopVPN();
			else
				throw new Exception("service is not bound");
		} catch (Exception e) {
			if (DEBUG)
				Log.e(TAG, "Couldn't stop VPN service", e);
		}
	}

	private void executeSUcmd(String command) throws Exception {
		ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
		Process p = pb.start();
		int ret = p.waitFor();
		if (ret != 0) {
			if (DEBUG)
				Log.v(TAG, "su command returned : " + ret);
			throw new Exception(getString(R.string.suerror));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == START_VPN_PROFILE) {
			if (resultCode == Activity.RESULT_OK) {
				if (DEBUG)
					Log.v(TAG, "Vpn system service started");
				new startOpenVpnThread().start();
			}
		}
	}

	protected class startOpenVpnThread extends Thread {
		@Override
		public void run() {
			VPNLaunchHelper.startOpenVpn(getBaseContext(),
					ConnectActivity.this.selected_server_config);
		}
	}

	// Implements StateListener
	@Override
	public void updateState(final String status, final String logmessage,
			final int resid, final ConnectionStatus level) {

		if (DEBUG)
			Log.v(TAG, "Update state : " + status);

		if (status.equals("CONNERROR") || status.equals("AUTH_FAILED")) {
			if (DEBUG)
				Log.v(TAG, "Post error on CONNERROR or AUTH_FAILED");

			ErrorUtils.showErrorDialog(ConnectActivity.this,
					getString(R.string.error), logmessage,
					getString(R.string.ok));

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					unblockConnectUI();
				}
			});

			return;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch (resid) {
				case R.string.state_wait:
				case R.string.state_reconnecting:
				case R.string.state_connecting:
				case R.string.state_auth:
				case R.string.state_get_config:
				case R.string.state_assign_ip:
				case R.string.state_add_routes:
				case R.string.state_tcp_connect:
				case R.string.state_resolve:
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sconnecting).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_red_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.INVISIBLE);
					break;
				case R.string.state_connected:
					ConnectActivity.this.connectedUpdatePing = true;
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sconnected).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_green_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.VISIBLE);
					unblockConnectUI();
					updatePrivateIp();
					updatePublicIp();
					updateServerInfo();
					break;
				case R.string.state_auth_failed:
					// ConnectActivity.this.mConnectionPending=false;
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sauthfailed).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_red_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.INVISIBLE);
					unblockConnectUI();
					break;
				case R.string.state_conn_error:
					// ConnectActivity.this.mConnectionPending=false;
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sconnerror).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_red_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.INVISIBLE);
					unblockConnectUI();
					break;
				case R.string.state_exiting:
					updatePrivateIp();
					updatePublicIp();
					updateServerInfo();

					// ConnectActivity.this.mConnectionPending=false;
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sexiting).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_red_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.INVISIBLE);

					break;
				// case R.string.state_closed:
				//
				// if (ConnectActivity.this.reconnect) {
				// ConnectActivity.this.reconnect = false;
				// startVpnService();
				// }
				// else
				// unblockConnectUI();
				//
				// break;
				case R.string.state_noprocess:

					if (ConnectActivity.this.reconnect) {
						ConnectActivity.this.reconnect = false;
						startVpnService();
					} else
						unblockConnectUI();

				case R.string.state_nonetwork:
				case R.string.unknown_state:
					// ConnectActivity.this.mConnectionPending=false;
					ConnectActivity.this.mStatus.setText(getString(
							R.string.sdisconnected).toUpperCase());
					ConnectActivity.this.is
							.setImageResource(R.drawable.status_red_bg);
					ConnectActivity.this.connection_dots
							.setVisibility(View.INVISIBLE);
					if (ConnectActivity.this.mTrafficSource != null) {
						try {
							ConnectActivity.this.mTrafficSource.clearTraffic();
							updateTraffic();
						} catch (Exception e) {
							if (DEBUG)
								Log.v(TAG, "Cannot clear traffic");
						}
					}
					break;
				}
			}
		});
	}

	// implements byte count listenet
	@Override
	public void updateByteCount(long in, long out, long diffin, long diffout) {
		if (in != 0 && diffin > 0 && diffout > 0)
			updateTraffic();
	}

	@Override
	public void onPause() {
		OpenVPN.removeStateListener(this);
		OpenVPN.removeByteCountListener(this);
		super.onPause();
	}

	protected void stopAndCloseActivity() {
		stopVpnService();
		finish();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences shared_preferences,
			String key) {

		if (key.equals(Constants.OAUTH_TOKEN)
				|| key.equals(Constants.BAUTH_TOKEN)) {

			if (shared_preferences.getString(Constants.OAUTH_TOKEN, "")
					.length() == 0
					&& shared_preferences.getString(Constants.BAUTH_TOKEN, "")
							.length() == 0) {
				// if login credentials were removed we have been logged out
				stopAndCloseActivity();
			}

		}

	}

	/** Called when the activity is destroyed. */
	@Override
	public void onDestroy() {
		try {
			if (this.mHandler != null) {
				this.mHandler.removeCallbacks(this.mUpdateState);
				this.mHandler = null;
			}
			if (this.mTrafficSource != null) {
				this.mTrafficSource.close();
			}
			unbindService(this.mConnection);
		} catch (Exception ignored) {
		}
		super.onDestroy();
	}

}