package com.paulish.widgets.stocks.receivers;

import java.util.HashMap;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.paulish.widgets.stocks.QuoteViewActivity;
import com.paulish.widgets.stocks.R;
import com.paulish.widgets.stocks.StocksProvider;
import com.paulish.widgets.stocks.StocksWidget;

public class StocksWidgetSingle extends StocksWidget /*implements OnTouchListener*/ {
	
	private class CursorCache {
		private int appWidgetId;
		public int currentIndex = -1;
		public Cursor cursor = null;
		
		public CursorCache(int appWidgetId) {
			this.appWidgetId = appWidgetId;
		}
		
		protected void finalize(){
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		
		public Cursor prepareCursor(Context context) {
			if (cursor == null) {
				// query the data
				Uri quotes = StocksProvider.CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(
						Integer.toString(appWidgetId)).build();
				
				cursor = context.getContentResolver().query(quotes, StocksProvider.PROJECTION_QUOTES, null, null, null);				
			}
			
			if (cursor != null) {
				final int lastIndex = cursor.getCount() - 1;
				if (currentIndex > lastIndex)
					currentIndex = 0;
				if (currentIndex == -1)
					currentIndex = lastIndex;

				if (!cursor.moveToPosition(currentIndex))
					return null;				
			}
			
			return cursor;
		}
	}
	
	private HashMap<Integer, CursorCache> cache;
	
	@Override
	protected void updateWidget(Context context, int appWidgetId, Boolean loading) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocks_widget_single);
        
        updateWidgetData(context, appWidgetId, loading, views);
        final AppWidgetManager awm = AppWidgetManager.getInstance(context);        
        awm.updateAppWidget(appWidgetId, views);		
	}
	
	private void updateWidgetData(Context context, int appWidgetId, Boolean loading, RemoteViews views) {
		if (loading)
			return;
		
		CursorCache cursorCache = null;
		// get the cursor from cache
		if (cache == null)
			cache = new HashMap<Integer, CursorCache>();
		
		cursorCache = cache.get(appWidgetId);
		
		if (cursorCache == null) {
			cursorCache = new CursorCache(appWidgetId);
			cursorCache.currentIndex = 0;
			cache.put(appWidgetId, cursorCache);
		}
		
		Cursor cur = cursorCache.prepareCursor(context);
		if (cur != null) {						
			final String symbol = cur.getString(StocksProvider.QuotesColumns.symbol.ordinal());
			
			views.setTextViewText(R.id.quoteSymbol, symbol);
			views.setTextViewText(R.id.quotePrice, cur.getString(StocksProvider.QuotesColumns.price.ordinal()));
			views.setTextViewText(R.id.quoteChangePercent, cur.getString(StocksProvider.QuotesColumns.pchange.ordinal()));
			views.setTextViewText(R.id.quoteChange, cur.getString(StocksProvider.QuotesColumns.change.ordinal()));
			switch (cur.getInt(StocksProvider.QuotesColumns.stateimage.ordinal())) {
			case R.drawable.stocks_widget_state_red:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_negative);
				break;
			case R.drawable.stocks_widget_state_green:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_positive);
				break;
			default:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_zero);
				break;
			}
			cur.close();

			Intent openForSymbolIntent = QuoteViewActivity.getOpenForSymbolIntent(context, symbol);
	        views.setOnClickPendingIntent(R.id.widgetLayout, 
	        		PendingIntent.getActivity(context, appWidgetId, openForSymbolIntent, PendingIntent.FLAG_UPDATE_CURRENT));
			
		} else {
			// fill with default values 
			views.setTextViewText(R.id.quoteSymbol, "");
			views.setTextViewText(R.id.quotePrice, "0");
			views.setTextViewText(R.id.quoteChangePercent, "0.0%");
			views.setTextViewText(R.id.quoteChange, "0.0");
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_zero);			
		}			
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (cache != null)
			for (int appWidgetId : appWidgetIds)
				cache.remove(appWidgetId);
		
		super.onDeleted(context, appWidgetIds);		
	}

}
