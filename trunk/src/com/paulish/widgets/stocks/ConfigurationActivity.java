package com.paulish.widgets.stocks;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

public class ConfigurationActivity extends PreferenceActivity {

    
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		// Build GUI from resource
		addPreferencesFromResource(R.xml.preferences);
		
		// Get the starting Intent
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            // Cancel by default
            Intent cancelResultValue = new Intent();
            cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, cancelResultValue);
        } else {
            finish();
        }
        // prepare the GUI components
        prepareTickers();		
		prepareSaveBtn();
	}

	private void prepareTickers() {
		// Find control and set the right preference-key for the AppWidgetId
		EditTextPreference editTickers = (EditTextPreference)findPreference(Preferences.TICKERS);
		editTickers.setKey(Preferences.get(Preferences.TICKERS, appWidgetId));		
		// Set summary on value changed
		editTickers.setOnPreferenceChangeListener(new SetCurValue());
	}	
		
	private void prepareSaveBtn() {
		Preference pref = findPreference("SAVE");
		// Bind the "onClick" for the save preferences to close the activity
		// and postback "OK"
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Intent resultValue = new Intent();                    
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
                return false;
			}
		});		
	}

	// OnPreferenceChangeListener to set the summary of the preference
	// to the display text of the new value 
	private class SetCurValue implements OnPreferenceChangeListener {	
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			preference.setSummary(newValue.toString());
			return true;
		}
	}
}