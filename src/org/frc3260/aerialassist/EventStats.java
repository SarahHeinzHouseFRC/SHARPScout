package org.frc3260.aerialassist;

import android.os.AsyncTask;
import org.frc3260.database.XMLDBParser;
import org.sharp.scouting.ExpandableListItem;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

public class EventStats implements HttpCallback
{
    public List<ExpandableListItem<String>> contents;

    private EventCallback call;
    private List<String> teams;

    public EventStats(EventCallback callback)
    {
        call = callback;
    }

    public void processFromXML(String XML) throws XmlPullParserException, IOException
    {
        contents = new ArrayList<ExpandableListItem<String>>();
        Map<String, List<Map<String, String>>> data;
        data = new TreeMap<String, List<Map<String, String>>>();
        teams = XMLDBParser.extractColumn("team_id", XML);
        List<Map<String, String>> entries = XMLDBParser.extractRows(null, null, XML);

        if(teams.size() > 0)
        {
            HashSet<String> set = new HashSet<String>(teams);
            teams.clear();
            teams.addAll(set);
            for(String team : teams)
            {
                data.put(team, new ArrayList<Map<String, String>>());
            }

            for(Map<String, String> row : entries)
            {
                String t = row.get("team_id");
                List<Map<String, String>> teamData = data.get(t);
                teamData.add(row);
            }
            populateTopScoringTeams(data);
            populateTopAssistTeams(data);
            populateTopCatchingTeams(data);
            populateTopAutoScoringTeams(data);
            populateTopAccuracyTeams(data);
        }
        else
        {
            contents.add(new ExpandableListItem<String>("No data for this event"));
        }
    }

    private void populateTopScoringTeams(Map<String, List<Map<String, String>>> data) throws XmlPullParserException, IOException
    {
        String key = "Top Scoring Teams";
        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<Map<String, String>> entries;

        List<Team_Stat> teamList = new ArrayList<Team_Stat>();

        for(String team : teams)
        {
            entries = data.get(team);
            teamList.add(new Team_Stat(team, Stats.getAvgScore(entries)));
        }

        Collections.sort(teamList, Collections.reverseOrder());

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team #");
        tempList.add("Avg Score");
        list.add(tempList);
        selectable.add(false);

        for(Team_Stat stat : teamList)
        {
            tempList = new ArrayList<String>(2);
            tempList.add(stat.team);
            tempList.add(String.valueOf(stat.stat));
            list.add(tempList);
            selectable.add(true);
        }
        contents.add(new ExpandableListItem<String>(key, list, selectable));

    }

    private void populateTopAutoScoringTeams(Map<String, List<Map<String, String>>> data) throws XmlPullParserException, IOException
    {
        String key = "Top Autonomous Scorers";

        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<Map<String, String>> entries;

        List<Team_Stat> teamList = new ArrayList<Team_Stat>();

        for(String team : teams)
        {
            entries = data.get(team);
            teamList.add(new Team_Stat(team, Stats.getAvgAutoScore(entries)));
        }

        Collections.sort(teamList, Collections.reverseOrder());

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team #");
        tempList.add("Avg Autonomous Score");
        list.add(tempList);
        selectable.add(false);

        for(Team_Stat stat : teamList)
        {
            tempList = new ArrayList<String>(2);
            tempList.add(stat.team);
            tempList.add(String.valueOf(stat.stat));
            list.add(tempList);
            selectable.add(true);
        }
        contents.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private void populateTopAssistTeams(Map<String, List<Map<String, String>>> data) throws XmlPullParserException, IOException
    {
        String key = "Top Assist Points";

        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<Map<String, String>> entries;

        List<Team_Stat> teamList = new ArrayList<Team_Stat>();

        for(String team : teams)
        {
            entries = data.get(team);
            teamList.add(new Team_Stat(team, Stats.getTotalAssistPoints(entries)));
        }

        Collections.sort(teamList, Collections.reverseOrder());

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team #");
        tempList.add("Assist Points");
        list.add(tempList);
        selectable.add(false);

        for(Team_Stat stat : teamList)
        {
            tempList = new ArrayList<String>(2);
            tempList.add(stat.team);
            tempList.add(String.valueOf((int) stat.stat));
            list.add(tempList);
            selectable.add(true);
        }
        contents.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private void populateTopAccuracyTeams(Map<String, List<Map<String, String>>> data) throws XmlPullParserException, IOException
    {
        String key = "Top Accuracy";

        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<Map<String, String>> entries;

        List<Team_Stat> teamList = new ArrayList<Team_Stat>();

        for(String team : teams)
        {
            entries = data.get(team);

            if(Stats.getTotalAttempts(entries) > 5)
            {
                teamList.add(new Team_Stat(team, Stats.getAvgAccuracy(entries)));
            }
        }

        Collections.sort(teamList, Collections.reverseOrder());

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team #");
        tempList.add("Accuracy");
        list.add(tempList);
        selectable.add(false);

        for(Team_Stat stat : teamList)
        {
            tempList = new ArrayList<String>(2);
            tempList.add(stat.team);
            tempList.add(String.valueOf(stat.stat) + "%");
            list.add(tempList);
            selectable.add(true);
        }
        contents.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private void populateTopCatchingTeams(Map<String, List<Map<String, String>>> data) throws XmlPullParserException, IOException
    {
        String key = "Top Catchers";

        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<Map<String, String>> entries;

        List<Team_Stat> teamList = new ArrayList<Team_Stat>();

        for(String team : teams)
        {
            entries = data.get(team);

            Team_Stat stat = new Team_Stat(team, Stats.getTotalCatchPoints(entries));

            if(stat.stat > 0.0)
            {
                teamList.add(stat);
            }
        }

        Collections.sort(teamList, Collections.reverseOrder());

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team #");
        tempList.add("Catch Points");
        list.add(tempList);
        selectable.add(false);

        for(Team_Stat stat : teamList)
        {
            tempList = new ArrayList<String>(2);
            tempList.add(stat.team);
            tempList.add(String.valueOf((int) stat.stat));
            list.add(tempList);
            selectable.add(true);
        }
        contents.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private class Team_Stat implements Comparable<Team_Stat>
    {
        String team;
        float stat;

        public Team_Stat(String team, float stat)
        {
            this.team = team;
            this.stat = stat;
        }

        public int compareTo(Team_Stat another)
        {
            if(stat == another.stat)
            {
                return 0;
            }
            Float c = stat - another.stat;
            if(c < 1.0 && c > 0.0)
            {
                return 1;
            }
            if(c > -1.0 && c < 0.0)
            {
                return -1;
            }
            return c.intValue();
        }
    }

    public interface EventCallback
    {
        public void onResponse(EventStats stats);

        public void onError(Exception e, boolean network);
    }

    public void setCallback(EventCallback callback)
    {
        call = callback;
    }

    public void onResponse(HttpRequestInfo resp)
    {
        try
        {
            AsynchEventStatPopulate pop = new AsynchEventStatPopulate();
            pop.execute(resp.getResponseString());
        }
        catch(Exception e)
        {
            call.onError(e, false);
        }
    }

    public void onError(Exception e)
    {
        call.onError(e, true);
    }

    private class AsynchEventStatPopulate extends AsyncTask<String, Integer, EventStats>
    {
        private Exception ex = null;

        @Override
        protected EventStats doInBackground(String... params)
        {
            try
            {
                processFromXML(params[0]);
            }
            catch(Exception e)
            {
                ex = e;
            }
            return EventStats.this;
        }

        protected void onPostExecute(EventStats stats)
        {
            super.onPostExecute(stats);
            if(ex == null)
            {
                call.onResponse(stats);
            }
            else
            {
                call.onError(ex, false);
            }
        }
    }
}
