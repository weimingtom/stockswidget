package com.paulish.widgets.stocks;

import java.util.*;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.appwidget.AppWidgetManager;
import android.preference.PreferenceManager;

public class Preferences {
    public static final String TICKERS = "EditTickers-%d";
       
    public static String get(String aPref, int aAppWidgetId) {
    	return String.format(aPref, aAppWidgetId);    	
    }
    
    public static List<String> getTickers(Context context, int appWidgetId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		String commaTickers = prefs.getString(Preferences.get(Preferences.TICKERS, appWidgetId), context.getString(R.string.tickersDefault));
		return new ArrayList<String>(Arrays.asList(commaTickers.split(",")));
    }
    
    public static List<String> getAllTickers(Context context) {
    	ArrayList<String> result = new ArrayList<String>();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int[] appWidgetIds = getAllWidgetIds(context);
    	String commaTickers;
    	String[] tickers;
    	for (int appWidgetId : appWidgetIds) {
    		commaTickers = prefs.getString(Preferences.get(Preferences.TICKERS, appWidgetId), context.getString(R.string.tickersDefault));
    		tickers = commaTickers.split(",");
    		for (String ticker : tickers) {
    			if (!result.contains(ticker))
    			  result.add(ticker);
    		}
    	}
    	return result;
    }
    
    public static void setTickers(Context context, int appWidgetId, String tickers) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Editor edit = prefs.edit();
    	edit.putString(Preferences.get(Preferences.TICKERS, appWidgetId), tickers);
    	edit.commit();
    }
    
    public static void DropSettings(Context context, int[] appWidgetIds) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		for(int appWidgetId : appWidgetIds) {
			edit.remove(Preferences.get(Preferences.TICKERS, appWidgetId));
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
