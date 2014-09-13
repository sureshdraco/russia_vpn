package com.kernel5.dotvpn.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.util.Log;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.kernel5.dotvpn.Constants;
import com.kernel5.dotvpn.ErrorUtils;
import com.kernel5.dotvpn.R;

public class LaunchVPN extends Activity {

    public static final int START_VPN_PROFILE = 70;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Resolve the intent
        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit
        if(Intent.ACTION_MAIN.equals(action)) {
            launchVPN();
        }
    }

    void launchVPN () {
        Intent intent = VpnService.prepare(this);

        try {
            Log.v( Constants.TAG, "Inserting tun module" );
            executeSUcmd("insmod /system/lib/modules/tun.ko");
        } catch ( Exception e ) {
            Log.e( Constants.TAG, "Could not insert tun module", e );
        }

        try {
            Log.v( Constants.TAG, "Changing tun permission" );
            executeSUcmd("chown system /dev/tun");
        } catch ( Exception e ) {
            Log.e( Constants.TAG, "Could not change tun permissions", e );
        }

        try {

            if (intent != null) {
               // Start the query
               try {
                    Log.v( Constants.TAG, "starting android vpn service" );
                    startActivityForResult(intent, START_VPN_PROFILE);
               } catch (ActivityNotFoundException ane) {
                    // Shame on you Sony! At least one user reported that
                    // an official Sony Xperia Arc S image triggers this exception
                    Log.e( Constants.TAG, "Could not start VPN service", ane );
               }
            } else { // service is already running
               onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
            }

       } catch ( Exception e ) {
            ErrorUtils.showErrorDialog( LaunchVPN.this,
             getString( R.string.error ),
             getString( R.string.exception ) + " : " + e.getMessage() ,
             getString( R.string.ok ) );
       }

    }

    private void executeSUcmd(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("su","-c",command);
        Process p = pb.start();
        int ret = p.waitFor();
        if(ret!=0)
        {
           Log.v( Constants.TAG, "su command returned : " + ret );
           throw new Exception( getString( R.string.suerror ) );
        }
    }

    protected class startOpenVpnThread extends Thread {
        @Override
        public void run() {
        	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        	String server_config_file = preferences.getString(getString(R.string.SettingsKeyServerConfigFileName), Constants.VPN_CONFIG_DE);
            VPNLaunchHelper.startOpenVpn(getBaseContext(), server_config_file);
            finish();
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==START_VPN_PROFILE) {
            if(resultCode == Activity.RESULT_OK) {
                Log.v( Constants.TAG, "Vpn system service started" );
                new startOpenVpnThread().start();
            }
        }
    }

}
