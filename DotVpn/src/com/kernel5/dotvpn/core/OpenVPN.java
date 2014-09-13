package com.kernel5.dotvpn.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import com.kernel5.dotvpn.R;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

public class OpenVPN {

	private static Vector<StateListener> stateListener;
	private static Vector<ByteCountListener> byteCountListener;

	private static String mLaststatemsg="";

	private static String mLaststate = "NOPROCESS";

	private static int mLastStateresid=R.string.state_noprocess;

	private static long mlastByteCount[]={0,0,0,0};

	public enum ConnectionStatus {
		LEVEL_CONNECTED,
		LEVEL_VPNPAUSED,
		LEVEL_CONNECTING_SERVER_REPLIED,
		LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
		LEVEL_NONETWORK,
		LEVEL_NOTCONNECTED,
		LEVEL_AUTH_FAILED,
		LEVEL_WAITING_FOR_USER_INPUT,
		UNKNOWN_LEVEL
	}

	public static ConnectionStatus mLastLevel=ConnectionStatus.LEVEL_NOTCONNECTED;

	static {
		stateListener = new Vector<OpenVPN.StateListener>();
		byteCountListener = new Vector<OpenVPN.ByteCountListener>();
	}

	public interface StateListener {
		void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level);
	}

	public interface ByteCountListener {
		void updateByteCount(long in, long out, long diffin, long diffout);
	}

	public synchronized static void addByteCountListener(ByteCountListener bcl) {
		bcl.updateByteCount(mlastByteCount[0],	mlastByteCount[1], mlastByteCount[2], mlastByteCount[3]);
		byteCountListener.add(bcl);
	}	

	public synchronized static void removeByteCountListener(ByteCountListener bcl) {
		byteCountListener.remove(bcl);
	}

	public synchronized static void addStateListener(StateListener sl){
		if(!stateListener.contains(sl)){
			stateListener.add(sl);
			if(mLaststate!=null)
				sl.updateState(mLaststate, mLaststatemsg, mLastStateresid, mLastLevel);
		}
	}	

	private static int getLocalizedState(String state){
		if (state.equals("CONNECTING")) 
			return R.string.state_connecting;
		else if (state.equals("WAIT"))
			return R.string.state_wait;
		else if (state.equals("AUTH"))
			return R.string.state_auth;
		else if (state.equals("GET_CONFIG"))
			return R.string.state_get_config;
		else if (state.equals("ASSIGN_IP"))
			return R.string.state_assign_ip;
		else if (state.equals("ADD_ROUTES"))
			return R.string.state_add_routes;
		else if (state.equals("CONNECTED"))
			return R.string.state_connected;
		else if (state.equals("DISCONNECTED"))
			return R.string.state_disconnected;
		else if (state.equals("RECONNECTING"))
			return R.string.state_reconnecting;
		else if (state.equals("EXITING"))
			return R.string.state_exiting;
		else if (state.equals("RESOLVE"))
			return R.string.state_resolve;
		else if (state.equals("TCP_CONNECT"))
			return R.string.state_tcp_connect;
		else
			return R.string.unknown_state;
	}

	public static void updateStatePause(OpenVPNManagement.pauseReason pauseReason) {
		switch (pauseReason) {
		case noNetwork:
			OpenVPN.updateStateString("NONETWORK", "", R.string.state_nonetwork, ConnectionStatus.LEVEL_NONETWORK);
			break;
		case screenOff:
			OpenVPN.updateStateString("SCREENOFF", "", R.string.state_screenoff, ConnectionStatus.LEVEL_VPNPAUSED);
			break;
		case userPause:
			OpenVPN.updateStateString("USERPAUSE", "", R.string.state_userpause, ConnectionStatus.LEVEL_VPNPAUSED);
			break;
		}
	}

	private static ConnectionStatus getLevel(String state){
		String[] noreplyet = {"CONNECTING","WAIT", "RECONNECTING", "RESOLVE", "TCP_CONNECT"}; 
		String[] reply = {"AUTH","GET_CONFIG","ASSIGN_IP","ADD_ROUTES"};
		String[] connected = {"CONNECTED"};
		String[] notconnected = {"DISCONNECTED", "EXITING"};

		for(String x:noreplyet)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;

		for(String x:reply)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

		for(String x:connected)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTED;

		for(String x:notconnected)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_NOTCONNECTED;

		return ConnectionStatus.UNKNOWN_LEVEL;
	}

	public synchronized static void removeStateListener(StateListener sl) {
		stateListener.remove(sl);
	}

	public static void updateStateString (String state, String msg) {
		int rid = getLocalizedState(state);
		ConnectionStatus level = getLevel(state);
		updateStateString(state, msg, rid, level);
	}

	public synchronized static void updateStateString(String state, String msg, int resid, ConnectionStatus level) {
		mLaststate= state;
		mLaststatemsg = msg;
		mLastStateresid = resid;
		mLastLevel = level;

		for (StateListener sl : stateListener) {
			sl.updateState(state,msg,resid,level);
		}
	}

	public static synchronized void updateByteCount(long in, long out) {
		long lastIn = mlastByteCount[0];
		long lastOut = mlastByteCount[1];
		long diffin = mlastByteCount[2] = in - lastIn;
		long diffout = mlastByteCount[3] = out - lastOut;

		mlastByteCount = new long[] {in,out,diffin,diffout};
		for(ByteCountListener bcl:byteCountListener){
			bcl.updateByteCount(in, out, diffin,diffout);
		}
	}

}
