package com.paulish.widgets.stocks.receivers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View.OnTouchListener;
import android.widget.RemoteViews;

import com.paulish.widgets.stocks.QuoteViewActivity;
import com.paulish.widgets.stocks.R;
import com.paulish.widgets.stocks.StocksProvider;
import com.paulish.widgets.stocks.StocksWidget;

public class StocksWidgetSingle extends StocksWidget /*implements OnTouchListener*/ {
	
	private int currentIndex = 0;
	
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
		
		// query the data
		Uri quotes = StocksProvider.CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(
				Integer.toString(appWidgetId)).build();
		
		Cursor cur = context.getContentResolver().query(quotes, StocksProvider.PROJECTION_QUOTES, null, null, null);
		
		if (cur != null) {
			final int lastIndex = cur.getCount() - 1;
			if (currentIndex > lastIndex)
				currentIndex = 0;
			if (currentIndex == -1)
				currentIndex = lastIndex;

			if (!cur.moveToPosition(currentIndex))
				return;
						
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

}
