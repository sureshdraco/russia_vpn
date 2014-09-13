package com.kernel5.dotvpn;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.view.View;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;
import android.util.DisplayMetrics;

import java.net.HttpURLConnection;
import java.net.URL;

import android.net.Uri;
import android.view.inputmethod.EditorInfo;

import com.kernel5.dotvpn.rest.*;

public class SigninActivity extends Activity {

	private static final String TAG = Constants.TAG;
	private static final String TEST_URL = "http://vemeo.com";
	private boolean mDebug = false;
	private Context mContext;
	private DisplayMetrics mMetrics = null;
	private String mUsername = null;
	private String mPassword = null;

	protected SharedPreferences mPrefs;

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

		// Hide the window title.
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Acquire a reference to the application context
		this.mContext = this.getApplicationContext();

		this.mMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(this.mMetrics);

		// read user preferences
		this.mPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		// this.getApplicationContext().getSharedPreferences(Constants.AppName,MODE_WORLD_READABLE);

		// check there is data connection
		Runnable mCheckConnection = new Runnable() {
			@Override
			public void run() {

				try {
					HttpURLConnection urlc = (HttpURLConnection) (new URL(
							TEST_URL).openConnection());
					urlc.setRequestProperty("User-Agent", "Android");
					urlc.setRequestProperty("Connection", "close");
					urlc.setConnectTimeout(10000);
					urlc.connect();
					Log.v(TAG,
							TEST_URL + " returned : " + urlc.getResponseCode());
					if (urlc.getResponseCode() != 301) {
						throw new Exception(getString(R.string.network_error));
					}

				} catch (Exception e) {
					Log.v(TAG, "Could not reach : " + TEST_URL, e);
					ErrorUtils.showFatalErrorDialog(SigninActivity.this,
							getString(R.string.no_data_connection),
							getString(R.string.app_close),
							getString(R.string.close));
				}
			}
		};

//		// check if the user is already signed in
//		// and if so, go to the connecting screen
//		if (!this.mPrefs.getString(Constants.OAUTH_TOKEN, "").equals("")) {
//		}

		if (!this.mPrefs.getString(Constants.OAUTH_TOKEN, "").equals("")) {
			// go to connection screen
			Intent i = new Intent(SigninActivity.this, ConnectActivity.class);
			startActivity(i);
			finish();
		} else {
			showSigninScreen(true);
		}
	}

	// show signing screen
	public void showSigninScreen(boolean force) {
		setContentView(R.layout.signin);
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.SigninLayout);

		final EditText em = (EditText) findViewById(R.id.email);
		em.setTypeface(Constants.rrFont, Typeface.BOLD);

		final EditText pw = (EditText) findViewById(R.id.password);
		pw.setTypeface(Constants.rrFont, Typeface.BOLD);

		pw.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE
						|| event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

					doLogin();
					return true;

				}
				return false; // pass on to other listeners.
			}
		});

		TextView fgt = (TextView) findViewById(R.id.forgot);
		fgt.setTypeface(Constants.rrFont, Typeface.BOLD);

		fgt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// redirect to the web url for now
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri
						.parse(Constants.FORGOT_WEB_URL));
				startActivity(intent);
			}
		});

		TextView sin = (TextView) findViewById(R.id.signin);
		sin.setTypeface(Constants.rrFont, Typeface.BOLD);

		sin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doLogin();
			}
		});

		TextView sup = (TextView) findViewById(R.id.signup);
		sup.setTypeface(Constants.osFont, Typeface.BOLD);

		sup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// redirect to the web url for now
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri
						.parse(Constants.SIGNUP_WEB_URL));
				startActivity(intent);
			}
		});

	}

	public void doLogin() {
		final EditText em = (EditText) findViewById(R.id.email);
		final String email = em.getText().toString();
		if (email.equals("")) {
			ErrorUtils.showErrorDialog(SigninActivity.this,
					getString(R.string.error), getString(R.string.email_empty),
					getString(R.string.ok));
			return;
		}
		if (!email.contains("@") || !email.contains(".")) {
			ErrorUtils.showErrorDialog(SigninActivity.this,
					getString(R.string.error),
					getString(R.string.wrong_email_address),
					getString(R.string.ok));
			return;
		}
		final String[] pemail = email.split("@");
		if (pemail.length != 2) {
			ErrorUtils.showErrorDialog(SigninActivity.this,
					getString(R.string.error),
					getString(R.string.wrong_email_address),
					getString(R.string.ok));
			return;
		}

		final EditText pw = (EditText) findViewById(R.id.password);
		final String password = pw.getText().toString();
		if (password.equals("")) {
			ErrorUtils.showErrorDialog(SigninActivity.this,
					getString(R.string.error),
					getString(R.string.password_empty), getString(R.string.ok));
			return;
		}

		// check login
		Runnable mCheckLogin = new Runnable() {
			@Override
			public void run() {

				try {
					SigninRequest request = new SigninRequest();
					request.email = email;
					request.passwd = password;
					SigninResponse response = JacksonRequests
							.postForJson(Constants.SIGNIN_URL, request,
									SigninResponse.class);

					if (response.code == 0) {
						Log.v(TAG, "Signed in user : " + email);
						// store token
						SharedPreferences.Editor editor = SigninActivity.this.mPrefs
								.edit();
						editor.putString(Constants.OAUTH_TOKEN, response.token);
						String ckey = pemail[0] + "123456789101112";
						ckey = ckey.substring(0, 16);
						DESEncrypt encoder = new DESEncrypt(ckey);
						// store encrypted password for automatic login
						editor.putString(Constants.BAUTH_TOKEN,
								encoder.encrypt(request.passwd));
						editor.commit();

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(
										SigninActivity.this,
										email
												+ " "
												+ getString(R.string.user_signed_in),
										Toast.LENGTH_LONG).show();
							}
						});

						// start connection activity
						Intent i = new Intent(SigninActivity.this,
								ConnectActivity.class);
						startActivity(i);
						finish();
					} else {
						ErrorUtils.showErrorDialog(SigninActivity.this,
								getString(R.string.error),
								getString(R.string.wrong_user_or_pass),
								getString(R.string.ok));
					}

				} catch (Exception e) {
					ErrorUtils.showErrorDialog(SigninActivity.this,
							getString(R.string.error),
							getString(R.string.error_identifying),
							getString(R.string.ok));
				}
			}
		};
		Thread t = new Thread(mCheckLogin);
		t.start();
	}
}
