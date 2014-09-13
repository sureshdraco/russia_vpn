package com.kernel5.dotvpn.core;

import android.util.Log;

import com.kernel5.dotvpn.R;
import com.kernel5.dotvpn.VpnProfile;
import com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus;
import com.kernel5.dotvpn.Constants;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class OpenVPNThread implements Runnable {
    private static final String DUMP_PATH_STRING = "Dump path: ";
    private static final String TAG = Constants.TAG;
    private String[] mArgv;
    private Process mProcess;
    private String mNativeDir;
    private OpenVpnService mService;
    private String mDumpPath;
    private Map<String, String> mProcessEnv;

    public OpenVPNThread(OpenVpnService service,String[] argv, Map<String,String> processEnv, String nativelibdir) {
        this.mArgv = argv;
        this.mNativeDir = nativelibdir;
        this.mService = service;
        this.mProcessEnv = processEnv;
    }

    public void stopProcess() {
        this.mProcess.destroy();
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "Starting openvpn");            
            startOpenVPNThreadArgs(this.mArgv, this.mProcessEnv);
            Log.i(TAG, "Giving up");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "OpenVPNThread Got " + e.toString());
        } finally {
            int exitvalue = 0;
            try {
               if (this.mProcess!=null)
               {
                  exitvalue = this.mProcess.waitFor();
               }
            } catch ( IllegalThreadStateException ite) {
                Log.v(TAG, "Illegal Thread state: " + ite.getLocalizedMessage());
            } catch (InterruptedException ie) {
                Log.v(TAG, "InterruptedException: " + ie.getLocalizedMessage());
            }
            if( exitvalue != 0)
            {
               Log.v( TAG, "openvpn exited with error : " + exitvalue );
               OpenVPN.updateStateString("CONNERROR","Could not connect to this server (error=" + exitvalue + ").", R.string.state_conn_error,ConnectionStatus.LEVEL_NOTCONNECTED);
            }
            
            OpenVPN.updateStateString("NOPROCESS","No process running.", R.string.state_noprocess,ConnectionStatus.LEVEL_NOTCONNECTED);
            
//            OpenVPN.updateStateString("CLOSED", "OpenVPN thread closed.", R.string.state_closed, ConnectionStatus.LEVEL_NOTCONNECTED);
            
            this.mService.processDied(exitvalue);
            Log.i(TAG, "Exiting");
        }
    }

    private void startOpenVPNThreadArgs(String[] argv, Map<String, String> env) {
        LinkedList<String> argvlist = new LinkedList<String>();

        Collections.addAll(argvlist, argv);
        for ( String arg :argvlist)
        {
           Log.v(TAG, "openvpn arg : " + arg );
        }

        ProcessBuilder pb = new ProcessBuilder(argvlist);
        String lbpath = genLibraryPath(argv, pb);

        pb.environment().put("LD_LIBRARY_PATH", lbpath);
        pb.environment().put("LANG", "en"); // execution env to english

        Iterator<Entry<String, String>> iter = pb.environment().entrySet().iterator();
        while (iter.hasNext()) {
           Entry<String, String> entry = iter.next();
           Log.v( TAG, entry.getKey() + "=" + entry.getValue() );
        }

        // Add extra variables
        for(Entry<String,String> e:env.entrySet()){
                        Log.v( TAG, "openvpn env : " + e.getKey() + " : " + e.getValue());
            pb.environment().put(e.getKey(), e.getValue());
        }

        pb.redirectErrorStream(true);
        try {
            this.mProcess = pb.start();
            // Close the output, since we don't need it
            this.mProcess.getOutputStream().close();
            InputStream in = this.mProcess.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            while(true) {
                String logline = br.readLine();
                Log.v( TAG, "openvpn said : " + logline );
                if(logline==null) 
                {
                    Log.e( TAG, "openvpn returned null ( ending? )" );
                    return;
                }

                if (logline.startsWith(DUMP_PATH_STRING))
                    this.mDumpPath = logline.substring(DUMP_PATH_STRING.length());
                Log.v(TAG, logline);
            }
        } catch (IOException e) {
            Log.v(TAG, "Error reading from output of OpenVPN process"+ e.getLocalizedMessage());
            e.printStackTrace();
            stopProcess();
        }
    }

    private String genLibraryPath(String[] argv, ProcessBuilder pb) {
        // Hack until I find a good way to get the real library path
        String applibpath = argv[0].replace("/cache/" + VpnProfile.MINIVPN , "/lib");

        String lbpath = pb.environment().get("LD_LIBRARY_PATH");
        if(lbpath==null) 
            lbpath = applibpath;
        else
            lbpath = lbpath + ":" + applibpath;

        if (!applibpath.equals(this.mNativeDir)) {
            lbpath = lbpath + ":" + this.mNativeDir;
        }
        return lbpath;
    }
}
