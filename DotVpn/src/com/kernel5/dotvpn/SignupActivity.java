package com.kernel5.dotvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.Notification;
import android.os.Bundle;
import android.location.*;
import android.content.*;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnClickListener;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.AdapterView.OnItemSelectedListener;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.text.Html;
import android.text.InputFilter.LengthFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.util.Log;
import android.util.DisplayMetrics;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.net.Socket;
import java.util.Random;
import java.util.ArrayList;
import java.lang.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;

import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.inputmethod.EditorInfo;

import com.kernel5.dotvpn.rest.*;

public class SignupActivity extends Activity {

    private static final String TAG = Constants.TAG;
    private static final String TEST_URL = "http://vemeo.com";
    private boolean mDebug = false;
    private Context mContext;
    private DisplayMetrics mMetrics=null;
    private String mUsername=null;
    private String mPassword=null;

    private SharedPreferences mPrefs;

    /** Called when the activity is destroyed.     */
    @Override
    public void onDestroy() {
      Log.v( TAG, "destroy" );
      super.onDestroy();
    }

    @Override
    public void onPause() {
      Log.v( TAG, "pause" );
      super.onPause();
    }

    /** Called when the activity is resumed.     */
    @Override
    public void onResume() {
      Log.v( TAG, "resume" );
      super.onResume();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
      Log.v( TAG, "create" );
      super.onCreate(savedInstanceState);

      // Hide the window title.
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

      // Acquire a reference to the application context
      mContext = this.getApplicationContext();

      mMetrics = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

      // read user preferences
      mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext()); 
//    		  this.getApplicationContext().getSharedPreferences(Constants.AppName,MODE_WORLD_READABLE);

      showSignupScreen(true);

    }

    // show signup screen
    public void showSignupScreen(boolean force)
    {
         setContentView(R.layout.signup);
         RelativeLayout rl = (RelativeLayout) findViewById(R.id.SignupLayout);

         ImageView im = (ImageView) findViewById(R.id.dotlogo);
         RelativeLayout.LayoutParams lp;
         lp = new RelativeLayout.LayoutParams( 181, 39);
         lp.leftMargin=(int)((mMetrics.widthPixels-181)/2);
         lp.topMargin=64;
         rl.updateViewLayout(im, lp);

         final EditText fn = (EditText) findViewById(R.id.first_name);
         lp = new RelativeLayout.LayoutParams( 242, 30);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=166;
         rl.updateViewLayout(fn, lp);
         fn.setTypeface( Constants.rrFont, Typeface.BOLD );

         ImageView fnu = (ImageView) findViewById(R.id.first_nameu);
         lp = new RelativeLayout.LayoutParams( 242, 5);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=196;
         rl.updateViewLayout(fnu, lp);

         final EditText ln = (EditText) findViewById(R.id.last_name);
         lp = new RelativeLayout.LayoutParams( 242, 30);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=229;
         rl.updateViewLayout(ln, lp);
         ln.setTypeface( Constants.rrFont, Typeface.BOLD );

         ImageView lnu = (ImageView) findViewById(R.id.last_nameu);
         lp = new RelativeLayout.LayoutParams( 242, 5);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=259;
         rl.updateViewLayout(lnu, lp);

         final EditText em = (EditText) findViewById(R.id.email);
         lp = new RelativeLayout.LayoutParams( 242, 30);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=292;
         rl.updateViewLayout(em, lp);
         em.setTypeface( Constants.rrFont, Typeface.BOLD );

         ImageView emu = (ImageView) findViewById(R.id.emailu);
         lp = new RelativeLayout.LayoutParams( 242, 5);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=322;
         rl.updateViewLayout(emu, lp);

         final EditText pw = (EditText) findViewById(R.id.password);
         lp = new RelativeLayout.LayoutParams( 242, 30);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=355;
         rl.updateViewLayout(pw, lp);
         pw.setTypeface( Constants.rrFont, Typeface.BOLD );

         pw.setOnEditorActionListener(
          new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
              if ( actionId == EditorInfo.IME_ACTION_DONE ||
               event.getAction() == KeyEvent.ACTION_DOWN &&
               event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

               doSignup();
               return true;

            }
            return false; // pass on to other listeners.
          }
         });

         ImageView pwu = (ImageView) findViewById(R.id.passwordu);
         lp = new RelativeLayout.LayoutParams( 242, 5);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=385;
         rl.updateViewLayout(pwu, lp);

         TextView sin = (TextView) findViewById(R.id.signup);
         lp = new RelativeLayout.LayoutParams( 242, 39);
         lp.leftMargin=(int)((mMetrics.widthPixels-242)/2);
         lp.topMargin=428;
         rl.updateViewLayout(sin, lp);
         sin.setTypeface( Constants.rrFont, Typeface.BOLD );

         sin.setOnClickListener(
          new View.OnClickListener()
          {
            public void onClick(View v)
            {
               doSignup();
            }
         });

    }

    public void doSignup()
    {
               final EditText fn = (EditText) findViewById(R.id.first_name);
               final String firstName = fn.getText().toString();
               if ( firstName.equals("") )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.name_empty ),
                     getString( R.string.ok ) );
                  return;
               }

               final EditText ln = (EditText) findViewById(R.id.last_name);
               final String lastName = ln.getText().toString();
               if ( lastName.equals("") )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.name_empty ),
                     getString( R.string.ok ) );
                  return;
               }

               final EditText em = (EditText) findViewById(R.id.email);
               final String email = em.getText().toString();
               if ( email.equals("") )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.email_empty ),
                     getString( R.string.ok ) );
                  return;
               }
               if ( !email.contains("@") || !email.contains(".") )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.wrong_email_address ),
                     getString( R.string.ok ) );
                  return;
               }
               final String[] pemail = email.split("@");
               if ( pemail.length != 2 )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.wrong_email_address ),
                     getString( R.string.ok ) );
                  return;
               }

               final EditText pw = (EditText) findViewById(R.id.password);
               final String password = pw.getText().toString();
               if ( password.equals("") )
               {
                  ErrorUtils.showErrorDialog( SignupActivity.this,
                     getString( R.string.error ),
                     getString( R.string.password_empty ),
                     getString( R.string.ok ) );
                  return;
               }

               // do the signup
               Runnable mSignup = new Runnable() {
                 @Override
                 public void run() {

                      try {
                         SignupRequest request = new SignupRequest();
                         request.name = firstName;
                         request.lastName = lastName;
                         request.email = email;
                         request.passwd = password;
                         SignupResponse response = JacksonRequests.postForJson( Constants.SIGNUP_URL, 
                                   request, SignupResponse.class );

                         if ( response.code == 0 )
                         { 
                           Log.v( TAG, "Signed up user : " + email );
                           // store token
                           SharedPreferences.Editor editor = mPrefs.edit();
                           editor.putString(Constants.OAUTH_TOKEN, response.token);
                           String ckey = pemail[0] + "123456789101112";
                           ckey = ckey.substring( 0, 16 );
                           DESEncrypt encoder = new DESEncrypt( ckey );
                           // store encrypted password for automatic login
                           editor.putString(Constants.BAUTH_TOKEN, encoder.encrypt(request.passwd) );
                           editor.commit();
                         
                           runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                               Toast.makeText(SignupActivity.this, 
                                 email + " " + getString( R.string.user_signed_up), Toast.LENGTH_LONG).show();
                             }
                           });

                           // start connection activity
                           Log.v( TAG, "Starting connect activity" );
                           Intent i = new Intent(SignupActivity.this, ConnectActivity.class);
                           startActivity(i);
                           finish();
                         }
                         else
                         { 
                           if ( response.code == 4 )
                           {
                              ErrorUtils.showErrorDialog( SignupActivity.this,
                                 getString( R.string.error ),
                                 getString( R.string.user_already_exists ),
                                 getString( R.string.ok ) );
                           }
                           else
                           {
                              ErrorUtils.showErrorDialog( SignupActivity.this,
                                 getString( R.string.error ),
                                 getString( R.string.error_registering_user ),
                                 getString( R.string.ok ) );
                           }
                         }

                      } catch ( Exception e ) {
                         ErrorUtils.showErrorDialog( SignupActivity.this,
                            getString( R.string.error ),
                            getString( R.string.error_registering_user ),
                            getString( R.string.ok ) );
                         Log.v( TAG, "Exception while signup", e );
                      }
                  }
               };
               Thread t = new Thread( mSignup );
               t.start();
    }
}
