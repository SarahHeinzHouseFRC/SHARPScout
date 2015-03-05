/*
 * Copyright 2015 Daniel Logan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frc836.database;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import org.frc836.database.FRCScoutingContract.*;
import org.growingstems.scouting.DashboardActivity;
import org.growingstems.scouting.Prefs;
import org.growingstems.scouting.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;
import org.sigmond.net.HttpUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.*;

public class DBSyncService extends Service
{

    private static final String FILENAME = "DBtimestamp";

    private final IBinder mBinder = new LocalBinder();

    private HttpUtils utils;

    private String password;

    private Handler mTimerTask;
    private SyncDataTask dataTask;

    private Timestamp lastSync;

    private List<Map<String, String>> outgoing;

    private boolean syncForced = false;
    private boolean initSync = false;
    private boolean running = true;

    private static String version;

    private String dbVersion;

    private static final int notifyID = 74392;

    private static volatile boolean syncInProgress = false;

    private static enum Action
    {
        NOTHING, INSERT, UPDATE, DELETE
    }

    ;

    private static final int DELAY = 60000;

    @Override
    public void onCreate()
    {
        super.onCreate();
        // loadTimestamp();
        initSync = !loadTimestamp();
        password = "";

        mTimerTask = new Handler();

        dataTask = new SyncDataTask();

        password = Prefs.getSavedPassword(getApplicationContext());

        outgoing = new ArrayList<Map<String, String>>();

        utils = new HttpUtils();

        DBSyncService.version = getString(R.string.VersionID);

        mTimerTask.post(dataTask);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.service_notify_title))
                .setContentText(getString(R.string.service_notify_text))
                .setOngoing(true).setWhen(0);
        Intent notifyIntent = new Intent(this, DashboardActivity.class);
        notifyIntent.setAction(Intent.ACTION_MAIN);
        notifyIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notifyIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentIntent(intent);

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
                notifyID, mBuilder.build());
    }

    private boolean loadTimestamp()
    {
        try
        {
            BufferedInputStream bis = new BufferedInputStream(
                    openFileInput(FILENAME));
            byte[] buffer = new byte[bis.available()];
            bis.read(buffer, 0, buffer.length);
            lastSync = new Timestamp(Long.valueOf(new String(buffer)));
            return true;
        }
        catch (Exception e)
        {
            if (lastSync == null)
            {
                lastSync = new Timestamp(0);
            }
            return false;
        }
    }

    private void updateTimeStamp(long time)
    {
        lastSync = new Timestamp(time);
        saveTimestamp();
    }

    private void saveTimestamp()
    {
        try
        {
            FileOutputStream fos = openFileOutput(FILENAME,
                    Context.MODE_PRIVATE);
            fos.write(String.valueOf(lastSync.getTime()).getBytes());
            fos.close();
        }
        catch (Exception e)
        {
        }
    }

    private boolean checkVersion(String version)
    {
        String verCode = version.substring(0, version.lastIndexOf("."));
        String localVersion = DBSyncService.version;
        localVersion = localVersion.substring(0, localVersion.lastIndexOf("."));
        dbVersion = version;
        return verCode.trim().compareToIgnoreCase(localVersion.trim()) == 0;
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        super.onUnbind(intent);
        return false;
    }

    @Override
    public void onDestroy()
    {
        mTimerTask.removeCallbacks(dataTask);
        running = false;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(notifyID);
        super.onDestroy();
    }

    public class LocalBinder extends Binder
    {
        public DBSyncService getService()
        {
            return DBSyncService.this;
        }

        public void setPassword(String pass)
        {
            password = pass;
        }

        public void startSync()
        {
            mTimerTask.removeCallbacks(dataTask);
            mTimerTask.post(dataTask);
        }

        public void forceSync()
        {
            mTimerTask.removeCallbacks(dataTask);
            syncForced = true;
            mTimerTask.post(dataTask);
        }

        public void initSync()
        {
            mTimerTask.removeCallbacks(dataTask);
            initSync = true;
            mTimerTask.post(dataTask);
        }
    }

    // start an initial sync
    private void initialSync()
    {
        Map<String, String> args = new HashMap<String, String>();

        password = Prefs.getSavedPassword(getApplicationContext());
        args.put("password", password);
        args.put("type", "fullsync");
        args.put("version", version);
        utils.doPost(Prefs.getScoutingURLNoDefault(getApplicationContext()),
                args, new SyncCallback());
    }

    // task to process a sync response
    private class ProcessData extends AsyncTask<String, Integer, Integer>
    {

        @Override
        protected Integer doInBackground(String... params)
        {
            if (!running)
            {
                return null;
            }
            try
            {

                JSONObject json = new JSONObject(params[0]);

                if (!checkVersion(json.getString("version")))
                {
                    return -2;
                }

                processConfig(json
                        .getJSONArray(CONFIGURATION_LU_Entry.TABLE_NAME));

                processEvents(json.getJSONArray(EVENT_LU_Entry.TABLE_NAME));

                if (!running)
                {
                    return null;
                }
                processMatches(json.getJSONArray(MatchStatsStruct.TABLE_NAME));

                processNotes(json.getJSONArray(NOTES_OPTIONS_Entry.TABLE_NAME));

                processRobots(json.getJSONArray(ROBOT_LU_Entry.TABLE_NAME));

                if (!running)
                {
                    return null;
                }
                processPits(json.getJSONArray(PitStats.TABLE_NAME));

                processWheelBase(json
                        .getJSONArray(WHEEL_BASE_LU_Entry.TABLE_NAME));

                processWheelType(json
                        .getJSONArray(WHEEL_TYPE_LU_Entry.TABLE_NAME));

                processPositions(json
                        .getJSONArray(POSITION_LU_Entry.TABLE_NAME));

                updateTimeStamp(json.getLong("timestamp"));

                if (!running)
                {
                    return null;
                }
                sendMatches();
                sendPits();

            }
            catch (Exception e)
            {
                return -1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result)
        {
            super.onPostExecute(result);
            if (!running)
            {
                return;
            }
            if (DB.debug)
            {
                Toast.makeText(getApplicationContext(),
                        "Processed Response: " + result, Toast.LENGTH_SHORT)
                        .show();
            }
            synchronized (outgoing)
            {
                if (!outgoing.isEmpty())
                {
                    if (DB.debug)
                    {
                        Toast.makeText(getApplicationContext(),
                                "Sending " + outgoing.size() + " records",
                                Toast.LENGTH_SHORT).show();
                    }
                    utils.doPost(Prefs
                                    .getScoutingURLNoDefault(getApplicationContext()),
                            outgoing.get(0), new ChangeResponseCallback());
                }
                else
                {
                    if (syncForced)
                    {
                        syncForced = false;
                        if (result == -2)
                        {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Vesion does not match with Database. Database using version: "
                                            + dbVersion, Toast.LENGTH_LONG)
                                    .show();
                        }
                        else if (result < 0)
                        {
                            Toast.makeText(getApplicationContext(),
                                    "Error Syncing with Database",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(),
                                    "Synced with Database", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                    syncInProgress = false;
                    mTimerTask.postDelayed(dataTask, Prefs
                            .getMilliSecondsBetweenSyncs(
                                    getApplicationContext(), DELAY));
                }
            }

        }
    }

    // callback for initial sync request. Expects JSON response
    private class SyncCallback implements HttpCallback
    {

        public void onResponse(HttpRequestInfo resp)
        {
            ProcessData dataProc = new ProcessData();
            dataProc.execute(resp.getResponseString());
        }

        public void onError(Exception e)
        {
            if (!running)
            {
                return;
            }
            if (syncForced)
            {
                syncForced = false;
                Toast.makeText(getApplicationContext(),
                        "Error syncing with Database", Toast.LENGTH_SHORT)
                        .show();
            }
            syncInProgress = false;
            mTimerTask.postDelayed(dataTask, Prefs.getMilliSecondsBetweenSyncs(
                    getApplicationContext(), DELAY));
        }
    }

    // callback for updates sent to server
    private class ChangeResponseCallback implements HttpCallback
    {

        public void onResponse(HttpRequestInfo resp)
        {
            ChangeResponseProcess dataProc = new ChangeResponseProcess();
            if (DB.debug)
            {
                Toast.makeText(getApplicationContext(),
                        "Response to submission:" + resp.getResponseString(),
                        Toast.LENGTH_SHORT).show();
            }
            dataProc.execute(resp);
        }

        public void onError(Exception e)
        {
            if (!running)
            {
                return;
            }
            if (syncForced)
            {
                syncForced = false;
                Toast.makeText(getApplicationContext(),
                        "Error syncing with Database", Toast.LENGTH_SHORT)
                        .show();
            }
            syncInProgress = false;
            mTimerTask.postDelayed(dataTask, Prefs.getMilliSecondsBetweenSyncs(
                    getApplicationContext(), DELAY));
        }

    }

    // processes responses from server for updates sent
    private class ChangeResponseProcess extends
            AsyncTask<HttpRequestInfo, Integer, Integer>
    {

        @Override
        protected Integer doInBackground(HttpRequestInfo... params)
        {
            if (!running)
            {
                return null;
            }
            int match = -1, event = -1, team = -1, practice = -1;
            Map<String, String> sent = params[0].getParams();
            if (params[0].getResponseString().contains("Failed"))
            {
                return null;
            }
            synchronized (outgoing)
            {
                for (int i = 0; i < outgoing.size(); i++)
                {
                    Map<String, String> args = outgoing.get(i);

                    // TODO could probably be abstracted further
                    if (sent.get("type").equalsIgnoreCase("match")
                            && sent.get(MatchStatsStruct.COLUMN_NAME_EVENT_ID)
                            .equalsIgnoreCase(
                                    args.get(MatchStatsStruct.COLUMN_NAME_EVENT_ID))
                            && sent.get(MatchStatsStruct.COLUMN_NAME_MATCH_ID)
                            .equalsIgnoreCase(
                                    args.get(MatchStatsStruct.COLUMN_NAME_MATCH_ID))
                            && sent.get(MatchStatsStruct.COLUMN_NAME_TEAM_ID)
                            .equalsIgnoreCase(
                                    args.get(MatchStatsStruct.COLUMN_NAME_TEAM_ID))
                            && sent.get(
                            MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH)
                            .equalsIgnoreCase(
                                    args.get(MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH)))
                    {

                        match = Integer.valueOf(sent
                                .get(MatchStatsStruct.COLUMN_NAME_MATCH_ID));
                        event = Integer.valueOf(sent
                                .get(MatchStatsStruct.COLUMN_NAME_EVENT_ID));
                        team = Integer.valueOf(sent
                                .get(MatchStatsStruct.COLUMN_NAME_TEAM_ID));
                        practice = Integer
                                .valueOf(sent
                                        .get(MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH));
                        outgoing.remove(i);
                        break;
                    }
                    else if (sent.get("type").equalsIgnoreCase("pits")
                            && sent.get(PitStats.COLUMN_NAME_TEAM_ID)

                            .equalsIgnoreCase(
                                    args.get(PitStats.COLUMN_NAME_TEAM_ID)))
                    {
                        team = Integer.valueOf(sent
                                .get(PitStats.COLUMN_NAME_TEAM_ID));
                        outgoing.remove(i);
                        break;
                    }
                }
            }

            String[] r = params[0].getResponseString().split(",");
            synchronized (ScoutingDBHelper.lock)
            {
                SQLiteDatabase db = ScoutingDBHelper.getInstance()
                        .getWritableDatabase();
                ContentValues values = new ContentValues();

                // TODO could probably be abstracted further
                if (match > 0)
                { // match was updated
                    values.put(MatchStatsStruct.COLUMN_NAME_ID,
                            Integer.valueOf(r[0].trim()));
                    values.put(MatchStatsStruct.COLUMN_NAME_TIMESTAMP,
                            DB.dateParser.format(new Date(Long.valueOf(r[1]
                                    .trim()) * 1000)));
                    values.put(MatchStatsStruct.COLUMN_NAME_INVALID, 0);

                    String[] selectionArgs = {String.valueOf(event),
                            String.valueOf(match), String.valueOf(team),
                            String.valueOf(practice)};
                    db.update(
                            MatchStatsStruct.TABLE_NAME,
                            values,
                            MatchStatsStruct.COLUMN_NAME_EVENT_ID
                                    + "=? AND "
                                    + MatchStatsStruct.COLUMN_NAME_MATCH_ID
                                    + "=? AND "
                                    + MatchStatsStruct.COLUMN_NAME_TEAM_ID
                                    + "=? AND "
                                    + MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH
                                    + "=?", selectionArgs);
                }
                else if (team > 0)
                { // pits were updated
                    values.put(PitStats.COLUMN_NAME_ID,
                            Integer.valueOf(r[0].trim()));
                    values.put(PitStats.COLUMN_NAME_TIMESTAMP, DB.dateParser
                            .format(new Date(Long.valueOf(r[1].trim()) * 1000)));
                    values.put(PitStats.COLUMN_NAME_INVALID, 0);

                    String[] selectionArgs = {String.valueOf(team)};
                    db.update(PitStats.TABLE_NAME, values,
                            PitStats.COLUMN_NAME_TEAM_ID + "=?", selectionArgs);
                }
                ScoutingDBHelper.getInstance().close();
            }

            return null;
        }

        protected void onPostExecute(Integer result)
        {
            super.onPostExecute(result);
            if (!running)
            {
                return;
            }
            synchronized (outgoing)
            {
                if (!outgoing.isEmpty())
                {
                    utils.doPost(Prefs
                                    .getScoutingURLNoDefault(getApplicationContext()),
                            outgoing.get(0), new ChangeResponseCallback());
                }
                else
                {
                    if (syncForced)
                    {
                        syncForced = false;
                        Toast.makeText(getApplicationContext(),
                                "Synced with Database", Toast.LENGTH_SHORT)
                                .show();
                    }
                    syncInProgress = false;
                    mTimerTask.postDelayed(dataTask, Prefs
                            .getMilliSecondsBetweenSyncs(
                                    getApplicationContext(), DELAY));
                }
            }
        }

    }

    // task to perform periodic sync
    private class SyncDataTask implements Runnable
    {

        public void run()
        {
            if (DB.debug)
            {
                Toast.makeText(getApplicationContext(), "Starting Sync",
                        Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(),
                        "Sync Forced: " + syncForced, Toast.LENGTH_SHORT)
                        .show();
                Toast.makeText(getApplicationContext(),
                        "Sync in Progress: " + syncInProgress,
                        Toast.LENGTH_SHORT).show();
            }

            if (syncInProgress || !running)
            {
                return;
            }

            if (!syncForced
                    && !Prefs.getAutoSync(getApplicationContext(), true))
            {
                mTimerTask.postDelayed(dataTask, Prefs
                        .getMilliSecondsBetweenSyncs(getApplicationContext(),
                                DELAY));
                return;
            }

            syncInProgress = true;

            if (syncForced || initSync)
            {
                initSync = false;
                initialSync();
                return;
            }

            Map<String, String> args = new HashMap<String, String>();

            args.put("password", password);
            args.put("type", "sync");
            args.put("timestamp", String.valueOf(lastSync.getTime()));
            args.put("version", version);
            utils.doPost(
                    Prefs.getScoutingURLNoDefault(getApplicationContext()),
                    args, new SyncCallback());
        }
    }

    private void processConfig(JSONArray config)
    {
        try
        {
            for (int i = 0; i < config.length(); i++)
            {
                JSONObject row = config.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(CONFIGURATION_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(CONFIGURATION_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(CONFIGURATION_LU_Entry.COLUMN_NAME_ID));
                vals.put(
                        CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC,
                        row.getString(CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC));
                vals.put(
                        CONFIGURATION_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(CONFIGURATION_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {CONFIGURATION_LU_Entry.COLUMN_NAME_CONFIGURATION_DESC};
                String[] where = {vals
                        .getAsString(CONFIGURATION_LU_Entry.COLUMN_NAME_ID)};
                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(
                            CONFIGURATION_LU_Entry.TABLE_NAME,
                            projection, // select
                            CONFIGURATION_LU_Entry.COLUMN_NAME_ID + "=?",
                            where, null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(CONFIGURATION_LU_Entry.TABLE_NAME, vals,
                                        CONFIGURATION_LU_Entry.COLUMN_NAME_ID
                                                + " = ?", where);
                                break;
                            case INSERT:
                                db.insert(CONFIGURATION_LU_Entry.TABLE_NAME, null,
                                        vals);
                                break;
                            case DELETE:
                                db.delete(CONFIGURATION_LU_Entry.TABLE_NAME,
                                        CONFIGURATION_LU_Entry.COLUMN_NAME_ID
                                                + " = ?", where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processEvents(JSONArray events)
    {
        try
        {
            for (int i = 0; i < events.length(); i++)
            {
                JSONObject row = events.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(EVENT_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(EVENT_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(EVENT_LU_Entry.COLUMN_NAME_ID));
                vals.put(EVENT_LU_Entry.COLUMN_NAME_EVENT_NAME,
                        row.getString(EVENT_LU_Entry.COLUMN_NAME_EVENT_NAME));
                vals.put(EVENT_LU_Entry.COLUMN_NAME_EVENT_CODE,
                        row.getString(EVENT_LU_Entry.COLUMN_NAME_EVENT_CODE));
                vals.put(EVENT_LU_Entry.COLUMN_NAME_DATE_START,
                        row.getString(EVENT_LU_Entry.COLUMN_NAME_DATE_START));
                vals.put(
                        EVENT_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(EVENT_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {EVENT_LU_Entry.COLUMN_NAME_EVENT_NAME};
                String[] where = {vals
                        .getAsString(EVENT_LU_Entry.COLUMN_NAME_ID)};
                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(EVENT_LU_Entry.TABLE_NAME, projection, // select
                            EVENT_LU_Entry.COLUMN_NAME_ID + "=?", where, null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(EVENT_LU_Entry.TABLE_NAME, vals,
                                        EVENT_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(EVENT_LU_Entry.TABLE_NAME, null, vals);
                                break;
                            case DELETE:
                                db.delete(EVENT_LU_Entry.TABLE_NAME,
                                        EVENT_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }

            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processMatches(JSONArray matches)
    {
        // TODO could be abstracted further
        try
        {
            for (int i = 0; i < matches.length(); i++)
            {
                JSONObject row = matches.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(MatchStatsStruct.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = MatchStatsStruct.getNewMatchStats()
                        .jsonToCV(row);

                // check if this entry exists already
                String[] projection = {MatchStatsStruct.COLUMN_NAME_ID,
                        MatchStatsStruct.COLUMN_NAME_INVALID};
                String[] where = {
                        vals.getAsString(MatchStatsStruct.COLUMN_NAME_EVENT_ID),
                        vals.getAsString(MatchStatsStruct.COLUMN_NAME_MATCH_ID),
                        vals.getAsString(MatchStatsStruct.COLUMN_NAME_TEAM_ID),
                        vals.getAsString(MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH)};

                synchronized (ScoutingDBHelper.lock)
                {
                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db
                            .query(MatchStatsStruct.TABLE_NAME,
                                    projection, // select
                                    MatchStatsStruct.COLUMN_NAME_EVENT_ID
                                            + "=? AND "
                                            + MatchStatsStruct.COLUMN_NAME_MATCH_ID
                                            + "=? AND "
                                            + MatchStatsStruct.COLUMN_NAME_TEAM_ID
                                            + "=? AND "
                                            + MatchStatsStruct.COLUMN_NAME_PRACTICE_MATCH
                                            + "=?", where, null, // don't
                                    // group
                                    null, // don't filter
                                    null, // don't order
                                    "0,1"); // limit to 1
                    try
                    {
                        int id = 0, invalid = 0;
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }
                        else
                        {
                            id = c.getInt(c
                                    .getColumnIndexOrThrow(MatchStatsStruct.COLUMN_NAME_ID));
                            invalid = c
                                    .getInt(c
                                            .getColumnIndexOrThrow(MatchStatsStruct.COLUMN_NAME_INVALID));
                            if (invalid > 0) // this field has not been sent to
                            // server yet.
                            {
                                action = Action.NOTHING;
                            }
                        }

                        String[] where2 = {String.valueOf(id)};

                        switch (action)
                        {
                            case UPDATE:
                                db.update(MatchStatsStruct.TABLE_NAME, vals,
                                        MatchStatsStruct.COLUMN_NAME_ID + " = ?",
                                        where2);
                                break;
                            case INSERT:
                                db.insert(MatchStatsStruct.TABLE_NAME, null, vals);
                                break;
                            case DELETE:
                                db.delete(MatchStatsStruct.TABLE_NAME,
                                        MatchStatsStruct.COLUMN_NAME_ID + " = ?",
                                        where2);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processNotes(JSONArray notes)
    {
        try
        {
            for (int i = 0; i < notes.length(); i++)
            {
                JSONObject row = notes.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(NOTES_OPTIONS_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(NOTES_OPTIONS_Entry.COLUMN_NAME_ID,
                        row.getInt(NOTES_OPTIONS_Entry.COLUMN_NAME_ID));
                vals.put(NOTES_OPTIONS_Entry.COLUMN_NAME_OPTION_TEXT, row
                        .getString(NOTES_OPTIONS_Entry.COLUMN_NAME_OPTION_TEXT));
                vals.put(
                        NOTES_OPTIONS_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(NOTES_OPTIONS_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {NOTES_OPTIONS_Entry.COLUMN_NAME_OPTION_TEXT};
                String[] where = {vals
                        .getAsString(NOTES_OPTIONS_Entry.COLUMN_NAME_ID)};

                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(
                            NOTES_OPTIONS_Entry.TABLE_NAME,
                            projection, // select
                            NOTES_OPTIONS_Entry.COLUMN_NAME_ID + "=?", where,
                            null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(
                                        NOTES_OPTIONS_Entry.TABLE_NAME,
                                        vals,
                                        NOTES_OPTIONS_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(NOTES_OPTIONS_Entry.TABLE_NAME, null,
                                        vals);
                                break;
                            case DELETE:
                                db.delete(
                                        NOTES_OPTIONS_Entry.TABLE_NAME,
                                        NOTES_OPTIONS_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processRobots(JSONArray robots)
    {
        try
        {
            for (int i = 0; i < robots.length(); i++)
            {
                JSONObject row = robots.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(ROBOT_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(ROBOT_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(ROBOT_LU_Entry.COLUMN_NAME_ID));
                vals.put(ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID,
                        row.getString(ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID));
                vals.put(ROBOT_LU_Entry.COLUMN_NAME_ROBOT_PHOTO,
                        row.getString(ROBOT_LU_Entry.COLUMN_NAME_ROBOT_PHOTO));
                vals.put(
                        ROBOT_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(ROBOT_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {ROBOT_LU_Entry.COLUMN_NAME_ID};
                String[] where = {vals
                        .getAsString(ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID)};
                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(ROBOT_LU_Entry.TABLE_NAME,
                            projection, // select
                            ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID + "=?", where,
                            null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(
                                        ROBOT_LU_Entry.TABLE_NAME,
                                        vals,
                                        ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(ROBOT_LU_Entry.TABLE_NAME, null, vals);
                                break;
                            case DELETE:
                                db.delete(
                                        ROBOT_LU_Entry.TABLE_NAME,
                                        ROBOT_LU_Entry.COLUMN_NAME_TEAM_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processPits(JSONArray pits)
    {
        // TODO could be abstracted further
        try
        {
            for (int i = 0; i < pits.length(); i++)
            {
                JSONObject row = pits.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(PitStats.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = PitStats.getNewPitStats().jsonToCV(row);

                // check if this entry exists already
                String[] projection = {PitStats.COLUMN_NAME_ID,
                        PitStats.COLUMN_NAME_INVALID};
                String[] where = {vals
                        .getAsString(PitStats.COLUMN_NAME_TEAM_ID)};

                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(PitStats.TABLE_NAME, projection, // select
                            PitStats.COLUMN_NAME_TEAM_ID + "=?", where, null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }
                        else
                        {
                            int invalid = c
                                    .getInt(c
                                            .getColumnIndexOrThrow(PitStats.COLUMN_NAME_INVALID));
                            if (invalid > 0) // Current entry has not been sent
                            // to server, don't overwrite
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(PitStats.TABLE_NAME, vals,
                                        PitStats.COLUMN_NAME_TEAM_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(PitStats.TABLE_NAME, null, vals);
                                break;
                            case DELETE:
                                db.delete(PitStats.TABLE_NAME,
                                        PitStats.COLUMN_NAME_TEAM_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processWheelBase(JSONArray wheelBase)
    {
        try
        {
            for (int i = 0; i < wheelBase.length(); i++)
            {
                JSONObject row = wheelBase.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(WHEEL_BASE_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(WHEEL_BASE_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(WHEEL_BASE_LU_Entry.COLUMN_NAME_ID));
                vals.put(
                        WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC,
                        row.getString(WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC));
                vals.put(
                        WHEEL_BASE_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(WHEEL_BASE_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {WHEEL_BASE_LU_Entry.COLUMN_NAME_WHEEL_BASE_DESC};
                String[] where = {vals
                        .getAsString(WHEEL_BASE_LU_Entry.COLUMN_NAME_ID)};

                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(
                            WHEEL_BASE_LU_Entry.TABLE_NAME,
                            projection, // select
                            WHEEL_BASE_LU_Entry.COLUMN_NAME_ID + "=?", where,
                            null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(
                                        WHEEL_BASE_LU_Entry.TABLE_NAME,
                                        vals,
                                        WHEEL_BASE_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(WHEEL_BASE_LU_Entry.TABLE_NAME, null,
                                        vals);
                                break;
                            case DELETE:
                                db.delete(
                                        WHEEL_BASE_LU_Entry.TABLE_NAME,
                                        WHEEL_BASE_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processWheelType(JSONArray wheelType)
    {
        try
        {
            for (int i = 0; i < wheelType.length(); i++)
            {
                JSONObject row = wheelType.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(WHEEL_TYPE_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID));
                vals.put(
                        WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC,
                        row.getString(WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC));
                vals.put(
                        WHEEL_TYPE_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(WHEEL_TYPE_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {WHEEL_TYPE_LU_Entry.COLUMN_NAME_WHEEL_TYPE_DESC};
                String[] where = {vals
                        .getAsString(WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID)};

                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(
                            WHEEL_TYPE_LU_Entry.TABLE_NAME,
                            projection, // select
                            WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID + "=?", where,
                            null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(
                                        WHEEL_TYPE_LU_Entry.TABLE_NAME,
                                        vals,
                                        WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(WHEEL_TYPE_LU_Entry.TABLE_NAME, null,
                                        vals);
                                break;
                            case DELETE:
                                db.delete(
                                        WHEEL_TYPE_LU_Entry.TABLE_NAME,
                                        WHEEL_TYPE_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void processPositions(JSONArray positions)
    {
        try
        {
            for (int i = 0; i < positions.length(); i++)
            {
                JSONObject row = positions.getJSONObject(i);
                Action action = Action.UPDATE;
                if (row.getInt(POSITION_LU_Entry.COLUMN_NAME_INVALID) != 0)
                {
                    action = Action.DELETE;
                }
                ContentValues vals = new ContentValues();
                vals.put(POSITION_LU_Entry.COLUMN_NAME_ID,
                        row.getInt(POSITION_LU_Entry.COLUMN_NAME_ID));
                vals.put(POSITION_LU_Entry.COLUMN_NAME_POSITION,
                        row.getString(POSITION_LU_Entry.COLUMN_NAME_POSITION));
                vals.put(
                        POSITION_LU_Entry.COLUMN_NAME_TIMESTAMP,
                        DB.dateParser.format(new Date(
                                row.getLong(POSITION_LU_Entry.COLUMN_NAME_TIMESTAMP) * 1000)));

                // check if this entry exists already
                String[] projection = {POSITION_LU_Entry.COLUMN_NAME_POSITION};
                String[] where = {vals
                        .getAsString(POSITION_LU_Entry.COLUMN_NAME_ID)};

                synchronized (ScoutingDBHelper.lock)
                {

                    SQLiteDatabase db = ScoutingDBHelper.getInstance()
                            .getWritableDatabase();

                    Cursor c = db.query(
                            POSITION_LU_Entry.TABLE_NAME,
                            projection, // select
                            POSITION_LU_Entry.COLUMN_NAME_ID + "=?", where,
                            null, // don't
                            // group
                            null, // don't filter
                            null, // don't order
                            "0,1"); // limit to 1
                    try
                    {
                        if (!c.moveToFirst())
                        {
                            if (action == Action.UPDATE)
                            {
                                action = Action.INSERT;
                            }
                            else if (action == Action.DELETE)
                            {
                                action = Action.NOTHING;
                            }
                        }

                        switch (action)
                        {
                            case UPDATE:
                                db.update(POSITION_LU_Entry.TABLE_NAME, vals,
                                        POSITION_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            case INSERT:
                                db.insert(POSITION_LU_Entry.TABLE_NAME, null, vals);
                                break;
                            case DELETE:
                                db.delete(POSITION_LU_Entry.TABLE_NAME,
                                        POSITION_LU_Entry.COLUMN_NAME_ID + " = ?",
                                        where);
                                break;
                            default:
                        }
                    }
                    finally
                    {
                        if (c != null)
                        {
                            c.close();
                        }
                        ScoutingDBHelper.getInstance().close();
                    }
                }
            }
        }
        catch (JSONException e)
        {
            // TODO handle error
        }
    }

    private void sendMatches()
    {
        // TODO could be abstracted further?

        // repurposed invalid flag for marking fields that need to be uploaded
        String[] matchProjection = MatchStatsStruct.getNewMatchStats()
                .getProjection();

        synchronized (ScoutingDBHelper.lock)
        {

            SQLiteDatabase db = ScoutingDBHelper.getInstance()
                    .getReadableDatabase();

            Cursor c = db.query(MatchStatsStruct.TABLE_NAME, matchProjection,
                    MatchStatsStruct.COLUMN_NAME_INVALID + "<>0", null, null,
                    null, null);
            try
            {

                synchronized (outgoing)
                {

                    if (c.moveToFirst())
                    {
                        do
                        {
                            Map<String, String> args = new HashMap<String, String>();
                            args.put("password", password);
                            args.put("type", "match");
                            args.put("version", version);
                            for (int i = 0; i < matchProjection.length; i++)
                            {
                                args.put(matchProjection[i], c.getString(c
                                        .getColumnIndex(matchProjection[i])));
                            }
                            outgoing.add(args);
                        } while (c.moveToNext());
                    }
                }
            }
            finally
            {
                if (c != null)
                {
                    c.close();
                }
                ScoutingDBHelper.getInstance().close();
            }

        }
    }

    private void sendPits()
    {
        // TODO could be abstracted further
        String[] pitProjection = PitStats.getNewPitStats().getProjection();
        synchronized (ScoutingDBHelper.lock)
        {

            SQLiteDatabase db = ScoutingDBHelper.getInstance()
                    .getReadableDatabase();

            Cursor c = db.query(PitStats.TABLE_NAME, pitProjection,
                    PitStats.COLUMN_NAME_INVALID + "<>0", null, null, null,
                    null);
            try
            {

                synchronized (outgoing)
                {

                    if (c.moveToFirst())
                    {
                        do
                        {
                            Map<String, String> args = new HashMap<String, String>();
                            args.put("password", password);
                            args.put("type", "pits");
                            args.put("version", version);
                            for (int i = 0; i < pitProjection.length; i++)
                            {
                                args.put(pitProjection[i], c.getString(c
                                        .getColumnIndex(pitProjection[i])));
                            }
                            outgoing.add(args);
                        } while (c.moveToNext());
                    }
                }
            }
            finally
            {
                if (c != null)
                {
                    c.close();
                }
                ScoutingDBHelper.getInstance().close();
            }
        }
    }
}
