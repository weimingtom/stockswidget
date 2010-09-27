package com.paulish.widgets.stocks;

import android.app.Activity;
import android.os.Bundle;

public class SymbolSearchActivity extends Activity {
	
	private static final String SEARCH_URL = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=%s&callback=YAHOO.Finance.SymbolSuggest.ssCallback";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
	}

}
