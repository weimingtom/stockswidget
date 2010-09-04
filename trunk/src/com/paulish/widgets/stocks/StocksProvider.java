package com.paulish.widgets.stocks;

import java.util.List;

import org.json.*;
import com.paulish.internet.*;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
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
			String sql = "CREATE TABLE IF NOT EXISTS quotes (symbol TEXT PRIMARY KEY ON CONFLICT REPLACE, name TEXT, price TEXT, change DOUBLE, pchange TEXT);";
			db.execSQL(sql);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS quotes");
			onCreate(db);
		}
	}
	
	private static class YahooUpdateTask extends AsyncTask<Integer, Void, Void> {
		@Override
		protected Void doInBackground(Integer... params) {
			StocksProvider.loadFromYahoo(params[0]);
			return null;
		}	
	}

	public static final String AUTHORITY = "com.paulish.widgets.stocks.provider";	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_MESSAGES = CONTENT_URI.buildUpon().appendEncodedPath("quotes").build();
	
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int URI_QUOTES = 0;
	private static final int URI_QUOTE = 0;
	
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
        
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

        // Set the table we're querying.
        qBuilder.setTables(QUOTES_TABLE_NAME);

        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        if ((URI_MATCHER.match(uri)) == URI_QUOTE) {
        	List<String> pathSegs = uri.getPathSegments();
			int appWId = Integer.parseInt(pathSegs.get(pathSegs.size() - 1));
			List<String> tickers = Preferences.getTickers(ctx, appWId);
			qBuilder.appendWhere("symbol in (" + prepareTickers(tickers) + ")");
			sortOrder = buildSortOrder(tickers);
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
		Uri widgetUri = CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(Integer.toString(widgetId)).build();
		ctx.getContentResolver().notifyChange(widgetUri, null);
	}
	
	public static void notifyAllWidgetsModification() {
		int[] appWidgetIds = Preferences.getAllWidgetIds(ctx);
		for (int appWidgetId : appWidgetIds) {
			notifyDatabaseModification(appWidgetId);
		}
	}
	
	private static String prepareTickers(List<String> tickers) {
		StringBuffer result = new StringBuffer();
		final int size = tickers.size(); 
	    if (size > 0) {
	    	result.append("\"");
	        result.append(tickers.get(0));
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
		StringBuffer result = new StringBuffer();
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
			List<String> tickers = Preferences.getAllTickers(ctx);
			loadFromYahoo(tickers);
		    notifyAllWidgetsModification();
		}
		else {
			List<String> tickers = Preferences.getTickers(ctx, appWidgetId);
			loadFromYahoo(tickers);
			notifyDatabaseModification(appWidgetId);
		}
	}
	
	public static void loadFromYahooInBackgroud(Integer appWidgetId) {
		final YahooUpdateTask yahooUpdateTask = new YahooUpdateTask();
        yahooUpdateTask.execute(appWidgetId); 
	}
	
	private static void loadFromYahoo(List<String> tickers) {
		
		ContentValues values = new ContentValues();
		
		RestClient client = new RestClient("http://query.yahooapis.com/v1/public/yql");
        client.AddParam("q", "select Symbol, Name, LastTradePriceOnly, Change, PercentChange from yahoo.finance.quotes where symbol in (" + prepareTickers(tickers) + ")");
        client.AddParam("format", "json");
        client.AddParam("env", "http://datatables.org/alltables.env");
        client.AddParam("callback", "");
         
        try {
            client.Execute(RequestMethod.POST);
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        String response = client.getResponse();
        //Log.d(TAG, "... response: " + response);
        try {
			JSONObject jo = new JSONObject(response);
			JSONArray ja = jo.getJSONObject("query").getJSONObject("results").getJSONArray("quote");
			for (int i = 0; i < ja.length(); i++) {
				jo = ja.getJSONObject(i);
				values.clear();
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
				stocksDB.insert(QUOTES_TABLE_NAME, null, values);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}				
	}	
}