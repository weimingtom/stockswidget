package com.paulish.widgets.stocks;

import java.util.List;

import org.json.*;
import com.paulish.internet.*;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class DataProvider extends ContentProvider {
	public static final String TAG = "paulish.DataProvider";
	
	public static final String AUTHORITY = "com.paulish.widgets.stocks.provider";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_MESSAGES = CONTENT_URI.buildUpon().appendEncodedPath("data").build();
	
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int URI_DATA = 0;
	
	public enum DataProviderColumns {
		symbol, name, lastTradePrice, change, percentChange, stateImage
	}

	public static final String[] PROJECTION_APPWIDGETS = new String[] { 
		DataProviderColumns.symbol.toString(),
		DataProviderColumns.name.toString(), 
		DataProviderColumns.lastTradePrice.toString(), 
		DataProviderColumns.change.toString(),
		DataProviderColumns.percentChange.toString(),
		DataProviderColumns.stateImage.toString()
	};

	private static Context ctx = null;

	static {
		URI_MATCHER.addURI(AUTHORITY, "data/*", URI_DATA);
	}

	@Override
	public boolean onCreate() {
		if (ctx == null)
		  ctx = getContext();
		return false;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.d(TAG, "in delete");
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		Log.d(TAG, "in getType");
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, "in insert");
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		int match = URI_MATCHER.match(uri);		
		switch (match) {
			case URI_DATA:
				List<String> pathSegs = uri.getPathSegments();
				int appWId = Integer.parseInt(pathSegs.get(pathSegs.size() - 1));
				String[] tickers = Preferences.getTickers(ctx, appWId);
				return loadNewData(this, projection, tickers);
			default:
				throw new IllegalStateException("Unrecognized URI:" + uri);
		}
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Log.d(TAG, "in update");
		return 0;
	}
	
	public static void notifyDatabaseModification(int widgetId) {		
		Uri widgetUri = CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(Integer.toString(widgetId)).build();
		Log.d(TAG, "notifyDatabaseModification -> UPDATE widgetUri : " + widgetUri);
		ctx.getContentResolver().notifyChange(widgetUri, null);
	}
	
	public static int getStateImage(Double change) {
		if (change == null)
			return R.drawable.stocks_widget_state_black;
		else
		if (change < 0)
			return R.drawable.stocks_widget_state_red;
		else
		if (change > 0)
			return R.drawable.stocks_widget_state_green;
		else
			return R.drawable.stocks_widget_state_gray;
	}
	
	private static String prepareTickers(String[] tickers) {
		StringBuffer result = new StringBuffer();
	    if (tickers.length > 0) {
	    	result.append("\"");
	        result.append(tickers[0]);
	    	result.append("\"");
	        for (int i = 1; i < tickers.length; i++) {
		    	result.append(",\"");
	            result.append(tickers[i]);
		    	result.append("\"");
	        }
	    }
	    return result.toString();
	}
	
	public static MatrixCursor loadNewData(ContentProvider mcp, String[] projection, String[] tickers) {
		MatrixCursor ret = new MatrixCursor(projection);
		
		Log.d(TAG, "start loading data");
        
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
				Object[] values = new Object[projection.length];
				values[0] = jo.getString("Symbol");
				values[1] = jo.getString("Name");		
				values[2] = jo.getString("LastTradePriceOnly");
				values[3] = jo.getString("Change");
				values[4] = jo.getString("PercentChange");
				if (jo.isNull("Change"))
				  values[5] = getStateImage(null);
				else
				  values[5] = getStateImage(jo.getDouble("Change"));
				
				ret.addRow(values);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		
        Log.d(TAG, "end loading data");
		return ret;
	}
}