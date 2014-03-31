package org.sharp.scouting;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import org.frc3260.database.DB;
import org.frc3260.database.DBSyncService.LocalBinder;

public class MainMenuSelection
{

    public static final int HELPDIALOG = 74920442;

    private static LocalBinder mBinder;

    public static boolean onOptionsItemSelected(MenuItem item, Activity context)
    {
        switch(item.getItemId())
        {
            case R.id.settingsitem:
                openSettings(context);
                return true;
            case R.id.refreshMatchesItem:
                refresh(context);
                return true;
            case R.id.helpitem:
                showHelp(context);
                return true;
            case R.id.exportItem:
                exportDB(context);
                return true;
            case R.id.exitItem:
                exit(context);
                return true;
            case R.id.syncItem:
                return forceSync(context);
            default:
                return false;
        }
    }

    public static void openSettings(Activity context)
    {
        Intent intent = new Intent(context.getBaseContext(), Prefs.class);
        context.startActivityForResult(intent, Prefs.PREFS_ACTIVITY_CODE);
    }

    public static void refresh(Activity context)
    {
        if(context instanceof DataActivity)
        {
            DataActivity act = (DataActivity) context;
            act.refreshCurrentTab();
        }
        else if(context instanceof MatchStatsActivity)
        {
            MatchStatsActivity act = (MatchStatsActivity) context;
            act.refreshStats();
        }
        else
        {
            MatchSchedule schedule = new MatchSchedule();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getBaseContext());
            schedule.updateSchedule(prefs.getString("eventPref", "Buckeye Regional"), context, true);
        }
    }

    public static void showHelp(Activity context)
    {
        context.showDialog(HELPDIALOG);
    }

    public static void setRefreshItem(Menu menu, int item)
    {
        MenuItem i = menu.findItem(R.id.refreshMatchesItem);
        i.setTitle(item);
    }

    public static void exportDB(Activity context)
    {
        DB.exportToCSV(context);
    }

    public static void exit(Activity context)
    {
        if(context instanceof DashboardActivity)
        {
            context.finish();
        }
        else
        {
            Intent intent = new Intent(context, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("ExitApp", true);
            context.startActivity(intent);
            context.finish();
        }
    }

    public static void setBinder(LocalBinder binder)
    {
        mBinder = binder;
    }

    public static boolean forceSync(Activity context)
    {
        if(mBinder != null)
        {
            mBinder.forceSync();

            return true;
        }
        return false;
    }
}
