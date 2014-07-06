package net.joncaplan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class OpenDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private String createStatement = "";
    private static final String DEBUG_TAG = "ForecastManager "; // For log file.
    
    /**
     * Creates the OpenDbHelper 
     * 
     * @param context app-context
     * @param tableName name of the table to open/create
     * @param fields fields of the table to create
     */
    public OpenDbHelper(Context context, String tableName, String fields) {
        super(context, tableName, null, DATABASE_VERSION);
        
			this.createStatement  = "CREATE TABLE ";
			this.createStatement += tableName + " (";
			this.createStatement += fields + ");";
        }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(this.createStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
		Log.i(DEBUG_TAG, "Upgrading database from version " + oldVersion + "to version " + newVersion + ".");
		Log.i(DEBUG_TAG, "Attempting to add latitude and longitude columns to location table.");
    	try {
	    	db.execSQL("ALTER TABLE location ADD COLUMN latitude  DOUBLE;");
	    	db.execSQL("ALTER TABLE location ADD COLUMN longitude DOUBLE;");
    	} catch (Exception e){
			Log.e(DEBUG_TAG, "Error adding latitude and longitude columns to location table." + e);
    	}
		Log.i(DEBUG_TAG, "Added latitude and longitude columns to location table.");
    }
}