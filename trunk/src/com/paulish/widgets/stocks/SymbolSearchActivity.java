package com.paulish.widgets.stocks;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

public class SymbolSearchActivity extends ListActivity {
	
	int position = -1;
	
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
            // search the query

        } else if (Intent.ACTION_VIEW.equals(action)) {
        	position = intent.getIntExtra("position", -1);
        	final String ticker = intent.getStringExtra("ticker");
        	startSearch(ticker, false, null, false);
        } 
    }

}
