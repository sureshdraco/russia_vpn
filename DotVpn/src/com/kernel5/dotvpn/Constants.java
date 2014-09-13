package com.kernel5.dotvpn;

import android.graphics.Typeface;

public class Constants {
   public static final String AppName = "Dot Vpn";
   public static final String TAG = "Dotvpn";
   
//   public static final String OVPNCONFIGFILE = "android.conf";
   public static final String VPN_CONFIG_DE = "vpn_connect_de.conf";
   public static final String VPN_CONFIG_FR = "vpn_connect_fr.conf";
   public static final String VPN_CONFIG_JP = "vpn_connect_jp.conf";
   public static final String VPN_CONFIG_NL = "vpn_connect_nl.conf";
   public static final String VPN_CONFIG_RU = "vpn_connect_ru.conf";
   public static final String VPN_CONFIG_SE = "vpn_connect_se.conf";
   public static final String VPN_CONFIG_SG = "vpn_connect_sg.conf";
   public static final String VPN_CONFIG_UK = "vpn_connect_uk.conf";
   public static final String VPN_CONFIG_US = "vpn_connect_us.conf";
   
// public static final String SERVER_NAME = "vpn-de.dotvpn.com"; // "46.165.200.70";
   public static final String SERVER_NAME_DE = "vpn-de.dotvpn.com";
   public static final String SERVER_NAME_UK = "vpn-uk.dotvpn.com";
   public static final String SERVER_NAME_US = "vpn-us.dotvpn.com";
   public static final String SERVER_NAME_FR = "vpn-fr.dotvpn.com";
   public static final String SERVER_NAME_JP = "vpn-jp.dotvpn.com";
   public static final String SERVER_NAME_NL = "vpn-nl.dotvpn.com";
   public static final String SERVER_NAME_RU = "vpn-ru.dotvpn.com";
   public static final String SERVER_NAME_SG = "vpn-sg.dotvpn.com";
   public static final String SERVER_NAME_SE = "vpn-se.dotvpn.com";
   
   public static final String FONT_RR="fonts/Roboto-Regular.ttf";
   public static final String FONT_SO="fonts/OpenSans-Regular.ttf";
   
   public static final String SERVER_PORT = "1194";
   public static final String WHOIS_SERVER = "whois.internic.net";
   public static final int WHOIS_PORT = 43;

   public static final String PATH_SEPARATOR = "/";
   public static final String API_VERSION = "2";
   public static final String USER_DOMAIN = "user";
   public static final String REST_SERVER = "https://api.dotvpn.com";
   public static final String SIGNUP_WEB_URL = "https://vemeo.com/en/#signup";
   public static final String FORGOT_WEB_URL = "https://vemeo.com/en/#reminder";
   public static final String MANAGE_WEB_URL = "https://vemeo.com/en/private";
   public static final String SUPPORT_WEB_URL = "https://vemeo.com/en/faq";
   public static final String SIGNIN_URL = REST_SERVER + PATH_SEPARATOR + API_VERSION + PATH_SEPARATOR + USER_DOMAIN + PATH_SEPARATOR + "signin";
   public static final String SIGNUP_URL = REST_SERVER + PATH_SEPARATOR + API_VERSION + PATH_SEPARATOR + USER_DOMAIN + PATH_SEPARATOR + "signup";
   public static final String INFO_URL = REST_SERVER + PATH_SEPARATOR + API_VERSION + PATH_SEPARATOR + USER_DOMAIN + PATH_SEPARATOR + "info";
   public static final String IP_URL = REST_SERVER + PATH_SEPARATOR + API_VERSION + PATH_SEPARATOR + USER_DOMAIN + PATH_SEPARATOR + "ip";

   public static final String OAUTH_TOKEN	=	"oauth_token";
   public static final String BAUTH_TOKEN	=	"bauth_token";
   
   public static final int NB_GRAPH_POINTS=100;
   public static final int NB_GRAPH_LABELS=5;

   // global variables
   public static Typeface rrFont;
   public static Typeface osFont;
   public static boolean isBigScreen;
}
