package org.frc3260.aerialassist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import org.frc3260.database.DB;
import org.frc3260.database.DBSyncService;
import org.frc3260.database.DBSyncService.LocalBinder;
import org.sharp.scouting.MainMenuSelection;
import org.sharp.scouting.Prefs;
import org.sharp.scouting.R;

import java.util.ArrayList;
import java.util.List;

public class MatchActivity extends Activity implements OnItemSelectedListener
{
	private static final int CANCEL_DIALOG = 0;

	private String HELPMESSAGE;

	private DB submitter;

	private MatchStatsAA team1Data;
	private MatchStatsAA team2Data;
	private MatchStatsAA team3Data;

	private EditText teamText1;
	private EditText teamText2;
	private EditText teamText3;

	private int currentCycle;

	private EditText matchT;

	private Button submitB;

	private EditText team1Notes;
	private EditText team2Notes;
	private EditText team3Notes;

	private Spinner team1CommonNotes;
	private Spinner team2CommonNotes;
	private Spinner team3CommonNotes;

	private TabHost matchTabHost;

	private LocalBinder binder;

	private ServiceWatcher watcher = new ServiceWatcher();

	private int prevTab;

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.match);

		Intent sync = new Intent(this, DBSyncService.class);
		bindService(sync, watcher, Context.BIND_AUTO_CREATE);

		submitB = (Button) findViewById(R.id.submitB);

		HELPMESSAGE = "Record the match data on these screens.\n"
				+ "Record data for the 3 autonomous balls on the first screen, then separate data for each cycle of the match.\n\n"
				+ "AH = Auto High\n" + "AL = Auto Low\n"
				+ "Yellow Text = Hot Goal\n";

		prevTab = 0;

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		matchTabHost = (TabHost) findViewById(R.id.tabHost);

		matchTabHost.setup();

		TabHost.TabSpec autonTab = matchTabHost.newTabSpec("autonomous");
		autonTab.setContent(R.id.autonomous);
		autonTab.setIndicator("AUTONOMOUS");

		TabHost.TabSpec teleopTab = matchTabHost.newTabSpec("teleop");
		teleopTab.setContent(R.id.teleop);
		teleopTab.setIndicator("TELEOP");

		TabHost.TabSpec notesTab = matchTabHost.newTabSpec("notes");
		notesTab.setContent(R.id.notes);
		notesTab.setIndicator("NOTES");

		matchTabHost.addTab(autonTab);
		matchTabHost.addTab(teleopTab);
		matchTabHost.addTab(notesTab);

		matchTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener()
		{
			@Override
			public void onTabChanged(String tabId)
			{
				if(tabId.equalsIgnoreCase("autonomous"))
				{
					saveCycle();
					saveEnd();
				}

				if(tabId.equalsIgnoreCase("teleop"))
				{
					saveAuto();
					saveEnd();
				}

				if(tabId.equalsIgnoreCase("notes"))
				{
					saveAuto();
					saveCycle();
				}
			}
		});

		teamText1 = (EditText) findViewById(R.id.team1T);
		teamText2 = (EditText) findViewById(R.id.team2T);
		teamText3 = (EditText) findViewById(R.id.team3T);

		team1Notes = (EditText) findViewById(R.id.team1notes);
		team2Notes = (EditText) findViewById(R.id.team2notes);
		team3Notes = (EditText) findViewById(R.id.team3notes);

		team1CommonNotes = (Spinner) findViewById(R.id.team1common_notes);
		team2CommonNotes = (Spinner) findViewById(R.id.team2common_notes);
		team3CommonNotes = (Spinner) findViewById(R.id.team3common_notes);

		team1CommonNotes.setOnItemSelectedListener(this);
		team2CommonNotes.setOnItemSelectedListener(this);
		team3CommonNotes.setOnItemSelectedListener(this);

		matchT = (EditText) findViewById(R.id.matchT);

		LinearLayout[] autoLayouts = new LinearLayout[9];

		autoLayouts[0] = (LinearLayout) findViewById(R.id.team1Ball1Box);
		autoLayouts[1] = (LinearLayout) findViewById(R.id.team1Ball2Box);
		autoLayouts[2] = (LinearLayout) findViewById(R.id.team1Ball3Box);
		autoLayouts[3] = (LinearLayout) findViewById(R.id.team2Ball1Box);
		autoLayouts[4] = (LinearLayout) findViewById(R.id.team2Ball2Box);
		autoLayouts[5] = (LinearLayout) findViewById(R.id.team2Ball3Box);
		autoLayouts[6] = (LinearLayout) findViewById(R.id.team3Ball1Box);
		autoLayouts[7] = (LinearLayout) findViewById(R.id.team3Ball2Box);
		autoLayouts[8] = (LinearLayout) findViewById(R.id.team3Ball3Box);

		Intent intent = getIntent();

		String team1 = intent.getStringExtra("team1");
		String team2 = intent.getStringExtra("team2");
		String team3 = intent.getStringExtra("team3");

		String match = intent.getStringExtra("match");

		submitter = new DB(this, binder);

		teamText1.setText(team1);
		teamText2.setText(team2);
		teamText3.setText(team3);

		matchT.setText(match);

		if(team1 != null && team1.length() > 0 && match != null && match.length() > 0)
		{
			team1Data = (MatchStatsAA) submitter.getMatchStats(Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match), Integer.valueOf(team1));
			if(team1Data == null)
			{
				team1Data = new MatchStatsAA(Integer.valueOf(team1), Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match));
			}
		}
		else
		{
			team1Data = new MatchStatsAA();
		}

		if(team2 != null && team2.length() > 0 && match != null && match.length() > 0)
		{
			team2Data = (MatchStatsAA) submitter.getMatchStats(Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match), Integer.valueOf(team2));
			if(team2Data == null)
			{
				team2Data = new MatchStatsAA(Integer.valueOf(team2), Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match));
			}
		}
		else
		{
			team2Data = new MatchStatsAA();
		}

		if(team3 != null && team3.length() > 0 && match != null && match.length() > 0)
		{
			team3Data = (MatchStatsAA) submitter.getMatchStats(Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match), Integer.valueOf(team3));
			if(team3Data == null)
			{
				team3Data = new MatchStatsAA(Integer.valueOf(team3), Prefs.getEvent(getApplicationContext(), "Buckeye Regional"), Integer.valueOf(match));
			}
		}
		else
		{
			team3Data = new MatchStatsAA();
		}

		currentCycle = 1;

		loadAuto();
		loadCycle(currentCycle);
		loadEndgame();
	}

	public void onResume()
	{
		super.onResume();

		team1Data.event = Prefs.getEvent(getApplicationContext(), "Buckeye Regional");
		team2Data.event = Prefs.getEvent(getApplicationContext(), "Buckeye Regional");
		team3Data.event = Prefs.getEvent(getApplicationContext(), "Buckeye Regional");

		updatePosition();

		List<String> options = submitter.getNotesOptions();

		if(options == null)
		{
			options = new ArrayList<String>(1);
		}

		options.add(0, team1CommonNotes.getItemAtPosition(0).toString());

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		team1CommonNotes.setAdapter(adapter);
		team2CommonNotes.setAdapter(adapter);
		team3CommonNotes.setAdapter(adapter);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unbindService(watcher);
	}

	protected class ServiceWatcher implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			if(service instanceof LocalBinder)
			{
				binder = (LocalBinder) service;
				submitter.setBinder(binder);
			}
		}

		public void onServiceDisconnected(ComponentName name)
		{
		}
	}

	private void updatePosition()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String pos = prefs.getString("alliancePref", "Red");

		if(pos.contains("Blue"))
		{
			teamText1.setBackgroundResource(R.drawable.blueborder);
			teamText2.setBackgroundResource(R.drawable.blueborder);
			teamText3.setBackgroundResource(R.drawable.blueborder);

			findViewById(R.id.team1FarPoss).setBackgroundResource(R.drawable.bluebackground);
			findViewById(R.id.team2FarPoss).setBackgroundResource(R.drawable.bluebackground);
			findViewById(R.id.team3FarPoss).setBackgroundResource(R.drawable.bluebackground);
			findViewById(R.id.team1NearPoss).setBackgroundResource(R.drawable.redbackground);
			findViewById(R.id.team2NearPoss).setBackgroundResource(R.drawable.redbackground);
			findViewById(R.id.team3NearPoss).setBackgroundResource(R.drawable.redbackground);
		}
		else
		{
			teamText1.setBackgroundResource(R.drawable.redborder);
			teamText2.setBackgroundResource(R.drawable.redborder);
			teamText3.setBackgroundResource(R.drawable.redborder);

			findViewById(R.id.team1FarPoss).setBackgroundResource(R.drawable.redbackground);
			findViewById(R.id.team2FarPoss).setBackgroundResource(R.drawable.redbackground);
			findViewById(R.id.team3FarPoss).setBackgroundResource(R.drawable.redbackground);
			findViewById(R.id.team1NearPoss).setBackgroundResource(R.drawable.bluebackground);
			findViewById(R.id.team2NearPoss).setBackgroundResource(R.drawable.bluebackground);
			findViewById(R.id.team3NearPoss).setBackgroundResource(R.drawable.bluebackground);
		}
	}

	public void autoMovedGoalie(View v)
	{
		boolean checked = ((CheckBox) v).isChecked();
		if(checked)
		{
			switch(v.getId())
			{
				case R.id.team1AutoGoalie:
					((CheckBox) findViewById(R.id.team1AutoMoved)).setChecked(false);
					break;
				case R.id.team1AutoMoved:
					((CheckBox) findViewById(R.id.team1AutoGoalie)).setChecked(false);
					break;
				case R.id.team2AutoGoalie:
					((CheckBox) findViewById(R.id.team2AutoMoved)).setChecked(false);
					break;
				case R.id.team2AutoMoved:
					((CheckBox) findViewById(R.id.team2AutoGoalie)).setChecked(false);
					break;
				case R.id.team3AutoGoalie:
					((CheckBox) findViewById(R.id.team3AutoMoved)).setChecked(false);
					break;
				case R.id.team3AutoMoved:
					((CheckBox) findViewById(R.id.team3AutoGoalie)).setChecked(false);
					break;
			}
		}
	}

	public void onAutoScoreClick(View v)
	{
		int viewId = v.getId();

		if(((ToggleButton) v).isChecked())
		{
			switch(viewId)
			{
				//<editor-fold desc="Team 1 Box 1">
				case R.id.team1AutoHigh1:
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					break;

				case R.id.team1AutoHighHot1:
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					break;

				case R.id.team1AutoLow1:
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					break;

				case R.id.team1AutoLowHot1:
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 1 Box 2">
				case R.id.team1AutoHigh2:
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					break;

				case R.id.team1AutoHighHot2:
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					break;

				case R.id.team1AutoLow2:
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					break;

				case R.id.team1AutoLowHot2:
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 1 Box 3">
				case R.id.team1AutoHigh3:
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					break;

				case R.id.team1AutoHighHot3:
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					break;

				case R.id.team1AutoLow3:
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					break;

				case R.id.team1AutoLowHot3:
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 2 Box 1">
				case R.id.team2AutoHigh1:
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					break;

				case R.id.team2AutoHighHot1:
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					break;

				case R.id.team2AutoLow1:
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					break;

				case R.id.team2AutoLowHot1:
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 2 Box 2">
				case R.id.team2AutoHigh2:
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					break;

				case R.id.team2AutoHighHot2:
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					break;

				case R.id.team2AutoLow2:
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					break;

				case R.id.team2AutoLowHot2:
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 2 Box 3">
				case R.id.team2AutoHigh3:
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					break;

				case R.id.team2AutoHighHot3:
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					break;

				case R.id.team2AutoLow3:
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					break;

				case R.id.team2AutoLowHot3:
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 3 Box 1">
				case R.id.team3AutoHigh1:
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					break;

				case R.id.team3AutoHighHot1:
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					break;

				case R.id.team3AutoLow1:
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					break;

				case R.id.team3AutoLowHot1:
					((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 3 Box 2">
				case R.id.team3AutoHigh2:
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					break;

				case R.id.team3AutoHighHot2:
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					break;

				case R.id.team3AutoLow2:
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					break;

				case R.id.team3AutoLowHot2:
					((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(false);
					break;
				//</editor-fold>

				//<editor-fold desc="Team 3 Box 3">
				case R.id.team3AutoHigh3:
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					break;

				case R.id.team3AutoHighHot3:
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					break;

				case R.id.team3AutoLow3:
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					break;

				case R.id.team3AutoLowHot3:
					((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(false);
					break;
				//</editor-fold>
			}
		}
	}

	public void onPoss(View v)
	{
		if(!(v instanceof Button))
		{
			return;
		}

		Button poss = (Button) v;

		if(poss.getText().toString().contains("X"))
		{
			poss.setText("");
		}
		else
		{
			poss.setText("X");
		}

		highlightAssists();
	}

	public void onTruss(View v)
	{
		if(!(v instanceof ToggleButton))
		{
			return;
		}
		ToggleButton truss = (ToggleButton) v;

		if(truss.isChecked())
		{
			switch(truss.getId())
			{
				case R.id.team1Truss:
					findViewById(R.id.team1Catch).setEnabled(false);
					findViewById(R.id.team2Catch).setEnabled(true);
					findViewById(R.id.team3Catch).setEnabled(true);

					((ToggleButton) findViewById(R.id.team2Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1Catch)).setChecked(false);
					break;

				case R.id.team2Truss:
					findViewById(R.id.team1Catch).setEnabled(true);
					findViewById(R.id.team2Catch).setEnabled(false);
					findViewById(R.id.team3Catch).setEnabled(true);

					((ToggleButton) findViewById(R.id.team1Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2Catch)).setChecked(false);
					break;

				case R.id.team3Truss:
					findViewById(R.id.team1Catch).setEnabled(true);
					findViewById(R.id.team2Catch).setEnabled(true);
					findViewById(R.id.team3Catch).setEnabled(false);

					((ToggleButton) findViewById(R.id.team2Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1Truss)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3Catch)).setChecked(false);
					break;
			}
		}
		else
		{
			findViewById(R.id.team1Catch).setEnabled(false);
			findViewById(R.id.team2Catch).setEnabled(false);
			findViewById(R.id.team3Catch).setEnabled(false);
			((ToggleButton) findViewById(R.id.team1Catch)).setChecked(false);
			((ToggleButton) findViewById(R.id.team2Catch)).setChecked(false);
			((ToggleButton) findViewById(R.id.team3Catch)).setChecked(false);
		}
	}

	public void onCatch(View v)
	{
		if(!(v instanceof ToggleButton))
		{
			return;
		}

		ToggleButton c = (ToggleButton) v;

		if(c.isChecked())
		{
			switch(c.getId())
			{
				case R.id.team1Catch:
					((ToggleButton) findViewById(R.id.team2Catch)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3Catch)).setChecked(false);
					break;

				case R.id.team2Catch:
					((ToggleButton) findViewById(R.id.team1Catch)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3Catch)).setChecked(false);
					break;

				case R.id.team3Catch:
					((ToggleButton) findViewById(R.id.team2Catch)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1Catch)).setChecked(false);
					break;
			}
		}
	}

	public void onHigh(View v)
	{
		if(!(v instanceof ToggleButton))
		{
			return;
		}

		ToggleButton c = (ToggleButton) v;

		if(c.isChecked())
		{
			switch(c.getId())
			{
				case R.id.team1HighScore:
					((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);
					break;

				case R.id.team2HighScore:
					((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);
					break;

				case R.id.team3HighScore:
					((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);
					break;
			}
		}
	}

	public void onLow(View v)
	{
		if(!(v instanceof ToggleButton))
		{
			return;
		}

		ToggleButton c = (ToggleButton) v;

		if(c.isChecked())
		{
			switch(c.getId())
			{
				case R.id.team1LowScore:
					((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
					break;
				case R.id.team2LowScore:
					((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
					break;

				case R.id.team3LowScore:
					((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
					((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
					break;
			}
		}
	}

	public void nextCycle(View v)
	{
		saveCycle();
		currentCycle++;
		loadCycle(currentCycle);
		findViewById(R.id.lastCycleB).setEnabled(true);
		((TextView) findViewById(R.id.cycleText)).setText(String.valueOf(currentCycle));
	}

	public void addTeam1Miss(View v)
	{
		team1Data.misses++;
	}

	public void addTeam2Miss(View v)
	{
		team2Data.misses++;
	}

	public void addTeam3Miss(View v)
	{
		team3Data.misses++;
	}

	public void lastCycle(View v)
	{
		if(currentCycle <= 1)
		{
			return;
		}

		saveCycle();
		currentCycle--;
		loadCycle(currentCycle);

		if(currentCycle <= 1)
		{
			findViewById(R.id.lastCycleB).setEnabled(false);
		}

		((TextView) findViewById(R.id.cycleText)).setText(String.valueOf(currentCycle));
	}

	private void saveCycle()
	{
		saveTeamInfo();

		MatchStatsAA.CycleStatsStruct cycle1 = team1Data.cycles.get(currentCycle);
		MatchStatsAA.CycleStatsStruct cycle2 = team2Data.cycles.get(currentCycle);
		MatchStatsAA.CycleStatsStruct cycle3 = team3Data.cycles.get(currentCycle);

		if(cycle1 == null)
		{
			cycle1 = team1Data.new CycleStatsStruct();
		}

		if(cycle2 == null)
		{
			cycle2 = team2Data.new CycleStatsStruct();
		}

		if(cycle3 == null)
		{
			cycle3 = team3Data.new CycleStatsStruct();
		}

		cycle1.cycle_number = currentCycle;
		cycle1.near_poss = ((Button) findViewById(R.id.team1NearPoss)).getText().toString().contains("X");
		cycle1.white_poss = ((Button) findViewById(R.id.team1WhitePoss)).getText().toString().contains("X");
		cycle1.far_poss = ((Button) findViewById(R.id.team1FarPoss)).getText().toString().contains("X");
		cycle1.truss = ((ToggleButton) findViewById(R.id.team1Truss)).isChecked();
		cycle1.truss_catch = ((ToggleButton) findViewById(R.id.team1Catch)).isChecked();
		cycle1.high = ((ToggleButton) findViewById(R.id.team1HighScore)).isChecked();
		cycle1.low = ((ToggleButton) findViewById(R.id.team1LowScore)).isChecked();

		cycle2.cycle_number = currentCycle;
		cycle2.near_poss = ((Button) findViewById(R.id.team2NearPoss)).getText().toString().contains("X");
		cycle2.white_poss = ((Button) findViewById(R.id.team2WhitePoss)).getText().toString().contains("X");
		cycle2.far_poss = ((Button) findViewById(R.id.team2FarPoss)).getText().toString().contains("X");
		cycle2.truss = ((ToggleButton) findViewById(R.id.team2Truss)).isChecked();
		cycle2.truss_catch = ((ToggleButton) findViewById(R.id.team2Catch)).isChecked();
		cycle2.high = ((ToggleButton) findViewById(R.id.team2HighScore)).isChecked();
		cycle2.low = ((ToggleButton) findViewById(R.id.team2LowScore)).isChecked();

		cycle3.cycle_number = currentCycle;
		cycle3.near_poss = ((Button) findViewById(R.id.team3NearPoss)).getText().toString().contains("X");
		cycle3.white_poss = ((Button) findViewById(R.id.team3WhitePoss)).getText().toString().contains("X");
		cycle3.far_poss = ((Button) findViewById(R.id.team3FarPoss)).getText().toString().contains("X");
		cycle3.truss = ((ToggleButton) findViewById(R.id.team3Truss)).isChecked();
		cycle3.truss_catch = ((ToggleButton) findViewById(R.id.team3Catch)).isChecked();
		cycle3.high = ((ToggleButton) findViewById(R.id.team3HighScore)).isChecked();
		cycle3.low = ((ToggleButton) findViewById(R.id.team3LowScore)).isChecked();

		// TODO save off assist count
		if(cycle1.truss)
		{
			if(!team1Data.trussCycles.contains(currentCycle))
			{
				team1Data.trussCycles.add(currentCycle);
			}

			team2Data.trussCycles.remove((Integer) currentCycle);

			team3Data.trussCycles.remove((Integer) currentCycle);
		}

		if(cycle2.truss)
		{
			if(!team2Data.trussCycles.contains(currentCycle))
			{
				team2Data.trussCycles.add(currentCycle);
			}

			team1Data.trussCycles.remove((Integer) currentCycle);

			team3Data.trussCycles.remove((Integer) currentCycle);
		}

		if(cycle3.truss)
		{
			if(!team3Data.trussCycles.contains(currentCycle))
			{
				team3Data.trussCycles.add(currentCycle);
			}

			team1Data.trussCycles.remove((Integer) currentCycle);

			team2Data.trussCycles.remove((Integer) currentCycle);
		}

		if(cycle1.truss_catch)
		{
			if(!team1Data.trussCycles.contains(currentCycle))
			{
				team1Data.trussCycles.add(currentCycle);
			}

			team2Data.trussCycles.remove((Integer) currentCycle);

			team3Data.trussCycles.remove((Integer) currentCycle);
		}

		if(cycle2.truss_catch)
		{
			if(!team2Data.caughtCycles.contains(currentCycle))
			{
				team2Data.caughtCycles.add(currentCycle);
			}

			team1Data.caughtCycles.remove((Integer) currentCycle);

			team3Data.caughtCycles.remove((Integer) currentCycle);
		}

		if(cycle3.truss_catch)
		{
			if(!team3Data.caughtCycles.contains(currentCycle))
			{
				team3Data.caughtCycles.add(currentCycle);
			}

			team1Data.caughtCycles.remove((Integer) currentCycle);

			team2Data.caughtCycles.remove((Integer) currentCycle);
		}

		int assists = highlightAssists();

		cycle1.assists = assists;
		cycle2.assists = assists;
		cycle3.assists = assists;

		team1Data.cycles.put(currentCycle, cycle1);
		team2Data.cycles.put(currentCycle, cycle2);
		team3Data.cycles.put(currentCycle, cycle3);
	}

	private void loadAuto()
	{
		int currentrow;

		((CheckBox) findViewById(R.id.team1AutoGoalie)).setChecked(team1Data.auto_goalie);
		((CheckBox) findViewById(R.id.team1AutoMoved)).setChecked(team1Data.auto_mobile);

		currentrow = 1;

		if(team1Data.auto_high > 0)
		{
			for(int i = 0; i < team1Data.auto_high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team1Data.auto_high_hot > 0)
		{
			for(int i = 0; i < team1Data.auto_high_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoHighHot1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team1AutoHighHot2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team1AutoHighHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team1Data.auto_low > 0)
		{
			for(int i = 0; i < team1Data.auto_low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team1Data.auto_low_hot > 0)
		{
			for(int i = 0; i < team1Data.auto_low_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoLowHot1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team1AutoLowHot2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team1AutoLowHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team1Data.high > 0)
		{
			for(int i = 0; i < team1Data.high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoHigh1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team1AutoHigh2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team1AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		if(team1Data.low > 0)
		{
			for(int i = 0; i < team1Data.low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team1AutoLow1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team1AutoLow2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team1AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		// team2
		((CheckBox) findViewById(R.id.team2AutoGoalie)).setChecked(team2Data.auto_goalie);
		((CheckBox) findViewById(R.id.team2AutoMoved)).setChecked(team2Data.auto_mobile);

		currentrow = 1;

		if(team2Data.auto_high > 0)
		{
			for(int i = 0; i < team2Data.auto_high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		if(team2Data.auto_high_hot > 0)
		{
			for(int i = 0; i < team2Data.auto_high_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoHighHot1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team2AutoHighHot2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team2AutoHighHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		if(team2Data.auto_low > 0)
		{
			for(int i = 0; i < team2Data.auto_low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		if(team2Data.auto_low_hot > 0)
		{
			for(int i = 0; i < team2Data.auto_low_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoLowHot1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team2AutoLowHot2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team2AutoLowHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		
		if(team2Data.high > 0)
		{
			for(int i = 0; i < team2Data.high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoHigh1)).setChecked(true);
						break;
					case 2:
						((ToggleButton) findViewById(R.id.team2AutoHigh2)).setChecked(true);
						break;
					case 3:
						((ToggleButton) findViewById(R.id.team2AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team2Data.low > 0)
		{
			for(int i = 0; i < team2Data.low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team2AutoLow1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team2AutoLow2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team2AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		// team3
		((CheckBox) findViewById(R.id.team3AutoGoalie)).setChecked(team3Data.auto_goalie);
		((CheckBox) findViewById(R.id.team3AutoMoved)).setChecked(team3Data.auto_mobile);
		
		currentrow = 1;
		
		if(team3Data.auto_high > 0)
		{
			for(int i = 0; i < team3Data.auto_high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		
		if(team3Data.auto_high_hot > 0)
		{
			for(int i = 0; i < team3Data.auto_high_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoHighHot1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoHighHot2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoHighHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
		
		if(team3Data.auto_low > 0)
		{
			for(int i = 0; i < team3Data.auto_low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team3Data.auto_low_hot > 0)
		{
			for(int i = 0; i < team3Data.auto_low_hot; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoLowHot1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoLowHot2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoLowHot3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team3Data.high > 0)
		{
			for(int i = 0; i < team3Data.high; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoHigh1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoHigh2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoHigh3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}

		if(team3Data.low > 0)
		{
			for(int i = 0; i < team3Data.low; i++)
			{
				switch(currentrow)
				{
					case 1:
						((ToggleButton) findViewById(R.id.team3AutoLow1)).setChecked(true);
						break;

					case 2:
						((ToggleButton) findViewById(R.id.team3AutoLow2)).setChecked(true);
						break;

					case 3:
						((ToggleButton) findViewById(R.id.team3AutoLow3)).setChecked(true);
						break;
				}
				currentrow++;
			}
		}
	}

	private void loadEndgame()
	{
		((EditText) findViewById(R.id.team1notes)).setText(team1Data.notes);
		((CheckBox) findViewById(R.id.team1tipped)).setChecked(team1Data.tipOver);
		((CheckBox) findViewById(R.id.team1foul)).setChecked(team1Data.foul);
		((CheckBox) findViewById(R.id.team1techfoul)).setChecked(team1Data.tech_foul);
		((CheckBox) findViewById(R.id.team1redcard)).setChecked(team1Data.redCard);
		((CheckBox) findViewById(R.id.team1yellowcard)).setChecked(team1Data.yellowCard);

		((EditText) findViewById(R.id.team2notes)).setText(team2Data.notes);
		((CheckBox) findViewById(R.id.team2tipped)).setChecked(team2Data.tipOver);
		((CheckBox) findViewById(R.id.team2foul)).setChecked(team2Data.foul);
		((CheckBox) findViewById(R.id.team2techfoul)).setChecked(team2Data.tech_foul);
		((CheckBox) findViewById(R.id.team2redcard)).setChecked(team2Data.redCard);
		((CheckBox) findViewById(R.id.team2yellowcard)).setChecked(team2Data.yellowCard);

		((EditText) findViewById(R.id.team3notes)).setText(team3Data.notes);
		((CheckBox) findViewById(R.id.team3tipped)).setChecked(team3Data.tipOver);
		((CheckBox) findViewById(R.id.team3foul)).setChecked(team3Data.foul);
		((CheckBox) findViewById(R.id.team3techfoul)).setChecked(team3Data.tech_foul);
		((CheckBox) findViewById(R.id.team3redcard)).setChecked(team3Data.redCard);
		((CheckBox) findViewById(R.id.team3yellowcard)).setChecked(team3Data.yellowCard);
	}


	private void loadCycle(int cycle)
	{
		MatchStatsAA.CycleStatsStruct cycle1 = team1Data.cycles.get(cycle);
		MatchStatsAA.CycleStatsStruct cycle2 = team2Data.cycles.get(cycle);
		MatchStatsAA.CycleStatsStruct cycle3 = team3Data.cycles.get(cycle);

		boolean truss = false, cat = false;

		//<editor-fold desc="Team 1">
		if(cycle1 == null)
		{
			((Button) findViewById(R.id.team1FarPoss)).setText("");
			((Button) findViewById(R.id.team1WhitePoss)).setText("");
			((Button) findViewById(R.id.team1NearPoss)).setText("");

			((ToggleButton) findViewById(R.id.team1Truss)).setChecked(false);
			((ToggleButton) findViewById(R.id.team1Catch)).setChecked(false);
			((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(false);
			((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(false);

			onTruss(findViewById(R.id.team1Truss));
			onCatch(findViewById(R.id.team1Catch));
		}
		else
		{
			((Button) findViewById(R.id.team1FarPoss)).setText(cycle1.far_poss ? "X" : "");
			((Button) findViewById(R.id.team1WhitePoss)).setText(cycle1.white_poss ? "X" : "");
			((Button) findViewById(R.id.team1NearPoss)).setText(cycle1.near_poss ? "X" : "");

			((ToggleButton) findViewById(R.id.team1Truss)).setChecked(cycle1.truss);
			if(cycle1.truss)
			{
				onTruss(findViewById(R.id.team1Truss));
				truss = true;
			}

			((ToggleButton) findViewById(R.id.team1Catch)).setChecked(cycle1.truss_catch);
			if(cycle1.truss_catch)
			{
				onCatch(findViewById(R.id.team1Catch));
				cat = true;
			}

			((ToggleButton) findViewById(R.id.team1HighScore)).setChecked(cycle1.high);
			if(cycle1.high)
			{
				onHigh(findViewById(R.id.team1HighScore));
			}

			((ToggleButton) findViewById(R.id.team1LowScore)).setChecked(cycle1.low);
			if(cycle1.low)
			{
				onLow(findViewById(R.id.team1LowScore));
			}
		}
		//</editor-fold>

		//<editor-fold desc="Team 2">
		if(cycle2 == null)
		{
			((Button) findViewById(R.id.team2FarPoss)).setText("");
			((Button) findViewById(R.id.team2WhitePoss)).setText("");
			((Button) findViewById(R.id.team2NearPoss)).setText("");

			((ToggleButton) findViewById(R.id.team2Truss)).setChecked(false);
			((ToggleButton) findViewById(R.id.team2Catch)).setChecked(false);
			((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(false);
			((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(false);

			onTruss(findViewById(R.id.team2Truss));
			onCatch(findViewById(R.id.team2Catch));
		}
		else
		{
			((Button) findViewById(R.id.team2FarPoss)).setText(cycle2.far_poss ? "X" : "");
			((Button) findViewById(R.id.team2WhitePoss)).setText(cycle2.white_poss ? "X" : "");
			((Button) findViewById(R.id.team2NearPoss)).setText(cycle2.near_poss ? "X" : "");

			((ToggleButton) findViewById(R.id.team2Truss)).setChecked(cycle2.truss);
			if(cycle2.truss)
			{
				onTruss(findViewById(R.id.team2Truss));
				truss = true;
			}

			((ToggleButton) findViewById(R.id.team2Catch)).setChecked(cycle2.truss_catch);
			if(cycle2.truss_catch)
			{
				onCatch(findViewById(R.id.team2Catch));
				cat = true;
			}

			((ToggleButton) findViewById(R.id.team2HighScore)).setChecked(cycle2.high);
			if(cycle2.high)
			{
				onHigh(findViewById(R.id.team2HighScore));
			}

			((ToggleButton) findViewById(R.id.team2LowScore)).setChecked(cycle2.low);
			if(cycle2.low)
			{
				onLow(findViewById(R.id.team2LowScore));
			}
		}
		//</editor-fold>

		//<editor-fold desc="Team 3">
		if(cycle3 == null)
		{
			((Button) findViewById(R.id.team3FarPoss)).setText("");
			((Button) findViewById(R.id.team3WhitePoss)).setText("");
			((Button) findViewById(R.id.team3NearPoss)).setText("");

			((ToggleButton) findViewById(R.id.team3Truss)).setChecked(false);
			((ToggleButton) findViewById(R.id.team3Catch)).setChecked(false);
			((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(false);
			((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(false);

			onTruss(findViewById(R.id.team3Truss));
			onCatch(findViewById(R.id.team3Catch));
		}
		else
		{
			((Button) findViewById(R.id.team3FarPoss)).setText(cycle3.far_poss ? "X" : "");
			((Button) findViewById(R.id.team3WhitePoss)).setText(cycle3.white_poss ? "X" : "");
			((Button) findViewById(R.id.team3NearPoss)).setText(cycle3.near_poss ? "X" : "");

			((ToggleButton) findViewById(R.id.team3Truss)).setChecked(cycle3.truss);
			if(cycle3.truss)
			{
				onTruss(findViewById(R.id.team3Truss));
				truss = true;
			}

			((ToggleButton) findViewById(R.id.team3Catch)).setChecked(cycle3.truss_catch);
			if(cycle3.truss_catch)
			{
				onCatch(findViewById(R.id.team3Catch));
				cat = true;
			}

			((ToggleButton) findViewById(R.id.team3HighScore)).setChecked(cycle3.high);
			if(cycle3.high)
			{
				onHigh(findViewById(R.id.team3HighScore));
			}

			((ToggleButton) findViewById(R.id.team3LowScore)).setChecked(cycle3.low);
			if(cycle3.low)
			{
				onLow(findViewById(R.id.team3LowScore));
			}
		}
		//</editor-fold>

		if(!cat)
		{
			onCatch(findViewById(R.id.team1Catch));
		}
		if(!truss)
		{
			onTruss(findViewById(R.id.team1Truss));
		}

		highlightAssists();
	}

	public void onBack(View v)
	{
		if(matchTabHost.getCurrentTab() == 0) // auto
		{
			showDialog(CANCEL_DIALOG);
		}
		else if(matchTabHost.getCurrentTab() == 1)
		{
			saveCycle();
			matchTabHost.setCurrentTab(0);
		}
		else if(matchTabHost.getCurrentTab() == 2)
		{
			saveEnd();
			matchTabHost.setCurrentTab(1);
		}
	}

	public void onSubmit(View v)
	{
		saveEnd();
		submit();
	}

	private void saveTeamInfo()
	{
		String team1 = teamText1.getText().toString();
		if(team1 != null && team1.length() > 0)
		{
			team1Data.team = Integer.valueOf(team1);
		}
		String team2 = teamText2.getText().toString();
		if(team2 != null && team2.length() > 0)
		{
			team2Data.team = Integer.valueOf(team2);
		}
		String team3 = teamText3.getText().toString();
		if(team3 != null && team3.length() > 0)
		{
			team3Data.team = Integer.valueOf(team3);
		}
		String match = matchT.getText().toString();
		if(match != null && match.length() > 0)
		{
			team1Data.match = Integer.valueOf(match);
			team2Data.match = Integer.valueOf(match);
			team3Data.match = Integer.valueOf(match);
		}
	}

	private void saveAuto()
	{
		saveTeamInfo();

		//<editor-fold desc="Team 1">
		team1Data.auto_mobile = ((CheckBox) findViewById(R.id.team1AutoMoved)).isChecked();

		team1Data.auto_goalie = ((CheckBox) findViewById(R.id.team1AutoGoalie)).isChecked();

		team1Data.auto_high = (((ToggleButton) findViewById(R.id.team1AutoHigh1)).isChecked() ? 1 : 0) + (((ToggleButton) findViewById(R.id.team1AutoHigh2)).isChecked() ? 1 : 0) + (((ToggleButton) findViewById(R.id.team1AutoHigh3)).isChecked() ? 1 : 0);

		team1Data.auto_high_hot = (((ToggleButton) findViewById(R.id.team1AutoHighHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoHighHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoHighHot3)).isChecked() ? 1 : 0);

		team1Data.auto_low = (((ToggleButton) findViewById(R.id.team1AutoLow1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoLow2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoLow3)).isChecked() ? 1 : 0);

		team1Data.auto_low_hot = (((ToggleButton) findViewById(R.id.team1AutoLowHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoLowHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team1AutoLowHot3)).isChecked() ? 1 : 0);
		//</editor-fold>

		//<editor-fold desc="Team 2">
		team2Data.auto_mobile = ((CheckBox) findViewById(R.id.team2AutoMoved)).isChecked();

		team2Data.auto_goalie = ((CheckBox) findViewById(R.id.team2AutoGoalie)).isChecked();

		team2Data.auto_high = (((ToggleButton) findViewById(R.id.team2AutoHigh1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoHigh2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoHigh3)).isChecked() ? 1 : 0);

		team2Data.auto_high_hot = (((ToggleButton) findViewById(R.id.team2AutoHighHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoHighHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoHighHot3)).isChecked() ? 1 : 0);

		team2Data.auto_low = (((ToggleButton) findViewById(R.id.team2AutoLow1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoLow2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoLow3)).isChecked() ? 1 : 0);

		team2Data.auto_low_hot = (((ToggleButton) findViewById(R.id.team2AutoLowHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoLowHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team2AutoLowHot3)).isChecked() ? 1 : 0);
		//</editor-fold>

		//<editor-fold desc="Team 3">
		team3Data.auto_mobile = ((CheckBox) findViewById(R.id.team3AutoMoved)).isChecked();

		team3Data.auto_goalie = ((CheckBox) findViewById(R.id.team3AutoGoalie)).isChecked();

		team3Data.auto_high = (((ToggleButton) findViewById(R.id.team3AutoHigh1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoHigh2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoHigh3)).isChecked() ? 1 : 0);

		team3Data.auto_high_hot = (((ToggleButton) findViewById(R.id.team3AutoHighHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoHighHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoHighHot3)).isChecked() ? 1 : 0);

		team3Data.auto_low = (((ToggleButton) findViewById(R.id.team3AutoLow1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoLow2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoLow3)).isChecked() ? 1 : 0);

		team3Data.auto_low_hot = (((ToggleButton) findViewById(R.id.team3AutoLowHot1)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoLowHot2)).isChecked() ? 1 : 0)
				+ (((ToggleButton) findViewById(R.id.team3AutoLowHot3)).isChecked() ? 1 : 0);
		//</editor-fold>
	}

	private void saveEnd()
	{
		saveTeamInfo();

		//<editor-fold desc="Team 1">
		team1Data.notes = team1Notes.getText().toString();
		team1Data.tipOver = ((CheckBox) findViewById(R.id.team1tipped)).isChecked();
		team1Data.foul = ((CheckBox) findViewById(R.id.team1foul)).isChecked();
		team1Data.tech_foul = ((CheckBox) findViewById(R.id.team1techfoul)).isChecked();
		team1Data.yellowCard = ((CheckBox) findViewById(R.id.team1yellowcard)).isChecked();
		team1Data.redCard = ((CheckBox) findViewById(R.id.team1redcard)).isChecked();
		//</editor-fold>

		//<editor-fold desc="Team 2">
		team2Data.notes = team2Notes.getText().toString();
		team2Data.tipOver = ((CheckBox) findViewById(R.id.team2tipped)).isChecked();
		team2Data.foul = ((CheckBox) findViewById(R.id.team2foul)).isChecked();
		team2Data.tech_foul = ((CheckBox) findViewById(R.id.team2techfoul)).isChecked();
		team2Data.yellowCard = ((CheckBox) findViewById(R.id.team2yellowcard)).isChecked();
		team2Data.redCard = ((CheckBox) findViewById(R.id.team2redcard)).isChecked();
		//</editor-fold>

		//<editor-fold desc="Team 3">
		team3Data.notes = team3Notes.getText().toString();
		team3Data.tipOver = ((CheckBox) findViewById(R.id.team3tipped)).isChecked();
		team3Data.foul = ((CheckBox) findViewById(R.id.team3foul)).isChecked();
		team3Data.tech_foul = ((CheckBox) findViewById(R.id.team3techfoul)).isChecked();
		team3Data.yellowCard = ((CheckBox) findViewById(R.id.team3yellowcard)).isChecked();
		team3Data.redCard = ((CheckBox) findViewById(R.id.team3redcard)).isChecked();
		//</editor-fold>
	}

	public void submit()
	{
		submitter.submitMatch(team1Data, team2Data, team3Data);

		submitB.setEnabled(false);

		if(matchT.getText().length() > 0)
		{
			setResult(Integer.valueOf(matchT.getText().toString()) + 1);
		}

		finish();
	}

	private int highlightAssists()
	{

		Button[][] buttons = new Button[3][3];
		buttons[0][0] = ((Button) findViewById(R.id.team1FarPoss));
		buttons[0][1] = ((Button) findViewById(R.id.team1WhitePoss));
		buttons[0][2] = ((Button) findViewById(R.id.team1NearPoss));
		buttons[1][0] = ((Button) findViewById(R.id.team2FarPoss));
		buttons[1][1] = ((Button) findViewById(R.id.team2WhitePoss));
		buttons[1][2] = ((Button) findViewById(R.id.team2NearPoss));
		buttons[2][0] = ((Button) findViewById(R.id.team3FarPoss));
		buttons[2][1] = ((Button) findViewById(R.id.team3WhitePoss));
		buttons[2][2] = ((Button) findViewById(R.id.team3NearPoss));

		Boolean[][] possessions = new Boolean[3][3];

		for(int team = 0; team < 3; team++)
		{
			for(int zone = 0; zone < 3; zone++)
			{
				if(buttons[team][zone].getText().toString().length() > 0)
				{
					possessions[team][zone] = true;
				}
				else
				{
					possessions[team][zone] = false;
				}
			}
		}
		int[] zonelocs = {-1, -1, -1};

		int count = 0;

		for(int zone = 0; zone < 3; zone++)
		{
			for(int zone2 = 0; zone2 < 3; zone2++)
			{
				if(zone2 != zone)
				{
					for(int zone3 = 0; zone3 < 3; zone3++)
					{
						if(zone3 != zone && zone3 != zone2)
						{
							int tempcount = 0;
							tempcount += possessions[0][zone] ? 1 : 0;
							tempcount += possessions[1][zone2] ? 1 : 0;
							tempcount += possessions[2][zone3] ? 1 : 0;

							if(tempcount > count)
							{zonelocs[0] = possessions[0][zone] ? zone : -1;zonelocs[1] = possessions[1][zone2] ? zone2 : -1;zonelocs[2] = possessions[2][zone3] ? zone3 : -1;count = tempcount;
							}
						}
					}
				}
			}
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String pos = prefs.getString("alliancePref", "Red");
		if(pos.contains("Blue"))
		{
			buttons[0][0].setBackgroundResource(zonelocs[0] == 0 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);
			buttons[1][0].setBackgroundResource(zonelocs[1] == 0 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);
			buttons[2][0].setBackgroundResource(zonelocs[2] == 0 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);

			buttons[0][2].setBackgroundResource(zonelocs[0] == 2 ? R.drawable.redbackyellowborder : R.drawable.redbackground);
			buttons[1][2].setBackgroundResource(zonelocs[1] == 2 ? R.drawable.redbackyellowborder : R.drawable.redbackground);
			buttons[2][2].setBackgroundResource(zonelocs[2] == 2 ? R.drawable.redbackyellowborder : R.drawable.redbackground);

		}
		else
		{
			buttons[0][0].setBackgroundResource(zonelocs[0] == 0 ? R.drawable.redbackyellowborder : R.drawable.redbackground);
			buttons[1][0].setBackgroundResource(zonelocs[1] == 0 ? R.drawable.redbackyellowborder : R.drawable.redbackground);
			buttons[2][0].setBackgroundResource(zonelocs[2] == 0 ? R.drawable.redbackyellowborder : R.drawable.redbackground);

			buttons[0][2].setBackgroundResource(zonelocs[0] == 2 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);
			buttons[1][2].setBackgroundResource(zonelocs[1] == 2 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);
			buttons[2][2].setBackgroundResource(zonelocs[2] == 2 ? R.drawable.bluebackyellowborder : R.drawable.bluebackground);
		}

		buttons[0][1].setBackgroundResource(zonelocs[0] == 1 ? R.drawable.whitebackyellowborder : R.drawable.whitebackground);
		buttons[1][1].setBackgroundResource(zonelocs[1] == 1 ? R.drawable.whitebackyellowborder : R.drawable.whitebackground);
		buttons[2][1].setBackgroundResource(zonelocs[2] == 1 ? R.drawable.whitebackyellowborder : R.drawable.whitebackground);

		return count;

	}

	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch(id)
		{
			case CANCEL_DIALOG:
				builder.setMessage("Cancel Match Entry?\nChanges will not be saved.").setCancelable(false)
						.setPositiveButton("Yes",new DialogInterface.OnClickListener(){	public void onClick(DialogInterface dialog, int id)	{		MatchActivity.this.finish();	}})
						.setNegativeButton("No",new DialogInterface.OnClickListener(){	public void onClick(DialogInterface dialog, int id)	{		dialog.cancel();	}});
				dialog = builder.create();
				break;

			case MainMenuSelection.HELPDIALOG:
				builder.setMessage(HELPMESSAGE).setCancelable(true)
						.setPositiveButton("OK",new DialogInterface.OnClickListener(){	public void onClick(DialogInterface dialog, int which)	{		dialog.cancel();
	}});
				dialog = builder.create();
				break;

			default:
				dialog = null;
		}
		return dialog;
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		return MainMenuSelection.onOptionsItemSelected(item, this) ? true : super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		onBack(null);

		return;
	}

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		if(position == 0 || !(parent instanceof Spinner))
		{
			return;
		}

		EditText notes;

		switch(parent.getId())
		{
			case R.id.team1common_notes:
				notes = team1Notes;
				break;

			case R.id.team2common_notes:
				notes = team2Notes;
				break;

			case R.id.team3common_notes:
				notes = team3Notes;
				break;

			default:
				notes = team1Notes;
				break;
		}
		Spinner par = (Spinner) parent;
		String note = notes.getText().toString();
		if(!note.contains(par.getItemAtPosition(position).toString()))
		{
			if(!note.trim().equals(""))
			{
				note = note + "; ";
			}
			note = note + par.getItemAtPosition(position);
			notes.setText(note);
		}
		par.setSelection(0);
	}

	public void onNothingSelected(AdapterView<?> arg0)
	{
		// do nothing
	}
}
