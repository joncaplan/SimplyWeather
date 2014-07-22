// SimplyWeather (c) 2014 Jonathan Caplan.

package net.joncaplan;

import java.util.ArrayList;

//import net.joncaplan.simply_weather_free.SimplyWeatherFree;

//import net.joncaplan.simply_weather_free.SimplyWeather;

import android.util.Log;

class Forecast {
	public int    id;           // Just a database ID. When an ID is deleted it is not reused, unless it is the last ID. (Holes in sequence are not filled.)
	public String location;     // The location as the user has entered it, as a ZIP code, City & State or whatever future format becomes allowed.
	public double latitude;     // Latitude of this location. Values over 180 indicate location not set.
	public double longitude;    // Longitude of this location.*/
	public String forecastHTML; // The forecast in HTML, all ready for display.
	public long   forecastTime; // System time in ms.
    private static final String DEBUG_TAG         = "Forecast class"; // For log file.

	public ArrayList<String>   temperatures;    // 3-hourly temperature. [Fahrenheit]
	public ArrayList<String>   wind_speeds;     // 3-hourly wind speed.  [in KNOTS]
	public ArrayList<String>   wind_directions; // 3-hourly wind speed.  [in degrees]
	public ArrayList<String>   hazards;         // 1-hourly wind speed.  [as text]
	
	public ArrayList<String>   times_1h;        // 1-hourly time stamps. [Local time. Format: YYYY-MM-DDTHH:MM:SS-HH:MM "T" is time-date separator. HH:MM at end is offset from GMT] 
	public ArrayList<String>   times_3h;        // 3-hourly time stamps. [Local time. Format: YYYY-MM-DDTHH:MM:SS-HH:MM "T" is time-date separator. HH:MM at end is offset from GMT] 
	
	// This constructor is used when a new forecast is received from the weather service.
	public Forecast(int id, String location, String forecastHTML, double latitude, double longitude){ 
		this.id           = id;
		this.location     = location;
		this.latitude     = latitude;
		this.longitude    = longitude;
		this.forecastHTML = forecastHTML;
		this.forecastTime = System.currentTimeMillis() /1000;
	}
	
	// This constructor used when loading a forecast object from the database.
	public Forecast(int id, String location, String forecastHTML, int forecastTime, double latitude, double longitude){ 
		this.id           = id;
		this.location     = location;
		this.latitude     = latitude;
		this.longitude    = longitude;
		this.forecastHTML = forecastHTML;
		this.forecastTime = forecastTime;
	}
	
	// Update fields for forecast when a forecast from NOAA has been updated.
	public void setForecast( int id, String location, String forecastHTML, double latitude, double longitude){
		this.id           = id;       
		this.location     = location;
		this.latitude     = latitude;
		this.longitude    = longitude;
		this.forecastHTML = forecastHTML;
		forecastTime      = System.currentTimeMillis() /1000;
	}
	
	public String toString() {
		return location; // Return the location rather than the text, since this is what is needed for the drop down (spinner).
	}
		
    // Return an element of wind_speeds as a String in MPH.
    String getWindSpeedMPH(Integer i){
    	String speedMPH = "";
    	try{
    		speedMPH = Math.round(Double.valueOf(wind_speeds.get(i))*1.15077945) + "";
    	}catch(NumberFormatException e){
    		Log.i(DEBUG_TAG, "NumberFormatException in getWindSpeedMPH. " + e);
    	}
    	return speedMPH;
    }
    
    public String getHourlyHTMLTable(){
    	String theContent;
    	try{
	    	if (times_3h != null){ // See if the hourly data has been loaded. 
		    	theContent = "<html><center><H3><font color=\"#000088\">"+ location+"</font></h3></center>" ;
		    	theContent += "<center><table><tr>" +
		    				  "<td width=\"70\"><b>Date</b></td>" +
		    				  "<td width=\"70\"><b>Time</b></td>" +
		    				  "<td width=\"70\"><b>Temp.(F)</td>" +
		    				  "<td width=\"60\" colspan =\"2\"><b><center>Wind (mph)</center></b></td> </tr>";
				//if (log_level > 0) {Log.i(DEBUG_TAG, "Building 3-hourly HTML. currentForecast.times_3h.size()="+times_3h.size());}
				String lastDate   = "";
				String lastAMPM   = "";
				String theBGColor; // Background for rows.
				int    bgColorIndex = 1;
		    	for (int i=0; i<times_3h.size(); i++){  // Iterate through each time period, adding a line to the table.
					//if (log_level > 0) {Log.i(DEBUG_TAG, "Building hourly HTML. i="+i);} 
					MyDate theDate = new MyDate(times_3h.get(i));
					String date = theDate.getDate();
					String hour = theDate.getHour();
					String AMPM = theDate.getAMPM();
					String displayDate = date;
					String displayAMPM = AMPM;
					if (date.equals(lastDate)) {
						displayDate = ""; // Don't repeatedly display the same date.
					}
					if (AMPM.equals(lastAMPM)) {
						displayAMPM = ""; // Don't repeatedly display the AM/PM.
					}
					if (i==0){displayDate = "Today";}
					bgColorIndex = (++bgColorIndex)%2; // Alternate colors for readability.
					lastDate = date;
					lastAMPM = AMPM;
					
					String direction   = getWindDirection(wind_directions.get(i)); // Converts "45" to "NE"
					String temperature = temperatures.get(i);
					String speed       = getWindSpeedMPH(i);
					if (bgColorIndex == 0) {theBGColor = "#CCFFFF";} else {theBGColor = "#FFFFFF";}
					theContent +=   "<tr bgcolor=\"" + theBGColor + "\">" +
									"<td>" + displayDate +               "</td>" +
									"<td>" + hour + " "  + displayAMPM + "</td>" +
									"<td>" + temperature +               "</td>" +
									"<td>" + direction   +               "</td>" +
									"<td align=\"right\">" + speed +     "</td></tr>";
	    		}
		    	theContent += "</table></center>";
		    	theContent += "</html>";
		    	return theContent;
	    	} else { // Build up a nice message with JavaScript. Starts with "Loading" and moves to error message after 6 second timeout. 
	    		String message ="";
	    		if ( ! SimplyWeather.isAlaska(location)){
		    		message += "<!-- Error --><html><body><br><br><br><p id=\"text\"><b>Loading temperature and wind data</b><br><br><i id=\"dots\">o</i></p>";
		    		message += "<script type=\"text/javascript\">";
		    		message += "setTimeout(function() {document.getElementById(\"dots\").innerHTML=\" o o\";},1500);";
					message += "setTimeout(function() {document.getElementById(\"dots\").innerHTML=\" o o o\";},3000);";
					message += "setTimeout(function() {document.getElementById(\"dots\").innerHTML=\" o o o o\";},4500);";
					message += "setTimeout(function() {document.getElementById(\"dots\").innerHTML=\" o o o o o\";},6000);";
					message += "setTimeout(function() {document.getElementById(\"text\").innerHTML=\"<p><b>Hourly data not available.</b><br>Please try again later.<br> (Check network connection and reload forecast.)</p>\";},7500);";
					message += "</script></body></html>";
				}else{
					message += "<html><body><br><br><br><p><b>This data is currently not available for Alaska.</b><br><br></i></p></body></html>";
				}
	    		return message;
	    	}
		}catch(Exception e){
			Log.e(DEBUG_TAG, "Error building hourly HTML. " + e); // Log the error.
			return "<!-- Error --><html><body>Hourly data not available.<br><br></body></html>";
		} 		
    }
    
    String getCurrentHazardHTML(){
    	String hazardHTML = "";
    	if (hazards != null && times_1h != null){
	    	for (int i=0; i<hazards.size() && i<4; i++){
	    		if (!hazards.get(i).equals("")){
	    			MyDate theDate = new MyDate(times_1h.get(i));
	    			hazardHTML += theDate.getHour() + ":" + theDate.getMinute() + " " + theDate.getAMPM() + " "; 
	    			hazardHTML += hazards.get(i) + "<br>";    			
	    		}
	    	}
    	}
    	if (hazardHTML.length() !=0) { // Put a little header on if there are hazards to report.
    		hazardHTML = "<font color=\"red\"><b>Hazard Outlook</b></font><br>" + hazardHTML + "<br>";
		} 
    	return hazardHTML;
    }
        
    public String getForecastAndHazardHTML(){
    	return "<html><body>" + getCurrentHazardHTML() + forecastHTML + "</body></html>";
    	
    }
    
    // Convert degrees into a familiar direction.
    public static String getWindDirection(String directionString){
    	int direction;
    	try{direction = Integer.valueOf(directionString);} catch (NumberFormatException e) {return "";}
    	double div = 360/32; // Divide circle into 32 pieces for convenience.
    	if (direction >   1*div  && direction <= 3* div ) {return "NNE";}
    	if (direction >   3*div  && direction <= 5* div ) {return "NE";}
    	if (direction >   5*div  && direction <= 7* div ) {return "ENE";}
    	if (direction >   7*div  && direction <= 9* div ) {return "E";}
    	if (direction >   9*div  && direction <= 11*div ) {return "ESE";}
    	if (direction >  11*div  && direction <= 13*div ) {return "SE";}
    	if (direction >  13*div  && direction <= 15*div ) {return "SSE";}
    	if (direction >  15*div  && direction <= 17*div ) {return "S";}
    	if (direction >  17*div  && direction <= 19*div ) {return "SSW";}
    	if (direction >  19*div  && direction <= 21*div ) {return "SW";}
    	if (direction >  21*div  && direction <= 23*div ) {return "WSW";}
    	if (direction >  23*div  && direction <= 25*div ) {return "W";}
    	if (direction >  25*div  && direction <= 27*div ) {return "WNW";}
    	if (direction >  27*div  && direction <= 29*div ) {return "NW";}
    	if (direction >  29*div  && direction <= 31*div ) {return "NNW";}
    	if (direction >  31*div  && direction <= 360    ) {return "N";}
    	if (direction >= 0       && direction <= 1* div ) {return "N";}
    	return ""; // Fail silently for values over 360 or under 0. Calling function can test for empty string.
    }
}

class ForecastHTMLPage {
	String  forecastText = "";
	String  errorText    = "";
	boolean hasError     = true; 
}

class ForecastXMLPage {
	String  forecastXML = "";
	String  errorText   = "";
	boolean hasError    = true;
	boolean isZone      = false; // Some locations have "zone forecasts" and are handled differently.
	double  latitude    = 0;
	double  longitude   = 0;
}