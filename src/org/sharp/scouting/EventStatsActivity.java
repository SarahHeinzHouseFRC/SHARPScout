package org.sharp.scouting;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;
import org.acra.ACRA;
import org.frc3260.aerialassist.EventStats;
import org.frc3260.database.DB;

import java.util.List;

public class EventStatsActivity extends Activity implements
        EventStats.EventCallback
{

    private static String eventName;
    private static DB db;
    private static EventStats stats;
    private ExpandableListView eventStatView;
    private List<ExpandableListItem<String>> items;
    private TextListAdapter adapter;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventstats);
        db = DataActivity.db;

        eventStatView = (ExpandableListView) findViewById(R.id.event_data_list);
        DataActivity.eventTab = this;

        String ev = Prefs.getEvent(getApplicationContext(),
                                   "Buckeye Regional");
        if(eventName != null && stats != null && eventName.compareTo(ev) == 0)
        {
            items = stats.contents;
            adapter = new TextListAdapter(this, items);
            eventStatView.setAdapter(adapter);
        }

        eventStatView.setOnChildClickListener(new TeamClickListener());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String ev = Prefs.getEvent(getApplicationContext(),
                                   "Buckeye Regional");
        if(eventName == null || stats == null || eventName.compareTo(ev) != 0)
        {
            eventName = ev;
            refreshStats();
        }

        DataActivity.mTabs.setTitle("Stats for " + eventName);

    }

    public void refreshStats()
    {
        eventName = Prefs.getEvent(getApplicationContext(), "Buckeye Regional");
        db.getEventStats(eventName, this);
        pd = ProgressDialog.show(this, "Busy", "Retrieving Event Stats", false);
        pd.setCancelable(true);
    }

    public void onResponse(EventStats stats)
    {
        pd.dismiss();
        EventStatsActivity.stats = stats;
        items = stats.contents;
        adapter = new TextListAdapter(this, items);
        eventStatView.setAdapter(adapter);
        Toast.makeText(getBaseContext(), "Event Stats Updated", Toast.LENGTH_SHORT).show();
        DataActivity.mTabs.setTitle("Stats for " + eventName);
    }

    public void onError(Exception e, boolean network)
    {
        pd.dismiss();

        if(network)
        {
            Toast.makeText(getBaseContext(), "Network Error", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getBaseContext(), "Error Processing Network Response", Toast.LENGTH_SHORT).show();
        }

        ACRA.getErrorReporter().handleException(e);
    }

    private class TeamClickListener implements ExpandableListView.OnChildClickListener
    {
        @SuppressWarnings("rawtypes")
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
        {
            ExpandableListAdapter adapt = parent.getExpandableListAdapter();
            Object l = adapt.getChild(groupPosition, childPosition);
            if(l instanceof List && ((List) l).size() > 0)
            {
                Object t = ((List) l).get(0);
                int team = Integer.valueOf(t.toString());
                DataActivity.mTabs.setCurrentTab(DataActivity.TEAM_TAB);
                DataActivity.teamTab.setTeam(team);
            }
            return true;
        }
    }
}
