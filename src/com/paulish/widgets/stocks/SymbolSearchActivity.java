package com.paulish.widgets.stocks;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SymbolSearchActivity extends ListActivity {
	
	public final static String TAG_POSITION = "position";
	public final static String TAG_SYMBOL = "symbol";

	private int position = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stocks_widget_search);
		
		onNewIntent(getIntent());
	}
	
    @Override
    public void onNewIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            // Start query for incoming search request
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null)
            	query = intent.getDataString();
            // search the query
            Cursor cur = getContentResolver().query(StocksSearchProvider.CONTENT_URI.buildUpon().appendEncodedPath(SearchManager.SUGGEST_URI_PATH_QUERY).appendEncodedPath(query).build(), null, null, null, null);
            startManagingCursor(cur);

            ListAdapter adapter = new SimpleCursorAdapter(
                    this, // Context.
                    android.R.layout.two_line_list_item,
                    cur,                                              	  
                    new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2},
                    new int[] {android.R.id.text1, android.R.id.text2});

            setListAdapter(adapter);            

        } else if (Intent.ACTION_VIEW.equals(action)) {
        	position = intent.getIntExtra(TAG_POSITION, -1);
        	final String ticker = intent.getStringExtra(TAG_SYMBOL);
        	startSearch(ticker, false, null, false);
        } 
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	final String symbol = ((CursorWrapper)getListView().getItemAtPosition(position)).getString(1);
		Intent resultValue = new Intent();                    
        resultValue.putExtra(TAG_POSITION, this.position);
        resultValue.putExtra(TAG_SYMBOL, symbol);
        setResult(RESULT_OK, resultValue);
        finish();
    }

}
