package com.paulish.widgets.stocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;

public class ConfigurationActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	private List<String> tickers;
	private ArrayAdapter<String> adapter;
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setTitle(R.string.editTickers);
		setContentView(R.layout.stocks_widget_tickers_edit);
		findViewById(R.id.add).setOnClickListener(this);
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
			tickers = new ArrayList<String>(Arrays.asList(Preferences.getTickers(this, appWidgetId)));
			adapter = new ArrayAdapter<String>(this, R.layout.stocks_widget_tickers_edit_list_item, tickers);
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
            DataProvider.notifyDatabaseModification(appWidgetId);
            finish();            
			break;
		case R.id.add:
			addSymbol();
			break;
		}						
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.tickersList) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle(adapter.getItem(info.position));
			menu.add(Menu.NONE, 0, 0, R.string.openTickerSymbol);
			menu.add(Menu.NONE, 1, 1, R.string.deleteTickerSymbol);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final int menuItemIndex = item.getItemId();
		switch (menuItemIndex) {
		case 0:
			QuoteViewActivity.openForSymbol(this, adapter.getItem(info.position));
			break;
		case 1:
			adapter.remove(adapter.getItem(info.position));
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		QuoteViewActivity.openForSymbol(this, adapter.getItem(position));
	}
	
	private void addSymbol() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.addTickerSymbol);
		alert.setMessage("Add ticker symbol");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						adapter.add(input.getText().toString());
					}
				});

		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();
	}
	
	private void savePreferences() {
		StringBuffer tickers = new StringBuffer();
		final int count = adapter.getCount();
		if (count > 0) {
			tickers.append(adapter.getItem(0));
			for (int i = 1; i < count; i++) {
				tickers.append(",");
				tickers.append(adapter.getItem(i));
			}
		}
		Preferences.setTickers(this, appWidgetId, tickers.toString());
	}
}
