package org.sharp.scouting;

import android.content.Context;
import android.widget.Toast;
import org.acra.ACRA;
import org.frc3260.database.DB;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;
import org.sigmond.net.HttpUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;

public class MatchSchedule implements HttpCallback
{

    private static final String FILENAME = "FRCscoutingschedule";

    private boolean offseason = false;
    private boolean toastComplete;

    private DB db;

    private Context _parent;

    public void updateSchedule(String event, Context parent, boolean toastWhenComplete)
    {
        HttpUtils utils = new HttpUtils();
        _parent = parent;
        toastComplete = toastWhenComplete;

        db = new DB(_parent, null);

        String url = db.getURLFromEventName(event);
        if(url != null)
        {
            utils.doGet(url, this);
        }
    }

    public void onResponse(HttpRequestInfo resp)
    {
        try
        {
            String r = resp.getResponseString();

            if(resp.getResponse().getStatusLine().toString().contains("200"))
            {
                FileOutputStream fos = _parent.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write(r.getBytes());
                fos.close();
            }
            else if(offseason)
            {
                FileOutputStream fos = _parent.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write("No Schedule".getBytes());
                fos.close();
            }

            if(toastComplete)
            {
                Toast.makeText(_parent, "Schedule Successfully Updated", Toast.LENGTH_SHORT).show();
            }
        }
        catch(Exception e)
        {
            if(toastComplete)
            {
                Toast.makeText(_parent, "Error Saving Schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onError(Exception e)
    {
        try
        {
            if(toastComplete)
            {
                Toast.makeText(_parent, "Error Downloading Schedule", Toast.LENGTH_SHORT).show();
            }
            if(offseason)
            {
                FileOutputStream fos = _parent.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write("No Schedule".getBytes());
                fos.close();
            }
        }
        catch(Exception es)
        {
            es.printStackTrace();

            ACRA.getErrorReporter().handleException(es);
        }

    }

    public String getTeam(int match, String pos, Context parent)
    {
        return getTeam(match, pos, parent, "");
    }

    public String getTeam(int match, String pos, Context parent, String defaultVal)
    {
        try
        {
            String schedule = getSchedule(parent);
            if(schedule.compareTo("No Schedule") == 0)
            {
                return defaultVal;
            }

            String m = "M</TD>\n<TD align=center style=\"font-family:arial;font-weight:normal;font-size:9.0pt\">" + String.valueOf(match) + "</TD>";
            int i = schedule.indexOf(m);
            int j = 0;
            if(pos.equalsIgnoreCase("red 1"))
            {
                j = 1;
            }
            else if(pos.equalsIgnoreCase("red 2"))
            {
                j = 2;
            }
            else if(pos.equalsIgnoreCase("red 3"))
            {
                j = 3;
            }
            else if(pos.equalsIgnoreCase("blue 1"))
            {
                j = 4;
            }
            else if(pos.equalsIgnoreCase("blue 2"))
            {
                j = 5;
            }
            else if(pos.equalsIgnoreCase("blue 3"))
            {
                j = 6;
            }
            else
            {
                return defaultVal;
            }
            i = i + 85;

            while(j > 0)
            {
                i = schedule.indexOf(">", i + 1);
                i = schedule.indexOf(">", i + 1);
                j--;
            }

            int k = schedule.indexOf("<", i);

            String ret = schedule.substring(i + 1, k).trim();
            if(ret.length() < 10 && Integer.valueOf(ret) > 0)
            {
                return ret;
            }
            else
            {
                return defaultVal;
            }

        }
        catch(Exception e)
        {
            return defaultVal;
        }
    }

    private String getSchedule(Context parent)
    {
        try
        {
            BufferedInputStream bis = new BufferedInputStream(parent.openFileInput(FILENAME));
            byte[] buffer = new byte[bis.available()];
            bis.read(buffer, 0, buffer.length);
            return new String(buffer);
        }
        catch(Exception e)
        {
            if(!e.toString().contains("FileNotFoundException"))
            {
                ACRA.getErrorReporter().handleException(e);
            }

            return "";
        }
    }

    public boolean isValid(Context parent)
    {
        String schedule = getSchedule(parent);
        if(schedule.contains(">1</TD>"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
