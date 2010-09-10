package com.paulish.widgets.stocks;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public abstract class StocksWidget extends AppWidgetProvider {
	// Tag for logging
	protected static final String TAG = "paulish.StocksWidget";
	// Actions
	public static final String ACTION_WIDGET_NOTIFY_LOADING = "notify_loading";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// If no specific widgets requested, collect list of all				

		if (appWidgetIds == null) 
			appWidgetIds = Preferences.getAllWidgetIds(context);

		for (int appWidgetId : appWidgetIds) { 
        	updateWidget(context, appWidgetId, false);
		}
	}
		
	protected abstract void updateWidget(Context context, int appWidgetId, Boolean loading);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		// Log.d(TAG, "received -> " +  action);
		if  (ACTION_WIDGET_NOTIFY_LOADING.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				updateWidget(context, appWidgetId, intent.getExtras().getBoolean("loading"));
			}			
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else
			super.onReceive(context, intent);
	}

	/**
	 * Will be executed when the widget is removed from the home screen 
	 */
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		
		// if we have no more widgets then stop the service
		if (Preferences.getAllWidgetIds(context).length == 0)
			UpdateService.removeService(context);
		
		// Drop the settings if the widget is deleted
		Preferences.DropSettings(context, appWidgetIds);
	}	
				
	public static void setLoading(Context context, Integer[] appWidgetIds, boolean loading) {
		final Intent intent = new Intent(context, StocksWidget.class);
		intent.setAction(ACTION_WIDGET_NOTIFY_LOADING);
		intent.putExtra("loading", loading);
		for (int appWidgetId : appWidgetIds) {
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			context.sendBroadcast(intent);
		}		
	}
}