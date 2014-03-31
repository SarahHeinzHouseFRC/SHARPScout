package org.sharp.scouting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import org.acra.ACRA;
import org.frc3260.aerialassist.PitsActivity;
import org.frc3260.database.DB;
import org.frc3260.database.DBSyncService;
import org.frc3260.database.DBSyncService.LocalBinder;
import org.sigmond.net.HttpCallback;
import org.sigmond.net.HttpRequestInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardActivity extends Activity
{
    private static final int VERSION_DIALOG = 7;
    private static final int PITS_ACTIVITY_CODE = 4639;
    private static final int MATCH_ACTIVITY_CODE = 4640;
    private static final int DATA_ACTIVITY_CODE = 4641;
    private static String VERSION_MESSAGE;
    private Button match;
    private Button pits;
    private Button data;
    private ImageView SHARPLogo;
    private ImageView SHARPScoutBanner;
    private LocalBinder binder;
    private ServiceWatcher watcher = new ServiceWatcher();
    private String HELPMESSAGE;
    private String versionCode;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard);
        if(getIntent().getBooleanExtra("ExitApp", false))
        {
            finish();
            return;
        }

        versionCode = "";

        HELPMESSAGE = "Version: " + getString(R.string.VersionID) + "\nDate: "
                      + getString(R.string.VersionDate)
                      + "\nRefer all questions or comments to "
                      + getString(R.string.dev_email);

        match = (Button) findViewById(R.id.matchB);
        pits = (Button) findViewById(R.id.pitB);
        data = (Button) findViewById(R.id.dataB);

        SHARPLogo = (ImageView) findViewById(R.id.sharpbanner);

        SHARPScoutBanner = (ImageView) findViewById(R.id.banner);

        Intent intent = new Intent(getApplicationContext(), DBSyncService.class);
        bindService(intent, watcher, Context.BIND_AUTO_CREATE);

        match.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(),
                                           MatchStartActivity.class);
                startActivityForResult(intent, MATCH_ACTIVITY_CODE);
            }
        });

        pits.setOnClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(), PitsActivity.class);
                startActivityForResult(intent, PITS_ACTIVITY_CODE);

            }
        });

        data.setOnClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(), DataActivity.class);
                startActivityForResult(intent, DATA_ACTIVITY_CODE);

            }
        });

        SHARPLogo.setOnClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                Uri uri = Uri.parse("http://www.sarahheinzhouse.com/sharp/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        SHARPScoutBanner.setOnClickListener(new OnClickListener()
        {
            public void onClick(View view)
            {
                AlertDialog ad = new AlertDialog.Builder(DashboardActivity.this).create();
                ad.setCancelable(false);
                ad.setMessage("Virginia, what does the Scouter say about his Power Level?");
                ad.setButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                ad.show();
            }
        });

        DB db = new DB(getBaseContext(), binder);

        String url = Prefs.getScoutingURL(getApplicationContext());

        if(url.length() > 0)
        {
            db.checkVersion(new VersionCallback());

            EventList ev = new EventList(getApplicationContext(), binder);
            ev.downloadEventsList(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        if(intent.getBooleanExtra("ExitApp", false))
        {
            finish();
            return;
        }
    }

    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        if(binder == null)
        {
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(watcher != null && watcher.serviceRegistered)
        {
            unbindService(watcher);
        }
    }

    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id)
        {
            case VERSION_DIALOG:
                builder.setMessage(VERSION_MESSAGE)
                        .setCancelable(true)
                        .setPositiveButton("Yes",
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick(DialogInterface dialog,
                                                                   int id)
                                               {
                                                   Uri uri = Uri.parse(getString(
                                                           R.string.APKURL, versionCode));
                                                   Intent intent = new Intent(
                                                           Intent.ACTION_VIEW, uri);
                                                   startActivity(intent);
                                                   finish();
                                               }
                                           }
                                          )
                        .setNegativeButton("No",
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick(DialogInterface dialog,
                                                                   int id)
                                               {
                                                   dialog.cancel();
                                               }
                                           }
                                          );
                dialog = builder.create();
                break;

            case MainMenuSelection.HELPDIALOG:
                builder.setMessage(HELPMESSAGE)
                        .setCancelable(true)
                        .setPositiveButton("OK",
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick(DialogInterface dialog, int which)
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

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        return MainMenuSelection.onOptionsItemSelected(item, this) ? true : super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode)
        {
            case Prefs.PREFS_ACTIVITY_CODE:
                MatchSchedule schedule = new MatchSchedule();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                schedule.updateSchedule(prefs.getString("eventPref", "Buckeye Regional"), this, false);
                break;

            case PITS_ACTIVITY_CODE:
            case MATCH_ACTIVITY_CODE:
            case DATA_ACTIVITY_CODE:
                DB db = new DB(getBaseContext(), binder);
                db.checkVersion(new VersionCallback());
                break;

            default:
                break;
        }
    }

    protected class ServiceWatcher implements ServiceConnection
    {

        boolean serviceRegistered = false;

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            if(service instanceof LocalBinder)
            {
                binder = (LocalBinder) service;
                serviceRegistered = true;
                MainMenuSelection.setBinder(binder);
            }
        }

        public void onServiceDisconnected(ComponentName name)
        {
            serviceRegistered = false;
        }

    }

    protected class VersionCallback implements HttpCallback
    {
        public void onResponse(HttpRequestInfo resp)
        {
            try
            {
                versionCode = resp.getResponseString().trim();
                VERSION_MESSAGE = "The server you have linked to was made for a different version of this app.\nInstalled Version: "
                                  + getString(R.string.VersionID)
                                  + "\nServer Version: "
                                  + versionCode
                                  + "\nWould you like to download the correct version?";
                String verCode = versionCode;
                String localVersion = getString(R.string.VersionID);

                String versionRegex = "(^[0-9]{4}\\.[0-9]+\\.[0-9]+)";
                Pattern pattern = Pattern.compile(versionRegex);
                Matcher matcher = pattern.matcher(verCode);

                if(!matcher.matches())
                {
                    Toast.makeText(DashboardActivity.this, "Malformed Version String. Server May Not Be Available.", Toast.LENGTH_LONG).show();
                    Log.e("VersionCallback", "Malformed Version String. Server May Not Be Available. Reply: " + versionCode);

                    return;
                }

                if(!verCode.contains(localVersion))
                {
                    Log.i("Verion Check", "Installed Version: " + getString(R.string.VersionID) + ", Server Reply: " + verCode);

                    showDialog(VERSION_DIALOG);
                }
            }
            catch(Exception e)
            {
                ACRA.getErrorReporter().handleException(e);
            }
        }

        public void onError(Exception e)
        {
            ACRA.getErrorReporter().handleException(e);
        }
    }
}
