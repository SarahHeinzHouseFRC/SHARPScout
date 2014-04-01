package org.sharp.scouting;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;
import org.frc3260.database.DB;
import org.frc3260.database.DBSyncService;
import org.frc3260.database.DBSyncService.LocalBinder;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;

import java.util.List;

public class Prefs extends PreferenceActivity
{
	public static final int PREFS_ACTIVITY_CODE = 64738;

	private EditTextPreference passP;

	private EditTextPreference urlP;

	private EventList eventList;

	private ListPreference eventP;

	private CheckBoxPreference syncPreference;

	private static final String URL = "http://www.sharpscouter.com/app/scouting.php";

	private LocalBinder binder;
	private ServiceWatcher watcher = new ServiceWatcher();

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.mainprefs);

		passP = (EditTextPreference) findPreference("passPref");
		urlP = (EditTextPreference) findPreference("databaseURLPref");

		passP.setOnPreferenceChangeListener(new onPassChangeListener(true));
		urlP.setOnPreferenceChangeListener(new onPassChangeListener(false));

		eventP = (ListPreference) findPreference("eventPref");

		syncPreference = (CheckBoxPreference) findPreference("enableSyncPref");

		syncPreference.setOnPreferenceChangeListener(new OnSyncChangeListener());

		findPreference("syncFreqPref").setEnabled(getAutoSync(getApplicationContext(), false));

		Preference refreshEventsButton = findPreference("refreshEventsButton");

		refreshEventsButton.setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						refreshEvents();

						return true;
					}
				});

		Preference deleteSQLiteDBButton = findPreference("deleteSQLiteButton");

		deleteSQLiteDBButton.setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						deleteSQLiteDatabase();

						return true;
					}
				});

		eventList = new EventList(getApplicationContext(), null);

		List<String> events = eventList.getEventList();

		if(events != null)
		{
			updateEventPreference(events);
		}

		if(events.isEmpty())
		{
			refreshEvents();
		}

		Intent intent = new Intent(getApplicationContext(), DBSyncService.class);
		ComponentName myService = startService(intent);
		bindService(intent, watcher, BIND_AUTO_CREATE);
	}

	private void deleteSQLiteDatabase()
	{
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				switch(which)
				{
					case DialogInterface.BUTTON_POSITIVE:
						boolean result = deleteDatabase("FRCscouting.db");

						if(result)
						{
							Toast.makeText(getBaseContext(), "Database Deleted", Toast.LENGTH_LONG).show();
						}
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						return;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you would like to delete the database?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
	}

	protected class ServiceWatcher implements ServiceConnection
	{
		boolean serviceRegistered = false;

		public void onServiceConnected(ComponentName name, IBinder service)
		{
			if(service instanceof LocalBinder)
			{
				binder = (LocalBinder) service;
			}
		}

		public void onServiceDisconnected(ComponentName name)
		{
		}

	}

	private void updateEventPreference(List<String> events)
	{
		if(!events.isEmpty())// || eventP.getEntries()==null)
		{
			String[] entries = events.toArray(new String[0]);
			eventP.setEntries(entries);
			eventP.setEntryValues(entries);
		}
	}

	private class onPassChangeListener implements OnPreferenceChangeListener
	{

		private boolean isPass = true;

		public onPassChangeListener(boolean pass)
		{
			isPass = pass;
		}

		public boolean onPreferenceChange(Preference preference, Object newValue)
		{

			DB db = new DB(getBaseContext(), null); // does not perform databse
			// sync operations
			db.checkPass(newValue.toString(), new PasswordCallback(isPass));
			return true;
		}

	}

	private void refreshEvents()
	{
		eventList.downloadEventsList(new EventListCallback());
	}

	protected class EventListCallback implements EventList.EventCallback
	{
		public void eventsUpdated(List<String> events)
		{
			if(events == null)
			{
				Toast.makeText(getBaseContext(), "Error Updating Event List.", Toast.LENGTH_SHORT).show();

				return;
			}

			Toast.makeText(getBaseContext(), "Updated Event List", Toast.LENGTH_SHORT).show();

			updateEventPreference(events);
		}

	}

	protected class PasswordCallback implements HttpCallback
	{
		private boolean isPass = true;

		public PasswordCallback(boolean pass)
		{
			isPass = pass;
		}

		public void onResponse(HttpRequestInfo resp)
		{
			Toast toast;
			try
			{
				if(resp.getResponseString().contains("success"))
				{
					toast = Toast.makeText(getBaseContext(), "Password Confirmed", Toast.LENGTH_SHORT);
					if(binder != null)
					{
						binder.initSync();
					}
				}
				else
				{
					toast = Toast.makeText(getBaseContext(), "Invalid Password", Toast.LENGTH_SHORT);
				}
			} catch(Exception e)
			{
				toast = Toast.makeText(getBaseContext(), "Invalid Password", Toast.LENGTH_SHORT);
			}
			if(isPass)
			{
				toast.show();
			}
		}

		public void onError(Exception e)
		{
			Toast.makeText(getBaseContext(), "Unable to Connect to Server", Toast.LENGTH_SHORT).show();
		}
	}

	private class OnSyncChangeListener implements OnPreferenceChangeListener
	{
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			if(!(newValue instanceof Boolean))
			{
				return false;
			}

			Boolean checked = (Boolean) newValue;

			findPreference("syncFreqPref").setEnabled(checked);
			return true;
		}
	}

	public static String getSavedPassword(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString("passPref", "");
	}

	public static String getScoutingURL(Context context)
	{
		String ret = PreferenceManager.getDefaultSharedPreferences(context).getString("databaseURLPref", URL);
		if(ret.length() > 0 && !ret.contains("://"))
		{
			ret = "http://" + ret;
		}
		return ret;
	}

	public static String getEvent(Context context, String defaultValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString("eventPref", defaultValue);
	}

	public static boolean getAutoSync(Context context, boolean defaultValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enableSyncPref", defaultValue);
	}

	public static int getMilliSecondsBetweenSyncs(Context context, final int defaultValue)
	{
		String val = PreferenceManager.getDefaultSharedPreferences(context).getString("syncFreqPref", "");

		int secs;

		if(val == null || val.length() == 0)
		{
			return defaultValue;
		}

		try
		{
			secs = Integer.valueOf(val.split(" ")[0]) * 60 * 1000;
		} catch(Exception e)
		{
			return defaultValue;
		}
		return secs;
	}

	public static String getPosition(Context context, String defaultValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString("alliancePref", defaultValue);
	}
}
