package com.kernel5.dotvpn.core;

import java.util.Locale;
import android.util.Log;

class CIDRIP{
  String mIp;
  int len;
        private static final String TAG = "CIDRIP";
  
  public CIDRIP(String ip, String mask){
    mIp=ip;
    long netmask=getInt(mask);

    // Add 33. bit to ensure the loop terminates
    netmask += 1l << 32;

    int lenZeros = 0;
    while((netmask & 0x1) == 0) {
      lenZeros++;
      netmask = netmask >> 1;
    }
    // Check if rest of netmask is only 1s
    if(netmask != (0x1ffffffffl >> lenZeros)) {
      // Asume no CIDR, set /32
      len=32;
    } else {
      len =32 -lenZeros; 
    }

  }

  public CIDRIP(String address, int prefix_length) {
    len = prefix_length;
    mIp = address;
  }

  @Override
  public String toString() {
                Log.v( TAG, "CIDRIP : " + String.format(Locale.US,"%s/%d",mIp,len) );
    return String.format(Locale.US,"%s/%d",mIp,len);
  }

  public boolean normalise(){
    long ip=getInt(mIp);

    long newip = ip & (0xffffffffl << (32 -len));
    if (newip != ip){
      mIp = String.format(Locale.US,"%d.%d.%d.%d", (newip & 0xff000000) >> 24,(newip & 0xff0000) >> 16, (newip & 0xff00) >> 8 ,newip & 0xff);
      return true;
    } else {
      return false;
    }
  }

  static long getInt(String ipaddr) {
    String[] ipt = ipaddr.split("\\.");
    long ip=0;

    ip += Long.parseLong(ipt[0])<< 24;
    ip += Integer.parseInt(ipt[1])<< 16;
    ip += Integer.parseInt(ipt[2])<< 8;
    ip += Integer.parseInt(ipt[3]);

    return ip;
  }

  public long getInt() {
    return getInt(mIp);
  }
 
}
