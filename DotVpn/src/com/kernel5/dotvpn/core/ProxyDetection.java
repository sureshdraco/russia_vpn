package com.kernel5.dotvpn.core;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import android.util.Log;

import com.kernel5.dotvpn.Constants;

public class ProxyDetection {

    static SocketAddress detectProxy(String server_url) {
        // Construct a new url with https as protocol
        try {
            URL url = new URL(String.format("https://%s:%s", server_url, Constants.SERVER_PORT));
            Proxy proxy = getFirstProxy(url);

            if(proxy==null)
                return null;
            SocketAddress addr = proxy.address();
            if (addr instanceof InetSocketAddress) {
                return addr; 
            }
            
        } catch (Exception e) {
            Log.e(Constants.TAG, "Get proxy error", e);
        }
        return null;
    }

    static Proxy getFirstProxy(URL url) throws URISyntaxException {
        System.setProperty("java.net.useSystemProxies", "true");
        List<Proxy> proxylist = ProxySelector.getDefault().select(url.toURI());

        if (proxylist != null) {
            for (Proxy proxy: proxylist) {
                SocketAddress addr = proxy.address();

                if (addr != null) {
                    return proxy;
                }
            }

        }
        return null;
    }
}
