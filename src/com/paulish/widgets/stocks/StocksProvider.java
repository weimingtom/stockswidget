package com.paulish.widgets.stocks;

import java.util.*;
import org.json.*;
import com.paulish.internet.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.net.Uri;
import android.os.AsyncTask;

public class StocksProvider extends ContentProvider {
	public static final String TAG = "paulish.StocksProvider";
	
	private class DatabaseHelper extends SQLiteOpenHelper {		
		public static final String DATABASE_NAME = "stocks.db";
		public static final int DATABASE_VERSION = 1;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL( "CREATE TABLE IF NOT EXISTS quotes (symbol TEXT PRIMARY KEY ON CONFLICT REPLACE, name TEXT, price TEXT, change DOUBLE, pchange TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS quotes");
			onCreate(db);
		}
	}
	
	private static class YahooUpdateTask extends AsyncTask<Integer, Void, Void> {
		private Integer[] appWidgetIds = null;
		
		@Override
		protected Void doInBackground(Integer... appWidgetIds) {
			
			// check that all passed widgets <> 0, else update all the widgets
			boolean isNull = appWidgetIds == null;
			if (!isNull)
				for (Integer appWidgetId : appWidgetIds)
					if (appWidgetId == null) {
						isNull = true;
						break;
					}					
			
			if (isNull) {
				final int[] tmpWidgetIds = Preferences.getAllWidgetIds(ctx);
				this.appWidgetIds = new Integer[tmpWidgetIds.length];
				for (int i = 0; i < tmpWidgetIds.length; i++)
					this.appWidgetIds[i] = tmpWidgetIds[i];
            } else
				this.appWidgetIds = appWidgetIds;
			StocksWidget.setLoading(ctx, this.appWidgetIds, true);
			if (isNull)
				StocksProvider.loadFromYahoo((Integer)null);
			else
				for (int appWidgetId : appWidgetIds) {
					   StocksProvider.loadFromYahoo(appWidgetId);
					}
			return null;
		}
				
		protected void onPostExecute(Void result) {
	        StocksWidget.setLoading(ctx, appWidgetIds, false);
	     }		
	}

	public static final String AUTHORITY = "com.paulish.widgets.stocks.provider";	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri CONTENT_URI_QUOTES = CONTENT_URI.buildUpon().appendEncodedPath("quotes").build();
	public static final Uri CONTENT_URI_WIDGET_QUOTES = CONTENT_URI.buildUpon().appendEncodedPath("widget_quotes").build();
	
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int URI_QUOTES = 0;
	private static final int URI_QUOTE = 1;
	private static final int URI_WIDGET_QUOTES = 2;
		
	public static final String QUOTES_TABLE_NAME = "quotes";
	
	public enum QuotesColumns {
		symbol, name, price, change, pchange, stateimage
	}

	public static final String[] PROJECTION_QUOTES = new String[] {
		QuotesColumns.symbol.toString(),
		QuotesColumns.name.toString(), 
		QuotesColumns.price.toString(), 
		"CASE WHEN change is NULL THEN \"\" WHEN change > 0 THEN \"+\" || change ELSE \"\" || change END as " + QuotesColumns.change.toString(),
		"CASE WHEN pchange is NULL THEN \"\" ELSE pchange END as " + QuotesColumns.pchange.toString(),
		"CASE WHEN change IS NULL THEN " + Integer.toString(R.drawable.stocks_widget_state_black) + 
		    " WHEN change = 0 THEN " + Integer.toString(R.drawable.stocks_widget_state_gray) + 
		    " WHEN change < 0 THEN " + Integer.toString(R.drawable.stocks_widget_state_red) + 
		    " ELSE " + Integer.toString(R.drawable.stocks_widget_state_green) + " END as " + QuotesColumns.stateimage.toString()
	};

	private static Context ctx = null;
	private static DatabaseHelper dbHelper = null;
	private static SQLiteDatabase stocksDB = null;

	static {
		URI_MATCHER.addURI(AUTHORITY, "quotes", URI_QUOTES);
		URI_MATCHER.addURI(AUTHORITY, "quotes/#", URI_QUOTE);
		URI_MATCHER.addURI(AUTHORITY, "widget_quotes/#", URI_WIDGET_QUOTES);
	}

	@Override
	public boolean onCreate() {
		if (ctx == null)
		   ctx = getContext();
		 
		if (dbHelper == null)
		   dbHelper = new DatabaseHelper(ctx);
		
		if (stocksDB == null)
		   stocksDB = dbHelper.getWritableDatabase();
		
	    return (stocksDB == null)? false:true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// Log.d(TAG, "start loading data");
        
		final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

        // Set the table we're querying.
        qBuilder.setTables(QUOTES_TABLE_NAME);

        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
		if ((URI_MATCHER.match(uri)) == URI_WIDGET_QUOTES) {
			final List<String> pathSegs = uri.getPathSegments();
			final int appWId = Integer.parseInt(pathSegs.get(pathSegs.size() - 1));
			final List<String> tickers = Preferences.getPortfolio(ctx, appWId);
			qBuilder.appendWhere("symbol in (" + prepareTickers(tickers) + ")");
			sortOrder = buildSortOrder(tickers);
		} else if ((URI_MATCHER.match(uri)) == URI_QUOTE) {
			final List<String> pathSegs = uri.getPathSegments();
			final String quote = pathSegs.get(pathSegs.size() - 1);
			qBuilder.appendWhere("symbol = \"" + quote.toUpperCase() + "\"");
		}        	
        
        // Log.d(TAG, "sort order = " + sortOrder);
        
        // Make the query.
        Cursor c = qBuilder.query(stocksDB,
                projection,
                selection,
                selectionArgs,
                "",
                "",
                sortOrder);        
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
	
	public static void notifyDatabaseModification(int widgetId) {		
		final Uri widgetUri = CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(Integer.toString(widgetId)).build();
		ctx.getContentResolver().notifyChange(widgetUri, null);
	}
	
	public static void notifyAllWidgetsModification() {
		final int[] appWidgetIds = Preferences.getAllWidgetIds(ctx);
		for (int appWidgetId : appWidgetIds) {
			notifyDatabaseModification(appWidgetId);
		}
	}
	
	private static String prepareTickers(List<String> tickers) {
		final StringBuffer result = new StringBuffer();
		final int size = tickers.size(); 
	    if (size > 0) {
	    	result.append("\"");
	        result.append(tickers.get(0).toUpperCase());
	    	result.append("\"");
	        for (int i = 1; i < size; i++) {
		    	result.append(",\"");
	            result.append(tickers.get(i).toUpperCase());
		    	result.append("\"");
	        }
	    }
	    return result.toString();
	}
	
	private static String buildSortOrder(List<String> tickers) {
		final StringBuffer result = new StringBuffer();
		final int size = tickers.size(); 
	    if (size > 1) {
	    	result.append("CASE symbol");
	    	for (int i = 0; i < size; i++) {
	    		result.append(" WHEN \"");
	    		result.append(tickers.get(i).toUpperCase());
	    		result.append("\" THEN ");
	    		result.append(Integer.toString(i));
	    	}
	    	result.append(" END");
	    }		
		return result.toString();
	}
	
	public static void loadFromYahoo(Integer appWidgetId) {
		if (appWidgetId == null) {
			final List<String> tickers = Preferences.getAllPortfolios(ctx);
			loadFromYahoo(tickers);
		    notifyAllWidgetsModification();
		}
		else {
			final List<String> tickers = Preferences.getPortfolio(ctx, appWidgetId);
			loadFromYahoo(tickers);
			notifyDatabaseModification(appWidgetId);
		}
	}
	
	public static void loadFromYahooInBackgroud(Integer appWidgetId) {
		final YahooUpdateTask yahooUpdateTask = new YahooUpdateTask();
        yahooUpdateTask.execute(appWidgetId); 
	}
	
	// helper for loadFromYahoo
	private static void setValuesFromJSONObject(ContentValues values, JSONObject jo) {
		values.clear();
		try {
			values.put(QuotesColumns.symbol.toString(), jo.getString("Symbol"));
			if (!jo.isNull("Name"))
				values.put(QuotesColumns.name.toString(), jo.getString("Name"));
			if (!jo.isNull("LastTradePriceOnly"))
				values.put(QuotesColumns.price.toString(), jo.getString("LastTradePriceOnly"));
			// percent change is N/A when change is null
			if (!jo.isNull("Change")) {
				values.put(QuotesColumns.change.toString(), jo.getDouble("Change"));
				values.put(QuotesColumns.pchange.toString(), jo.getString("PercentChange"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private static void loadFromYahoo(List<String> tickers) {
		
		final ContentValues values = new ContentValues();
		
		final RestClient client = new RestClient("http://query.yahooapis.com/v1/public/yql");
        client.AddParam("q", "select Symbol, Name, LastTradePriceOnly, Change, PercentChange from yahoo.finance.quotes where symbol in (" + prepareTickers(tickers) + ")");
        client.AddParam("format", "json");
        client.AddParam("env", "http://datatables.org/alltables.env");
        client.AddParam("callback", "");
         
        try {
            client.Execute(RequestMethod.POST);
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        final String response = client.getResponse();
        if (response == null)
        	return;
        //Log.d(TAG, "... response: " + response);
               
        try {
			JSONObject jo = new JSONObject(response).getJSONObject("query").getJSONObject("results");
			// we can get either an array of quotes or just one quote
			final JSONArray ja = jo.optJSONArray("quote");			
			if (ja != null) {
				for (int i = 0; i < ja.length(); i++) {
					jo = ja.getJSONObject(i);
					setValuesFromJSONObject(values, jo);
					stocksDB.insert(QUOTES_TABLE_NAME, null, values);
				}
			} else
			{
				jo = jo.optJSONObject("quote");
				setValuesFromJSONObject(values, jo);
				stocksDB.insert(QUOTES_TABLE_NAME, null, values);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}				
	}	
}