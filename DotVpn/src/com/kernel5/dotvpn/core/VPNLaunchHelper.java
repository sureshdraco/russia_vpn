package com.kernel5.dotvpn.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.kernel5.dotvpn.Constants;
import com.kernel5.dotvpn.VpnProfile;

public class VPNLaunchHelper {


    static private boolean writeMiniVPN(Context context) {
        File mvpnout = new File(context.getCacheDir(),VpnProfile.MINIVPN);
        if (mvpnout.exists() && mvpnout.canExecute())
            return true;
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open("minivpn." + Build.CPU_ABI);
            }
            catch (IOException errabi) {
                Log.v(Constants.TAG, "Failed getting assets for archicture " + Build.CPU_ABI);
                mvpn = context.getAssets().open("minivpn." + Build.CPU_ABI2);
            }

            FileOutputStream fout = new FileOutputStream(mvpnout);
            byte buf[]= new byte[4096];
            int lenread = mvpn.read(buf);
            while(lenread> 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if(!mvpnout.setExecutable(true)) {
                Log.v(Constants.TAG,"Failed to set minivpn executable");
                return false;
            }

            return true;
        } catch (IOException e) {
            Log.e(Constants.TAG, "Could not write minivpn", e);
            return false;
        }
    }

    static private boolean writeConfigFile(Context context, String selected_server_config) {
    	
        File confout = new File(context.getCacheDir(), selected_server_config);

        try {
            InputStream mconf;
            try {
               mconf = context.getAssets().open(selected_server_config);
            }
            catch (IOException errabi) {
               Log.v(Constants.TAG, "Could not open config file from assets");
               return false;
            }
    
            FileOutputStream fout = new FileOutputStream(confout);
            byte buf[]= new byte[4096];
            int lenread = mconf.read(buf);
            while(lenread> 0) {
                fout.write(buf, 0, lenread);
                lenread = mconf.read(buf);
            }
            fout.close();

            Log.e(Constants.TAG, "wrote conf file");
            return true;

        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not write conf file", e);
            return false;
        }
    }

    public static void startOpenVpn(Context context, String selected_server_config) {
        if(!writeMiniVPN(context)) {
            Log.v(Constants.TAG, "Error writing minivpn binary");
            return;
        }

        if(!writeConfigFile(context, selected_server_config)) {
            Log.v(Constants.TAG, "Error writing config file");
            return;
        }

        Intent startVPN = new Intent(context, OpenVpnService.class);
        if(startVPN!=null)
            context.startService(startVPN);
    }
}
