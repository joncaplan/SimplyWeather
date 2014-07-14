package net.joncaplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.widget.TextView;
import android.webkit.WebView;

// Code borrowed from: http://www.itkrauts.com/archives/26-Creating-a-simple-About-Dialog-in-Android-1.6.html
public class AboutDialogBuilder {
    public static AlertDialog create(Context context, boolean useAds) throws NameNotFoundException {
        // Try to load the a package matching the name of our own package
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        String versionInfo = pInfo.versionName;

        String aboutTitle = String.format("About %s", context.getString(R.string.app_name));
        String versionString = String.format("Version: %s", versionInfo);
        String aboutText;
        if (useAds)
            aboutText = "Simply Weather Free gives you just the forecast, in detail! " +
                    "Swipe left or right to see table of weather data. \n\n" +
                    "Visit apps.joncaplan.net to upgrade to ad-free version for $0.99.\n\n" +
                    "Feedback and bug reports welcome at:\nsimplyweather@joncaplan.net \n(c) 2014 Jon Caplan. " +
                    "This program is open source and available the GPL 2 license at GitHub.  " +
                    "(https://github.com/joncaplan/SimplyWeather)\n\n" +
                    "About, add, delete and refresh icons from: http://www.visualpharm.com under CC license.";
        else
            aboutText = "Simply Weather gives you just the forecast, in detail. " +
                    "Swipe left or right to see table of weather data. <br><br>" +
                    "Feedback and bug reports welcome at:<br>simplyweather@joncaplan.net <br>(c) 2014 Jon Caplan. <br><br>" +
                    "This program is open source and available under the GPL 2 license at GitHub.  " +
                    "<a href='http://github.com/joncaplan/SimplyWeather'>github.com/joncaplan/SimplyWeather</a><br><br>" +
                    "About, add, delete and refresh icons from: " +
                    "<a href='http://www.visualpharm.com'>www.visualpharm.com</a> under CC license.<br><br>"+
                    "Version " + versionString ;

        final WebView message = new WebView(context);
        String aboutBoxHTML = "<HTML><BODY>" + aboutText + "</BODY></HTML>";
        message.loadData(aboutBoxHTML, "text/html", "utf-8");
        return new AlertDialog.Builder(context).setTitle(aboutTitle).setCancelable(true).setIcon(R.drawable.icon).setPositiveButton(
                context.getString(android.R.string.ok), null).setView(message).create();

    }
}