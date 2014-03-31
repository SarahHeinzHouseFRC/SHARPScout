package org.sharp.scouting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.Toast;
import org.acra.ACRA;
import org.frc3260.aerialassist.MatchStats;
import org.frc3260.database.DB;

import java.util.List;

public class MatchStatsActivity extends Activity implements
        MatchStats.MatchCallback
{

    private String eventName;
    private String matchNum;
    private DB db;

    private ExpandableListView matchStatsView;

    private List<ExpandableListItem<String>> items;
    private TextListAdapter adapter;

    private ProgressDialog pd;

    private String HELPMESSAGE = "Displays recorded data about the selected match.";

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matchstats);
        db = new DB(getApplicationContext(),
                    Prefs.getSavedPassword(getApplicationContext()), null); // temporary
        // change
        // to
        // fix
        // errors
        // until
        // this
        // class
        // is
        // re-written
        matchStatsView = (ExpandableListView) findViewById(R.id.match_data_list);

        Intent intent = getIntent();

        eventName = intent.getStringExtra("event");
        matchNum = intent.getStringExtra("match");

        setTitle(eventName + ", Match " + matchNum);
        refreshStats();
    }

    protected void onResume()
    {
        super.onResume();
        db.setPass(Prefs.getSavedPassword(getApplicationContext()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        MainMenuSelection.setRefreshItem(menu, R.string.refresh_data);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return MainMenuSelection.onOptionsItemSelected(item, this) ? true
                                                                   : super.onOptionsItemSelected(item);
    }

    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id)
        {
            case MainMenuSelection.HELPDIALOG:
                builder.setMessage(HELPMESSAGE)
                        .setCancelable(true)
                        .setPositiveButton("OK",
                                           new DialogInterface.OnClickListener()
                                           {

                                               public void onClick(DialogInterface dialog,
                                                                   int which)
                                               {
                                                   dialog.cancel();

                                               }
                                           }
                                          );
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    public void refreshStats()
    {
        db.getMatchStats(eventName, matchNum, this);
        pd = ProgressDialog.show(this, "Busy", "Retrieving Match Stats", false);
        pd.setCancelable(true);
    }

    public void onResponse(MatchStats stats)
    {
        pd.dismiss();
        items = stats.contents;
        adapter = new TextListAdapter(this, items);
        matchStatsView.setAdapter(adapter);
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
            Toast.makeText(getBaseContext(), "Error processing server response", Toast.LENGTH_SHORT).show();
        }

        ACRA.getErrorReporter().handleException(e);
    }
}
