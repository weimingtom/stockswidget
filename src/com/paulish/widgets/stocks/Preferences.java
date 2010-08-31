package com.paulish.widgets.stocks;

import java.util.ArrayList;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
    public static final String TICKERS = "editTickers";
       
    public static String get(String aPref, int aAppWidgetId) {
    	return String.format(aPref, aAppWidgetId);    	
    }
    
    public static String[] getTickers(Context context, int aAppWidgetId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		String commaTickers = prefs.getString(Preferences.get(Preferences.TICKERS, aAppWidgetId), context.getString(R.string.tickersDefault));
		return commaTickers.split(",");
    }
    
    public static void DropSettings(Context context, int[] appWidgetIds) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		for(int appWId : appWidgetIds) {
			edit.remove(Preferences.get(Preferences.TICKERS, appWId));
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
