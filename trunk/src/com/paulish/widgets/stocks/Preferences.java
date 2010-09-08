package com.paulish.widgets.stocks;

import java.util.*;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.appwidget.AppWidgetManager;
import android.preference.PreferenceManager;

public class Preferences {
    public static final String PORTFOLIO = "Portfolio-%d";
    // let update interval be common for all the widgets
    public static final String UPDATE_INTERVAL = "UpdateInterval";   
    public static final int DEFAULT_UPDATE_INTERVAL = 15; // 15 minutes
       
    public static String get(String aPref, int aAppWidgetId) {
    	return String.format(aPref, aAppWidgetId);    	
    }
    
    public static List<String> getPortfolio(Context context, int appWidgetId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		String commaTickers = prefs.getString(Preferences.get(Preferences.PORTFOLIO, appWidgetId), context.getString(R.string.defaultPortfolio));
		return new ArrayList<String>(Arrays.asList(commaTickers.split(",")));
    }
    
    public static List<String> getAllPortfolios(Context context) {
    	ArrayList<String> result = new ArrayList<String>();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int[] appWidgetIds = getAllWidgetIds(context);
    	String commaTickers;
    	String[] tickers;
    	for (int appWidgetId : appWidgetIds) {
    		commaTickers = prefs.getString(Preferences.get(Preferences.PORTFOLIO, appWidgetId), context.getString(R.string.defaultPortfolio));
    		tickers = commaTickers.split(",");
    		for (String ticker : tickers) {
    			if (!result.contains(ticker))
    			  result.add(ticker);
    		}
    	}
    	return result;
    }
    
    public static void setPortfolio(Context context, int appWidgetId, String tickers) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Editor edit = prefs.edit();
    	edit.putString(Preferences.get(Preferences.PORTFOLIO, appWidgetId), tickers);
    	edit.commit();
    }
    
    public static int getUpdateInterval(Context context) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		return prefs.getInt(Preferences.UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
    }
    
    public static void setUpdateInterval(Context context, int interval) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Editor edit = prefs.edit();
    	edit.putInt(Preferences.UPDATE_INTERVAL, interval);
    	edit.commit();    	
    }
    
    public static void DropSettings(Context context, int[] appWidgetIds) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		for(int appWidgetId : appWidgetIds) {
			edit.remove(Preferences.get(Preferences.PORTFOLIO, appWidgetId));			
		}
		edit.commit();
    }
    
    public static int[] getAllWidgetIds(Context context) {
    	AppWidgetManager awm = AppWidgetManager.getInstance(context);
    	List<int[]> result = new ArrayList<int[]>();
    	
    	result.add(awm.getAppWidgetIds(new ComponentName(context, StocksWidget.class)));
    	
    	int i = 0;
    	for(int[] arr : result)
    	  i += arr.length;
    	
    	int[] res = new int[i];
    	i = 0;
    	for (int[] arr : result) {
    		for (int id : arr) {
    			res[i++] = id;
    		}
    	}
    	
    	return res;
    }
}
