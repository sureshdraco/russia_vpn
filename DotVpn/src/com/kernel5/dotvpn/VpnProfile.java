package com.kernel5.dotvpn;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.*;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

public class VpnProfile implements  Serializable{
	// Note that this class cannot be moved to core where it belongs since 
	// the profile loading depends on it being here
	// The Serializable documentation mentions that class name change are possible
	// but the how is unclear
	// 

	private static final long serialVersionUID = 7085688938959334563L;
	public static final int TYPE_CERTIFICATES=0;
	public static final int TYPE_PKCS12=1;
	public static final int TYPE_KEYSTORE=2;
	public static final int TYPE_USERPASS = 3;
	public static final int TYPE_STATICKEYS = 4;
	public static final int TYPE_USERPASS_CERTIFICATES = 5;
	public static final int TYPE_USERPASS_PKCS12 = 6;
	public static final int TYPE_USERPASS_KEYSTORE = 7;

	public static final int X509_VERIFY_TLSREMOTE=0;
	public static final int X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING=1;
	public static final int X509_VERIFY_TLSREMOTE_DN=2;
	public static final int X509_VERIFY_TLSREMOTE_RDN=3;
	public static final int X509_VERIFY_TLSREMOTE_RDN_PREFIX=4;

	// Don't change this, not all parts of the program use this constant
	public static final String EXTRA_PROFILEUUID = "com.kernel5.dotvpn.profileUUID";
	public static final String INLINE_TAG = "[[INLINE]]";
//	private static final String OVPNCONFIGFILE = "android.conf";

	public transient String mTransientPW=null;
	public transient String mTransientPCKS12PW=null;
	private transient PrivateKey mPrivateKey;

	// variable named wrong and should haven beeen transient
	// but needs to keep wrong name to guarante loading of old
	// profiles
	public transient boolean profileDleted=false;

	public static String DEFAULT_DNS1="8.8.8.8";
	public static String DEFAULT_DNS2="8.8.4.4";

	// Public attributes, since I got mad with getter/setter
	// set members to default values
	private UUID mUuid;
	public int mAuthenticationType = TYPE_KEYSTORE ;
	public String mName;
	public String mAlias;
	public String mClientCertFilename;
	public String mTLSAuthDirection="";
	public String mTLSAuthFilename;
	public String mClientKeyFilename;
	public String mCaFilename;
	public boolean mUseLzo=true;
	public String mServerPort= "1194" ;
	public boolean mUseUdp = true;
	public String mPKCS12Filename;
	public String mPKCS12Password;
	public boolean mUseTLSAuth = false;
	public String mServerName = "dotvpn.kernel5.com" ;
	public String mDNS1=DEFAULT_DNS1;
	public String mDNS2=DEFAULT_DNS2;
	public String mIPv4Address;
	public String mIPv6Address;
	public boolean mOverrideDNS=false;
	public String mSearchDomain="kernel5.de";
	public boolean mUseDefaultRoute=true;
	public boolean mUsePull=true;
	public String mCustomRoutes;
	public boolean mCheckRemoteCN=false;
	public boolean mExpectTLSCert=true;
	public String mRemoteCN="";
	public String mPassword="";
	public String mUsername="";
	public boolean mRoutenopull=false;
	public boolean mUseRandomHostname=false;
	public boolean mUseFloat=false;
	public boolean mUseCustomConfig=false;
	public String mCustomConfigOptions="";
	public String mVerb="1";
	public String mCipher="";
	public boolean mNobind=false;
	public boolean mUseDefaultRoutev6=true;
	public String mCustomRoutesv6="";
	public String mKeyPassword="";
	public boolean mPersistTun = false;
	public String mConnectRetryMax="2";
	public String mConnectRetry="2";
	public boolean mUserEditable=true;
	public String mAuth="";
	public int mX509AuthType=X509_VERIFY_TLSREMOTE_RDN;

	public static final String MINIVPN = "miniopenvpn";

	public void clearDefaults() {
		this.mServerName="unkown";
		this.mUsePull=false;
		this.mUseLzo=false;
		this.mUseDefaultRoute=false;
		this.mUseDefaultRoutev6=false;
		this.mExpectTLSCert=false;
		this.mPersistTun = false;
	}

	public static String openVpnEscape(String unescaped) {
		if(unescaped==null)
			return null;
		String escapedString = unescaped.replace("\\", "\\\\");
		escapedString = escapedString.replace("\"","\\\"");
		escapedString = escapedString.replace("\n","\\n");

		if (escapedString.equals(unescaped) && !escapedString.contains(" ") && !escapedString.contains("#"))
			return unescaped;
		else
			return '"' + escapedString + '"';
	}

	public VpnProfile(String name) {
		//mUuid = UUID.randomUUID();
		this.mUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		this.mName = name;
	}

	public UUID getUUID() {
		return this.mUuid;
	}

	public String getName() {
		return this.mName;
	}

	private static String getVersionEnvString(Context c) {
		String version="unknown";
		try {
			PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
			version = packageinfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			
		}
		return  String.format(Locale.US,"setenv IV_OPENVPN_GUI_VERSION \"%s %s\"\n",c.getPackageName(),version);
	}

	//! Put inline data inline and other data as normal escaped filename
	private static String insertFileData(String cfgentry, String filedata) {
		if(filedata==null) {
			// TODO: generate good error
			return String.format(Locale.US,"%s %s\n",cfgentry,"missing");
		}else if(filedata.startsWith(VpnProfile.INLINE_TAG)){
			String datawoheader = filedata.substring(VpnProfile.INLINE_TAG.length());
			return String.format(Locale.US,"<%s>\n%s\n</%s>\n",cfgentry,datawoheader,cfgentry);
		} else {
			return String.format(Locale.US,"%s %s\n",cfgentry,openVpnEscape(filedata));
		}
	}

//	private static boolean nonNull(String val) {
//		if(val == null || val.equals(""))
//			return false;
//		else
//			return true;
//	}

	private Collection<String> getCustomRoutes() {
		Vector<String> cidrRoutes=new Vector<String>();
		if(this.mCustomRoutes==null) {
			// No routes set, return empty vector
			return cidrRoutes;
		}
		for(String route:this.mCustomRoutes.split("[\n \t]")) {
			if(!route.equals("")) {
				String cidrroute = cidrToIPAndNetmask(route);
				if(cidrRoutes == null)
					return null;

				cidrRoutes.add(cidrroute);
			}
		}

		return cidrRoutes;
	}

	private Collection<String> getCustomRoutesv6() {
		Vector<String> cidrRoutes=new Vector<String>();
		if(this.mCustomRoutesv6==null) {
			// No routes set, return empty vector
			return cidrRoutes;
		}
		for(String route:this.mCustomRoutesv6.split("[\n \t]")) {
			if(!route.equals("")) {
				cidrRoutes.add(route);
			}
		}

		return cidrRoutes;
	}

	private String cidrToIPAndNetmask(String route) {
		String[] parts = route.split("/");

		// No /xx, assume /32 as netmask
		if (parts.length ==1)
			parts = (route + "/32").split("/");

		if (parts.length!=2)
			return null;
		int len;
		try { 
			len = Integer.parseInt(parts[1]);
		}	catch(NumberFormatException ne) {
			return null;
		}
		if (len <0 || len >32)
			return null;


		long nm = 0xffffffffl;
		nm = (nm << (32-len)) & 0xffffffffl;

		String netmask =String.format(Locale.US,"%d.%d.%d.%d", (nm & 0xff000000) >> 24,(nm & 0xff0000) >> 16, (nm & 0xff00) >> 8 ,nm & 0xff  );	
		return parts[0] + "  " + netmask;
	}

	// Used by the Array Adapter
	@Override
	public String toString() {
		return this.mName;
	}

	public String getUUIDString() {
		return this.mUuid.toString();
	}

	public PrivateKey getKeystoreKey() {
		return this.mPrivateKey;
	}

}
