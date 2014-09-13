package com.kernel5.dotvpn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TrafficSource {

  private final String TAG = Constants.TAG;

  // Database fields
  private SQLiteDatabase database;
  private TrafficDbHelper dbHelper;
  private boolean isOpen=false;
  private Context mContext=null;

  public TrafficSource(Context context) {
    this.dbHelper = new TrafficDbHelper(context);
    this.mContext=context;
  }

  public void open() throws SQLException {
    this.database = this.dbHelper.getWritableDatabase();
    Log.v( this.TAG, "opened database " + this.database );
    this.isOpen=true;
  }

  public void close() {
    this.dbHelper.close();
    this.isOpen=false;
  }

  public boolean isOpen() {
    return this.isOpen;
  }

  // record a traffic point
  public long recordTraffic( long in, long out, long diffin, long diffout ) {
    ContentValues values = new ContentValues();
    values.put(TrafficDb.COLUMN_TIMESTAMP, System.currentTimeMillis() );
    values.put(TrafficDb.COLUMN_TOTAL_IN, in );
    values.put(TrafficDb.COLUMN_TOTAL_OUT, out );
    values.put(TrafficDb.COLUMN_INSTANT_IN, diffin );
    values.put(TrafficDb.COLUMN_INSTANT_OUT, diffout );
    long insertId = this.database.insert(TrafficDb.TABLE_TRAFFIC, null, values);
    Log.v( this.TAG, "recorded traffic" );
    return insertId;
  }

  // clear traffic
  public void clearTraffic() {
    this.database.delete(TrafficDb.TABLE_TRAFFIC, null, null);
    Log.v( this.TAG, "cleared traffic" );
  }

  // get last traffic data
  public int getTraffic(float[] traffins, float[] traffouts, long[] totalin, long[] totalout, long[] max ) {

    int count=0;
    String query;
    Cursor cursor;

    int nbRecords = traffins.length;
    for ( int ti=0; ti<nbRecords; ti++ )
    {
        traffins[ti]=0.0f;
        traffouts[ti]=0.0f;
    }
    totalin[0]=0;
    totalout[0]=0;
    max[0]=0;

    query = "select " + TrafficDb.TABLE_TRAFFIC + "." + TrafficDb.COLUMN_INSTANT_IN + "," + TrafficDb.TABLE_TRAFFIC + "." + TrafficDb.COLUMN_INSTANT_OUT + "," + TrafficDb.TABLE_TRAFFIC + "." + TrafficDb.COLUMN_TOTAL_IN + "," + TrafficDb.TABLE_TRAFFIC + "." + TrafficDb.COLUMN_TOTAL_OUT + " from " + TrafficDb.TABLE_TRAFFIC + " order by " + TrafficDb.TABLE_TRAFFIC + "." + TrafficDb.COLUMN_TIMESTAMP + " desc limit " + nbRecords;
    Log.v( this.TAG, query );
    cursor = this.database.rawQuery(query, null);

    cursor.moveToFirst();
    while( !cursor.isAfterLast() )
    {
       if ( cursor.getLong(0) > max[0] ) max[0]=cursor.getLong(0);
       traffins[nbRecords-count-1]=(float)cursor.getLong(0);
       if ( cursor.getLong(1) > max[0] ) max[0]=cursor.getLong(1);
       traffouts[nbRecords-count-1]=(float)cursor.getLong(1);
       if ( count == 0 )
       {
          totalin[0]=cursor.getLong(2);
          totalout[0]=cursor.getLong(3);
       }
       cursor.moveToNext();
       count++;
    }

    cursor.close();
    return count;
  }
} 
