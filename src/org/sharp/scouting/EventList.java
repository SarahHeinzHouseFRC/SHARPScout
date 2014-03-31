package org.sharp.scouting;

import android.content.Context;
import org.acra.ACRA;
import org.frc3260.database.DB;
import org.frc3260.database.DBSyncService;
import org.frc3260.database.XMLDBParser;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EventList implements HttpCallback
{
    private static final String FILENAME = "FRCEventList.xml";
    private Context _parent;
    private DBSyncService.LocalBinder _binder;
    private DB database;

    private EventCallback _callback = null;

    public EventList(Context parent, DBSyncService.LocalBinder binder)
    {
        _parent = parent;
        _binder = binder;

        database = new DB(parent, binder);
        if(getEvents() == "")
        {
            downloadEventsList(null);
        }
    }

    public void downloadEventsList(EventCallback callback)
    {
        _callback = callback;
        database.getEventList(this);
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
                if(_callback != null)
                {
                    _callback.eventsUpdated(getEventList());
                }
            }
        }
        catch(Exception e)
        {
            if(_callback != null)
            {
                _callback.eventsUpdated(null);
            }
        }
    }

    public void onError(Exception e)
    {
        if(_callback != null)
        {
            _callback.eventsUpdated(null);
        }
    }

    private String getEvents()
    {
        try
        {
            BufferedInputStream bis = new BufferedInputStream(_parent.openFileInput(FILENAME));
            byte[] buffer = new byte[bis.available()];
            bis.read(buffer, 0, buffer.length);
            return new String(buffer);
        }
        catch(Exception e)
        {
            return "";
        }
    }

    public List<String> getEventList()
    {
        String events = getEvents();
        if(!events.equals(""))
        {
            try
            {
                List<String> eventList = XMLDBParser.extractColumn("event_name", events);

                return eventList;
            }
            catch(Exception e)
            {
                ACRA.getErrorReporter().handleException(e);
            }
        }

        return new ArrayList<String>();
    }

    public String getMatchScheduleURL(String event)
    {
        String ret = "";

        String events = getEvents();

        try
        {
            ret = XMLDBParser.dataLookupByValue("event_name", event, "match_url", events);
        }
        catch(Exception e)
        {
            ACRA.getErrorReporter().handleException(e);
        }

        return ret;
    }

    public interface EventCallback
    {
        public void eventsUpdated(List<String> events);
    }

}
