package com.kernel5.dotvpn.core;

import java.security.InvalidKeyException;

public class NativeUtils {
	public static native byte[] rsasign(byte[] input,int pkey) throws InvalidKeyException;
	static native void jniclose(int fdint);

	static {
		System.loadLibrary("opvpnutil");
	}
}
