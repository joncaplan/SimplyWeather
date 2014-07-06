// This is a simple date class which only handles local times and exists to parse the date format returned by NOAA.
package net.joncaplan;
import android.util.Log;

public class MyDate{
	
	private int year;
	private int month;
	private int day;
	private int hour_24;
	private int hour_12;
	private String hour;
	private String minute;
	private String second;
	private String AMPM;
	private static String DEBUG_TAG = "MyDate"; 
	
	// Input in in form YYYY-MM-DDTHH:MM:SS-HH:MM. "T" is a separator. Final HH:MM is offset from GMT.
	MyDate(String dateTime){
		try{
		int begin            = dateTime.indexOf("T")+1;            // Beginning of hour string.  
		String theDateString = dateTime.substring(0, begin-1);     // YYYY-MM-DD
		year                 = Integer.valueOf(theDateString.substring(0, 4));
		month                = Integer.valueOf(theDateString.substring(5, 7));
		day                  = Integer.valueOf(theDateString.substring(8, 10));
		hour_24              = Integer.valueOf(dateTime.substring(begin,   begin+2)); // The hour part of the date, in local time.
		minute               = dateTime.substring(begin+3, begin+5);
		second               = dateTime.substring(begin+6, begin+8); 
		hour_12              = hour_24%12;
		if (hour_12 == 0) {hour = "12";} else {hour = hour_12 + "";} 
		if (hour_24 > 12) {AMPM = "pm";} else {AMPM="am";}
		
		} catch (NumberFormatException e){
			Log.i(DEBUG_TAG, "NumberFormatException in MyDate class. " + e);
		}
	}
	
	// Accessor functions.
    public String  getDateTime(){ return getDate() + " " + getTime();}
    public String  getDate()    { return month + "/" + day;}
    public String  getTime()    { return hour_12 + ":" + minute + ":" + second + " " + AMPM;}
    public String  getAMPM()    { return AMPM;}
    public int     getYear()    { return year;}
    public int     getMonth()   { return month;}
    public int     getDay()     { return day;}
    public String  getHour()    { return hour;}
    public String  getMinute()  { return minute;}
    public String  getSecond()  { return second;}
}