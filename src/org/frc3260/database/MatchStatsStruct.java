package org.frc3260.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.frc3260.database.DB;
import org.frc3260.database.FRCScoutingContract;

import static org.frc3260.database.FRCScoutingContract.FACT_MATCH_DATA_Entry;

public abstract class MatchStatsStruct
{
	public int team;
	public String event;
	public int match;
	public boolean autonomous;
	public String notes;
	public boolean tipOver;
	public boolean foul;
	public boolean tech_foul;
	public boolean yellowCard;
	public boolean redCard;

	public MatchStatsStruct()
	{
		init();
	}

	public MatchStatsStruct(int team, String event, int match)
	{
		this.team = team;
		this.event = event;
		this.match = match;
		init();
	}

	public MatchStatsStruct(int team, String event, int match, boolean auto)
	{

		init();
		this.team = team;
		this.event = event;
		this.match = match;
		autonomous = auto;
	}

	public void init()
	{
		autonomous = true;
		tipOver = false;
		notes = "";
		foul = false;
		tech_foul = false;
		yellowCard = false;
		redCard = false;
	}

	public ContentValues getValues(DB db, SQLiteDatabase database)
	{
		ContentValues args = new ContentValues();
		long ev = db.getEventIDFromName(event, database);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_ID, ev * 10000000 + match * 10000 + team);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID, team);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID, ev);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID, match);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_NOTES, notes);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_TIP_OVER, tipOver ? 1 : 0);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_FOUL, foul ? 1 : 0);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_TECH_FOUL, tech_foul ? 1 : 0);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_YELLOW_CARD, yellowCard ? 1 : 0);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_RED_CARD, redCard ? 1 : 0);
		args.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_INVALID, 1);

		return args;
	}

	public void fromCursor(Cursor c, DB db, SQLiteDatabase database)
	{
		c.moveToFirst();
		notes = c.getString(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_NOTES));
		tipOver = c.getInt(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_TIP_OVER)) != 0;
		foul = c.getInt(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_FOUL)) != 0;
		tech_foul = c.getInt(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_TECH_FOUL)) != 0;
		yellowCard = c.getInt(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_YELLOW_CARD)) != 0;
		redCard = c.getInt(c.getColumnIndexOrThrow(FACT_MATCH_DATA_Entry.COLUMN_NAME_RED_CARD)) != 0;
	}
}
