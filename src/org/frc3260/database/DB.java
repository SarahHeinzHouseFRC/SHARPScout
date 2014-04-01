package org.frc3260.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.SparseArray;
import android.widget.Toast;
import org.acra.ACRA;
import org.frc3260.aerialassist.*;
import org.frc3260.database.DBSyncService.LocalBinder;
import org.frc3260.database.FRCScoutingContract.*;
import org.sharp.scouting.Prefs;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class DB
{
	public static final boolean debug = false;

	private HttpUtils utils;
	private String password;
	private Context context;
	private LocalBinder binder;

	private DB()
	{
	}

	public DB(Context context, String pass, LocalBinder binder)
	{
		utils = new HttpUtils();
		password = pass;
		this.context = context;
		ScoutingDBHelper.getInstance(context.getApplicationContext());
		this.binder = binder;
	}

	public DB(Context context, LocalBinder binder)
	{
		this.context = context;
		utils = new HttpUtils();
		password = Prefs.getSavedPassword(context);
		ScoutingDBHelper.getInstance(context.getApplicationContext());
		this.binder = binder;
	}

	protected static Map<String, String> getPostData(ContentValues values)
	{
		Map<String, String> data = new HashMap<String, String>();
		for(String key : values.keySet())
		{
			data.put(key, values.getAsString(key));
		}
		return data;
	}

	public static void exportToCSV(Context context)
	{
		try
		{
			ExportCallback cb = new ExportCallback();

			cb.context = context;

			CSVExporter export = new CSVExporter(context);
			export.execute(cb);
		} catch(Exception e)
		{
			Toast.makeText(context, "Error Exporting Database", Toast.LENGTH_LONG).show();
		}
	}

	public void setBinder(LocalBinder binder)
	{
		this.binder = binder;
	}

	public void startSync()
	{
		if(binder != null)
		{
			binder.setPassword(password);
			binder.startSync();
		}
	}

	private void insertOrUpdate(String table, String nullColumnHack, ContentValues values, String idColumnName, String whereClause, String[] whereArgs)
	{
		synchronized(ScoutingDBHelper.lock)
		{
			SQLiteDatabase db = ScoutingDBHelper.getInstance().getWritableDatabase();

			String[] projection = {idColumnName};

			Cursor c = db.query(table, projection, whereClause, whereArgs, null, null, null, "0,1");

			try
			{
				if(c.moveToFirst())
				{
					String[] id = {c.getString(c.getColumnIndexOrThrow(idColumnName))};
					values.put(FACT_MATCH_DATA_Entry.COLUMN_NAME_TIMESTAMP, DBSyncService.dateParser.format(new Date()));
					db.update(table, values, idColumnName + "=?", id);
				}
				else
				{
					db.insert(table, nullColumnHack, values);
				}
			} finally
			{
				c.close();

				ScoutingDBHelper.getInstance().close();
			}
		}
	}

	public boolean submitMatch(MatchStatsStruct team1Data, MatchStatsStruct team2Data, MatchStatsStruct team3Data)
	{
		try
		{
			String where = FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID + "=? AND " + FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID + "=? AND " + FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?";
			ContentValues values, values2, values3;
			List<ContentValues> cycles1, cycles2, cycles3;
			synchronized(ScoutingDBHelper.lock)
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				values = team1Data.getValues(this, db);
				values2 = team2Data.getValues(this, db);
				values3 = team3Data.getValues(this, db);
				MatchStatsAA data;
				if(team1Data instanceof MatchStatsAA)
				{
					data = (MatchStatsAA) team1Data;
					cycles1 = data.getCycles(this, db);
				}
				else
				{
					cycles1 = new ArrayList<ContentValues>();
				}
				if(team2Data instanceof MatchStatsAA)
				{
					data = (MatchStatsAA) team2Data;
					cycles2 = data.getCycles(this, db);
				}
				else
				{
					cycles2 = new ArrayList<ContentValues>();
				}
				if(team3Data instanceof MatchStatsAA)
				{
					data = (MatchStatsAA) team3Data;
					cycles3 = data.getCycles(this, db);
				}
				else
				{
					cycles3 = new ArrayList<ContentValues>();
				}

				ScoutingDBHelper.getInstance().close();
			}
			String[] whereArgs = {values.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID), values.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID), values.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID)};

			insertOrUpdate(FACT_MATCH_DATA_Entry.TABLE_NAME, null, values, FACT_MATCH_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);

			whereArgs[0] = values2.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID);
			whereArgs[1] = values2.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID);
			whereArgs[2] = values2.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID);

			insertOrUpdate(FACT_MATCH_DATA_Entry.TABLE_NAME, null, values2, FACT_MATCH_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);

			whereArgs[0] = values3.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID);
			whereArgs[1] = values3.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID);
			whereArgs[2] = values3.getAsString(FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID);

			insertOrUpdate(FACT_MATCH_DATA_Entry.TABLE_NAME, null, values3, FACT_MATCH_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);

			whereArgs = new String[4];
			where = FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID + "=? AND " + FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID + "=? AND " + FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID + "=? AND " + FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM + "=?";

			for(ContentValues cycle : cycles1)
			{
				whereArgs[0] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID);
				whereArgs[1] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID);
				whereArgs[2] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID);
				whereArgs[3] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM);

				insertOrUpdate(FACT_CYCLE_DATA_Entry.TABLE_NAME, null, cycle, FACT_CYCLE_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);
			}
			for(ContentValues cycle : cycles2)
			{
				whereArgs[0] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID);
				whereArgs[1] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID);
				whereArgs[2] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID);
				whereArgs[3] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM);

				insertOrUpdate(FACT_CYCLE_DATA_Entry.TABLE_NAME, null, cycle, FACT_CYCLE_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);
			}
			for(ContentValues cycle : cycles3)
			{
				whereArgs[0] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID);
				whereArgs[1] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID);
				whereArgs[2] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID);
				whereArgs[3] = cycle.getAsString(FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM);

				insertOrUpdate(FACT_CYCLE_DATA_Entry.TABLE_NAME, null, cycle, FACT_CYCLE_DATA_Entry.COLUMN_NAME_ID, where, whereArgs);
			}

			startSync();

			return true;

		} catch(Exception e)
		{
			return false;
		}
	}

	public boolean submitPits(PitStats stats)
	{
		try
		{
			ContentValues values;
			synchronized(ScoutingDBHelper.lock)
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getWritableDatabase();
				values = stats.getValues(this, db);
				ScoutingDBHelper.getInstance().close();
			}

			String[] where = {values.getAsString(SCOUT_PIT_DATA_Entry.COLUMN_NAME_TEAM_ID)};

			insertOrUpdate(SCOUT_PIT_DATA_Entry.TABLE_NAME, null, values,
					SCOUT_PIT_DATA_Entry.COLUMN_NAME_ID,
					SCOUT_PIT_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?", where);

			startSync();
			return true;
		} catch(Exception e)
		{
			return false;
		}
	}

	public void setPass(String pass)
	{
		password = pass;
	}

	public void checkPass(String pass, HttpCallback callback)
	{
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("password", pass);
		params.put("type", "passConfirm");

		utils.doPost(Prefs.getScoutingURL(context), params, callback);
	}

	public void checkVersion(HttpCallback callback)
	{
		Map<String, String> args = new HashMap<String, String>();
		args.put("type", "versioncheck");
		args.put("password", Prefs.getSavedPassword(context));
		utils.doPost(Prefs.getScoutingURL(context), args, callback);
	}

	public String getTeamPitInfo(String teamNum)
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {SCOUT_PIT_DATA_Entry.COLUMN_NAME_TIMESTAMP};
				String[] where = {teamNum};
				Cursor c = db.query(SCOUT_PIT_DATA_Entry.TABLE_NAME, // from the
						// scout_pit_data
						// table
						projection, // select
						SCOUT_PIT_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?", // where
						// team_id
						// ==
						where, // teamNum
						null, // don't group
						null, // don't filter
						null, // don't order
						"0,1"); // limit to 1

				String date;

				try
				{
					c.moveToFirst();

					date = c.getString(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_TIMESTAMP));

				}
				finally
				{
					c.close();

					ScoutingDBHelper.getInstance().close();
				}

				return date;

			} catch(Exception e)
			{
				return "";
			}
		}
	}

	public void getEventList(HttpCallback callback)
	{
		Map<String, String> args = new HashMap<String, String>();
		args.put("type", "paramRequest");
		args.put("req", "eventList");
		args.put("password", Prefs.getSavedPassword(context));

		utils.doPost(Prefs.getScoutingURL(context), args, callback);
	}

	public List<String> getConfigList()
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC};

				Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, projection, null, null, null, null, CONFIGURATION_LU_Entry.COLUMN_NAME_ID);

				List<String> ret = new ArrayList<String>(c.getCount());

				try
				{
					if(c.moveToFirst())
					{
						do
						{
							ret.add(c.getString(c.getColumnIndexOrThrow(CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC)));
						} while(c.moveToNext());
					}
					else
					{
						return null;
					}
				} finally
				{
					c.close();
				}

				return ret;
			} catch(Exception e)
			{
				return null;
			}
		}
	}

	public List<String> getWheelBaseList()
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC};

				Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, projection, null, null, null, null, WHEEL_BASE_LU_Entry.COLUMN_NAME_ID);

				List<String> ret = new ArrayList<String>(c.getCount());

				try
				{
					if(c.moveToFirst())
					{
						do
						{
							ret.add(c.getString(c.getColumnIndexOrThrow(WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC)));
						} while(c.moveToNext());
					}
					else
					{
						return null;
					}
				} finally
				{
					c.close();
				}
				return ret;
			} catch(Exception e)
			{
				return null;
			}
		}
	}

	public List<String> getWheelTypeList()
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC};

				Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, projection, null, null, null, null, WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID);

				List<String> ret = new ArrayList<String>(c.getCount());

				try
				{
					if(c.moveToFirst())
					{
						do
						{
							ret.add(c.getString(c.getColumnIndexOrThrow(WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC)));
						} while(c.moveToNext());
					}
					else
					{
						return null;
					}
				} finally
				{
					c.close();
				}
				return ret;
			} catch(Exception e)
			{
				return null;
			}
		}
	}

	public String getURLFromEventName(String eventName)
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();
				String[] projection = {EVENT_LU_Entry.COLUMN_NAME_MATCH_URL};
				String[] where = {eventName};
				Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, // from the
						// event_lu
						// table
						projection, // select
						EVENT_LU_Entry.COLUMN_NAME_EVENT_NAME + " LIKE ?", // where
						// event_name
						// ==
						where, // EventName
						null, // don't group
						null, // don't filter
						null, // don't order
						"0,1"); // limit to 1
				c.moveToFirst();
				return c.getString(c.getColumnIndexOrThrow(EVENT_LU_Entry.COLUMN_NAME_MATCH_URL));
			} catch(Exception e)
			{
				return null;
			}
		}
	}


	public List<String> getNotesOptions()
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {NOTES_OPTIONS_Entry.COLUMN_NAME_OPTION_TEXT};

				Cursor c = db.query(NOTES_OPTIONS_Entry.TABLE_NAME, projection, null, null, null, null, NOTES_OPTIONS_Entry.COLUMN_NAME_ID);

				List<String> ret = new ArrayList<String>(c.getCount());

				try
				{
					if(c.moveToFirst())
					{
						do
						{
							ret.add(c.getString(c.getColumnIndexOrThrow(NOTES_OPTIONS_Entry.COLUMN_NAME_OPTION_TEXT)));
						} while(c.moveToNext());
					}
					else
					{
						return null;
					}
				} finally
				{
					c.close();
				}

				return ret;
			} catch(Exception e)
			{
				return null;
			}
		}
	}

	public void getMatchStats(String event, String match, MatchStats.MatchCallback callback)
	{
		// TODO
		/*
		 * Map<String, String> args = new HashMap<String, String>();
		 * args.put("event_name", event); args.put("match_id", match);
		 * args.put("password", password); args.put("type", "matchStats");
		 * utils.doPost(Prefs.getScoutingURL(context), args, new MatchStats(
		 * callback));
		 */
	}

	public void getEventStats(String eventName, EventStats.EventCallback callback)
	{
		Map<String, String> args = new HashMap<String, String>();
		args.put("type", "eventStats");
		args.put("password", password);
		args.put("event_name", eventName);
		utils.doPost(Prefs.getScoutingURL(context), args, new EventStats(callback));
	}

	public void getTeamStats(int teamId, TeamStats.TeamCallback callback)
	{
		Map<String, String> args = new HashMap<String, String>();
		args.put("type", "teamStats");
		args.put("password", password);
		args.put("team_id", String.valueOf(teamId));
		utils.doPost(Prefs.getScoutingURL(context), args, new TeamStats(callback, teamId, Prefs.getEvent(context, "Buckeye Regional")));
	}

	public PitStats getTeamPitStats(int teamNum)
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				PitStatsAA stats = new PitStatsAA();

				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {SCOUT_PIT_DATA_Entry.COLUMN_NAME_TEAM_ID, SCOUT_PIT_DATA_Entry.COLUMN_NAME_CONFIGURATION_ID, SCOUT_PIT_DATA_Entry.COLUMN_NAME_WHEEL_TYPE_ID, SCOUT_PIT_DATA_Entry.COLUMN_NAME_WHEEL_BASE_ID, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTONOMOUS_MODE, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HIGH, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_LOW, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HOT, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_MOBILE, SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_GOALIE, SCOUT_PIT_DATA_Entry.COLUMN_NAME_TRUSS, SCOUT_PIT_DATA_Entry.COLUMN_NAME_CATCH, SCOUT_PIT_DATA_Entry.COLUMN_NAME_ACTIVE_CONTROL, SCOUT_PIT_DATA_Entry.COLUMN_NAME_LAUNCH_BALL, SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_HIGH, SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_LOW, SCOUT_PIT_DATA_Entry.COLUMN_NAME_MAX_HEIGHT, SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCOUT_COMMENTS};
				String[] where = {String.valueOf(teamNum)};
				Cursor c = db.query(SCOUT_PIT_DATA_Entry.TABLE_NAME, projection, SCOUT_PIT_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?", where, null, null, null, "0,1"); // limit to 1

				try
				{
					stats.fromCursor(c, this, db);
				} finally
				{
					c.close();

					ScoutingDBHelper.getInstance().close();
				}

				return stats;
			} catch(Exception e)
			{
				return null;
			}

		}
	}

	public MatchStatsStruct getMatchStats(String eventName, int match, int team)
	{
		synchronized(ScoutingDBHelper.lock)
		{
			try
			{
				MatchStatsAA stats = new MatchStatsAA(team, eventName, match);
				SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();

				String[] projection = {FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID, FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID, FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_HIGH, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_HIGH_HOT, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_LOW, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_LOW_HOT, FACT_MATCH_DATA_Entry.COLUMN_NAME_ASSIST_POINTS, FACT_MATCH_DATA_Entry.COLUMN_NAME_HIGH, FACT_MATCH_DATA_Entry.COLUMN_NAME_LOW, FACT_MATCH_DATA_Entry.COLUMN_NAME_TRUSS, FACT_MATCH_DATA_Entry.COLUMN_NAME_CAUGHT, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_MOBILE, FACT_MATCH_DATA_Entry.COLUMN_NAME_AUTO_GOALIE, FACT_MATCH_DATA_Entry.COLUMN_NAME_NUM_CYCLES, FACT_MATCH_DATA_Entry.COLUMN_NAME_MISSES, FACT_MATCH_DATA_Entry.COLUMN_NAME_FOUL, FACT_MATCH_DATA_Entry.COLUMN_NAME_TECH_FOUL, FACT_MATCH_DATA_Entry.COLUMN_NAME_TIP_OVER, FACT_MATCH_DATA_Entry.COLUMN_NAME_YELLOW_CARD, FACT_MATCH_DATA_Entry.COLUMN_NAME_RED_CARD, FACT_MATCH_DATA_Entry.COLUMN_NAME_NOTES};

				String[] where = {String.valueOf(match), String.valueOf(getEventIDFromName(eventName, db)), String.valueOf(team)};

				Cursor c = db.query(FACT_MATCH_DATA_Entry.TABLE_NAME, projection, FACT_MATCH_DATA_Entry.COLUMN_NAME_MATCH_ID + "=? AND " + FACT_MATCH_DATA_Entry.COLUMN_NAME_EVENT_ID + "=? AND " + FACT_MATCH_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?", where, null, null, null, "0,1");

				String[] projection2 = {FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID, FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID, FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID, FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM, FACT_CYCLE_DATA_Entry.COLUMN_NAME_NEAR_POSS, FACT_CYCLE_DATA_Entry.COLUMN_NAME_WHITE_POSS, FACT_CYCLE_DATA_Entry.COLUMN_NAME_FAR_POSS, FACT_CYCLE_DATA_Entry.COLUMN_NAME_TRUSS, FACT_CYCLE_DATA_Entry.COLUMN_NAME_CATCH, FACT_CYCLE_DATA_Entry.COLUMN_NAME_HIGH, FACT_CYCLE_DATA_Entry.COLUMN_NAME_LOW, FACT_CYCLE_DATA_Entry.COLUMN_NAME_ASSISTS};

				Cursor c2 = db.query(FACT_CYCLE_DATA_Entry.TABLE_NAME, projection2, FACT_CYCLE_DATA_Entry.COLUMN_NAME_MATCH_ID + "=? AND " + FACT_CYCLE_DATA_Entry.COLUMN_NAME_EVENT_ID + "=? AND " + FACT_CYCLE_DATA_Entry.COLUMN_NAME_TEAM_ID + "=?", where, null, null, FACT_CYCLE_DATA_Entry.COLUMN_NAME_CYCLE_NUM);

				try
				{
					stats.fromCursor(c, c2, this, db);
				}
				finally
				{
					c2.close();

					ScoutingDBHelper.getInstance().close();
				}

				return stats;

			} catch(Exception e)
			{
				return null;
			}

		}
	}

	public long getEventIDFromName(String eventName, SQLiteDatabase db)
	{

		String[] projection = {EVENT_LU_Entry.COLUMN_NAME_ID};
		String[] where = {eventName};
		Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, // from the event_lu
				// table
				projection, // select
				EVENT_LU_Entry.COLUMN_NAME_EVENT_NAME + " LIKE ?", // where
				// event_name
				// ==
				where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getLong(c.getColumnIndexOrThrow(EVENT_LU_Entry.COLUMN_NAME_ID));
		} finally
		{
			c.close();
		}
	}

	public long getConfigIDFromName(String config, SQLiteDatabase db)
	{

		String[] projection = {CONFIGURATION_LU_Entry.COLUMN_NAME_ID};
		String[] where = {config};
		Cursor c = db.query(CONFIGURATION_LU_Entry.TABLE_NAME, projection, // select
				CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC + " LIKE ?", where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1

		try
		{
			c.moveToFirst();
			return c.getLong(c.getColumnIndexOrThrow(CONFIGURATION_LU_Entry.COLUMN_NAME_ID));
		} finally
		{
			c.close();
		}
	}

	public long getWheelBaseIDFromName(String base, SQLiteDatabase db)
	{

		String[] projection = {WHEEL_BASE_LU_Entry.COLUMN_NAME_ID};
		String[] where = {base};
		Cursor c = db.query(WHEEL_BASE_LU_Entry.TABLE_NAME,
				projection, // select
				WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC + " LIKE ?",
				where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getLong(c.getColumnIndexOrThrow(WHEEL_BASE_LU_Entry.COLUMN_NAME_ID));
		} finally
		{
			c.close();
		}
	}

	public long getWheelTypeIDFromName(String type, SQLiteDatabase db)
	{

		String[] projection = {WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID};
		String[] where = {type};
		Cursor c = db.query(WHEEL_TYPE_LU_Entry.TABLE_NAME,
				projection, // select
				WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC + " LIKE ?",
				where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getLong(c.getColumnIndexOrThrow(WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID));
		} finally
		{
			c.close();
		}
	}

	public static String getConfigNameFromID(int config, SQLiteDatabase db)
	{
		String[] projection = {CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC};
		String[] where = {String.valueOf(config)};
		Cursor c = db.query(CONFIGURATION_LU_Entry.TABLE_NAME, projection, // select
				CONFIGURATION_LU_Entry.COLUMN_NAME_ID + "= ?", where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getString(c.getColumnIndexOrThrow(CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC));
		} finally
		{
			c.close();
		}
	}

	public static String getWheelBaseNameFromID(int base, SQLiteDatabase db)
	{
		String[] projection = {WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC};
		String[] where = {String.valueOf(base)};
		Cursor c = db.query(WHEEL_BASE_LU_Entry.TABLE_NAME, projection, // select
				WHEEL_BASE_LU_Entry.COLUMN_NAME_ID + "= ?", where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getString(c.getColumnIndexOrThrow(WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC));
		} finally
		{
			c.close();
		}
	}

	public static String getWheelTypeNameFromID(int type, SQLiteDatabase db)
	{

		String[] projection = {WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC};
		String[] where = {String.valueOf(type)};
		Cursor c = db.query(WHEEL_TYPE_LU_Entry.TABLE_NAME, projection, // select
				WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID + " LIKE ?", where, // EventName
				null, // don't group
				null, // don't filter
				null, // don't order
				"0,1"); // limit to 1
		try
		{
			c.moveToFirst();
			return c.getString(c.getColumnIndexOrThrow(WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC));
		} finally
		{
			c.close();
		}
	}

	private static class ExportCallback
	{
		Context context;

		public void finish(String result)
		{
			Toast.makeText(context, result, Toast.LENGTH_LONG).show();
		}
	}

	private static class CSVExporter extends AsyncTask<ExportCallback, Integer, String>
	{
		Context context;

		ExportCallback callback;

		public CSVExporter(Context context)
		{
			this.context = context;
		}

		@Override
		protected String doInBackground(ExportCallback... params)
		{
			synchronized(ScoutingDBHelper.lock)
			{
				try
				{
					SQLiteDatabase db = ScoutingDBHelper.getInstance().getReadableDatabase();
					callback = params[0];

					SparseArray<String> configs = new SparseArray<String>();
					SparseArray<String> types = new SparseArray<String>();
					SparseArray<String> bases = new SparseArray<String>();

					Cursor c;
					c = db.rawQuery("SELECT * FROM " + FRCScoutingContract.FACT_MATCH_DATA_Entry.TABLE_NAME, null);
					StringBuilder match_data = new StringBuilder(c.getCount() * c.getColumnCount() * 2);
					for(int i = 0; i < c.getColumnCount(); i++)
					{
						if(i > 0)
						{
							match_data.append(",");
						}
						match_data.append(c.getColumnName(i));
					}
					match_data.append("\n");
					try
					{
						if(c.moveToFirst())
						{
							do
							{
								for(int j = 0; j < c.getColumnCount(); j++)
								{
									if(j > 0)
									{
										match_data.append(",");
									}
									if(FACT_MATCH_DATA_Entry.COLUMN_NAME_INVALID.equalsIgnoreCase(c.getColumnName(j)))
									{
										match_data.append("0");
									}
									else if(FACT_MATCH_DATA_Entry.COLUMN_NAME_NOTES.equalsIgnoreCase(c.getColumnName(j)))
									{
										match_data.append("\"").append(c.getString(j)).append("\"");
									}
									else
									{
										match_data.append(c.getString(j));
									}
								}
								match_data.append("\n");
							} while(c.moveToNext());
						}
					} finally
					{
						c.close();
					}
					c = db.rawQuery("SELECT * FROM " + FRCScoutingContract.FACT_CYCLE_DATA_Entry.TABLE_NAME, null);
					StringBuilder cycle_data = new StringBuilder(c.getCount() * c.getColumnCount() * 2);
					for(int i = 0; i < c.getColumnCount(); i++)
					{
						if(i > 0)
						{
							cycle_data.append(",");
						}
						cycle_data.append(c.getColumnName(i));
					}
					cycle_data.append("\n");
					try
					{
						if(c.moveToFirst())
						{
							do
							{
								for(int j = 0; j < c.getColumnCount(); j++)
								{
									if(j > 0)
									{
										cycle_data.append(",");
									}
									cycle_data.append(c.getString(j));
								}
								cycle_data.append("\n");
							} while(c.moveToNext());
						}
					} finally
					{
						c.close();
					}
					c = db.rawQuery("SELECT * FROM " + FRCScoutingContract.SCOUT_PIT_DATA_Entry.TABLE_NAME, null);
					StringBuilder pit_data = new StringBuilder(c.getCount() * c.getColumnCount() * 2);
					for(int i = 0; i < c.getColumnCount(); i++)
					{
						if(i > 0)
						{
							pit_data.append("\n");
						}
						pit_data.append(c.getColumnName(i));
					}
					pit_data.append("\n");
					try
					{
						if(c.moveToFirst())
						{
							do
							{
								for(int j = 0; j < c.getColumnCount(); j++)
								{
									if(j > 0)
									{
										pit_data.append(",");
									}
									if(SCOUT_PIT_DATA_Entry.COLUMN_NAME_INVALID.equalsIgnoreCase(c.getColumnName(j)))
									{
										pit_data.append("0");
									}
									else if(SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCOUT_COMMENTS.equalsIgnoreCase(c.getColumnName(j)))
									{
										pit_data.append("\"").append(c.getString(j)).append("\"");
									}
									else if(SCOUT_PIT_DATA_Entry.COLUMN_NAME_CONFIGURATION_ID.equalsIgnoreCase(c.getColumnName(j)))
									{
										String config = configs.get(c.getInt(j));
										if(config == null)
										{
											config = getConfigNameFromID(c.getInt(j), db);
											configs.append(c.getInt(j), config);
										}
										pit_data.append(config);
									}
									else if(SCOUT_PIT_DATA_Entry.COLUMN_NAME_WHEEL_BASE_ID.equalsIgnoreCase(c.getColumnName(j)))
									{
										String base = bases.get(c.getInt(j));
										if(base == null)
										{
											base = getWheelBaseNameFromID(c.getInt(j), db);
											bases.append(c.getInt(j), base);
										}
										pit_data.append(base);
									}
									else if(SCOUT_PIT_DATA_Entry.COLUMN_NAME_WHEEL_TYPE_ID.equalsIgnoreCase(c.getColumnName(j)))
									{
										String type = types.get(c.getInt(j));
										if(type == null)
										{
											type = getWheelTypeNameFromID(c.getInt(j), db);
											types.append(c.getInt(j), type);
										}
										pit_data.append(type);
									}
									else
									{
										pit_data.append(c.getString(j));
									}
								}
								pit_data.append("\n");
							} while(c.moveToNext());
						}
					} finally
					{
						c.close();
					}
					File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
					File folderSharpScout = new File(sd, "/SHARPScout/");
					folderSharpScout.mkdirs();
					File match = new File(folderSharpScout, "matches.csv");
					File cycle = new File(folderSharpScout, "cycles.csv");
					File pits = new File(folderSharpScout, "pits.csv");
					FileOutputStream destination;
					destination = new FileOutputStream(match);
					destination.write(match_data.toString().getBytes());
					destination.close();
					destination = new FileOutputStream(cycle);
					destination.write(cycle_data.toString().getBytes());
					destination.close();
					destination = new FileOutputStream(pits);
					destination.write(pit_data.toString().getBytes());
					destination.close();
					ScoutingDBHelper.getInstance().close();
					return "DB exported to " + sd.getAbsolutePath();
				} catch(Exception e)
				{
					ScoutingDBHelper.getInstance().close();
					ACRA.getErrorReporter().handleException(e);
					return e.toString();
				}
			}
		}

		protected void onPostExecute(String result)
		{
			callback.finish(result);
		}

	}
}
