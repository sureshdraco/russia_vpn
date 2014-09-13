package com.kernel5.dotvpn.rest;

import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Base64;
import android.util.Pair;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.kernel5.dotvpn.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

// the real classic network layer
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

public final class JacksonRequests {

    private static final String MediaTypeJson = "application/json";

    public static <T> T getForJson(final String url, final Class<T> clazz) throws Exception {

        Log.v( Constants.TAG, "Getting : " + url );
        String GetURL = url;
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 30000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 60000; // 60 seconds
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

        HttpGet httpGet = new HttpGet(GetURL);
        httpGet.setHeader("Accept", MediaTypeJson);
        httpGet.setHeader("Content-type", MediaTypeJson);

        HttpResponse httpResponse=null; 
        try {

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            Log.v( Constants.TAG, url + " : Got answer : " + content );
            return new ObjectMapper().readValue(content, clazz);

        } catch ( Exception e ) {
            Log.v( Constants.TAG, "getForJson failed for url : " + url, e );
            throw e; 
        }
    }

    public static <T> T postForJson(final String url, final Object postObject, final Class<T> clazz) throws Exception {

        HttpResponse httpResponse = postForResponse(url, postObject);
        HttpEntity entity = httpResponse.getEntity();
        String content = EntityUtils.toString(entity);
        Log.v( Constants.TAG, url + " : Got answer : " + content );
        return new ObjectMapper().readValue(content, clazz);
    }

    public static HttpResponse postForResponse(final String url, final Object postObject) throws Exception {

        final ObjectMapper objectMapper = new ObjectMapper();
        String params = objectMapper.writeValueAsString(postObject);
        Log.e(Constants.TAG, "postForResponse postObject : " + params);

        // deactivate certificate checking
//        Log.v( Constants.TAG, "Setting https host verifier" );
//        HostnameVerifier hostnameVerifier =
//                org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        DefaultHttpClient client = new DefaultHttpClient();

        // make sure certificate url and our url don't become a conflict 
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);

        // Set verifier
//        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        
        String postUrl = url;
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 30000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 60000; // 60 seconds
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, httpParameters);
  
        HttpPost httpPost = new HttpPost(postUrl);
        httpPost.setHeader("Accept", MediaTypeJson);
        httpPost.setHeader("Content-type", MediaTypeJson);
        httpPost.setEntity(new StringEntity(params));

        HttpResponse httpResponse=null;

        try {
           httpResponse = httpClient.execute(httpPost);
        } catch ( Exception e ) {
           Log.v( Constants.TAG, "postForJson failed for url : " + url + " - " + e.getMessage() );
           throw e; 
        }

        return httpResponse;
    }

    public static <T> T putForJson(String url, Object putObject, Class<T> clazz) throws Exception {

        final ObjectMapper objectMapper = new ObjectMapper();
        String params = objectMapper.writeValueAsString(putObject); 
        Log.e(Constants.TAG, "putForJson putObject : " + params);

        String putUrl = url;
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 30000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 60000; // 60 seconds
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        DefaultHttpClient httpClient = new DefaultHttpClient(null, httpParameters);
  
        HttpPut httpPut = new HttpPut(putUrl);
        httpPut.setHeader("Accept", MediaTypeJson);
        httpPut.setHeader("Content-type", MediaTypeJson);
        httpPut.setEntity(new StringEntity(params));

        try {
            HttpResponse response = httpClient.execute(httpPut);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            Log.v( Constants.TAG, url + " : Got answer : " + content );
            return new ObjectMapper().readValue(content, clazz);
        } catch ( Exception e ) {
            Log.v( Constants.TAG, "putForJson failed for : " + url, e );
            throw e; 
        }
    }

    public static void establishTrustManager() {
    	
    	X509TrustManager easyTrustManager = new X509TrustManager() {
    		
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            
			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
				
			}

        };

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {easyTrustManager};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}
