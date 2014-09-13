package com.kernel5.dotvpn;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.crypto.spec.DESKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import android.util.Base64;

/**
* Utility class to crypt and decrypt lockcode and more
*
* @author chevil
*/
public class DESEncrypt {

   private static final String TAG = Constants.TAG;

   private Context mContext;
   private DESKeySpec mKeySpec=null;
   private SecretKeyFactory mKeyFactory=null;
   private SecretKey mKey=null;

   public DESEncrypt( String key) {
     try {
      mKeySpec = new DESKeySpec(key.getBytes("UTF8"));
      mKeyFactory = SecretKeyFactory.getInstance("DES");
      mKey = mKeyFactory.generateSecret(mKeySpec);
    } catch ( Exception e ) {
      Log.e( TAG, "could not create cipher classes", e );
    }
   }

   public String encrypt(String input) {
     try {
      byte[] cleartext = input.getBytes("UTF8"); 
      Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
      cipher.init(Cipher.ENCRYPT_MODE, mKey);
      return Base64.encodeToString(cipher.doFinal(cleartext), Base64.DEFAULT);
    } catch ( Exception e ) {
      Log.e( TAG, "could not encrypt data", e );
      return "";
    }
   }

   public String decrypt(String input) {
     try {
      byte[] encryptedPwdBytes = Base64.decode(input, Base64.DEFAULT);
      Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
      cipher.init(Cipher.DECRYPT_MODE, mKey);
      return new String( cipher.doFinal(encryptedPwdBytes) );
    } catch ( Exception e ) {
      Log.e( TAG, "could not decrypt data", e );
      return "";
    }
   }

}
