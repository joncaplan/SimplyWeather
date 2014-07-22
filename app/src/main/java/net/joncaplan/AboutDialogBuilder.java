package net.joncaplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.webkit.WebView;

// Code adapted from: http://www.itkrauts.com/archives/26-Creating-a-simple-About-Dialog-in-Android-1.6.html
class AboutDialogBuilder {
    public static AlertDialog create(Context context) throws NameNotFoundException {
        // Try to load the a package matching the name of our own package
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        String versionInfo = pInfo.versionName;

        String aboutTitle = String.format("About %s", context.getString(R.string.app_name));
        String versionString = String.format("Version: %s", versionInfo);
        String aboutText;
        aboutText = "Simply Weather gives you just the forecast, in detail. " +
                "Swipe left or right in the app to see table of temperature, wind speed and wind direction. <br><br>" +
                "Feedback and bug reports welcome at:<br>simplyweather@joncaplan.net<br><br>" +
                "This program is open source and available under the GPL 2 license at GitHub.  " +
                "<a href='http://github.com/joncaplan/SimplyWeather'>github.com/joncaplan/SimplyWeather</a>" +
                "<br>(c) 2014 Jon Caplan.<br><br>" +
                "About, add, delete and refresh icons from: " +
                "<a href='http://www.visualpharm.com'>www.visualpharm.com</a> under CC license.<br><br>"+
                "<center><img src='http://ecx.images-amazon.com/images/I/71S-R%2BOZ1dL._SL500_AA300_.png' ></center>"
                ;

        String donateButton_PayPal = "I've made this program free and open source, but " +
                "if you'd like to contribute a buck or two to say thanks, just use the PayPal button below.<br><br>"+
                "<center>" +
                "<form action=\"https://www.paypal.com/cgi-bin/webscr\" method=\"post\" target=\"_top\">\n" +
                        "<input type=\"hidden\" name=\"cmd\" value=\"_s-xclick\">\n" +
                        "<input type=\"hidden\" name=\"hosted_button_id\" value=\"ZP6F22F5ANF5G\">\n" +
                        "<input type=\"image\" src=\"https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif\" border=\"0\" name=\"submit\" alt=\"PayPal - The safer, easier way to pay online!\">\n" +
                        "<img alt=\"\" border=\"0\" src=\"https://www.paypalobjects.com/en_US/i/scr/pixel.gif\" width=\"1\" height=\"1\">\n" +
                        "</form>" +
                        "</center>";

        final WebView message = new WebView(context);
        String aboutBoxHTML = "<HTML><HEAD></HEAD><BODY>" +
                aboutText + donateButton_PayPal + "<br>" + versionString +
                "</BODY></HTML>";
        message.loadData(aboutBoxHTML, "text/html", "utf-8");
        return new AlertDialog.Builder(context).setTitle(aboutTitle).setCancelable(true).setIcon(R.drawable.icon).setPositiveButton(
                context.getString(android.R.string.ok), null).setView(message).create();

    }
}