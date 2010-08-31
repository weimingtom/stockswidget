package com.paulish.widgets.stocks;

import mobi.intuitit.android.content.LauncherIntent;
import android.app.PendingIntent;
import android.appwidget.*;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;


public class StocksWidget extends AppWidgetProvider {
	// Tag for logging
	private static final String TAG = "paulish.StocksWidget";
	// Actions
	public static String ACTION_WIDGET_REFRESH = "refresh";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// If no specific widgets requested, collect list of all		
		
		if (appWidgetIds == null) {
			appWidgetIds = Preferences.getAllWidgetIds(context);
		}
        
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            // Construct views
        	int appWidgetId = appWidgetIds[i];
        	updateWidget(context, appWidgetId);           
        }	
	}
	
	protected void updateWidget(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocks_widget);
        
        Intent refreshIntent = new Intent(context, StocksWidget.class);
        refreshIntent.setAction(ACTION_WIDGET_REFRESH);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
        
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        awm.updateAppWidget(appWidgetId, views); 
	}
		
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "recieved -> " +  action);
		if (ACTION_WIDGET_REFRESH.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				DataProvider.notifyDatabaseModification(appWidgetId);
			}
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			// Receive ready signal
			Log.d(TAG, "widget ready");
			onAppWidgetReady(context, intent);			
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {

		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_ITEM_CLICK)) {
			// onItemClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			// onClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			// An error occurred
		    Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE));
		} else
			super.onReceive(context, intent);
	}

	/**
	 * Will be executed when the widget is removed from the homescreen 
	 */
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		// Drop the settings if the widget is deleted
		Preferences.DropSettings(context, appWidgetIds);
	}	
	
	/**
	 * On click of a child view in an item
	 */
	private void onClick(Context context, Intent intent) {
		// open quote view activity
		Intent quoteViewIntent = new Intent(context, QuoteViewActivity.class);
		quoteViewIntent.putExtra(QuoteViewActivity.EXTRA_QUOTE_SYMBOL, intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS));
		quoteViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(quoteViewIntent);
	}
		
	/**
	 * Receive ready intent from Launcher, prepare scroll view resources
	 */
	public void onAppWidgetReady(Context context, Intent intent) {
		if (intent == null)
			return;

		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);

		if (appWidgetId < 0) {
			return;
		}
		updateWidget(context, appWidgetId);
		Intent replaceDummy = CreateMakeScrollableIntent(context, appWidgetId);

		// Send it out
		context.sendBroadcast(replaceDummy);
	}
	
	/**
	 * Constructs a Intent that tells the launcher to replace the dummy with the ListView
	 */
	public Intent CreateMakeScrollableIntent(Context context, int appWidgetId) {
		Log.d(TAG, "creating ACTION_SCROLL_WIDGET_START intent");
		Intent result = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

		// Put widget info
		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		result.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.content_view);

		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, false);

		// Give a layout resource to be inflated. If this is not given, the launcher will create one		
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.stocks_widget_list);
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.stocks_widget_list_item);
		
		putProvider(result, DataProvider.CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(
				Integer.toString(appWidgetId)).toString());
		putMapping(context, appWidgetId, result);

		// Launcher can set onClickListener for each children of an item. Without
		// explicitly put this
		// extra, it will just set onItemClickListener by default
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
		return result;
	}
			
	/**
	 * Put provider info as extras in the specified intent
	 * 
	 * @param intent
	 */
	protected void putProvider(Intent intent, String widgetUri) {
		if (intent == null)
			return;

		String whereClause = null;
		String orderBy = null;
		String[] selectionArgs = null;

		// Put the data uri in as a string. Do not use setData, Home++ does not
		// have a filter for that
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);

		// Other arguments for managed query
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, DataProvider.PROJECTION_APPWIDGETS);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);

	}

	/**
	 * Put mapping info as extras in intent
	 */
	protected void putMapping(Context context, int appWidgetId, Intent intent) {
		if (intent == null)
			return;

		int NB_ITEMS_TO_FILL = 6;
		int[] cursorIndices = new int[NB_ITEMS_TO_FILL];
		int[] viewTypes = new int[NB_ITEMS_TO_FILL];
		int[] layoutIds = new int[NB_ITEMS_TO_FILL];
		boolean[] clickable = new boolean[NB_ITEMS_TO_FILL];
		int[] defResources = new int[NB_ITEMS_TO_FILL];

		int iItem = 0;
		
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, 
				DataProvider.DataProviderColumns.symbol.ordinal());
		
		cursorIndices[iItem] = DataProvider.DataProviderColumns.symbol.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteSymbol;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = DataProvider.DataProviderColumns.name.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteName;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = DataProvider.DataProviderColumns.lastTradePrice.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quotePrice;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;

		cursorIndices[iItem] = DataProvider.DataProviderColumns.change.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteChange;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = DataProvider.DataProviderColumns.percentChange.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteChangePercent;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;

		cursorIndices[iItem] = DataProvider.DataProviderColumns.stateImage.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.IMAGERESOURCE;
		layoutIds[iItem] = R.id.stateImage;
		clickable[iItem] = true;
		defResources[iItem] = 0;			

		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defResources);
	}
    
}