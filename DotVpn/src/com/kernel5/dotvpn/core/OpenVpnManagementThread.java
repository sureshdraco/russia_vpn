package com.kernel5.dotvpn.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kernel5.dotvpn.R;
import com.kernel5.dotvpn.DESEncrypt;
import com.kernel5.dotvpn.rest.*;
import com.kernel5.dotvpn.Constants;
import com.kernel5.dotvpn.core.OpenVPN.ConnectionStatus;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

public class OpenVpnManagementThread implements Runnable, OpenVPNManagement {

    private static final String TAG = OpenVpnManagementThread.class.getSimpleName();
    private LocalSocket mSocket;
    private String server_url;
    private OpenVpnService mOpenVPNService;
    private LinkedList<FileDescriptor> mFDList=new LinkedList<FileDescriptor>();
    private LocalServerSocket mServerSocket;
    private boolean mReleaseHold=true;
    private boolean mWaitingForRelease=false;
    private long mLastHoldRelease=0;
    private String mUsername=null;
    private String mPassword=null;

    private static Vector<OpenVpnManagementThread> active=new Vector<OpenVpnManagementThread>();
    private LocalSocket mServerSocketLocal;

    private pauseReason lastPauseReason = pauseReason.noNetwork;

    public OpenVpnManagementThread(OpenVpnService openVpnService, String server_url) {
        this.mOpenVPNService = openVpnService;
        this.server_url = server_url;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(openVpnService);
        boolean managemeNetworkState = prefs.getBoolean("netchangereconnect", true);
        if(managemeNetworkState)
            this.mReleaseHold=false;
    }

    public boolean openManagementInterface(final Context c) {
        // Could take a while to open connection
        int tries=8;

        String socketName = (c.getCacheDir().getAbsolutePath() + "/" + "mgmtsocket");
        this.mServerSocketLocal = new LocalSocket();

        // retrieving username and password
        Runnable mGetUser = new Runnable() {
          @Override
          public void run() {

            try {
               SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(c);

               InfoRequest request = new InfoRequest();
               request.token = mPrefs.getString( Constants.OAUTH_TOKEN, "" );
               InfoResponse response = JacksonRequests.postForJson( Constants.INFO_URL,
                             request, InfoResponse.class );
               if ( response.code == 0 )
               {
                  OpenVpnManagementThread.this.mUsername = response.email;
                  final String[] pemail = OpenVpnManagementThread.this.mUsername.split("@");
                  if ( pemail.length != 2 )
                  {
                     Log.e( TAG, "Couldn't get user name properly" );
                  }
                  String epass = mPrefs.getString( Constants.BAUTH_TOKEN, "" );
                  String ckey = pemail[0] + "123456789101112";
                  ckey = ckey.substring( 0, 16 );
                  DESEncrypt decoder = new DESEncrypt( ckey );
                  OpenVpnManagementThread.this.mPassword = decoder.decrypt( epass );
               }

            } catch ( Exception e ) {
               Log.v( TAG, "Couldn't get user info", e );
            }
          }
        };
        Thread t  = new Thread( mGetUser );
        t.start();
        Log.v( TAG, "Waiting user info ... " );
        try { 
            t.join();
        } catch ( Exception e ) {
            Log.v( TAG, "Getting user threadus interruptus", e );
        }
        if ( this.mUsername == null || this.mPassword == null ) return false;

        while(tries > 0 && !this.mServerSocketLocal.isConnected()) {
            try {
                this.mServerSocketLocal.bind(new LocalSocketAddress(socketName,
                        LocalSocketAddress.Namespace.FILESYSTEM));
                Log.v( TAG, "created : " + socketName );
            } catch (IOException e) {
                // wait 300 ms before retrying
                try { Thread.sleep(300);
                Log.v( TAG, "retrying for socket : " + socketName );
             } catch (InterruptedException e1) {
                Log.e( TAG, "sould not create socket : " + socketName, e );
            }

            }
            tries--;
        }

        try {
            this.mServerSocket = new LocalServerSocket(this.mServerSocketLocal.getFileDescriptor());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static {
        System.loadLibrary("opvpnutil");
    }

    public void managmentCommand(String cmd) {
        if(this.mSocket!=null) {
            try {
                this.mSocket.getOutputStream().write(cmd.getBytes());
                this.mSocket.getOutputStream().flush();
            } catch (IOException e) {
                // Ignore socket stack traces
            }
        }
    }

    @Override
    public void run() {
        byte [] buffer  =new byte[2048];
        //    mSocket.setSoTimeout(5); // Setting a timeout cannot be that bad

        String pendingInput="";
        active.add(this);

        try {
            // Wait for a client to connect
            this.mSocket= this.mServerSocket.accept();
            InputStream instream = this.mSocket.getInputStream();
            // Close the management socket after client connected

            this.mServerSocket.close();
            // Closing one of the two sockets also closes the other
            //mServerSocketLocal.close();

            while(true) {
                int numbytesread = instream.read(buffer);
                if(numbytesread==-1)
                    return;

                FileDescriptor[] fds = null;
                try {
                    fds = this.mSocket.getAncillaryFileDescriptors();
                } catch (IOException e) {
                    Log.v(TAG, "Error reading fds from socket" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                if(fds!=null){
                    Collections.addAll(this.mFDList, fds);
                }

                String input = new String(buffer,0,numbytesread,"UTF-8");
                pendingInput += input;
                pendingInput=processInput(pendingInput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        active.remove(this);
    }

    //! Hack O Rama 2000!
    private void protectFileDescriptor(FileDescriptor fd) {
        Exception exp;
        try {
            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            int fdint = (Integer) getInt.invoke(fd);

            // You can even get more evil by parsing toString() and extract the int from that :)

            this.mOpenVPNService.protect(fdint);

            //ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fdint);
            //pfd.close();
            NativeUtils.jniclose(fdint);
            return;
        } catch (NoSuchMethodException e) {
            exp =e;
        } catch (IllegalArgumentException e) {
            exp =e;
        } catch (IllegalAccessException e) {
            exp =e;
        } catch (InvocationTargetException e) {
            exp =e;
        } catch (NullPointerException e) {
            exp =e;
        }

        Log.e(TAG, "Failed to retrieve fd from socket: " + fd, exp);
    }

    private String processInput(String pendingInput) {
        while(pendingInput.contains("\n")) {
            String[] tokens = pendingInput.split("\\r?\\n", 2);
            processCommand(tokens[0]);
            if(tokens.length == 1)
                // No second part, newline was at the end
                pendingInput="";
            else
                pendingInput=tokens[1];
        }
        return pendingInput;
    }

    private void processCommand(String command) {
        Log.v( TAG, "got command : " + command );
        if (command.startsWith(">") && command.contains(":")) {
            String[] parts = command.split(":",2);
            String cmd = parts[0].substring(1);
            String argument = parts[1];

            if(cmd.equals("INFO")) {
                /* Ignore greeting from management */
                return;
            }else if (cmd.equals("PASSWORD")) {
                processPWCommand(argument);
            } else if (cmd.equals("HOLD")) {
                handleHold();
            } else if (cmd.equals("NEED-OK")) {
                processNeedCommand(argument);
            } else if (cmd.equals("BYTECOUNT")){
                processByteCount(argument);
            } else if (cmd.equals("STATE")) {
                processState(argument);
            } else if (cmd.equals("PROXY")) {
                processProxyCMD(argument);
            } else if (cmd.equals("LOG")) {
                String[] args = argument.split(",",3);
                // 0 unix time stamp
                // 1 log level N,I,E etc.
                // 2 log message
                Log.v(TAG, args[2]);
            } else if (cmd.equals("RSA_SIGN")) {
                processSignCommand(argument);
            } else {
                Log.e(TAG, "Got unrecognized command" + command);
            }
        } else if (command.startsWith("SUCCESS:")) {
            /* Ignore this kind of message too */
            return;
        } else {
            Log.i(TAG, "Got unrecognized line from managment" + command);
        }
    }

    private void handleHold() {
        if(this.mReleaseHold) {
            releaseHoldCmd();
        } else { 
            this.mWaitingForRelease=true;

            OpenVPN.updateStatePause(this.lastPauseReason);
        }
    }

    private void releaseHoldCmd() {
        if ((System.currentTimeMillis()- this.mLastHoldRelease) < 5000) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.mWaitingForRelease=false;
        this.mLastHoldRelease  = System.currentTimeMillis();
        managmentCommand("hold release\n");
        managmentCommand("bytecount " + mBytecountInterval + "\n");
        managmentCommand("state on\n");
    }

    public void releaseHold() {
        this.mReleaseHold=true;
        if(this.mWaitingForRelease)
            releaseHoldCmd();
    }

    private void processProxyCMD(String argument) {
        
    	String[] args = argument.split(",",3);
        
        SocketAddress proxyaddr = ProxyDetection.detectProxy(this.server_url);

        if(args.length >= 2) {
            String proto = args[1];
            if(proto.equals("UDP")) {
                proxyaddr=null;
            }
        }

        if(proxyaddr instanceof InetSocketAddress ){
            InetSocketAddress isa = (InetSocketAddress) proxyaddr;

            String proxycmd = String.format(Locale.US,"proxy HTTP %s %d\n", isa.getHostName(),isa.getPort());
            managmentCommand(proxycmd);
        } else {
            managmentCommand("proxy NONE\n");
        }
    }

    private static void processState(String argument) {
        String[] args = argument.split(",",3);
        String currentstate = args[1];
        if(args[2].equals(",,"))
            OpenVPN.updateStateString(currentstate,"");
        else
            OpenVPN.updateStateString(currentstate,args[2]);
    }

    private static void processByteCount(String argument) {
        //   >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
        int comma = argument.indexOf(',');
        long in = Long.parseLong(argument.substring(0, comma));
        long out = Long.parseLong(argument.substring(comma+1));

        OpenVPN.updateByteCount(in,out);
    }

    private void processNeedCommand(String argument) {
        int p1 =argument.indexOf('\'');
        int p2 = argument.indexOf('\'',p1+1);

        String needed = argument.substring(p1+1, p2);
        String extra = argument.split(":",2)[1];

        String status = "ok";

        if (needed.equals("PROTECTFD")) {
            FileDescriptor fdtoprotect = this.mFDList.pollFirst();
            protectFileDescriptor(fdtoprotect);
        } else if (needed.equals("DNSSERVER")) {
            this.mOpenVPNService.addDNS(extra);
        }else if (needed.equals("DNSDOMAIN")){
            this.mOpenVPNService.setDomain(extra);
        } else if (needed.equals("ROUTE")) {
            String[] routeparts = extra.split(" ");
            this.mOpenVPNService.addRoute(routeparts[0], routeparts[1]);
        } else if (needed.equals("ROUTE6")) {
            this.mOpenVPNService.addRoutev6(extra);
        } else if (needed.equals("IFCONFIG")) {
            String[] ifconfigparts = extra.split(" ");
            int mtu = Integer.parseInt(ifconfigparts[2]);
            this.mOpenVPNService.setLocalIP(ifconfigparts[0], ifconfigparts[1],mtu,ifconfigparts[3]);
        } else if (needed.equals("IFCONFIG6")) {
            this.mOpenVPNService.setLocalIPv6(extra);

        } else if (needed.equals("OPENTUN")) {
            if(sendTunFD(needed,extra))
                return;
            else
                status="cancel";
            // This not nice or anything but setFileDescriptors accepts only FilDescriptor class :(

        } else {
            Log.e(TAG,"Unkown needok command " + argument);
            return;
        }

        String cmd = String.format(Locale.US,"needok '%s' %s\n", needed, status);
        managmentCommand(cmd);
    }

    private boolean sendTunFD (String needed, String extra) {
        Exception exp;
        if(!extra.equals("tun")) {
            // We only support tun
            String errmsg = String.format(Locale.US,"Devicetype %s requested, but only tun is possible with the Android API, sorry!",extra);
            Log.v(TAG, errmsg );

            return false;
        }
        ParcelFileDescriptor pfd = this.mOpenVPNService.openTun(); 
        if(pfd==null)
            return false;

        Method setInt;
        int fdint = pfd.getFd();
        try {
            setInt = FileDescriptor.class.getDeclaredMethod("setInt$",int.class);
            FileDescriptor fdtosend = new FileDescriptor();

            setInt.invoke(fdtosend,fdint);

            FileDescriptor[] fds = {fdtosend};
            this.mSocket.setFileDescriptorsForSend(fds);

            Log.d("Openvpn", "Sending FD tosocket: " + fdtosend + " " + fdint + "  " + pfd);
            // Trigger a send so we can close the fd on our side of the channel
            // The API documentation fails to mention that it will not reset the file descriptor to
            // be send and will happily send the file descriptor on every write ...
            String cmd = String.format(Locale.US,"needok '%s' %s\n", needed, "ok");
            managmentCommand(cmd);

            // Set the FileDescriptor to null to stop this mad behavior 
            this.mSocket.setFileDescriptorsForSend(null);

            pfd.close();            

            return true;
        } catch (NoSuchMethodException e) {
            exp =e;
        } catch (IllegalArgumentException e) {
            exp =e;
        } catch (IllegalAccessException e) {
            exp =e;
        } catch (InvocationTargetException e) {
            exp =e;
        } catch (IOException e) {
            exp =e;
        }
        Log.e(TAG, "Could not send fd over socket:" + exp.getLocalizedMessage());
        exp.printStackTrace();

        return false;
    }

    private void processPWCommand(String argument) {
        // argument has the form     
        // Need 'Auth' username/password
        // or  ">PASSWORD:Verification Failed: '%s' ['%s']"
        String needed;

        try{
            int p1 = argument.indexOf('\'');
            int p2 = argument.indexOf('\'',p1+1);
            needed = argument.substring(p1+1, p2);
            if (needed.equals("Auth")) {
               Log.v( TAG, "password requested" );
               String usercmd = String.format(Locale.US,"username '%s' %s\n", needed, this.mUsername );
               managmentCommand(usercmd);
               String passcmd = String.format(Locale.US,"password '%s' %s\n", needed, this.mPassword );
               managmentCommand(passcmd);
            }
            if (argument.startsWith("Verification Failed")) {
                processPWFailed(needed, argument.substring(p2+1));
                return;
            }
        } catch (StringIndexOutOfBoundsException sioob) {
            Log.v(TAG, "Could not parse management Password command: "  + argument);
            return;
        }

    }

    private void processPWFailed(String needed, String args) {
        OpenVPN.updateStateString("AUTH_FAILED", needed + args,R.string.state_auth_failed,ConnectionStatus.LEVEL_AUTH_FAILED);
    }

    private static boolean stopOpenVPN() {
        boolean sendCMD=false;
        for (OpenVpnManagementThread mt: active){
            mt.managmentCommand("signal SIGINT\n");
            sendCMD=true;
            try {
                if(mt.mSocket !=null)
                    mt.mSocket.close();
            } catch (IOException e) {
                // Ignore close error on already closed socket
            }
        }
        return sendCMD;        
    }

    public void signalusr1() {
        this.mReleaseHold=false;

        if(!this.mWaitingForRelease)
            managmentCommand("signal SIGUSR1\n");
        else
            // If signalusr1 is called update the state string
            // if there is another for stopping
            OpenVPN.updateStatePause(this.lastPauseReason);
    }

    public void reconnect() {
    	Log.d("suresh", "open mgmnt reconnect");
        signalusr1();
        releaseHold();
    }

    private void processSignCommand(String b64data) {
    }

    @Override
    public void pause (pauseReason reason) {
        this.lastPauseReason = reason;
        signalusr1();
    }

    @Override
    public void resume() {
        releaseHold();
        /* Reset the reason why we are disconnected */
        this.lastPauseReason = pauseReason.noNetwork;
    }

    @Override
    public boolean stopVPN() {
        return stopOpenVPN();
    }
}
