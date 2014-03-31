package org.frc3260.aerialassist;

import android.os.AsyncTask;
import org.acra.ACRA;
import org.frc3260.database.XMLDBParser;
import org.sharp.scouting.ExpandableListItem;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

public class TeamStats implements HttpCallback
{
    public List<ExpandableListItem<String>> teamInfo;
    public String pits;
    public String matches;
    private TeamCallback call;
    private int team;
    private String eventName;
    private List<String> matchNums;

    public TeamStats(TeamCallback callback, int teamId, String event)
    {
        call = callback;
        team = teamId;
        eventName = event;
    }

    public void setCallback(TeamCallback callback)
    {
        call = callback;
    }

    public void processFromXML(String XML) throws XmlPullParserException, IOException
    {
        String[] split = XML.split("\\<\\/result\\>");
        for(int i = 0; i < split.length; i++)
        {
            if(split[i].trim().length() > 0)
            {
                split[i] = (split[i] + "</result>\n").trim();
                int j = split[i].indexOf("<result");
                j = split[i].indexOf("table", j + 1);
                j = split[i].indexOf("=", j + 1);
                j = split[i].indexOf("\"", j + 1);
                int k = split[i].indexOf("\"", j + 1);
                String table = split[i].substring(j + 1, k);
                if(table.trim().compareToIgnoreCase("fact_match_data") == 0)
                {
                    matches = split[i];
                    if(i > 0)
                    {
                        matches = "<?xml version=\"1.0\"?>\n" + matches;
                    }
                }
                else if(table.trim().compareToIgnoreCase("scout_pit_data") == 0)
                {
                    pits = split[i];
                    if(i > 0)
                    {
                        pits = "<?xml version=\"1.0\"?>\n" + pits;
                    }
                }
            }
        }

        matchNums = XMLDBParser.extractColumn("match_id", matches);

        teamInfo = new ArrayList<ExpandableListItem<String>>();
        populateTextStats(pits, matches);
        populateMatchStats(matches);
        populateGraphList();
    }

    private void populateGraphList()
    {
        String key = "Graphs";
        List<List<String>> list = new ArrayList<List<String>>();
        List<Boolean> selectable = new ArrayList<Boolean>();

        List<String> tempList = new ArrayList<String>(1);
        tempList.add("All");
        list.add(tempList);
        selectable.add(true);

        for(String name : GraphStats.graphList)
        {
            tempList = new ArrayList<String>(1);
            tempList.add(name);
            list.add(tempList);
            selectable.add(true);
        }
        teamInfo.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private void populateTextStats(String pitsXML, String matchXML)
            throws XmlPullParserException, IOException
    {
        String key = "General Team Info";
        List<List<String>> list = new ArrayList<List<String>>();

        List<String> tempList = new ArrayList<String>(2);
        tempList.add("Team:");
        tempList.add(String.valueOf(team));
        list.add(tempList);

        // average score overall
        List<Map<String, String>> avgInfo = XMLDBParser.extractRows(null, null, matchXML);

        if(avgInfo.size() > 0)
        {
            // average score this event
            List<Map<String, String>> avgInfoEvent = XMLDBParser.extractRows("event_id", eventName, matchXML);
            tempList = new ArrayList<String>(2);
            tempList.add("Avg Score for " + eventName + ":");
            tempList.add(String.valueOf(Stats.getAvgScore(avgInfoEvent)));
            list.add(tempList);

            // average auto score this event
            tempList = new ArrayList<String>(2);
            tempList.add("Avg Auto Score for " + eventName + ":");
            tempList.add(String.valueOf(Stats.getAvgAutoScore(avgInfoEvent)));
            list.add(tempList);

            // average accuracy this event
            tempList = new ArrayList<String>(2);
            tempList.add("Avg Accuracy for " + eventName + ":");
            tempList.add(String.valueOf(Stats.getAvgAccuracy(avgInfoEvent)) + "%");
            list.add(tempList);

        }

        List<Map<String, String>> mp = XMLDBParser.extractRows(null, null,
                                                               pitsXML);
        if(mp.size() > 0)
        {
            Map<String, String> pitInfo = mp.get(0);
            tempList = new ArrayList<String>(2);
            if(pitInfo.size() > 0)
            {
                tempList = new ArrayList<String>(2);
                tempList.add("Base:");
                tempList.add(pitInfo.get("configuration_id"));
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Wheel Type:");
                tempList.add(pitInfo.get("wheel_type_id"));
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Wheel Setup:");
                tempList.add(pitInfo.get("wheel_base_id"));
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Autonomous Mode:");
                tempList.add(pitInfo.get("autonomous_mode").compareTo("1") == 0 ? "Yes"
                                                                                : "No");
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Score High:");
                tempList.add(pitInfo.get("score_high").compareTo("1") == 0 ? "Yes"
                                                                           : "No");
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Score Low:");
                tempList.add(pitInfo.get("score_low").compareTo("1") == 0 ? "Yes"
                                                                          : "No");
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Maximum Height:");
                tempList.add(pitInfo.get("max_height") + " inches");
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Pit Scout Notes:");
                tempList.add(pitInfo.get("scout_comments"));
                list.add(tempList);
                tempList = new ArrayList<String>(2);
                tempList.add("Pit Data Collection Time:");
                tempList.add(pitInfo.get("timestamp"));
                list.add(tempList);
            }
            else
            {
                tempList.add("Team has yet to be scouted.");
                list.add(tempList);
            }
        }
        else
        {
            tempList = new ArrayList<String>(2);
            tempList.add("No pit info collected about this team.");
            list.add(tempList);
        }
        if(list.size() < 3)
        {
            key = "No data collected about this team.";
            list.clear();
        }
        List<Boolean> selectable = new ArrayList<Boolean>(list.size());
        for(int i = 0; i < list.size(); i++)
        {
            selectable.add(false);
        }

        teamInfo.add(new ExpandableListItem<String>(key, list, selectable));
    }

    private void populateMatchStats(String XML) throws XmlPullParserException,
            IOException
    {
        int numMatches = matchNums.size();

        if(numMatches > 0)
        {
            String key = "Matches (" + numMatches + ")";

            List<List<String>> list = new ArrayList<List<String>>();
            List<Boolean> selectable = new ArrayList<Boolean>();

            List<Map<String, String>> info = XMLDBParser.extractRows(null,
                                                                     null, XML);

            String event;
            Map<String, String> cur;
            List<String> events = XMLDBParser.extractColumn("event_id", XML);
            HashSet<String> s = new HashSet<String>(events);
            events.clear();
            events.addAll(s);
            List<String> tempList;

            for(int i = 0; i < events.size(); i++)
            {
                tempList = new ArrayList<String>(2);
                event = events.get(i);
                tempList.add(event + "");
                tempList.add("");
                list.add(tempList);
                selectable.add(false);
                List<Match_Stat> matches = new ArrayList<TeamStats.Match_Stat>();
                for(int j = 0; j < numMatches; j++)
                {
                    cur = info.get(j);
                    if(cur.get("event_id").compareToIgnoreCase(event) == 0)
                    {
                        try
                        {
                            matches.add(new Match_Stat(Integer.valueOf(matchNums.get(j)), String.valueOf(Stats.getMatchScore(cur))));
                        }
                        catch(NumberFormatException e)
                        {
                            ACRA.getErrorReporter().handleException(e);
                        }
                    }
                }
                Collections.sort(matches);
                for(Match_Stat match : matches)
                {
                    tempList = new ArrayList<String>(2);
                    tempList.add("Match: " + String.valueOf(match.match_id));
                    tempList.add("Score: " + match.score);
                    list.add(tempList);
                    selectable.add(true);
                }
            }

            teamInfo.add(new ExpandableListItem<String>(key, list, selectable));
        }
    }

    public void onResponse(HttpRequestInfo resp)
    {
        try
        {
            AsynchTeamStatPopulate pop = new AsynchTeamStatPopulate();
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

    public interface TeamCallback
    {
        public void onResponse(TeamStats stats);

        public void onError(Exception e, boolean network);
    }

    private class AsynchTeamStatPopulate extends
            AsyncTask<String, Integer, TeamStats>
    {
        private Exception ex = null;

        @Override
        protected TeamStats doInBackground(String... params)
        {
            try
            {
                processFromXML(params[0]);
            }
            catch(Exception e)
            {
                ex = e;
            }
            return TeamStats.this;
        }

        protected void onPostExecute(TeamStats stats)
        {
            super.onPostExecute(stats);
            try
            {
                if(ex == null)
                {
                    call.onResponse(stats);
                }
                else
                {
                    call.onError(ex, false);
                }
            }
            catch(Exception e)
            {
                call.onError(e, false);
            }
        }
    }

    private class Match_Stat implements Comparable<Match_Stat>
    {
        int match_id;
        String score;

        public Match_Stat(int match, String sc)
        {
            match_id = match;
            score = sc;
        }

        public int compareTo(Match_Stat arg0)
        {
            return match_id - arg0.match_id;
        }
    }
}
