package com.kernel5.dotvpn;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.Activity;
import android.util.Log;

public final class ErrorUtils {

    public static void showErrorDialog(Activity caller, String title, String message, String action) {

     final Activity fcaller = caller;
     final String ftitle = title;
     final String fmessage = message;
     final String faction = action;
     
     caller.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        AlertDialog.Builder builder = new AlertDialog.Builder(fcaller);
        builder.setTitle(ftitle);
        builder.setMessage(fmessage);
        builder.setPositiveButton(faction, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });
        builder.show();
      }
     });

    }

    public static void showFatalErrorDialog(Activity caller, String title, String message, String action) {

     final Activity fcaller = caller;
     final String ftitle = title;
     final String fmessage = message;
     final String faction = action;
     
     caller.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        AlertDialog.Builder builder = new AlertDialog.Builder(fcaller);
        builder.setTitle(ftitle);
        builder.setMessage(fmessage);
        builder.setPositiveButton(faction, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
               fcaller.finish();
            }
        });
        builder.show();
      }
     });

    }

}

