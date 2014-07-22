// SimplyWeather (c) 2014 Jonathan Caplan.

package net.joncaplan;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

class ForecastManager {

    private final OpenDbHelper dbHelper;
    private static final String LOCATION_TABLE    = "location"; ///sdcard/net.joncaplan/simply_weather/
    private static final String LOCATION_ID       = "ID";
    private static final String FORECAST_LOCATION = "forecast_location";
    private static final String FORECAST_HTML     = "forecast_html";
    private static final String FORECAST_TIME     = "forecast_time";
    private static final String LATITUDE          = "latitude";
    private static final String LONGITUDE         = "longitude";
    private static final String SELECT_LOCATIONS  = "SELECT * FROM " + LOCATION_TABLE;
    private static final String DEBUG_TAG         = "ForecastManager "; // For log file.

    public ForecastManager(Context context) {
        //dbHelper = new OpenDbHelper(context, LOCATION_TABLE, LOCATION_ID + " INTEGER," + FORECAST_LOCATION + " TEXT," + FORECAST_HTML + " TEXT," + FORECAST_TIME + " INTEGER");    	
        dbHelper = new OpenDbHelper(context, LOCATION_TABLE, LOCATION_ID + " INTEGER," + FORECAST_LOCATION + " TEXT," + FORECAST_HTML + " TEXT," + FORECAST_TIME + " INTEGER," +
        		LATITUDE + " DOUBLE,"  + LONGITUDE + " DOUBLE");    	
    }

    /**
     * Insert a Forecast-value into database
     * @param forecastLocation location of the forecast.
     * @return success or fail
     */
    String insert(int locationId, String forecastLocation, String forecastHTML, double latitude, double longitude) {
    	long row;
        try {
        	Log.i(DEBUG_TAG, "Attempting forecast insert.");
            SQLiteDatabase sqlite = dbHelper.getWritableDatabase();  // Opens database, creating it first, if needed.
            ContentValues initialValues = new ContentValues();
            initialValues.put(LOCATION_ID,       locationId);
            initialValues.put(FORECAST_LOCATION, forecastLocation);
            initialValues.put(FORECAST_HTML,     forecastHTML);
            initialValues.put(FORECAST_TIME,     System.currentTimeMillis() /1000);
            initialValues.put(LATITUDE,          latitude);
            initialValues.put(LONGITUDE,         longitude);
            row = sqlite.insert(LOCATION_TABLE, null, initialValues);
            dbHelper.close(); // Close the database.
        } catch (SQLException sqlerror) {
            Log.v(DEBUG_TAG + "Insert into forecast table error: ", sqlerror.getMessage());
            dbHelper.close(); // Close the database.
            return "Insert error: SQL exception";
        }
        if (row == -1) return "Insert failure." ;
        return "Insert success. Added row #" + row;
    }
    
    public String insert(Forecast theForecast){ // Just a wrapper for the previous insert method, which takes the Forecast object rather than fields.
    	return insert(theForecast.id, theForecast.location, theForecast.forecastHTML, theForecast.latitude, theForecast.longitude);
    }
    
    public boolean update(Forecast theForecast){ // Update a forecast already stored in the database.
    	try{
        	Log.i(DEBUG_TAG, "Attempting forecast update forecast in database.");
            SQLiteDatabase sqlite = dbHelper.getWritableDatabase();  // Opens database, creating it first, if needed.
            ContentValues updateValues = new ContentValues();
            updateValues.put(LOCATION_ID,       theForecast.id);
            updateValues.put(FORECAST_LOCATION, theForecast.location); // TODO: Check whether I need to update location and id. These should be unnecessary.
            updateValues.put(FORECAST_HTML,     theForecast.forecastHTML);
            updateValues.put(FORECAST_TIME,     System.currentTimeMillis() /1000);
            updateValues.put(LATITUDE,          theForecast.latitude); // Updating these values because old database version lacked latitude and longitude, so updated locations will get them.
            updateValues.put(LONGITUDE,         theForecast.longitude);
            boolean success = sqlite.update(LOCATION_TABLE, updateValues, LOCATION_ID + "=" + theForecast.id, null) > 0;
            dbHelper.close(); // Close the database.
        	Log.i(DEBUG_TAG, "Forecast updated in database.");            
            return success;

    	}catch (SQLException sqlerror){
    		Log.e(DEBUG_TAG, "Update forecast table error: " + sqlerror.getMessage());
            dbHelper.close(); // Close the database.
    	}
    	catch(IllegalStateException e){
    		Log.e(DEBUG_TAG, " Update forecast table error: " + e);
    	}
    	return false;
    }
    
    public int getLocationID(String theLocation){ // Find the ID of a location if it already exists in the database.
    	Cursor crsr = null;
    	try{
        	Log.i(DEBUG_TAG, "Attempting getLocationID.");
            SQLiteDatabase sqliteDB = dbHelper.getReadableDatabase();
            crsr = sqliteDB.rawQuery(SELECT_LOCATIONS, null);
            crsr.moveToFirst();
            Forecast aForecast;

            theLocation = theLocation.trim();
            for (int i = 0; i < crsr.getCount(); i++){ // Build the list of locations.
            	aForecast = new Forecast(crsr.getInt(0), crsr.getString(1),crsr.getString(2), crsr.getDouble(3), crsr.getDouble(4) ); 
            	if (aForecast.location.equalsIgnoreCase(theLocation)){
                	Log.i(DEBUG_TAG, "Got getLocationID for existing location.");
                    crsr.close();
                	dbHelper.close(); // Close the database.
            		return aForecast.id;
            	}
                crsr.moveToNext();
            }
    	}
    	catch(SQLException sqlerror){
            Log.v(DEBUG_TAG + "Query table for existing forecast location ID error: ", sqlerror.getMessage());
            if (!(crsr == null)) {crsr.close();}
            dbHelper.close(); // Close the database.
            return -2; // Error. Do not try to save forecast.
    	}
    	catch(IllegalStateException e){
            Log.v(DEBUG_TAG + "Query table for existing forecast location ID error: ", e.getMessage());
            if (!(crsr == null)) {crsr.close();}
            dbHelper.close(); // Close the database.
            return -2; // Error. Do not try to save forecast.
    	}
    	Log.i(DEBUG_TAG, "getLocationID: Location not yet in database.");
        crsr.close();
        dbHelper.close(); // Close the database.
		return -1; // theLocation does not exist in database.
    }

    /**
     * Get all available forecasts
     * @return List of forecasts
     */
    public ArrayList<Forecast> getForecasts() {
    	
    	//String clear_result = clear();
    	//String drop_result  = drop();
    	//String createTableResult = createForecastTable();
    	//String result = insert(78, "Somewhere2", "Test forecast2", 100); 
    	
        ArrayList<Forecast> forecasts = new ArrayList<Forecast>();
        Cursor crsr = null;
        try {    	
        	Log.i(DEBUG_TAG, "Attempting getForecasts.");
	        //SQLiteDatabase sqliteDB = dbHelper.getReadableDatabase();
        	SQLiteDatabase sqliteDB;
	        sqliteDB = dbHelper.getWritableDatabase();
	        
	        crsr = sqliteDB.rawQuery(SELECT_LOCATIONS, null);
	        crsr.moveToFirst();
	
	        for (int i = 0; i < crsr.getCount(); i++){ // Build the list of locations.
	        	forecasts.add(new Forecast(crsr.getInt(0), crsr.getString(1), crsr.getString(2), crsr.getInt(3), crsr.getDouble(4), crsr.getDouble(5)));
	            crsr.moveToNext();
	        }
        }
        catch (SQLException sqlerror){ // Report SQL error in place of a real forecast.
            Log.v(DEBUG_TAG + "SQL exception on getForecasts() ", sqlerror.getMessage());
        	Forecast forecast = new Forecast(-99,"xxxxx","SQL exception on getForecasts() "+ sqlerror, 0, 0);
        	ArrayList<Forecast> forecasts2 = new ArrayList<Forecast>();
        	forecasts2.add(forecast);
            if (!(crsr == null)) {crsr.close();}
        	dbHelper.close(); // Close the database.
        	return forecasts2;
    	}
        catch (Exception e){
            Log.v(DEBUG_TAG + "Unknown exception on getForecasts() ", e.getMessage());
            if (!(crsr == null)) {crsr.close();}
        	dbHelper.close(); // Close the database.
            return forecasts;
    	}
        catch (Error e){
            Log.v(DEBUG_TAG + "Unknown ERROR on getForecasts() ", e.getMessage());            	
        }
    	Log.i(DEBUG_TAG, "Completed getForecasts.");
        if (!(crsr == null)) {crsr.close();}
    	dbHelper.close(); // Close the database.
        return forecasts;
    }
    
    public boolean deleteForecast (int id){
    	try {
    		Log.i(DEBUG_TAG, "Attempting deleteForecast of id: " + id);
    		
            SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
            sqlite.execSQL("delete from " + LOCATION_TABLE + " where id =" + id);
    		
    	} catch (SQLException sqlerror){ 
    		Log.i(DEBUG_TAG, "Error dropping forecast " + id + " from " + LOCATION_TABLE + " \n" + sqlerror);
        	dbHelper.close(); // Close the database.
    		return false;
    	}
    	dbHelper.close(); // Close the database.
    	return true;
    }

    /**
     * Clear the table
     */
    public String clear() {
        try { 
        	Log.i(DEBUG_TAG, "Attempting clear table.");
            SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
            sqlite.delete(LOCATION_TABLE, "", null);
        } catch (SQLException sqlerror) {
            Log.v(DEBUG_TAG + "Delete from table error: ", sqlerror.getMessage());
            return " Error clearing table. ";
        }
        return " Table cleared successfully. ";
    }
    
    public String drop(){
    	try{
        	Log.i(DEBUG_TAG, "Attempting table drop.");
            SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
            sqlite.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
    	} catch (SQLException sqlerror){
        	dbHelper.close(); // Close the database.
    		return " Error dropping table. " + sqlerror;
    	}
    	dbHelper.close(); // Close the database.
    	return " Table dropped successfully. ";
    } 

    String createForecastTable(){
    	try{
        	Log.i(DEBUG_TAG, "Attempting create table.");
	        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
	        dbHelper.onCreate(sqlite);
	        //sqlite.rawQuery("CREATE TABLE "+LOCATION_TABLE+" "+LOCATION_ID+" INTEGER", null);
    	}catch(SQLException sqlerror){
            Log.v(DEBUG_TAG + "Error creating forecast table. ", sqlerror.getMessage());
        	dbHelper.close(); // Close the database.
    		return "Error creating forecast table. " + sqlerror;
    	}
    	dbHelper.close(); // Close the database.
    	return "Created forecast table.";
    }
	
}