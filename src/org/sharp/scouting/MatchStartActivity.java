package org.sharp.scouting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import org.frc3260.aerialassist.MatchActivity;

public class MatchStartActivity extends Activity
{
	private EditText teamNum1;
	private EditText teamNum2;
	private EditText teamNum3;
	private EditText matchNum;
	private Button startB;

	private String HELPMESSAGE;

	private MatchSchedule schedule;

	private static final int MATCH_ACTIVITY_REQUEST = 0;

	private ProgressDialog pd;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.matchstart);

		HELPMESSAGE = "Ensure correct Event and Position are selected in Settings.\n\n"
				+ "Enter the upcoming match number, and the team number will auto-populate if available.\n\n"
				+ "Match number and team number will automatically update upon successful submission of match data.";

		teamNum1 = (EditText) findViewById(R.id.startTeamNum1);
		teamNum2 = (EditText) findViewById(R.id.startTeamNum2);
		teamNum3 = (EditText) findViewById(R.id.startTeamNum3);
		matchNum = (EditText) findViewById(R.id.startMatchNum);
		startB = (Button) findViewById(R.id.startMatchB);

		startB.setOnClickListener(new StartClickListener());

		matchNum.addTextChangedListener(new matchTextListener());
		schedule = new MatchSchedule();

		matchNum.setFilters(new InputFilter[]{new InputFilterMinMax("1", "100")});
		teamNum1.setFilters(new InputFilter[]{new InputFilterMinMax("1", "9999")});
		teamNum2.setFilters(new InputFilter[]{new InputFilterMinMax("1", "9999")});
		teamNum3.setFilters(new InputFilter[]{new InputFilterMinMax("1", "9999")});
	}

	public void onResume()
	{
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		updatePosition();

		if(!schedule.isValid(this))
		{
			schedule.updateSchedule(prefs.getString("eventPref", "Buckeye Regional"), this, false);
		}
	}

	private class matchTextListener implements TextWatcher
	{
		public void afterTextChanged(Editable s)
		{
			if(s.length() > 0)
			{
				setMatch(Integer.valueOf(s.toString()));
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

		public void onTextChanged(CharSequence s, int start, int before, int count) { }
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

	private class StartClickListener implements OnClickListener
	{
		public void onClick(View v)
		{
			Intent intent = new Intent(MatchStartActivity.this, MatchActivity.class);
			intent.putExtra("team1", teamNum1.getText().toString());
			intent.putExtra("team2", teamNum2.getText().toString());
			intent.putExtra("team3", teamNum3.getText().toString());
			intent.putExtra("match", matchNum.getText().toString());
			startActivityForResult(intent, MATCH_ACTIVITY_REQUEST);
		}
	}

	private void updatePosition()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String pos = prefs.getString("alliancePref", "Red");

		if(pos.contains("Blue"))
		{
			teamNum1.setBackgroundResource(R.drawable.blueborder);
			teamNum2.setBackgroundResource(R.drawable.blueborder);
			teamNum3.setBackgroundResource(R.drawable.blueborder);
		}
		else
		{
			teamNum1.setBackgroundResource(R.drawable.redborder);
			teamNum2.setBackgroundResource(R.drawable.redborder);
			teamNum3.setBackgroundResource(R.drawable.redborder);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == Prefs.PREFS_ACTIVITY_CODE)
		{
			MatchSchedule schedule = new MatchSchedule();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			schedule.updateSchedule(prefs.getString("eventPref", "Buckeye Regional"), this, false);

			updatePosition();

			if(matchNum.getText().length() > 0)
			{
				setMatch(Integer.valueOf(matchNum.getText().toString()));
			}
		}
		if(requestCode == MATCH_ACTIVITY_REQUEST && resultCode > 0)
		{
			matchNum.setText(String.valueOf(resultCode));
		}
	}

	private void setMatch(int matchNum)
	{

		String def1 = teamNum1.getText().toString().trim();
		String def2 = teamNum2.getText().toString().trim();
		String def3 = teamNum3.getText().toString().trim();

		try
		{
			if(def1.length() > 9 || Integer.valueOf(def1) <= 0)
			{
				def1 = "";
			}
		} catch(Exception e)
		{
			def1 = "";
		}
		try
		{
			if(def2.length() > 9 || Integer.valueOf(def2) <= 0)
			{
				def2 = "";
			}
		} catch(Exception e)
		{
			def2 = "";
		}
		try
		{
			if(def3.length() > 9 || Integer.valueOf(def3) <= 0)
			{
				def3 = "";
			}
		} catch(Exception e)
		{
			def3 = "";
		}

		teamNum1.setText(schedule.getTeam(matchNum, Prefs.getPosition(this, "red") + " 1", this, def1));
		teamNum2.setText(schedule.getTeam(matchNum, Prefs.getPosition(this, "red") + " 2", this, def2));
		teamNum3.setText(schedule.getTeam(matchNum, Prefs.getPosition(this, "red") + " 3", this, def3));
	}

	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch(id)
		{
			case MainMenuSelection.HELPDIALOG:
				builder.setMessage(HELPMESSAGE)
						.setCancelable(true)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener()
								{

									public void onClick(DialogInterface dialog,
														int which)
									{
										dialog.cancel();

									}
								});
				dialog = builder.create();
				break;
			default:
				dialog = null;
		}
		return dialog;
	}

}
