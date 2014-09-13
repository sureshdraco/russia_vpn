package com.kernel5.dotvpn;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TrafficDb {

  private static final String TAG = Constants.TAG;

  // Categories table
  public static final String TABLE_TRAFFIC = "traffic";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_TIMESTAMP = "timestamp";
  public static final String COLUMN_TOTAL_IN = "total_in";
  public static final String COLUMN_TOTAL_OUT = "total_out";
  public static final String COLUMN_INSTANT_IN = "instant_in";
  public static final String COLUMN_INSTANT_OUT = "instant_out";

  // Database creation SQL statement
  private static final String DATABASE_CREATE_TRAFFIC = "create table " 
      + TABLE_TRAFFIC
      + " (" 
      + COLUMN_ID + " integer primary key autoincrement, " 
      + COLUMN_TIMESTAMP + " long not null, " 
      + COLUMN_TOTAL_IN + " long not null, " 
      + COLUMN_TOTAL_OUT + " long not null, " 
      + COLUMN_INSTANT_IN + " long not null, " 
      + COLUMN_INSTANT_OUT + " long not null " 
      + ");";

  public static void onCreate(SQLiteDatabase database) {
      database.execSQL(DATABASE_CREATE_TRAFFIC);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
      int newVersion) {
    Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAFFIC);
    onCreate(database);
  }
} 
