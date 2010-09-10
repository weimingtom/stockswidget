package com.paulish.widgets.stocks.receivers;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import com.paulish.widgets.stocks.R;
import com.paulish.widgets.stocks.StocksWidget;

public class StocksWidgetSingle extends StocksWidget {
	
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
		views.setTextViewText(R.id.quoteSymbol, "GOOG");
		views.setTextViewText(R.id.quotePrice, "1024");
		views.setTextViewText(R.id.quoteChangePercent, "+10.20%");
		views.setTextViewText(R.id.quoteChange, "+40.25");
		/* TODO: missing drawables 
		if (quoteChange == null)
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_state_unknown);
		else if (quoteChange == 0)
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_state_zero);
		else if (quoteChange < 0)
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_state_negative);
		else
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_state_positive); */		
	}

}
