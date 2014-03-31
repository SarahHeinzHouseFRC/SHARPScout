package org.frc3260.aerialassist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.frc3260.database.DB;
import org.frc3260.database.FRCScoutingContract.SCOUT_PIT_DATA_Entry;
import org.frc3260.database.PitStats;

public class PitStatsAA extends PitStats
{

    public boolean auto_score_high;
    public boolean auto_score_low;
    public boolean auto_score_hot;
    public boolean auto_score_mobile;
    public boolean truss;
    public boolean launch;
    public boolean active_control;
    public boolean catchBall;
    public boolean score_high;
    public boolean score_low;
    public boolean auto_goalie;
    public int height;

    public PitStatsAA()
    {
        init();
    }

    public void init()
    {
        super.init();
        auto_score_high = false;
        auto_score_low = false;
        auto_score_hot = false;
        auto_score_mobile = false;
        truss = false;
        launch = false;
        active_control = false;
        catchBall = false;
        score_high = false;
        score_low = false;
        height = 0;
    }

    public ContentValues getValues(DB db, SQLiteDatabase database)
    {
        ContentValues vals = super.getValues(db, database);

        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HIGH,
                 auto_score_high ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_LOW, auto_score_low ? 1
                                                                           : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HOT, auto_score_hot ? 1
                                                                           : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_MOBILE,
                 auto_score_mobile ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_GOALIE, auto_goalie ? 1
                                                                           : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_TRUSS, truss ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_LAUNCH_BALL, launch ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_ACTIVE_CONTROL,
                 active_control ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_CATCH, catchBall ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_HIGH, score_high ? 1
                                                                         : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_LOW, score_low ? 1 : 0);
        vals.put(SCOUT_PIT_DATA_Entry.COLUMN_NAME_MAX_HEIGHT, height);

        return vals;
    }

    public void fromCursor(Cursor c, DB db, SQLiteDatabase database)
    {
        super.fromCursor(c, db, database);

        auto_score_high = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HIGH)) != 0;
        auto_score_low = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_LOW)) != 0;
        auto_score_hot = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_HOT)) != 0;
        auto_score_mobile = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_MOBILE)) != 0;
        auto_goalie = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_AUTO_GOALIE)) != 0;
        truss = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_TRUSS)) != 0;
        launch = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_LAUNCH_BALL)) != 0;
        active_control = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_ACTIVE_CONTROL)) != 0;
        catchBall = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_CATCH)) != 0;
        score_high = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_HIGH)) != 0;
        score_low = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_SCORE_LOW)) != 0;
        height = c.getInt(c.getColumnIndexOrThrow(SCOUT_PIT_DATA_Entry.COLUMN_NAME_MAX_HEIGHT));
    }
}
