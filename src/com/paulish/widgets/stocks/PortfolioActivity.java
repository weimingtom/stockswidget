package com.paulish.widgets.stocks;

import java.util.List;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class PortfolioActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	public final static String TAG_SKIP_UPDATE = "SkipUpdate";
	private final static int requestSymbolSearch = 1;

	private List<String> tickers;
	private ArrayAdapter<String> adapter;
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean skipUpdate = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setTitle(R.string.editPortfolio);
		setContentView(R.layout.stocks_widget_portfolio_edit);
		findViewById(R.id.save).setOnClickListener(this);
		Button btn = (Button)findViewById(R.id.cancel);
		btn.setText(android.R.string.cancel);
		btn.setOnClickListener(this);			
		
		final ListView tickersList = (ListView)findViewById(R.id.tickersList);
		registerForContextMenu(tickersList);		
		tickersList.setOnItemClickListener(this);
		
		// prepare the listview
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
			if (extras.containsKey(TAG_SKIP_UPDATE))
				skipUpdate = extras.getBoolean(TAG_SKIP_UPDATE);
			tickers = Preferences.getPortfolio(this, appWidgetId);
			adapter = new ArrayAdapter<String>(this, R.layout.stocks_widget_portfolio_edit_list_item, tickers);
			adapter.add(getString(R.string.addTickerSymbol));
			tickersList.setAdapter(adapter);
		} else
			finish();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel: 
			finish(); 
			break;
		case R.id.save:
			savePreferences();
			Intent resultValue = new Intent();                    
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            if (!skipUpdate)
            	StocksProvider.loadFromYahooInBackgroud(this, appWidgetId);
            finish();            
			break;
		}						
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.tickersList) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			
			if (info.position != tickers.size() - 1) {			
				menu.setHeaderTitle(tickers.get(info.position));
				menu.add(Menu.NONE, 0, 0, R.string.openTickerSymbol);
				menu.add(Menu.NONE, 1, 1, R.string.editTickerSymbol);
				menu.add(Menu.NONE, 2, 2, R.string.deleteTickerSymbol);
				if (info.position > 0)
					menu.add(Menu.NONE, 3, 3, R.string.moveUp);
				if (info.position < tickers.size() - 2)
					menu.add(Menu.NONE, 4, 4, R.string.moveDown);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final int menuItemIndex = item.getItemId();
		final int position = info.position;
		switch (menuItemIndex) {
		case 0:
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
			break;
		case 1:
			editSymbol(position);
			break;
		case 2:
			tickers.remove(position);
			adapter.notifyDataSetChanged();
			break;
		case 3:
			if (position > 0) {
				final String curValue = tickers.get(position);
				tickers.set(position, tickers.get(position - 1));
				tickers.set(position - 1, curValue);
				adapter.notifyDataSetChanged();
			}
			break;
		case 4:
			if (position < tickers.size() - 2) {
				final String curValue = tickers.get(position);
				tickers.set(position, tickers.get(position + 1));
				tickers.set(position + 1, curValue);
				adapter.notifyDataSetChanged();
			}
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == tickers.size() - 1)
			editSymbol(-1);
		else
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == requestSymbolSearch) {
			if (resultCode == RESULT_OK) {
				final int position = data.getIntExtra(SymbolSearchActivity.TAG_POSITION, -1);
				final String symbol = data.getStringExtra(SymbolSearchActivity.TAG_SYMBOL);
				if (position == -1)
					adapter.insert(symbol, tickers.size() - 1);
				else 
					tickers.set(position, symbol);
				adapter.notifyDataSetChanged();
			}			
		} else
			super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void editSymbol(final int position) {
		Intent search = new Intent(this, SymbolSearchActivity.class);
		search.setAction(Intent.ACTION_EDIT);
		search.putExtra(SymbolSearchActivity.TAG_POSITION, position);
		if (position != -1)
			search.putExtra(SymbolSearchActivity.TAG_SYMBOL, tickers.get(position));
		startActivityForResult(search, requestSymbolSearch);
	}
		
	private void savePreferences() {
		StringBuffer result = new StringBuffer();
		final int count = tickers.size();
		if (count > 1) {
			result.append(tickers.get(0));
			for (int i = 1; i < count - 1; i++) {
				result.append(",");
				result.append(tickers.get(i));
			}
		}
		Preferences.setPortfolio(this, appWidgetId, result.toString());
	}
}
