package com.paulish.widgets.stocks;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;

public class UpdateService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			StocksProvider.loadFromYahooInBackgroud(null);
		else
			StocksProvider.loadFromYahooInBackgroud(appWidgetId);
		
		stopSelf(startId);
	    
	    return START_STICKY;
	}
}
