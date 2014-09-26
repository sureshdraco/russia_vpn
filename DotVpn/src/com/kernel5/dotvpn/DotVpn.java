package com.kernel5.dotvpn;

import com.kernel5.dotvpn.rest.JacksonRequests;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.DisplayMetrics;

public class DotVpn extends Activity {

	private static final String TAG = Constants.TAG;
	protected boolean _active = true;
	protected int _splashTime = 1000; // time to display the splash screen in ms
										// the first time, needs to download
										// some object ( at least 1 )
										// private boolean mDebug = false;
	// private Context mContext;
	protected int mNbAnim = 3;
	protected int mCurAnim = 0;
	protected ImageView mIv[];
	private DisplayMetrics mMetrics = null;
	private SharedPreferences mPrefs;

	/** Called when the activity is destroyed. */
	@Override
	public void onDestroy() {
		Log.v(TAG, "destroy");
		super.onDestroy();
	}

	@Override
	public void onPause() {
		Log.v(TAG, "pause");
		super.onPause();
	}

	/** Called when the activity is resumed. */
	@Override
	public void onResume() {
		Log.v(TAG, "resume");
		super.onResume();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		JacksonRequests.establishTrustManager();

		Constants.rrFont = Typeface.createFromAsset(getAssets(),
				Constants.FONT_RR);
		Constants.osFont = Typeface.createFromAsset(getAssets(),
				Constants.FONT_SO);

		// Hide the window title.
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Acquire a reference to the application context
		// this.mContext = this.getApplicationContext();

		this.mMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(this.mMetrics);

		Log.v(TAG, "Screen pixels : " + this.mMetrics.widthPixels + "x"
				+ this.mMetrics.heightPixels);
		Log.v(TAG, "Supposed size : " + this.mMetrics.widthPixels
				/ this.mMetrics.densityDpi + "x" + this.mMetrics.heightPixels
				/ this.mMetrics.densityDpi + " inches");
		Log.v(TAG, "Density : " + this.mMetrics.density);
		Log.v(TAG, "Density (dpi) : " + this.mMetrics.densityDpi);
		Log.v(TAG, "Density (scaled) : " + this.mMetrics.scaledDensity);
		Log.v(TAG, "Density (X) : " + this.mMetrics.xdpi);
		Log.v(TAG, "Density (Y) : " + this.mMetrics.ydpi);

		if (this.mMetrics.widthPixels / this.mMetrics.densityDpi >= 6
				|| this.mMetrics.heightPixels / this.mMetrics.densityDpi >= 6) {
			Constants.isBigScreen = true;
		}
		this.mPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		// read user preferences
		// this.mPrefs =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		// this.getApplicationContext().getSharedPreferences(Constants.AppName,MODE_WORLD_READABLE);

		if (!this.mPrefs.getString(Constants.OAUTH_TOKEN, "").equals("")) {
			// go to connection screen
			Intent i = new Intent(DotVpn.this, ConnectActivity.class);
			startActivity(i);
			finish();
			return;
		}
		showSplashScreen(true);

		// thread for displaying the SplashScreen
		Thread splashTread = new Thread() {
			@Override
			public void run() {
				try {
					int waited = 0;
					while (DotVpn.this._active
							&& waited < DotVpn.this._splashTime) {
						sleep(250);
						if (DotVpn.this._active) {
							waited += 250;
						}
						// ask handler to do the load anim
						Message msg = DotVpn.this.mUpdateAnim.obtainMessage();
						DotVpn.this.mUpdateAnim.sendMessage(msg);
					}
				} catch (InterruptedException e) {
					// do nothing
				} finally {
					// launch ChooseCity Activity
					DotVpn.this.finish();
					Intent i = new Intent(DotVpn.this, SigninActivity.class);
					startActivity(i);
				}
			}
		};

		splashTread.start();
	}

	// show splash screen
	public void showSplashScreen(boolean force) {
		setContentView(R.layout.splash);

		this.mIv = new ImageView[] {
				(ImageView) findViewById(R.id.SplashProgressA),
				(ImageView) findViewById(R.id.SplashProgressB),
				(ImageView) findViewById(R.id.SplashProgressC) };

		for (int bi = 0; bi < this.mNbAnim; bi++) {
			if (bi == 0) {
				this.mIv[bi].setBackgroundResource(R.drawable.loadingb);
				this.mCurAnim = 0;
			} else {
				this.mIv[bi].setBackgroundColor(Color.WHITE);
			}
		}
	}

	// anim progress
	public void animProgress() {
		DotVpn.this.mIv[DotVpn.this.mCurAnim].setBackgroundColor(Color.WHITE);
		DotVpn.this.mCurAnim = (DotVpn.this.mCurAnim + 1) % DotVpn.this.mNbAnim;
		DotVpn.this.mIv[DotVpn.this.mCurAnim]
				.setBackgroundResource(R.drawable.loadingb);
	}

	// Called to update the interface
	protected final Handler mUpdateAnim = new Handler() {
		public void handleMessage(Message msg) {
			animProgress();
		}
	};

}
