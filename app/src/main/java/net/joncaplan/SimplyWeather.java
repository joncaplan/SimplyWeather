// SimplyWeather (c) 2014 Jonathan Caplan.

        /*
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation; either version 2 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License along
        with this program; if not, write to the Free Software Foundation, Inc.,
        51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
        */

package net.joncaplan;
//package net.joncaplan.simply_weather_free;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
/*import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
*/
//import net.joncaplan.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
//import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import org.apache.commons.net.ftp.FTPClient; // Used to report back errors.

@SuppressLint("DefaultLocale")
public class SimplyWeather extends Activity implements android.view.View.OnClickListener {
	private ScrollView myMainScrollView    = null;
	private TextView   myTextView          = null;
    private WebView    myWebView           = null;
    private WebView    myHourlyWebView     = null;
    private TextView   timeSinceUpdateView = null;
    private Button     myButton            = null;
    private Spinner    locationSpinner     = null;
    private ArrayAdapter <CharSequence> adapter;

    private long       timeOfNetworkErrorMessage = System.currentTimeMillis()/1000;
    private boolean    userRequestedUpdate;     // Was the request for updated forecast initiated by user? Used to suppress frequent network availability error messages from automatic updates.
    private String     toastErrorMessage = "";  // Error message to appear in "Toast" pop-up.
    private static final String DEBUG_TAG = "SimplyWeather forecast";  // For log file.
    private int log_level = 1;                  // 0=errors only. 1=informational 2=verbose 3=very_verbose -1=logging off.
    private ForecastManager theForecastManager; // Manages interface between app and local forecast database.
    private ArrayList<Forecast> forecasts;      // All locations and forecasts stored locally.
    private Forecast currentForecast;           // Forecast being displayed.
    private int      currentForecastIndex  = 0; // The index in the forecasts Array of the currentForecast.
    private int      greatestLocationID    = 0; // Greatest LocationID in database. Used in assigning the ID for a new location.
    //Location physicalLocation      = null; // Where in the world is our user. Coarse-grained location from Cell network or WiFi. (No need for GPS battery drain or granularity.)
    //LocationListener locationListener;     // Used in determining user's location.
    //LocationManager  locationManager;      // Used in determining user's location.
    //int API_level  = 7;                    // Some features require certain android API levels. Currently the  makeUseOfNewLocation() method used geocoding or XML parsing only available in API level 8.

    //Needed to handle swiping left-right to switch locations.
	private Animation slideRightIn;
    private Animation slideRightOut;
	private Animation slideLeftIn;
	private Animation slideLeftOut;
    private ViewFlipper viewFlipper;
    private static final int SWIPE_MIN_DISTANCE       = 60;
    private static final int SWIPE_MAX_OFF_PATH       = 50;
    private static final int SWIPE_THRESHOLD_VELOCITY = 50;
    private GestureDetector gestureDetector;
    private boolean updating           = false; // Tracks whether a background update is in progress. Used to block herd of updates occurring at once.
    private boolean hourlyDataUpdating = false; // Tracks whether a background update is in progress. Used to block herd of updates occurring at once.

    private static final int ADD_LOCATION_DIALOG_ID    = 1;
    private static final int DELETE_LOCATION_DIALOG_ID = 2;

    @SuppressLint({ "SetJavaScriptEnabled", "CutPasteId" }) // JavaScript required for waiting to load animation. CutPasteId warning happens because there are two web views.
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

          theForecastManager = new ForecastManager(getApplicationContext());
          loadForecasts(); // Get forecasts from database and load into forecasts ArrayList.

          // Set up user interface.
          setContentView(R.layout.main);
          viewFlipper   = (ViewFlipper)findViewById(R.id.flipper);
          slideLeftIn   = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
          slideLeftOut  = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
          slideRightIn  = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
          slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

		  myMainScrollView    = (ScrollView) findViewById(R.id.scrollview0);
          myButton            = (Button)     findViewById(R.id.button1);
      	  myTextView          = (TextView)   findViewById(R.id.textView2);
		  myWebView           = (WebView)    findViewById(R.id.webview);        // This webView has the main forecast view.
		  myHourlyWebView     = (WebView)    findViewById(R.id.hourlyWebView);  // This displays the hourly data. (Wind speed etc.)
		  timeSinceUpdateView = (TextView)   findViewById(R.id.textView9);
          locationSpinner     = (Spinner)    findViewById(R.id.spinner);

          myHourlyWebView.getSettings().setJavaScriptEnabled(true); // Required to be able to display animated "Loading ..." message.
		  myWebView.getSettings().setJavaScriptEnabled(false);		// No JavaScript here.


          adapter = new ArrayAdapter <CharSequence> (this, android.R.layout.simple_spinner_item );
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
          loadSpinnerAdapterItems(); // Puts the forecast locations into the spinner drop-down.
          locationSpinner.setAdapter(adapter);
          locationSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
          myButton.setOnClickListener(this);

          // Gesture detection
          gestureDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };

       // Do this for each view added to the grid to enable swiping for each view item. (other than locationSpinner)
          myWebView.setOnClickListener(SimplyWeather.this);
          myWebView.setOnTouchListener(gestureListener);
          timeSinceUpdateView.setOnClickListener(SimplyWeather.this);
          timeSinceUpdateView.setOnTouchListener(gestureListener);
          myMainScrollView.setOnClickListener(SimplyWeather.this);
          myMainScrollView.setOnTouchListener(gestureListener);
          myTextView.setOnClickListener(SimplyWeather.this);
          myTextView.setOnTouchListener(gestureListener);
          myButton.setOnClickListener(SimplyWeather.this);
          myButton.setOnTouchListener(gestureListener);

          // Items on the hourly view need to become listeners too.
          WebView hourlyWebView = (WebView) findViewById(R.id.hourlyWebView);
          hourlyWebView.setOnClickListener(SimplyWeather.this);
          hourlyWebView.setOnTouchListener(gestureListener);
          
          /*initializeLocationService();
          if (physicalLocation != null){
        	  makeUseOfNewLocation(physicalLocation);
          }else{
        	  showToastMessage("Could not get physical location.",3);
          }*/
    }
    
    /*void initializeLocationService(){
    	// Acquire a reference to the system Location Manager
    	locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    	
    	// Get mock location for emulator. Only useful for development.
    	if ("google_sdk".equals( Build.PRODUCT ) || "sdk".equals( Build.PRODUCT ) ){ // Check see if we are running on an emulator, which may not know its location.
	    	Location tempLocation = new Location(LocationManager.NETWORK_PROVIDER);
	    	tempLocation.setLatitude(  38.897805); // The White House
	    	tempLocation.setLongitude(-77.036541);
	    	String mocLocationProvider = LocationManager.NETWORK_PROVIDER;
	    	locationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 0, 5);
	    	locationManager.setTestProviderEnabled(mocLocationProvider, true);
			locationManager.setTestProviderLocation(mocLocationProvider, tempLocation);
	    	physicalLocation = locationManager.getLastKnownLocation(mocLocationProvider);
    	}else{ // We're on a physical device, not an emulator.
    		physicalLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    	}
    	
    	// Define a listener that responds to location updates
    	locationListener = new LocationListener() {
    	    public void onLocationChanged(Location location) { // Called when a new location is found by the network location provider.
    	        makeUseOfNewLocation(location);
    	    }
    	    public void onStatusChanged(String provider, int status, Bundle extras) {}
    	    public void onProviderEnabled(String  provider) {}
    	    public void onProviderDisabled(String provider) {}
    	  };
    	  // Registration of locationListener happens in a moment when onResume() is called. (See Android activity life cycle.)
    }*/

/*    void makeUseOfNewLocation(Location location){
        if (location != null &&  API_level > 7){

	    	showToastMessage("Latitude "  + location.getLatitude() + "  Longitude " + location.getLongitude(), 5);
	    	physicalLocation = location; // Update global location variable.
	    	
	    	XPath xpath = XPathFactory.newInstance().newXPath();
	    	String expression_postal = "//GeocodeResponse/result/address_component[type=\"postal_code\"]/long_name/text()";
	    	String expression_local  = "//GeocodeResponse/result/address_component[type=\"locality\"]/long_name/text()";
	    	String expression_state  = "//GeocodeResponse/result/address_component[type=\"administrative_area_level_1\"]/short_name/text()";
	    	InputSource inputSource = new InputSource("https://maps.googleapis.com/maps/api/geocode/xml?latlng="+location.getLatitude()+","+location.getLongitude()+"&sensor=true");
	    	String postal_code    = "";
	    	String locality       = ""; // City or Town.
	    	String state_province = ""; // State, province, etc.
	    	try{
	    		postal_code    = (String) xpath.evaluate(expression_postal, inputSource, XPathConstants.STRING);
	    		locality       = (String) xpath.evaluate(expression_local,  inputSource, XPathConstants.STRING);
	    		state_province = (String) xpath.evaluate(expression_state,  inputSource, XPathConstants.STRING);
	    	} catch (XPathExpressionException e){
	    		if (log_level > -1) {Log.e(DEBUG_TAG, "Error getting postal code, locality or state from physical location. " + e);}
	    	}
			if (log_level > 0) {Log.i(DEBUG_TAG, "ZIP code is: " + postal_code);}
			showToastMessage(locality + ", " + state_province + " ZIP " + postal_code, 3);
        }else{
        	  showToastMessage("Could not get physical location.",3);
        }
    }*/

    // Puts the forecast locations into the spinner drop-down.
    void loadSpinnerAdapterItems(){
	    adapter.clear();
        for(Forecast theForecast:forecasts){
            adapter.add(theForecast.location);
        }
    }

    // Load forecasts from the database.
    void loadForecasts(){
    	forecasts = theForecastManager.getForecasts();
    	if (log_level > 0) {Log.i(DEBUG_TAG, "Got forecasts from database.");}
    	int num_forecasts = forecasts.size();
    	try{
    		if (num_forecasts>0){ // Load all the saved forecasts from database into memory.
    			if (log_level > 0) {Log.i(DEBUG_TAG, "num_forecasts = " + num_forecasts);}
    			currentForecastIndex = -1; // Start with illegal value.
    			int preferredLocationID = getPreferredLocationID();
    			for (int i=0; i<num_forecasts; i++){
    				if (forecasts.get(i).id > greatestLocationID) {greatestLocationID = forecasts.get(i).id;}
    				if (forecasts.get(i).id == preferredLocationID) { // Get the database ID of the preferred forecast.
    					currentForecastIndex = i;
    					currentForecast = forecasts.get(currentForecastIndex);
    				}
    			}
    			if (currentForecastIndex <0 || currentForecastIndex > forecasts.size()){ // Check if preferred forecast is out of bounds.
    				currentForecastIndex = 0;                                            // If no preferred forecast was found just use the first one.
    				currentForecast = forecasts.get(0);                                  // Use the first forecast in the list as preferred.
    				savePreferredLocationID(currentForecast.id);
    			}
    			if (log_level > 0) {Log.i(DEBUG_TAG, "Set current forecast");}
    			if (currentForecast.getHourlyHTMLTable().equals("") || currentForecast.getHourlyHTMLTable().startsWith("<!-- Error -->")){ // If the current location is missing hourly data.
    				backgroundUpdateHourlyXMLData(currentForecast);   // ... retrieve that data.
    			}
    		}else{ // Encourage user to add a location.
    			if (log_level > 0) {Log.i(DEBUG_TAG, "No saved forecasts");}
    			currentForecast = new Forecast(-1, "", "<b>No locations chosen.</b> <br><br> Go to menu to add new location. <br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>", 0, 0); // Represents no saved forecast. Line breaks ensure forecast pane is long enough.
    			onAddLocation(); // Pop up the add location dialog.
    		}
    	}catch (Exception e){
    		Log.e(DEBUG_TAG, "Error getting forecast " +e);
    	}
    	if (log_level > 0) {Log.i(DEBUG_TAG, "Set current forecast from forecasts ArrayList.");}
    }

    void refreshScreen(){ // Update all the elements of the screen.
    	if (log_level > 0) {Log.i(DEBUG_TAG, "refreshScreen() called for location: " + currentForecast.location + "viewFlipper child ID is: " + viewFlipper.getDisplayedChild());}
		refreshForecastView(); // Refresh both views, so they are ready when we flip to see them.
		refreshHourlyView();
		if (toastErrorMessage.length() != 0)
			showToastMessage(toastErrorMessage,6);
			toastErrorMessage = "";
    }

    void refreshForecastView(){
    	loadSpinnerAdapterItems();
    	locationSpinner.setSelection(currentForecastIndex); // Make sure the correct item is selected in the spinner.
	    showLastUpdateTime(); // Display time since last forecast update.
    	refreshForecastPanel();
    	myMainScrollView.fullScroll(ScrollView.FOCUS_UP);// Scroll to top.
    }

    void refreshHourlyView(){
		if (log_level > 0) {Log.i(DEBUG_TAG, "Updating myHourlyWebView.");}
		myHourlyWebView.loadData(currentForecast.getHourlyHTMLTable(), "text/html", "utf-8");
    }

    void refreshForecastPanel(){
		  myWebView.loadData(currentForecast.getForecastAndHazardHTML(), "text/html", "utf-8");
    }

	public void onClick(View v){ // Handle press of Refresh button.
			userRequestedUpdate   = true; // Ensure network error messages don't appear too frequently if user requests update.
			//physicalLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); // Note: Test code to ensure we can trigger location services.
			//makeUseOfNewLocation( physicalLocation);                                                   // Note: Test code to ensure we can trigger location services.

			backgroundForecastUpdate(currentForecast.location);
			backgroundUpdateHourlyXMLData(currentForecast);
	}

	void getForecast(String theLocation){
		// Need to check if the location is being switched to an already saved location. In that case show saved forecast.
		// Never show location and forecast mismatch.
		if (log_level > 0) {Log.i(DEBUG_TAG, "Getting new forecast for " + theLocation);}
		String theForecast;
/*		if (isAlaska(theLocation)){ //////////////////////////////////////////////////////////////////// WARNING: Disabling Alaska test.
			if (log_level > 0) {Log.i(DEBUG_TAG,  theLocation + " is in Alaska");}
			theForecast = getAlaskaForecastHTML(preprocessPlaceName(theLocation));
			
			if (toastErrorMessage.equals("") && !theForecast.startsWith("Error:" )){ // Check for errors.
				int theLocationID = theForecastManager.getLocationID(theLocation); // Get the ID of for this location.
				//if (theLocationID <  -1){showToastMessage("Database error.\nCould not save forecast.",5);}
				if (theLocationID == -1){ // Location does not exist in database.
					currentForecast = new Forecast( ++greatestLocationID,  theLocation,  theForecast, 0 , 0); // WARNING: Alaska forecasts lat and long shouldn't be zero. It will prevent them from getting hourly weather data, if its even available.
					forecasts.add(currentForecast);
					theForecastManager.insert(currentForecast); // Save the forecast into the database.
					savePreferredLocationID(currentForecast.id); // Note: This just makes the most recent location preferred. This should become user-selectable.
				}
				if (theLocationID >= 0) { // Check for update to forecast to a location already in the database.
					// Get Forecast object for the location...
					int index = findForecastIndex(theLocationID);
					forecasts.get(index).setForecast(theLocationID, theLocation, theForecast, 0, 0); // Update forecast in memory. (0,0 is lat, lon, which is not used in Alaska forecasts.)
					theForecastManager.update(forecasts.get(index));
					savePreferredLocationID(forecasts.get(index).id); // Note: This just makes the most recent location preferred. This should become user-selectable.
				}
			}
			return;
		}*/
		// Handle non-Alaska forecasts here.
		String theURL = "http://forecast.weather.gov/zipcity.php";
		ForecastXMLPage theXMLPage = getXMLPage(theURL, preprocessPlaceName(theLocation));
		String forecastXML = theXMLPage.forecastXML;

		if (theXMLPage.hasError){
			toastErrorMessage = theXMLPage.errorText;
			if (theXMLPage.errorText.startsWith("Error: No location")){
				myWebView.loadData("<html><body><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br></body></html>", "text/html", "utf-8");
				// The <br> tags make to WebView tall enough.
			}else if (theXMLPage.errorText.startsWith("Error: No Internet connection")){
				// Should attempt to load a saved forecast here, if available.
				myWebView.loadData("<html><body><br> Forecast not loaded. No internet connection. </body></html>", "text/html", "utf-8");
			}else {
				myWebView.loadData("<html><body><br> " + forecastXML  +" <br><br> An error has been encountered loading your forecast." +
						" A summary of the error is being sent to the developer of SimplyWeather so I can fix this issue in a future release." +
						"</body></html>", "text/html", "utf-8"); // Show whatever error message has been passed up.
				sendErrorToJon(forecastXML, theLocation);
			}
			return;
		}

		if (theXMLPage.isZone){
			int start = forecastXML.indexOf("<a name=\"contents\">"); // NOTE: This is quite brittle and ad-hoc, as it depends on exact HTML returned. Works, but should fix later.
			int end   = forecastXML.indexOf("<br><br><br><br><br>");

			theForecast = forecastXML.substring(start, end); // Just copy the forecast part of the page to display.
		}else{
            ForecastHTMLPage theForecastHTML = new ForecastHTMLPage();
            try {
                parsePage(forecastXML, theForecastHTML);    // Convert the XML into HTML to display. (Include theLocation for error reporting.)
            }
            catch (Exception e){
                String errorMessage = "<br><br>Could not load forecast for this location. Please try again later.<br><br><br><br><br>";
                toastErrorMessage  += "Sorry, cannot get forecast for this location.\n\n ";

                if (log_level > 0) {Log.i(DEBUG_TAG, "Exception parsing XML page #4: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}

                errorMessage +=  " <br><br> An error has been encountered loading your forecast." +
                        " A summary of the error is being sent to the developer of SimplyWeather so I can fix this issue in a future release."; // Show whatever error message has been passed up.
                sendErrorToJon("Page content is: " + forecastXML, theLocation);
                theForecastHTML.errorText = errorMessage;
            }
			 if (theForecastHTML.hasError){
				 toastErrorMessage = theForecastHTML.errorText;
				 return;
			 }
			 theForecast = theForecastHTML.forecastText;
		}
		int theLocationID = theForecastManager.getLocationID(theLocation); // Get the ID of for this location.

		// Database error looking up "theLocation"
		if (theLocationID <  -1){
			if (log_level > 0) {Log.i(DEBUG_TAG, "Database error. Could not save forecast.");}
			return;
		}

		// Location does not exist in database.
		if (theLocationID == -1){
			if (log_level > 0) {Log.i(DEBUG_TAG, theLocation + " not found in database");}
			String result;
			try {
				currentForecast = new Forecast( ++greatestLocationID,  theLocation,  theForecast, theXMLPage.latitude, theXMLPage.longitude); //
				forecasts.add(currentForecast);                  // Add to in-memory forecast list.
			} catch (Exception e) {
				if (log_level > 0) {Log.i(DEBUG_TAG,"Error inserting new forecast into ArrayList. "+e);}
			}
			result = theForecastManager.insert(currentForecast); // Save the forecast into the database.
			if (log_level > 0) {
					Log.i(DEBUG_TAG, "Saved details for new location: " + theLocation);
					Log.i(DEBUG_TAG, "ID is now: " + currentForecast.id);
					Log.i(DEBUG_TAG, "Location is now: " + currentForecast.location);
					Log.i(DEBUG_TAG, "Database insert result is: " + result);
				}
			savePreferredLocationID(currentForecast.id); // Note: This just makes the most recent location preferred. This should become user-selectable.
		}

		// Check for update to forecast to location already in database.
		if(theLocationID >= 0){
			try {
				int index = findForecastIndex(theLocationID);
				currentForecast = forecasts.get(index);
				currentForecast.setForecast(theLocationID, theLocation, theForecast, theXMLPage.latitude, theXMLPage.longitude); // Update the current forecast in memory.
				theForecastManager.update(forecasts.get(index));                                           // Update the current forecast in the database.
				if (log_level > 0) {Log.i(DEBUG_TAG, "Saved details for existing location: " + theLocation);}
				savePreferredLocationID(currentForecast.id); // Note: This just makes the most recent location preferred. This should become user-selectable.
			} catch (Exception e){
				if (log_level > 0) {Log.i(DEBUG_TAG, "Error updating forecast. " + e);}
			}
		}
		backgroundUpdateHourlyXMLData(currentForecast); // Fire off request to get hourly data for this forecast.
	}
	
/*	String getAlaskaForecastHTML(String theLocation){
	//		<form action ="port_zc.php" method="get">
	//		<input type="text" name="inputstring" size="15" value="">
	//		<input type="submit" name="Go2" value="Go"></form>
		
	//		Then forecast link is at first link after <hr>
	//		Forecast is after first "</div>" and continues until "<hr>".
	//		
	//		http://mobile.weather.gov/port_mp_ns.php?             CityName=Atqasuk & site=AFG & State=AK & warnzone=AKZ201
	//		http://mobile.weather.gov/port_mp_ns.php?  select=1 & CityName=Atqasuk & site=AFG & State=AK & warnzone=AKZ201
	//		
	//		The only required part of the location is the zone. If I could look this up it would simplify things.
		String pageContent = "";
		String header2;
		String header3;
		String theURL      = "http://mobile.weather.gov/port_zc.php";
		try{
		    // Construct data
            String data = URLEncoder.encode("inputstring", "UTF-8") + "=" +
                          URLEncoder.encode(theLocation,   "UTF-8") + "&" +
                          URLEncoder.encode("Go2", "UTF-8")         + "=" +
                          URLEncoder.encode("Go", "UTF-8");
		    URL url       = new URL(theURL);
		    if (log_level > 0) {Log.i(DEBUG_TAG, "Created URL for Alaska forecast. data="+data);}
		    
		    // Send data
		    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		    if (log_level > 0) {Log.i(DEBUG_TAG, "Created URLConnection for Alaska forecast. conn="+conn);}
		    conn.setInstanceFollowRedirects(false); // Prevent redirect (default for Android 4), since our location data is in the header of the redirect page.
		    conn.setDoInput(true);
		    conn.setDoOutput(true);
		    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		    DataOutputStream out = new DataOutputStream (conn.getOutputStream ());
		    out.writeBytes(data);
		    out.flush();
		    out.close();
		    
		    // Get the first response
		    header2 = conn.getHeaderField(2); // This might be it for 4.0.
		    header3 = conn.getHeaderField(3); // This has the URL we need.
		    
		    if (header2.equals("230") || header3.equals("230")){
		    	toastErrorMessage += "Sorry, I couldn't find that location. \n\n The location should be in form of: \n\n City, State or a valid 5-digit zip code. \n\n US locations only.";
		    	pageContent += "Error: Unknown location"; 
		    } else {
			    // Get the final response
		    	String theRequest = "";
		    	if (header2.startsWith("port_mp")){theRequest = header2;}
		    	if (header3.startsWith("port_mp")){theRequest = header3;}
		    	theRequest = theRequest.replace(" ","%20"); // Replace spaces with URL space character.
			    URL url_2 = new URL("http://mobile.weather.gov/" + theRequest); // Spaces in header3 cause URL to be mangled here. (Spaces occur in some town names: "Elfin Cove, AK")
			    HttpURLConnection conn_2 = (HttpURLConnection) url_2.openConnection();
			    //conn.setInstanceFollowRedirects(false);
			    conn_2.setDoInput(true);
			    conn_2.setDoOutput(false);
			    
			    BufferedReader in = new BufferedReader(new InputStreamReader(conn_2.getInputStream()));
			    String line;
			    while ((line = in.readLine()) != null) {
					pageContent = pageContent + line + "\n";
			    }
			    in.close();
			    int beginForecastURL = pageContent.indexOf("<a href=\"") + 9; // This is the point where the web page give the URL of the forecast. (+9 gets us to the end of the string we are matching to.)
			    int endForecastURL   = pageContent.indexOf("\"><u>");
			    //if (log_level > 2) {Log.i(DEBUG_TAG, "Alaska page content is:\n" + pageContent + "\n");}
			    String forecastURL   = pageContent.substring(beginForecastURL, endForecastURL);
			    forecastURL = forecastURL.replace(" ","%20"); // Spaces will also cause URL to be mangled here.
			    //return "Begins at: " + beginForecastURL+ "<br>Ends at: " + endForecastURL + "<br>" + forecastURL;
			    pageContent = ""; // Clear for use with next page.
			    
			    URL url_3       = new URL("http://mobile.weather.gov/" + forecastURL);
			    URLConnection conn_3 = url_3.openConnection();
			    conn_3.setDoInput(true);
			    conn_3.setDoOutput(false);	
			    BufferedReader in_3 = new BufferedReader(new InputStreamReader(conn_3.getInputStream()));
			    while ((line = in_3.readLine()) != null) {
					pageContent = pageContent + line + "\n";
			    }
			    in_3.close();
			    int clipEndIndex = pageContent.lastIndexOf("<hr>"); // Clip off the final <hr> and "previous page"  button.
			    pageContent = pageContent.substring(0,clipEndIndex);
			    return pageContent;
		    }
		}
		catch (FileNotFoundException e){ // Triggered for unknown ZIP code. Alternate method is to parse the page, but this should work. // Can this be removed?
            pageContent = "Error: <br><br>Sorry, I couln't find that location. <br><br> The location should be in form of: <br><br> City, State or a valid 5-digit zip code. \n\n US locations only.<br><br>ZIP codes for PO boxes in Alaska may not work.";
		}
		catch (IOException e) {
			pageContent = "Error: Could not connect to server. <br>(Alaska forecast requested.).<br>"+e ;
			
			if ((System.currentTimeMillis()/1000 - timeOfNetworkErrorMessage) > 10 || userRequestedUpdate ){ // Make sure the error message isn't popping up too frequently.
				toastErrorMessage = "\nserver. \n\nPlease check that your Internet connection is turned on and working.\n";
				timeOfNetworkErrorMessage = System.currentTimeMillis()/1000;
				if (log_level > 0) {Log.i(DEBUG_TAG, "IOError getting Alaska forecast: " + e.getMessage());}
			}
		}catch (Exception e) {
			String errorMessage = "Error getting Alaska forecast: " + e.getMessage();			
			pageContent = errorMessage;
			toastErrorMessage = errorMessage;
			if (log_level > 0) {Log.i(DEBUG_TAG, "Error getting Alaska forecast: " + e.getMessage() + Arrays.toString(e.getStackTrace()));}
		}
		return pageContent;
	}*/

	// Update the hourly XML data (wind speed, temperature, ...) for a forecast.
	void backgroundUpdateHourlyXMLData(Forecast theForecast){

		if (!hourlyDataUpdating){ // Don't do more than one simultaneous update.
			hourlyDataUpdating = true;
			new AsyncTask<Forecast, Void, String>(){
				@Override
				protected void onPostExecute(String result) {
					hourlyDataUpdating = false;
					if (viewFlipper.getDisplayedChild() == 1){ // Only refresh screen is the hourly panel is showing.
						refreshScreen();
					}
				}
				@Override
				protected String doInBackground(Forecast... theForecasts){
					Forecast theForecast = theForecasts[0]; // We only need the one Forecast passed by .execute() below.
					String forecastHourlyXML;
					if (theForecast.latitude != 0 || theForecast.longitude != 0){ // Don't get hourly data if location not set.
						Log.i(DEBUG_TAG, "Latitude is: " + theForecast.latitude + " Longitude is: " + theForecast.longitude);
						forecastHourlyXML = getHourlyDetailsXMLPage(theForecast.latitude, theForecast.longitude);
						parseHourlyData(forecastHourlyXML, theForecast); // Parse the hourly data from the XML file and place it into theForecast data structures.
					}
					return "";
				}
			}.execute(theForecast);
		}else{
			if (log_level > 0) {Log.i(DEBUG_TAG, "Skipped HourlyXMLData update due to \"hourlyDataUpdating\" being true.");}
		}

	}

	// Get forecast details for a geographic point.
    String getHourlyDetailsXMLPage(double latitude, double longitude){
		String pageContent = "";
		try {
			// Build up query string based on what features we want in forecast.
			String latString   = "&lat=" + latitude;
			String lonString   = "&lon=" + longitude;
			String units       = "&Unit=e";    // English units. "m" for metric units.
			String temperature = "&temp=temp"; // Should have data point every 3 or 6 hours.
			String windSpeed   = "&wspd=wspd";
			String windDir     = "&wdir=wdir";
			String hazard      = "&wwa=wwa";   // Hazards are Watches, Warnings and Advisories.
			String product     = "&product=time-series"; // "time-series" gives us the detailed 3-hour updates.
			String submit      = "&Submit=Submit";
			String queryURL ="http://graphical.weather.gov/xml/SOAP_server/ndfdXMLclient.php?whichClient=NDFDgen" +
								latString + lonString + units + temperature + windSpeed + windDir + hazard + product + submit;
	    	if (log_level > 0) {Log.i(DEBUG_TAG, "Query URL for hourly XML is: " + queryURL);}

			// Submit the query to NOAA
		    URL url       = new URL(queryURL);
		    URLConnection conn = url.openConnection();
		    conn.setDoInput(true);
		    conn.setDoOutput(false);
		    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		    String line;
		    while ((line = in.readLine()) != null) { // Get next line of file
				pageContent = pageContent + line + "\n";
		    }
	    	if (log_level > 0) {Log.i(DEBUG_TAG, "Closing BufferedReader.");}
		    in.close();

		} catch (MalformedURLException ignored){ // Just return empty string on error.
		} catch (IOException ignored){
		}
		return pageContent; // Return the XML.
	}

	// Parse the data from hourly XML and place into current forecast data structures.
	void parseHourlyData(String hourlyXMLdata, Forecast currentForecast){

		// Do initial parsing of XML document.
		Document doc;
		try{
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	        InputSource inputSource = new InputSource();
	        inputSource.setCharacterStream(new StringReader(hourlyXMLdata));
		    doc = dBuilder.parse(inputSource);
		} catch (IOException e){ // NOTE: Clean up exception handling.
			if (log_level > 0) {Log.i(DEBUG_TAG, "IOException parsing hourly XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
			return;
		} catch (SAXException e){
			if (log_level > 0) {Log.i(DEBUG_TAG, "SAXException parsing hourly XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
			return;
		} catch (IllegalArgumentException e){
			if (log_level > 0) {Log.i(DEBUG_TAG, "IllegalArgumentException parsing hourly XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
			return;
		} catch (ParserConfigurationException e) {
			if (log_level > 0) {Log.i(DEBUG_TAG, "ParserConfigurationException parsing hourly XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
			return;
		}

		// Get 3-hour time periods data from XML file.
		currentForecast.times_3h = getHourlyXMLData ( doc, "time-layout", "k-p3h"); // Get time period values, checking format.
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for time layout. ");}

		// Get temperature data from XML file.
		currentForecast.temperatures = getHourlyXMLData ( doc, "temperature", "");
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for hourly temperature. ");}

		// Get wind speed data from XML file.
		currentForecast.wind_speeds  = getHourlyXMLData ( doc, "wind-speed", "");
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for hourly wind speed. ");}

		// Get wind direction data from XML file.
		currentForecast.wind_directions = getHourlyXMLData ( doc, "direction", "");
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for hourly wind direction. ");}

		// Get 1-hour time period data from XML file. Used for hazards.
		currentForecast.times_1h = getHourlyXMLData ( doc, "time-layout", "k-p1h"); // Get time period values, checking format.
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for time layout. ");}

		// Get hazard from XML file. This includes: "Watches, Warnings, and Advisories."
		currentForecast.hazards = getHazardXMLData ( doc, "hazards");
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got data for hourly temperature. ");}
	}

    // Generic method for getting 3-hourly data from XML file. 
    ArrayList<String> getHourlyXMLData (
    		Document doc,             // doc is theXML preprocessed document.
    		String tag_name,          // tag_name is the tag that the data is stored under in the XML file.
    		String layout_key         // Only used for time-layout. Checks the time format, because multiple time layouts may be present.
    	){                            // Only requires match to beginning of attribute value. For time values such as "k-p1h" matches k-p1h-n65-2.
		// Get data from XML file.
		ArrayList<String> data = new ArrayList<String>();
		try{
		    NodeList dataLayoutList = doc.getElementsByTagName(tag_name);          // Get all elements that are tagged with tag_name. e.g "temperature".
		    Node     dataTagNode = null;                                           // The node the parent of where all our data is.
		    if (!layout_key.equals("")){                                           // If a layout_key is specified we have to seek the correct one so we ...
			    for (int i=0; i<dataLayoutList.getLength(); i++){                  // ... iterate through all the matching tags and check the attributes.
			    	Node dataTagNodeTest       = dataLayoutList.item(i);           // Get Node for first <data> tag.
					NodeList dataTagChildNodes = dataTagNodeTest.getChildNodes();  // Now extract the data from the node.
					Node     theDataValueNode  = dataTagChildNodes.item(1);        // Get second child node. (Node 0 is "<layout-key>"; 1 is "<k-p1h...>";)
					if (theDataValueNode.getChildNodes().item(0).getNodeValue().startsWith(layout_key)){ // Test to see if they layout-key matches the required value.
						dataTagNode = dataTagNodeTest;                                        // If so, use it.
						if (log_level > 0) {Log.i(DEBUG_TAG, "Got match for layout-key " + layout_key + " i="+i);}
					}
			    }
		    }else{
		    	dataTagNode       = dataLayoutList.item(0);                  // Get Node for first <data> tag, if no layout_key is specified.
				//if (log_level > 0) {Log.i(DEBUG_TAG, "No layout-key specified. Using first matching tag.");}
		    }
		    if (dataTagNode == null) {throw new Exception();}
			NodeList dataTagChildNodes = dataTagNode.getChildNodes();        // Now extract the data from the node.
			int end = dataTagChildNodes.getLength();
			for (int i=3; i<end; i += 2){ // Get data starting with the fourth child node. (Node 0 is "<name>"; 1 is e.g "Wind Speed"; Node 2 is "<value>"; Node 3 is the data.)
				Node     theDataValueNode  = dataTagChildNodes.item(i);
				NodeList theDataNodeList   = theDataValueNode.getChildNodes();   // This node list just has the one item ...
				Node     theDataNode       = theDataNodeList.item(0);            // ... which we fetch here ...
				String   theData           = theDataNode.getNodeValue();         // ... and finally convert to a String.
	    		data.add(theData);      // Add the data to the list.
				//if (log_level > 0) {Log.i(DEBUG_TAG, tag_name + " data is: " + theData);}
			}
		}catch (Exception e){
			if (log_level > 0) {Log.i(DEBUG_TAG, "Error getting " + tag_name + " data node. Error message is" + e);}
			return data;
		}
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got time layout for hourly " + tag_name +" data. ");}
		return data;
    }

    // Method for getting hazard data from XML file. 
    ArrayList<String> getHazardXMLData (
    		Document doc,             // doc is theXML preprocessed document.
    		String tag_name          // tag_name is the tag that the data is stored under in the XML file.
    	){
		// Get data from XML file.
		ArrayList<String> data = new ArrayList<String>();
		try{
		    NodeList dataLayoutList = doc.getElementsByTagName(tag_name);
            Node     dataTagNode   = dataLayoutList.item(0);                  // Get Node for first <data> tag, if no layout_key is specified.

			NodeList dataTagChildNodes = dataTagNode.getChildNodes();        // Now extract the data from the node.
			int end = dataTagChildNodes.getLength();
			for (int i=3; i<end; i += 2){ // Get data starting with the fourth child node. (Node 0 is "<name>"; 1 is e.g "Wind Speed"; Node 2 is "<value>"; Node 3 is the data.)
				String hazardString = "";
				Node     theDataValueNode  = dataTagChildNodes.item(i);          // YES
				//if (log_level > 0) {Log.i(DEBUG_TAG, "i="+i+" theDataValueNode is: " + theDataValueNode);}
				NodeList theDataNodeList   = theDataValueNode.getChildNodes();
				//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNodeList is: " + theDataNodeList);}
				//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNodeList length is: " + theDataNodeList.getLength());}
				if (theDataNodeList.getLength() == 3){
					Node     theDataNode       = theDataNodeList.item(1); // Get the node with the data.
					//String   theData           = theDataNode.getNodeValue();
					//if (log_level > 0) {Log.i(DEBUG_TAG, " data is: " + theData);}
					//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode j " +" is : " + theDataNode);}
					//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode j " +" is : " + theDataNode.getNodeName());}  // Node name is "hazard"
					//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode j " +" is : " + theDataNode.getNodeValue());} // Node value is "null"
					if(theDataNode != null){
						//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode j " +" attributes are : " + theDataNode.getAttributes());}
						NamedNodeMap theNameNodeMap = theDataNode.getAttributes();
						if (theNameNodeMap != null){
							Node phenomena    = theNameNodeMap.getNamedItem("phenomena");    // e.g. "Flash Flood"
							Node significance = theNameNodeMap.getNamedItem("significance"); // e.g. "Warning"
							if (phenomena != null){
								String phenomenaString    = phenomena.getNodeValue();
								String significanceString = significance.getNodeValue();
								//if(phenomenaString    != null){Log.i(DEBUG_TAG, " phenomena is: "    + phenomenaString   );}else{Log.i(DEBUG_TAG, "No phenomena"   );}
								//if(significanceString != null){Log.i(DEBUG_TAG, " significance is: " + significanceString);}else{Log.i(DEBUG_TAG, "No significance");}
								hazardString += phenomenaString + " " + significanceString + " "; // e.g. "Flash Flood Warning"
							} else {
								Log.i(DEBUG_TAG, "No phenomena node");
							}
						}
					}
					/*
					NodeList theDataNodeList2   = theDataNode.getChildNodes(); 
					if (theDataNodeList2.getLength() > 0) {
						for (int k = 0; k < theDataNodeList2.getLength(); k++){
							Node     theDataNode2       = theDataNodeList2.item(j);
							if (theDataNode2 != null){
								//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode k " + k +" is : " + theDataNode2);}
								//if (log_level > 0) {Log.i(DEBUG_TAG, " theDataNode k " + k +" is : " + theDataNode2.getNodeName());} // "hazardTextURL"
								if (log_level > 0 && k ==1) {Log.i(DEBUG_TAG, " theDataNode k " + k +" is : " + theDataNode2.getNodeValue());}
								
							}
						}
					}
					*/

				}
				data.add(hazardString);
			}
		}catch (Exception e){
			if (log_level > 0) {Log.i(DEBUG_TAG, "Error getting " + tag_name + " data node: " + e);}
		}
		if (log_level > 0) {Log.i(DEBUG_TAG, "Got time layout for hourly " + tag_name +" data. ");}
		return data;
    }


	ForecastXMLPage getXMLPage(String theURL, String theLocation){
		ForecastXMLPage theXMLPage = new ForecastXMLPage();

	    Log.i(DEBUG_TAG, "theURL is: " + theURL);
	    Log.i(DEBUG_TAG, "theLocation: " + theLocation);

		String pageContent = "";
		String header0     = "";
		String header1     = "";
		String header2     = "";

		if (theLocation.equals("")) {
			theXMLPage.errorText = "Error: No location.";
			return theXMLPage;
		}
		try{
		    // Construct data for first HTTP packet

		    String data = URLEncoder.encode("inputstring", "UTF-8") + "=" +
                          URLEncoder.encode(theLocation,   "UTF-8") + "&" +
                          URLEncoder.encode("Go2",         "UTF-8") + "=" +
                          URLEncoder.encode("Go",          "UTF-8");

		    // Send data for first HTTP packet
		    URL url       = new URL(theURL);
		    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		    conn.setInstanceFollowRedirects(false); // Prevent redirect (default for Android 4), since our location data is in the header of the redirect page.
		    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		    conn.setDoInput(true);
		    conn.setDoOutput(true);
		    DataOutputStream out = new DataOutputStream (conn.getOutputStream ());
		    out.writeBytes(data);
		    out.flush();
		    out.close();

		    // Get the first response (HTTP packet #2)
		    header0 = conn.getHeaderField(0);
		    header1 = conn.getHeaderField(1);
		    header2 = conn.getHeaderField(2); // This has the URL we need.

		    String theNextURL = "";

		    if(header1.startsWith("http://") ){ theNextURL = header1;} // The NOAA web server might return the URL either on header 1 or 2.
		    if(header2.startsWith("http://") ){ theNextURL = header2;}

		    if (theNextURL.equals("")){
		    	String errorMessage = "Sorry, I couldn't find that location. \n\n The location should be in form of: \n\n City, State or a valid 5-digit zip code. \n\n US locations only.";
		    	pageContent = "Error: Unknown location";
		    	toastErrorMessage =  errorMessage;
		    	if (log_level > 0) {Log.i(DEBUG_TAG, "Could not find URL in either header 1 or 2.");}
		    } else {

		    	if (theNextURL.contains("zone")){
		    		theXMLPage.isZone = true;
		    	} else {
			    	// Find the latitude and longitude from the URL.
			    	try{
				    	if (log_level > 0) {Log.i(DEBUG_TAG, "nextURL is: " + theNextURL);}
				    	int latIndex = theNextURL.indexOf("&lat="); int latLength = 5; // Find where latitude  is in the URL
				    	int lonIndex = theNextURL.indexOf("&lon="); int lonLength = 5; // Find where longitude is in the URL
				    	if (latIndex == -1) {latIndex = theNextURL.indexOf("&textField1="); latLength = 12;} // Use alternate name if URL doesn't have lat= & lon=.
				    	if (lonIndex == -1) {lonIndex = theNextURL.indexOf("&textField2="); lonLength = 12;}
				    	int endIndex = theNextURL.indexOf("&", lonIndex+lonLength );    // End of longitude string at at start of next item in URL ...
				    	if (endIndex == -1) { endIndex = theNextURL.length();}  // ... unless there are no more items in the URL.

				    	String latString = theNextURL.substring(latIndex+latLength, lonIndex); // Get the latitude  string, ignoring the 5 characters of "&lat="
				    	String lonString = theNextURL.substring(lonIndex+lonLength, endIndex); // Get the longitude string, ignoring the 5 characters of "&lon="
				    	theXMLPage.latitude  = Double.valueOf(latString);
				    	theXMLPage.longitude = Double.valueOf(lonString);
				    	if (log_level > 0) {Log.i(DEBUG_TAG, "Lat: " + theXMLPage.latitude + " Lon: " + theXMLPage.longitude);}
			    	}catch (Exception e){
				    	if (log_level > 0) {Log.i(DEBUG_TAG, "Could not get latitude and longitude from URL. " + e);}
			    	}
		    	}
			    // Ask for the XML forecast.
		    	theNextURL = theNextURL + "&FcstType=dwml"; // DWML is "Digital Weather Markup Language," a type of XML.
		    	if (log_level > 0) {Log.i(DEBUG_TAG, "URL is: " + theNextURL);}
			    URL url_2 = new URL(theNextURL);
			    URLConnection conn_2 = url_2.openConnection();
			    conn_2.setDoInput(true);
			    conn_2.setDoOutput(false);
		    	if (log_level > 0) {Log.i(DEBUG_TAG, "Creating BufferedReader");}
			    BufferedReader in = new BufferedReader(new InputStreamReader(conn_2.getInputStream()));
		    	if (log_level > 0) {Log.i(DEBUG_TAG, "Created BufferedReader");}
			    String line;
			    while ((line = in.readLine()) != null) {
					pageContent = pageContent + line + "\n";
			    	if (log_level > 0) {Log.i(DEBUG_TAG, line);}
			    }

		    	if (log_level > 0) {Log.i(DEBUG_TAG, "Closing BufferedReader.");}
			    in.close();
		    	if (log_level > 0) {Log.i(DEBUG_TAG, "BufferedReader closed.");}
		    }
		}catch (UnknownHostException e) {
			theXMLPage.hasError  = true;
			if ((System.currentTimeMillis()/1000 - timeOfNetworkErrorMessage) > 10 || userRequestedUpdate ){ // Make sure the error message isn't popping up too frequently.
				theXMLPage.errorText = "\nCould not connect to network. \n\nPlease check that your Internet connection is turned on and working.\n";
    			if (log_level > 0) {Log.i(DEBUG_TAG, "IOException is: " + e);}
				timeOfNetworkErrorMessage = System.currentTimeMillis()/1000;
			}
			return theXMLPage;
		}catch (Exception e) {
			theXMLPage.hasError  = true;
			theXMLPage.errorText = "Error: Could not load forecast. Please try again later."  ;
			if (log_level > 0) {Log.e(DEBUG_TAG, "Exception is: " + e.getMessage() + "\n Header 0: " + header0 + "\n Header 1: " + header1 + "\n Header 2: " + header2);}
			return theXMLPage;
		}

		theXMLPage.forecastXML = pageContent;
		theXMLPage.hasError    = false;
		return theXMLPage;
	}

	// Parse the XML document and return an HTML page.
    void parsePage(String forecastXML, ForecastHTMLPage forecast){

		forecast.forecastText = "";
		forecast.errorText    = "";
		forecast.hasError     = true;

		String displayText  = ""; // Text to present to the user.
		int forecastPeriods;      // Number of time periods (e.g. "This evening", "Tomorrow morning", ...) in the forecast.
		String[] forecastTexts;   // Text of forecast for each period.
		String[] forecastTitles;  // Title for each period. ("This evening" ...)
		if (forecastXML.startsWith("<!doctype html public \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html>")){
			forecast.errorText = forecastXML;
			return;
		}
		if (!forecastXML.startsWith("<?xml")){
			Log.i(DEBUG_TAG, "Page appears not to be XML. Page content is: ");
			Log.i(DEBUG_TAG, forecastXML);
			forecast.errorText = "<br><br>Could not load forecast for this location. Please try again later.<br><br><br><br><br>";
			return;
		}

        Document doc;
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(forecastXML));
            doc = dBuilder.parse(inputSource);
        } catch (IOException e){
            if (log_level > 0) {Log.i(DEBUG_TAG, "IOException parsing XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            return;
        } catch (SAXException e){
            if (log_level > 0) {Log.i(DEBUG_TAG, "SAXException parsing XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            return;
        } catch (IllegalArgumentException e){
            if (log_level > 0) {Log.i(DEBUG_TAG, "IllegalArgumentException parsing XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            return;
        } catch (ParserConfigurationException e) {
            if (log_level > 0) {Log.i(DEBUG_TAG, "ParserConfigurationException parsing XML page #1: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            return;
        }

    // Get the text of the forecasts
        try {
            NodeList wordedForecastList = doc.getElementsByTagName("wordedForecast");
            Node nNode = wordedForecastList.item(0);
            NodeList childNodes = nNode.getChildNodes();
            forecastPeriods = childNodes.getLength() - 2;
            forecastTexts  = new String[forecastPeriods];
            forecastTitles = new String[forecastPeriods];
            for (int i = 2; i < childNodes.getLength(); i++) { // Skip element 1 which is just a title. (WARNING: This parsing is brittle and depends on NWS preserving ordering which is not guaranteed.)
                if(childNodes.item(i).getNodeType() == Node.ELEMENT_NODE){
                    Element line = (Element) childNodes.item(i);
                    NodeList gChildNodes = line.getChildNodes();
                    forecastTexts[i-2] = gChildNodes.item(0).getNodeValue(); // Add the forecast text for a time period to the array.
                }
            }
        }catch(Exception e){
            if (log_level > 0) {Log.i(DEBUG_TAG, "Exception parsing XML page #2: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            forecast.errorText = "<br><br>Could not load forecast for this location. Please try again later.<br><br><br><br><br>";
            return;

        }

        try{
            // Find the titles for the time periods
            NodeList timeLayoutList = doc.getElementsByTagName("time-layout");
            Element layoutKey;
            for (int x=0; x<timeLayoutList.getLength(); x++){
                Node timeLayoutNode = timeLayoutList.item(x);
                NodeList timeLayoutItems = timeLayoutNode.getChildNodes();
                layoutKey = (Element) timeLayoutItems.item(1); // First item should be "layout-key"
                if (layoutKey.getChildNodes().item(0).getNodeValue().startsWith("k-p12h")) { // can be ...-n13-1 or ...-n14-1
                    for (int i = 3; i < timeLayoutItems.getLength(); i=i+2) { // Skip first item, which is "layout-key".
                        if(timeLayoutItems.item(i).getNodeType() == Node.ELEMENT_NODE){
                            Element line = (Element) timeLayoutItems.item(i);
                            forecastTitles[i-2] = line.getAttribute("period-name");
                        }
                    }
                }
            }
        } catch (Exception e){
            if (log_level > 0) {Log.i(DEBUG_TAG, "Exception parsing XML page #3: "+ e.getMessage() + "<p>Cause is:" +  e.getCause()+"<p>Stack trace"+ Arrays.toString(e.getStackTrace()));}
            forecast.errorText = "<br><br>Could not load forecast for this location. Please try again later.<br><br><br><br><br>";
            return;
        }

        // Get current conditions. These all should fail gently, with current conditions not displayed if they are not available.
        String theTemperature   = getTemperature(doc);
        String theHumidity      = getHumidity(doc);
        double theWindSpeed     = getWindSpeed(doc);     // Get current wind speed. (Converted from knots to mph.)
        double theWindGust      = getWindGust(doc);      // Get current wind gust.  (Converted from knots to mph.)
        String theWindDirection = getWindDirection(doc); // Get wind direction. (Degrees.)


        // Get the icon for the current conditions.
        String theConditionsIconURL = getIconURL( doc);

        // Get the icon for the current conditions.
        ArrayList<String>  theConditionIconURLs = getIconURLsList(doc, forecastPeriods);

        // Get latitude and longitude.
/*			double latitude  = 0;
        double longitude = 0;
        try{
            NodeList pointNodeList      = doc.getElementsByTagName("point" );
            Node     pointTagNode       = pointNodeList.item(0);             // Get Node for first <latitude> tag.
            if (log_level > 0) {Log.i(DEBUG_TAG, "Point name is: " + pointTagNode.getNodeName() + " Point value is: " + pointTagNode.getNodeValue()) ;}

        }catch(Exception e){
            latitude  = 0;
            longitude = 0;
        }
*/
        // Build up the HTML to display to user.

        // Start with current conditions.
        // Check for presence of local conditions. Zero wind speed is default if no value set. -1 Wind speed mean no value set.
        if (!(theWindSpeed < 0 && (theTemperature.equals("0")) || theTemperature.equals("") || theTemperature.equals("NA") )  ){
            String currentObservations = "<font color=\"blue\"><b>Current Conditions</b></font> <br>";
            displayText = displayText + "<img src=\"" + theConditionsIconURL + "\""  +   " align=\"right\" >  "; // Note: Align right is deprecated, but still supported. (Replaced with styles.)
            displayText = displayText + currentObservations +"<b>Temperature: " + theTemperature + "&deg;F</b><br> ";
            if (theWindSpeed >= 0){ // Sanity check. (-1 signals non-numeric or missing value. "NA" has been seen in forecast XML when local conditions aren't given. (San Francisco))
                displayText = displayText + "<b>Wind:</b> ";
                if (!theWindDirection.equals("")) {displayText = displayText + theWindDirection + " at ";}
                displayText = displayText + (int)theWindSpeed + " mph";
            }
            if (theWindGust > 1) {	 // "0" often appears as gust speed even when wind speed is non-zero.
                displayText = displayText +", gusting to " + (int)theWindGust  + " mph";
            }
            displayText = displayText + "<br><b>Humidity:</b> " + theHumidity + "&#37;<br><br><br><br><br>";
        }

        // Now build HTML for forecast.
        for (int i =1; i<forecastPeriods; i=i+2){ // Hack: I'm recording in every other slot because tag and value each get a slot.
            displayText = displayText + "<img src=\"" + theConditionIconURLs.get((i-1)/2) + "\""  +   " align=\"right\" >  ";
            displayText += "<font color=\"blue\"><b>" + forecastTitles[i] + "</b></font><br>";
            displayText += "     " +  forecastTexts[i]  + "<br><br><br><br>";
        }
        displayText = displayText.replaceAll("%", "%25");
           //displayText = displayText.replaceAll("#", "%23").replaceAll("\\","%27").replaceAll("?","%3f");

        forecast.forecastText = displayText;
		forecast.hasError = false;
	}

    String getTemperature(Document doc) {
        // Get current temperature.
        String theTemperature;
        try {
            NodeList temperatureNodeList = doc.getElementsByTagName("temperature");
            if (log_level > 0) {
                Log.i(DEBUG_TAG, "Got  \"temperature\" node list");
            }
            Log.i(DEBUG_TAG, "currentObservationsNodeList.getLength() = " + temperatureNodeList.getLength());
            Node temperatureTagNode = temperatureNodeList.item(2);             // Get Node for third <temperature> tag.
            NodeList temperatureTagChildNodes = temperatureTagNode.getChildNodes();      // Now extract the temperature from the node.
            Node theTemperatureValueNode = temperatureTagChildNodes.item(1);        // Get second child node. (Node 0 is "<value>"; Node 1 is the temperature Node; ...)
            NodeList theTemperatureNodeList = theTemperatureValueNode.getChildNodes(); // This node list just has the one item ...
            Node theTemperatureNode = theTemperatureNodeList.item(0);          // ... which we fetch here ...
            theTemperature = theTemperatureNode.getNodeValue();       // ... and finally convert to a String.
            if (log_level > 0) {
                Log.i(DEBUG_TAG, "Got actual temperature. " + theTemperature);
            }
        } catch (Exception e) {
            theTemperature = "";
        }
        return theTemperature;
    }

    String getHumidity(Document doc) {
        String theHumidity;
        try {
            NodeList humidityNodeList = doc.getElementsByTagName("humidity");
            Node humidityTagNode = humidityNodeList.item(0);             // Get Node for first <humidity> tag.
            NodeList humidityTagChildNodes = humidityTagNode.getChildNodes();      // Now extract the humidity from the node.
            Node theHumidityValueNode = humidityTagChildNodes.item(1);        // Get second child node. (Node 0 is "<value>"; Node 1 is the humidity Node; ...)
            NodeList theHumidityNodeList = theHumidityValueNode.getChildNodes(); // This node list just has the one item ...
            Node theHumidityNode = theHumidityNodeList.item(0);          // ... which we fetch here ...
            theHumidity = theHumidityNode.getNodeValue();       // ... and finally convert to a String.
            if (log_level > 0) {
                Log.i(DEBUG_TAG, "Got actual humidity. " + theHumidity);
            }
        }
        catch(Exception e){
            theHumidity = "";
        }
        return theHumidity;
    }

    double getWindSpeed(Document doc){
        double theWindSpeed;
        try {
            NodeList windSpeedNodeList = doc.getElementsByTagName("wind-speed");
            Node windSpeedTagNode = windSpeedNodeList.item(1);                    // Get Node for second <wind-speed> tag. (First is gust, second is sustained.)
            NodeList windSpeedTagChildNodes = windSpeedTagNode.getChildNodes();             // Now extract the wind-speed from the node.
            Node theWindSpeedValueNode = windSpeedTagChildNodes.item(1);               // Get second child node. (Node 0 is "<value>"; Node 1 is the wind-speed Node; ...)
            NodeList theWindSpeedNodeList = theWindSpeedValueNode.getChildNodes();        // This node list just has the one item ...
            Node theWindSpeedNode = theWindSpeedNodeList.item(0);                 // ... which we fetch here ...
            theWindSpeed = Double.valueOf(theWindSpeedNode.getNodeValue());                 // ... and finally convert to a String.
            theWindSpeed = Math.round(theWindSpeed * 1.15077945);                            // Convert from knots to mph.
            if (log_level > 0) {Log.i(DEBUG_TAG, "Got wind speed. "+ theWindSpeed);}
        }
        catch (NumberFormatException e){
            theWindSpeed = -1; // Sometimes the wind speed is "NA" and this appears to be different than zero, since nearby locations have substantial wind speed. -1 flags unknown value.
        }
        return theWindSpeed;
    }

    double getWindGust(Document doc){
        double theWindGust;
        try {
            NodeList windGustNodeList = doc.getElementsByTagName("wind-speed");
            Node windGustTagNode = windGustNodeList.item(0);                   // Get Node for first <wind-speed> tag. (This is gust, second is sustained.)
            NodeList windGustTagChildNodes = windGustTagNode.getChildNodes();            // Now extract the wind-Gust from the node.
            Node theWindGustValueNode = windGustTagChildNodes.item(1);              // Get second child node. (Node 0 is "<value>"; Node 1 is the wind-Gust Node; ...)
            NodeList theWindGustNodeList = theWindGustValueNode.getChildNodes();       // This node list just has the one item ...
            Node theWindGustNode = theWindGustNodeList.item(0);                // ... which we fetch here ...
            theWindGust = Double.valueOf(theWindGustNode.getNodeValue());                // ... and finally convert to a String. (Throws NumberFormatException)
            theWindGust = Math.round(theWindGust * 1.15077945);                            // Convert from knots to mph.
            if (log_level > 0) {Log.i(DEBUG_TAG, "Got wind gust. "+ theWindGust);}
        }
        catch (Exception e){
            theWindGust = 0; // Zero flags an unknown value and will not be displayed.
        }
        return theWindGust;
    }

    String getWindDirection (Document doc){
        String theWindDirection;
        try {
            NodeList windDirectionNodeList = doc.getElementsByTagName("direction");
            Node windDirectionTagNode = windDirectionNodeList.item(0);             // Get Node for first <wind-Direction> tag.
            NodeList windDirectionTagChildNodes = windDirectionTagNode.getChildNodes();      // Now extract the wind-Direction from the node.
            Node theWindDirectionValueNode = windDirectionTagChildNodes.item(1);        // Get second child node. (Node 0 is "<value>"; Node 1 is the wind-Direction Node; ...)
            NodeList theWindDirectionNodeList = theWindDirectionValueNode.getChildNodes(); // This node list just has the one item ...
            Node theWindDirectionNode = theWindDirectionNodeList.item(0);          // ... which we fetch here ...
            String theWindDirectionDegrees = theWindDirectionNode.getNodeValue();       // ... and finally convert to a String.
            theWindDirection = Forecast.getWindDirection(theWindDirectionDegrees);
            if (log_level > 0) {Log.i(DEBUG_TAG, "Got wind direction. "+ theWindDirection);}
        }
        catch (Exception e){
            theWindDirection = "";
            if (log_level > 0) {Log.i(DEBUG_TAG, "Failed to get wind direction.");}
        }
        return theWindDirection;
    }

    String getIconURL(Document doc){
        String   theConditionsIconURL;
        try {
            NodeList conditionsIconNodeList      = doc.getElementsByTagName("conditions-icon");
            Node     conditionsIconTagNode       = conditionsIconNodeList.item(1);             // Get Node for second <conditions-icon> tag.
            NodeList conditionsIconTagChildNodes = conditionsIconTagNode.getChildNodes();      // Now extract the icon from the node.
            Node     theConditionsIconValueNode  = conditionsIconTagChildNodes.item(3);        // Get fourth child node. (Skip the pair for "<name>Conditions Icon</name>" and  the one for the "<icon-link tag>")
            NodeList theConditionsIconNodeList   = theConditionsIconValueNode.getChildNodes(); // This node list just has the one item ...
            Node     theConditionsIconNode       = theConditionsIconNodeList.item(0);          // ... which we fetch here ...
            theConditionsIconURL        = theConditionsIconNode.getNodeValue();       // ... and finally convert to a String.
            if (log_level > 0) {Log.i(DEBUG_TAG, "Got conditions icon URL. "+ theConditionsIconURL);}
        } catch (Exception e){
            theConditionsIconURL = ""; // Just skip the icon if we can't get it.
            if (log_level > 0) {Log.i(DEBUG_TAG, "Failed to get conditions icon.");}
        }
        if (theConditionsIconURL.equals("http://forecast.weather.gov/images/wtf/NULL")){ // This value is used if no local conditions are set.
            theConditionsIconURL = ""; // Display nothing.
        }
        return   theConditionsIconURL;
    }

    ArrayList<String> getIconURLsList(Document doc, int forecastPeriods){
        ArrayList<String>  theConditionIconURLs  = new  ArrayList<String>();
        try{
            NodeList conditionsIconsNodeList      = doc.getElementsByTagName("conditions-icon");
            Node     conditionsIconsTagNode       = conditionsIconsNodeList.item(0);             // Get Node for first <conditions-icon> tag.
            NodeList conditionsIconsTagChildNodes = conditionsIconsTagNode.getChildNodes();      // Now extract the temperature from the node.
            if (log_level > 0) {Log.i(DEBUG_TAG, "conditionsIconsTagChildNodes.getLength() " + conditionsIconsTagChildNodes.getLength());} // 31, but only 14 icon links.
            for (int iconIndex = 3; iconIndex < conditionsIconsTagChildNodes.getLength(); iconIndex +=2){
                Node     theConditionsIconsValueNode  = conditionsIconsTagChildNodes.item(iconIndex); // Get child node. Node counting: I think the <tag> counts as one as does the <>value</> between the tags, so we get every other and skip the initial pair for "<name>Conditions Icon</name>"
                NodeList theConditionsIconsNodeList   = theConditionsIconsValueNode.getChildNodes();  // This node list just has the one item ...
                Node     theConditionsIconsNode       = theConditionsIconsNodeList.item(0);           // ... which we fetch here ...
                String   theConditionsIconsURL        = theConditionsIconsNode.getNodeValue();        // ... and finally convert to a String.
                //if (log_level > 0) {Log.i(DEBUG_TAG, "Got conditions icon URL. "+ theConditionsIconsURL);}
                theConditionIconURLs.add(theConditionsIconsURL);
            }
        }catch(Exception e){
            for (int i=0; i<forecastPeriods; i++){theConditionIconURLs.add("");} // Pad out the array with empty strings so the HTML is generated without errors.
        }
        return theConditionIconURLs;
    }
	
	long timeSinceLastForecast(){
		if (currentForecast.forecastTime == 0) {
			return 60*60*24*7;// Default value of 1 week ago ensures that forecast is clearly expired.
		}else{
			return(System.currentTimeMillis()/1000 - currentForecast.forecastTime); //forecastTime); // Time is UTC, so wrong/changed time zone has no effect.
		}
	}
	
	int getPreferredLocationID(){
		int preferredLocationID = 1;
		try{
			SharedPreferences settings = getPreferences(0);
			preferredLocationID = settings.getInt("preferredLocationID", 0);
			if (log_level > 0) {Log.i(DEBUG_TAG, "Got location ID "+preferredLocationID+ " from preferences.");}
		}catch (Exception e){
			Log.e(DEBUG_TAG, "Error in getPreferredLocationID: " + e);
		}
		return preferredLocationID;
	}
	
	boolean savePreferredLocationID(int preferredLocationID){
		try{
			SharedPreferences settings = getPreferences(0);
			Editor theEditor = settings.edit();
			theEditor.putInt("preferredLocationID", preferredLocationID);
			theEditor.apply();
			if (log_level > 0) {Log.i(DEBUG_TAG, "Saved preferredLocationID "+preferredLocationID);}
			return true;
		}
		catch (Exception e) {
			Log.e(DEBUG_TAG, toastErrorMessage += "Error saving preferredLocationID\n" + e);
			return false;
		}
	}

    @Override
    protected void onPause(){
		super.onPause();
		//locationManager.removeUpdates(locationListener); // Stop getting updates to physical location when we're not in foreground. (For the sake of the battery.)
	}	
    
    @Override
    protected void onStop(){
		super.onStop(); 
		// Place holder in case anything needs to happen when application is killed. Currently state is saved on successful retrieval of forecast from Internet. 
    }	
    
    @Override
    protected void onResume(){
		if (log_level > 0) {Log.i(DEBUG_TAG, "onResume called.");}
		super.onResume();
		refreshScreen();
		showLastUpdateTime();
		boolean tooMuchTimeHasPassed = (timeSinceLastForecast() > (60 * 30)); // Set to 30 minutes. The weather service currently updates once per hour at 45 minutes after the hour.
		if (tooMuchTimeHasPassed) { // Run update in background thread so we don't freeze up UI on waiting for network.
			if (log_level > 0) {Log.i(DEBUG_TAG, "Too much time has passed. Updating forecast in background." );}
			backgroundForecastUpdate(currentForecast.location);
		}
    	// Register the listener with the Location Manager to receive location updates
    	/*locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
    			1000*60*20, // Time in ms. (20 minutes.)
    			5000,       // Distance in meters.(5 km) Low value used to reflect area with micro-climate conditions.
    			locationListener);*/
    }
    
    void backgroundForecastUpdate(String theLocation){ 
    	if (!updating){ // Avoid herd of simultaneous updates. 
    		updating = true;
	    	new AsyncTask<String, Void, String>(){
				@Override
				protected void onPostExecute(String result) {
					updating = false;
					refreshScreen();
				}
				@Override
				protected String doInBackground(String... param){
					String theLocation = param[0];
					getForecast(theLocation);
					return "";
				}
			}.execute(theLocation);
		}else {
			if (log_level > 0) {Log.i(DEBUG_TAG, "Attempt to update forecast blocked due to another update in progress." );}
		}
	}
    
    void showAboutBox(){
        AlertDialog builder;
        try {
        	builder = AboutDialogBuilder.create(this);
        	builder.show();
        } catch (NameNotFoundException e) {
        	e.printStackTrace();
        }
    }

    void deleteCurrentLocation(){
	    // Delete the location from the database.
    	theForecastManager.deleteForecast(currentForecast.id);
	    // Reload the forecast list from the database, resetting preferred forecast, if needed.	
    	loadForecasts();
    	// Refresh the display, showing another location.
    	refreshScreen();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weather_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.about:
        	showAboutBox();
            return true;
        case R.id.add:
        	onAddLocation();
        	return true;
        case R.id.delete:
        	showDialog(DELETE_LOCATION_DIALOG_ID);
        	//deleteCurrentLocation();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    
    
	public static boolean isAlaska (String theLocation){
    	theLocation = theLocation.trim().toUpperCase(Locale.US); // Remove white space and make upper case.
    	if (theLocation.endsWith("AK") || theLocation.endsWith("ALASKA")){return true;}
    	try{
    		int zip = Integer.parseInt(theLocation);
    		if (zip>=99500 && zip<=99999){return true;} // Alaska ZIP codes.
    	}catch (NumberFormatException ignored) {} // Throws exception if the location is not a ZIP code, which is fine - we simply return false.
    	return false; // theLocation is not in Alaska.
    }
    
    @SuppressLint("DefaultLocale")
    String preprocessPlaceName(String thePlace){ // Inserts comma if needed into place name, between city and state.
    	thePlace = thePlace.trim().toUpperCase(Locale.US); 
    	if (thePlace.matches("\\d\\d\\d\\d\\d")){ // Check for 5-digit ZIP code.
    		return thePlace;
    	}
    	if (! thePlace.contains(",")){ // Check for missing comma.
    		String[] states = { // States and other districts.
				"ALABAMA",
				"AL",
				"ALASKA",
				"AK",
				"AMERICAN SAMOA",
				"AS",
				"ARIZONA",
				"AZ",
				"ARKANSAS",
				"AR",
				"CALIFORNIA",
				"CA",
				"COLORADO",
				"CO",
				"CONNECTICUT",
				"CT",
				"DELAWARE",
				"DE",
				"DISTRICT OF COLUMBIA",
				"DC",
				"FEDERATED STATES OF MICRONESIA",
				"FM",
				"FLORIDA",
				"FL",
				"GEORGIA",
				"GA",
				"GUAM",
				"GU",
				"HAWAII",
				"HI",
				"IDAHO",
				"ID",
				"ILLINOIS",
				"IL",
				"INDIANA",
				"IN",
				"IOWA",
				"IA",
				"KANSAS",
				"KS",
				"KENTUCKY",
				"KY",
				"LOUISIANA",
				"LA",
				"MAINE",
				"ME",
				"MARSHALL ISLANDS",
				"MH",
				"MARYLAND",
				"MD",
				"MASSACHUSETTS",
				"MA",
				"MICHIGAN",
				"MI",
				"MINNESOTA",
				"MN",
				"MISSISSIPPI",
				"MS",
				"MISSOURI",
				"MO",
				"MONTANA",
				"MT",
				"NEBRASKA",
				"NE",
				"NEVADA",
				"NV",
				"NEW HAMPSHIRE",
				"NH",
				"NEW JERSEY",
				"NJ",
				"NEW MEXICO",
				"NM",
				"NEW YORK",
				"NY",
				"NORTH CAROLINA",
				"NC",
				"NORTH DAKOTA",
				"ND",
				"NORTHERN MARIANA ISLANDS",
				"MP",
				"OHIO",
				"OH",
				"OKLAHOMA",
				"OK",
				"OREGON",
				"OR",
				"PALAU",
				"PW",
				"PENNSYLVANIA",
				"PA",
				"PUERTO RICO",
				"PR",
				"RHODE ISLAND",
				"RI",
				"SOUTH CAROLINA",
				"SC",
				"SOUTH DAKOTA",
				"SD",
				"TENNESSEE",
				"TN",
				"TEXAS",
				"TX",
				"UTAH",
				"UT",
				"VERMONT",
				"VT",
				"VIRGIN ISLANDS",
				"VI",
				"VIRGINIA",
				"VA",
				"WASHINGTON",
				"WA",
				"WEST VIRGINIA",
				"WV",
				"WISCONSIN",
				"WI",
				"WYOMING"};
    		
    		// Look for a US state or territory name.
            for(String theState:states){
                if (thePlace.endsWith(theState)){
                    int stateIndex = thePlace.lastIndexOf(theState); // Returns the index of the first character of the state name.
                    String state = thePlace.substring(stateIndex);
                    String city  = thePlace.substring(0, stateIndex);
                    return city + ", " + state;
                }

            }

    	}
        // These are the largest cities in the US.
    	String[] cities = {
    			"NEW YORK", "NY",
    			"LOS ANGELES","CA",
    			"CHICAGO","IL",
    			"HOUSTON","TX",
    			"PHILADELPHIA","PA",
    			"PHOENIX","AZ",
    			"SAN ANTONIO","TX",
    			"DALLAS","TX",
    			"SAN JOSE","CA",
    			"JACKSONVILLE","FL",
    			"INDIANAPOLIS","IN",
    			"AUSTIN","TX",
    			"SAN FRANCISCO","CA",
    			"COLUMBUS","OH",
    			"FORT WORTH","TX",
    			"CHARLOTTE","NC",
    			"DETROIT","MI",
    			"EL PASO","TX",
    			"MEMPHIS","TN",
    			"BOSTON","MA",
    			"SEATTLE","WA",
    			"DENVER","CO",
    			"BALTIMORE","MD",
    			"WASHINGTON","DC"
    	};
    	for (int i=0; i<cities.length; i=i+2){
    		if (thePlace.toUpperCase(Locale.US).equals(cities[i].toUpperCase(Locale.US)) ){
    			thePlace +=  ", " + cities[i+1]; // Add the missing state, with comma.
    		}
    	}
    	
    	String[] nicknames = {
    			"PHILLY",    "Philadelphia, PA",
    			"LA",        "Los Angeles, CA",
    			"NY",        "New York, NY",
    			"NYC",       "New York, NY",
    			"BEAN TOWN", "Boston, MA",
    			"DC",        "Washington, DC"
    	};
    	for (int i=0; i<nicknames.length; i=i+2){
    		if (thePlace.toUpperCase(Locale.US).equals(nicknames[i])){
    			thePlace = nicknames[i + 1]; // Substitute name for nickname.
    		}
    	}
    	return thePlace;
    } 

    void showLastUpdateTime(){
        String theSavedForecastText = currentForecast.forecastHTML;//getSavedForecastText();
        if ((theSavedForecastText == null) || theSavedForecastText.equals("")) {
            return;
        } // Ensure that there is an existing forecast.
        String updateTimeMessage;
        int secondsSinceLastUpdate = (int) Math.floor(timeSinceLastForecast());
        int minutesSinceLastUpdate = (int) Math.floor(secondsSinceLastUpdate/60);
        int hoursSinceLastUpdate   = (int) Math.floor(minutesSinceLastUpdate/60);
        int daysSinceLastUpdate    = (int) Math.floor(hoursSinceLastUpdate/24);
        switch (daysSinceLastUpdate ){
        case 0:
              switch (hoursSinceLastUpdate){
              case 0:
                  switch (minutesSinceLastUpdate){
                  case 0:  {  // Use this case for debugging, since "seconds" become stale quickly.
                      if (secondsSinceLastUpdate == 1){
                          updateTimeMessage = secondsSinceLastUpdate + " second";
                      }else{
                          updateTimeMessage = secondsSinceLastUpdate + " seconds";
                      }
                      break;
                      }
                  case 1:  updateTimeMessage = "1 minute" ; break;
                  default: updateTimeMessage = minutesSinceLastUpdate + " minutes" ;
                  }
                  break;
              case 1: updateTimeMessage  = "over 1 hour" ; break;
              default: updateTimeMessage = "over " + hoursSinceLastUpdate + " hours" ;
              }
              break;

        case 1: updateTimeMessage =  "over 1 day" ; break;
        default:
            updateTimeMessage = "over " + daysSinceLastUpdate + " days" ;
        }
        timeSinceUpdateView.setText("(Updated " + updateTimeMessage + " ago.)");
    }
    
    // Show info messages with toast. Adds variable display length.
    void showToastMessage(String theMessage, int duration){

    	Context context = getApplicationContext();
    	final Toast toast = Toast.makeText(context, theMessage, Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.TOP, 0, 0);
    	final int theDuration = duration;
    	
        Thread t = new Thread() {
            public void run() {
                int count = 0;
                try {
                    while (count < theDuration) {
                        toast.show();
                        sleep(1000);
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
        };
        t.start();
    }
    
    private class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    viewFlipper.setInAnimation(slideLeftIn);
                    viewFlipper.setOutAnimation(slideLeftOut);
                    refreshScreen();
                    viewFlipper.showNext();
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	viewFlipper.setInAnimation(slideRightIn);
                    viewFlipper.setOutAnimation(slideRightOut);
                    refreshScreen();
                	viewFlipper.showPrevious();
                }
            } catch (Exception e) {
          	  if (log_level > -1) {Log.i(DEBUG_TAG, "Error in horizontal swipe. "+e); }
            } 
            showLastUpdateTime();
            return false;
        }
    }
    
    void onAddLocation() {
    	try {
            showDialog(ADD_LOCATION_DIALOG_ID);
    	} catch (Exception e){
    		if (log_level > 0) {Log.i(DEBUG_TAG, "Error creating dialog." +e );}
    	}
    }
    
/*    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	try{
	        super.onPrepareDialog(id, dialog);
            AlertDialog myDialog = (AlertDialog) dialog;
            myDialog.setMessage("A message.");
	        return;
    	} catch (Exception e) {
    		showToastMessage("Error preparing dialog. " +e ,3);
    	}
    }*/
    
    @Override
    // Add location dialog addLocation()
    protected Dialog onCreateDialog(int id) {
    	switch (id){
    	case 1: // Add location 
    		return createAddLocationDialog();
    	default:
    		return createConfirmDeleteDialog();
    	}
    }
    
    Dialog createAddLocationDialog(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.dialog_title);
    	builder.setMessage(R.string.dialog_message);

    	//Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	builder.setView(input);

    	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    			Toast.makeText(getApplicationContext(), "New location is: " + input.getText(), Toast.LENGTH_SHORT).show();
    			String theLocation = input.getText().toString();

    			new AsyncTask<String, Void, String>(){
    				@Override
    				protected void onPostExecute(String result) {
    					refreshScreen();
    					myMainScrollView.fullScroll(View.FOCUS_UP);// Scroll to top.
    					if ( !toastErrorMessage.equals("") ) {      // Show error dialog pop-up, if needed.
    						showToastMessage(toastErrorMessage+"\n", 5);
    						if (log_level > -1) {Log.i(DEBUG_TAG, toastErrorMessage);} // Log the error.
    						toastErrorMessage="";
    					} 
    				}
    				@Override
    				protected String doInBackground(String... param){
    					String theLocation = param[0]; // param[0] is theLocation, passed by .execute(theLocation)
    					getForecast(theLocation);      // NOTE: I could do this with the existing background forecast update call, I think, as long the the index is handled correctly.
    					int newForecastIndex = findForecastIndex(theLocation);
    					if (newForecastIndex >= 0 && newForecastIndex < forecasts.size()) {currentForecastIndex = newForecastIndex;}
    					return "";
    				}
    			}.execute(theLocation);
            } });
    	builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
            } });
    	return builder.create();    	
    }
    
    Dialog createConfirmDeleteDialog(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.dialog_confirm_delete_title);
    	builder.setMessage(R.string.dialog_confirm_delete_message);
    	
    	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    			deleteCurrentLocation();
            } });
    	builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
            } });
    	return builder.create();    	    	
    }
    
    // Handle selection of item from drop-down spinner for location selection.
    private class MyOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long itemID) {
        	Log.i(DEBUG_TAG, "onItemSelected called.");
        	if (currentForecastIndex != (int) itemID){ // Don't do anything unless a new location is selected. onItemSelected() gets called more than I'd expect. 
	            currentForecastIndex = (int) itemID; // My location indices match the drop-down positions and itemIDs.
	            currentForecast = forecasts.get(currentForecastIndex);
	            refreshScreen();  
        	}
            try{
	    		boolean tooMuchTimeHasPassed = timeSinceLastForecast() > 60*30; // Set to 30 minutes. NOAA updates every hour at 45 minutes after the hour.
	    		if (tooMuchTimeHasPassed) { // Run update in background thread so we don't freeze up UI on waiting for network.
	    			backgroundForecastUpdate(currentForecast.location);
    				backgroundUpdateHourlyXMLData(currentForecast);
	    		}else{
	    			Log.i(DEBUG_TAG, "Value of \"hourlyDataUpdating\" is: " + hourlyDataUpdating);
	    			//if (currentForecast.hourlyHTML.equals("") || currentForecast.hourlyHTML.startsWith("<!-- Error -->")){ // If the current location is missing hourly data ... 
	    			if (currentForecast.getHourlyHTMLTable().equals("") || currentForecast.getHourlyHTMLTable().startsWith("<!-- Error -->")){ // If the current location is missing hourly data ... 
	    				backgroundUpdateHourlyXMLData(currentForecast);   // ... retrieve that data.
	    			}
	    		}
            }catch(Exception e){
            	if (log_level > 0) {Log.i(DEBUG_TAG, "Error on tooMuchTimeHasPassed backgroundForecastUpdate. " + e);}
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
    
    // Get the forecast's ArrayList index from its database ID.
    int findForecastIndex(int theLocationID){
    	for (int i = 0; i<forecasts.size(); i++){
    		if (forecasts.get(i).id == theLocationID){
    			return i;
    		}
    	}
    	return -1;
    }
    // Get the forecast's ArrayList index from its location.
    int findForecastIndex(String theLocation){
    	for (int i = 0; i<forecasts.size(); i++){
    		if (forecasts.get(i).location.equals( theLocation) ){
    			return i;
    		}
    	}
    	return -1;
    }
        
    // Quick and dirty FTP of error code to my server.
    void sendErrorToJon(String theError, String theLocation){

    	String tag = "FTP";
	    Log.i("FTP info", "Called sendErrorToJon");

    	// Send error home for bug fixing.
    	String theErrorReport = theLocation + "\n\n" + theError; 

    	FTPClient ftpClient = new FTPClient();
    	String server = "ftp.downhomebiketours.com";
    	String user   = "simplyweather@downhomebiketours.com";
    	String pass   = "simply651730";
    	String path   = "/";
    	 
    	try {
    	    ftpClient.connect(InetAddress.getByName(server));
    	    Log.i("FTP info", "Connected");
    	    ftpClient.login(user, pass);
    	    Log.i("FTP info", "Logged in");
    	    ftpClient.changeWorkingDirectory(path);
    	    Log.i("FTP info", "Changed directory");
    	 
    	    for (int i=0; i < 4; i++){ // Give a few tries in case server is chatty. 
        	    Log.i("FTP info", "Reply string is: " + ftpClient.getReplyString());
        	    
	    	    if (ftpClient.getReplyString() != null && ftpClient.getReplyString().contains("250")) {
	        	    Log.i("FTP info", "Got 250 reply");
	
	    	        ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
	    	        BufferedInputStream buffIn;
	        		InputStream is = new ByteArrayInputStream(theErrorReport.getBytes());
	    	        buffIn = new BufferedInputStream(is);
	    	        ftpClient.enterLocalPassiveMode();
	    	 
	    	        String fileName = path + "Error_log_from_" + theLocation + Math.floor(Math.random()*1000000);
	    	        //boolean result = 
	    	        ftpClient.storeFile(fileName, buffIn);
	    	        buffIn.close();
	    	        ftpClient.logout();
	    	        ftpClient.disconnect();
	    	    }
    	    }
    	 
    	} catch (SocketException e) {
    	    Log.e(tag, e + Arrays.toString(e.getStackTrace()));
    	} catch (UnknownHostException e) {
    	    Log.e(tag, e + Arrays.toString(e.getStackTrace()));
    	} catch (IOException e) {
    	    Log.e(tag, e + Arrays.toString(e.getStackTrace()));
    	}
    }

    // The following disabled code is for use once the application is able to get GPS coordinates
    // There will be an option to get forecast for the current location.

    //import android.location.Location;
    //import android.location.LocationListener;
    //import android.location.LocationManager;

    // URL to request specific XML data for a location with known latitude and longitude.
    // http://graphical.weather.gov/xml/SOAP_server/ndfdXML.htm generates sample queries. All empty requests can be dropped. http://site.com/foo.php?a=&b=2 can be http://site.com/foo.php?b=2 instead.

    /*String buildXMLRequestURL(double latitude, double longitude){
        String baseURL = "http://graphical.weather.gov/xml/SOAP_server/ndfdXMLclient.php?whichClient=NDFDgen";
        String lat     = "&lat="+Double.toString(latitude);
        String lng     = "&lon"+Double.toString(longitude);
        String product = "&product=time-series";
        String unit    = "&Unit=e";
        String maxTemp = "&maxt=maxt";
        String temp    = "&temp=temp";
        String submit  = "&Submit=Submit";
        return baseURL + lat + lng + product + unit + maxTemp + temp + submit;
    }*/

}