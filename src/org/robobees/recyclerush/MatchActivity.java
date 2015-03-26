/*
 * Copyright 2015 Daniel Logan, Matthew Berkin
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

package org.robobees.recyclerush;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import org.frc836.database.DB;
import org.frc836.database.DBSyncService;
import org.frc836.database.DBSyncService.LocalBinder;
import org.growingstems.scouting.MainMenuSelection;
import org.growingstems.scouting.Prefs;
import org.growingstems.scouting.R;

import java.util.ArrayList;
import java.util.List;

//TODO add timer for auto

public class MatchActivity extends Activity implements OnClickListener
{

    private static final int CANCEL_DIALOG = 0;
    private static final int LOAD_DIALOG = 353563;

    private String HELPMESSAGE;

    private DB submitter;

    private MatchStatsRR teamData;

    private EditText teamText;

    private int currentView; // 0=auto, 1=tele, 2=endgame

    private LinearLayout autoLayout;
    private LinearLayout teleLayout;
    private LinearLayout endGameLayout;

    private EditText matchT;
    private TextView posT;

    private Button lastB;
    private Button nextB;

    // Autonomous
    private CheckBox aMoved;
    private CheckBox aT3;
    private CheckBox aT2;
    private Button bins_scored_add;
    private Button aAdd1;
    private Button bins_step_add;
    private Spinner numBins_scored;
    private Spinner aCount1;
    private Spinner numBins_step;

    // Tele-Op
    private Button addStacked;
    private Spinner stackedCount;
    private Button addHTote;
    private Spinner hTote;
    private Button addNumCans;
    private Spinner numCans;
    private Button addHCan;
    private Spinner hCan;
    private Button addCoop;
    private Spinner coopTotes;
    private CheckBox fromLandfill;
    private CheckBox fromPlayer;
    /*private Button addLf;
    private Button addLBin;
    private Button gAdd6;
    private Button gAdd5;
    private Button gAdd4;
    private Button gAdd3;
    private Button gAdd2;
    private Button gAdd1;
    private Button yAdd4;
    private Button yAdd3;
    private Button yAdd2;
    private Button yAdd1;
    private Button binAdd6;
    private Button binAdd5;
    private Button binAdd4;
    private Button binAdd3;
    private Button binAdd2;
    private Button binAdd1;
    private Spinner LfCount;
    private Spinner LBinCount;
    private Spinner gCount6;
    private Spinner gCount5;
    private Spinner gCount4;
    private Spinner gCount3;
    private Spinner gCount2;
    private Spinner gCount1;
    private Spinner yCount4;
    private Spinner yCount3;
    private Spinner yCount2;
    private Spinner yCount1;
    private Spinner binCount6;
    private Spinner binCount5;
    private Spinner binCount4;
    private Spinner binCount3;
    private Spinner binCount2;
    private Spinner binCount1;*/

    // End Game
    private EditText teamNotes;
    private Spinner commonNotes;
    private CheckBox botTip;
    private CheckBox stackTip;
    private CheckBox yellowcard;
    private CheckBox redcard;
    private CheckBox foul;

    private LocalBinder binder;

    private ServiceWatcher watcher = new ServiceWatcher();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match);

        Intent sync = new Intent(this, DBSyncService.class);
        bindService(sync, watcher, Context.BIND_AUTO_CREATE);

        HELPMESSAGE = "Interface used for match recording.\n" +
                "Important note: records are to be based on end result of a scoring action, not current state of all stacks at all times.\n\n" +
                "When a robot adds a tote to a stack, increment the counter for the number of totes in that stack when that robot is finished with that scoring action.\n\n" +
                "If there is a bin on the stack, or a bin is being added to it, mark what level the bin is at when that scoring action is finished.\n\n" +
                "bin=shorthand for Recycle Container";

        getGUIRefs();
        setListeners();

        Intent intent = getIntent();

        String team = intent.getStringExtra("team");

        String match = intent.getStringExtra("match");

        submitter = new DB(this, binder);

        teamText.setText(team);
        matchT.setText(match);

        loadData();

        setAuto();

    }

    public void onResume()
    {
        super.onResume();

        teamData.event = Prefs.getEvent(getApplicationContext(),
                "Chesapeake Regional");

        teamData.practice_match = Prefs.getPracticeMatch(
                getApplicationContext(), false);

        updatePosition();

        List<String> options = submitter.getNotesOptions();

        if (options == null)
        {
            options = new ArrayList<String>(1);
        }

        options.add(0, commonNotes.getItemAtPosition(0).toString());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, options);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        commonNotes.setAdapter(adapter);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(watcher);
    }

    protected class ServiceWatcher implements ServiceConnection
    {

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            if (service instanceof LocalBinder)
            {
                binder = (LocalBinder) service;
                submitter.setBinder(binder);
            }
        }

        public void onServiceDisconnected(ComponentName name)
        {
        }

    }

    private void updatePosition()
    {
        String pos = Prefs.getPosition(getApplicationContext(), "Red 1");
        posT.setText(pos);
        if (pos.contains("Blue"))
        {
            posT.setTextColor(Color.BLUE);
        }
        else
        {
            posT.setTextColor(Color.RED);
        }
    }

    private void loadAuto()
    {
        aMoved.setChecked(teamData.auto_move);
        aT3.setChecked(teamData.auto_stack_3);
        aT2.setChecked(teamData.auto_stack_2);
        aCount1.setSelection(teamData.auto_totes);
        numBins_scored.setSelection(teamData.auto_bin);
        numBins_step.setSelection(teamData.auto_step_bin);
    }

    private void loadTele()
    {
        stackedCount.setSelection(teamData.stacked_count);
        hTote.setSelection(teamData.highest_tote);
        numCans.setSelection(teamData.cans_count);
        hCan.setSelection(teamData.highest_can);
        coopTotes.setSelection(teamData.coop_totes);
        fromLandfill.setChecked(teamData.from_landfill);
        fromPlayer.setChecked(teamData.from_player);
    }

    private void loadEndgame()
    {
        teamNotes.setText(teamData.notes);
        botTip.setChecked(teamData.tipOver);
        stackTip.setChecked(teamData.tipped_stack);
        foul.setChecked(teamData.foul);
        yellowcard.setChecked(teamData.yellowCard);
        redcard.setChecked(teamData.redCard);
    }

    public void onBack(View v)
    {
        if (currentView == 0) // auto
        {
            showDialog(CANCEL_DIALOG);
        }
        else if (currentView == 1)
        {
            saveTele();
            setAuto();
        }
        else if (currentView == 2)
        {
            saveEnd();
            setTele();
        }
    }

    public void onNext(View v)
    {
        if (currentView == 0)
        {
            saveAuto();
            setTele();
        }
        else if (currentView == 1)
        {
            saveTele();
            setEnd();
        }
        else if (currentView == 2)
        {
            saveEnd();
            submit();
        }
    }

    private void saveTeamInfo()
    {
        String team = teamText.getText().toString();
        if (team != null && team.length() > 0)
        {
            teamData.team = Integer.valueOf(team);
        }
        String match = matchT.getText().toString();
        if (match != null && match.length() > 0)
        {
            teamData.match = Integer.valueOf(match);
        }
        teamData.position = posT.getText().toString();
    }

    private void saveAuto()
    {
        saveTeamInfo();

        teamData.auto_move = aMoved.isChecked();
        teamData.auto_stack_3 = aT3.isChecked();
        teamData.auto_stack_2 = aT2.isChecked();
        teamData.auto_totes = (short) aCount1.getSelectedItemPosition();
        teamData.auto_bin = (short) numBins_scored.getSelectedItemPosition();
        teamData.auto_step_bin = (short) numBins_step.getSelectedItemPosition();
    }

    private void saveTele()
    {
        saveTeamInfo();

        stackedCount.setSelection(teamData.stacked_count);
        hTote.setSelection(teamData.highest_tote);
        numCans.setSelection(teamData.cans_count);
        hCan.setSelection(teamData.highest_can);
        coopTotes.setSelection(teamData.coop_totes);
        fromLandfill.setChecked(teamData.from_landfill);
        fromPlayer.setChecked(teamData.from_player);

        teamData.stacked_count = (short) stackedCount.getSelectedItemPosition();
        teamData.highest_tote = (short) hTote.getSelectedItemPosition();

    }

    private void saveEnd()
    {
        saveTeamInfo();

        teamData.notes = teamNotes.getText().toString();
        teamData.tipOver = botTip.isChecked();
        teamData.tipped_stack = stackTip.isChecked();
        teamData.foul = foul.isChecked();
        teamData.yellowCard = yellowcard.isChecked();
        teamData.redCard = redcard.isChecked();

    }

    public void setTele()
    {
        currentView = 1;
        autoLayout.setVisibility(View.GONE);
        teleLayout.setVisibility(View.VISIBLE);
        endGameLayout.setVisibility(View.GONE);
        lastB.setText("Auto");
        nextB.setText("End Game");
    }

    public void setAuto()
    {
        currentView = 0;
        autoLayout.setVisibility(View.VISIBLE);
        teleLayout.setVisibility(View.GONE);
        endGameLayout.setVisibility(View.GONE);
        lastB.setText("Cancel");
        nextB.setText("Tele op");
    }

    public void setEnd()
    {
        currentView = 2;
        autoLayout.setVisibility(View.GONE);
        teleLayout.setVisibility(View.GONE);
        endGameLayout.setVisibility(View.VISIBLE);
        lastB.setText("Tele op");
        nextB.setText("Submit");
    }

    public void submit()
    {
        submitter.submitMatch(teamData);
        nextB.setEnabled(false);
        if (matchT.getText().length() > 0)
        {
            setResult(Integer.valueOf(matchT.getText().toString()) + 1);
        }
        finish();
    }

    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id)
        {
            case CANCEL_DIALOG:
                builder.setMessage(
                        "Cancel Match Entry?\nChanges will not be saved.")
                        .setCancelable(false)
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog,
                                                        int id)
                                    {
                                        MatchActivity.this.finish();
                                    }
                                })
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog,
                                                        int id)
                                    {
                                        dialog.cancel();
                                    }
                                });
                dialog = builder.create();
                break;
            case LOAD_DIALOG:
                builder.setMessage("Data for this match Exists.\nLoad old match?")
                        .setCancelable(false)
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog,
                                                        int id)
                                    {
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog,
                                                        int id)
                                    {
                                        if (teamText.getText().toString().length() > 0
                                                && matchT.getText().toString()
                                                .length() > 0)
                                        {
                                            teamData = new MatchStatsRR(
                                                    Integer.valueOf(teamText
                                                            .getText().toString()),
                                                    Prefs.getEvent(
                                                            getApplicationContext(),
                                                            "Chesapeake Regional"),
                                                    Integer.valueOf(matchT
                                                            .getText().toString()
                                                            .length()));
                                        }
                                        else
                                        {
                                            teamData = new MatchStatsRR();
                                        }

                                        loadAuto();
                                        loadTele();
                                        loadEndgame();
                                    }
                                });
                dialog = builder.create();
                break;
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
                                });
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
        return MainMenuSelection.onOptionsItemSelected(item, this) ? true
                : super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        onBack(null);
        return;
    }

    private class positionClickListener implements OnClickListener
    {

        public void onClick(View v)
        {
            MainMenuSelection.openSettings(MatchActivity.this);
        }

    }

    ;

    public class NotesSelectedListener implements OnItemSelectedListener
    {

        public void onItemSelected(AdapterView<?> parent, View v, int position,
                                   long id)
        {
            if (position == 0 || !(parent instanceof Spinner))
            {
                return;
            }
            Spinner par = (Spinner) parent;
            String note = teamNotes.getText().toString();
            if (!note.contains(par.getItemAtPosition(position).toString()))
            {
                if (!note.trim().equals(""))
                {
                    note = note + "; ";
                }
                note = note + par.getItemAtPosition(position);
                teamNotes.setText(note);
            }
            par.setSelection(0);

        }

        public void onNothingSelected(AdapterView<?> parent)
        {
        }

    }

    ;

    @Override
    public void onClick(View v)
    {
        Spinner spin;

        switch (v.getId())
        {
            case R.id.bins_scored:
                spin = numBins_scored;
                break;
            case R.id.aAdd1:
                spin = aCount1;
                break;
            case R.id.bins_step:
                spin = numBins_step;
                break;
            case R.id.addStacked:
                spin = stackedCount;
                break;
            case R.id.addHTote:
                spin = hTote;
                break;
            case R.id.addNumCans:
                spin = numCans;
                break;
            case R.id.addHCan:
                spin = hCan;
                break;
            case R.id.addCoop:
                spin = coopTotes;
                break;
            case R.id.gAdd3:
                spin = gCount3;
            default:
                return;
        }

        if (spin.getSelectedItemPosition() < (spin.getCount() - 1))
        {
            spin.setSelection(spin.getSelectedItemPosition() + 1);
        }

    }

    private void getGUIRefs()
    {
        teamText = (EditText) findViewById(R.id.teamNum);

        autoLayout = (LinearLayout) findViewById(R.id.autoLayout);
        teleLayout = (LinearLayout) findViewById(R.id.teleLayout);
        endGameLayout = (LinearLayout) findViewById(R.id.endGameLayout);

        matchT = (EditText) findViewById(R.id.matchNum);
        posT = (TextView) findViewById(R.id.pos);

        lastB = (Button) findViewById(R.id.backB);
        nextB = (Button) findViewById(R.id.nextB);

        // Autonomous
        aMoved = (CheckBox) findViewById(R.id.aMoved);
        aT3 = (CheckBox) findViewById(R.id.aT3);
        aT2 = (CheckBox) findViewById(R.id.aT2);
        bins_scored_add = (Button) findViewById(R.id.bins_scored);
        aAdd1 = (Button) findViewById(R.id.aAdd1);
        bins_step_add = (Button) findViewById(R.id.bins_step);
        numBins_scored = (Spinner) findViewById(R.id.numBins_scored);
        aCount1 = (Spinner) findViewById(R.id.aCount1);
        numBins_step = (Spinner) findViewById(R.id.numBins_step);

        // Tele-Op


        addStacked = (Button) findViewById(R.id.addStacked);
        addHTote = (Button) findViewById(R.id.addHTote);
        adddNumCans = (Button) findViewById(R.id.addNumCans);
        addHCan = (Button) findViewById(R.id.addHCan);
        addCoop = (Button) findViewById(R.id.addCoop);
        LfCount = (Spinner) findViewById(R.id.lfCount);
        LBinCount = (Spinner) findViewById(R.id.lBinCount);

        // End Game
        teamNotes = (EditText) findViewById(R.id.notes);
        commonNotes = (Spinner) findViewById(R.id.commonNotes);
        botTip = (CheckBox) findViewById(R.id.botTip);
        stackTip = (CheckBox) findViewById(R.id.stackTip);
        yellowcard = (CheckBox) findViewById(R.id.yellowcard);
        redcard = (CheckBox) findViewById(R.id.redcard);
        foul = (CheckBox) findViewById(R.id.foul);
    }

    private void setListeners()
    {

        // lastB.setOnClickListener(this);
        // nextB.setOnClickListener(this); //taken care of in xml
        posT.setOnClickListener(new positionClickListener());

        // Autonomous
        bins_scored_add.setOnClickListener(this);
        aAdd1.setOnClickListener(this);
        bins_step_add.setOnClickListener(this);

        // Tele-Op

        addStacked.setOnClickListener(this);
        addHTote.setOnClickListener(this);
        addNumCans.setOnClickListener(this);
        addHCan.setOnClickListener(this);
        addCoop.setOnClickListener(this);


        // End Game
        commonNotes.setOnItemSelectedListener(new NotesSelectedListener());
    }

    private void loadData()
    {

        String team = teamText.getText().toString();
        String match = matchT.getText().toString();

        boolean loadData = false;
        if (team != null && team.length() > 0 && match != null
                && match.length() > 0)
        {
            teamData = (MatchStatsRR) submitter.getMatchStats(Prefs.getEvent(
                    getApplicationContext(), "Chesapeake Regional"), Integer
                    .valueOf(match), Integer.valueOf(team), Prefs
                    .getPracticeMatch(getApplicationContext(), false));
            if (teamData == null)
            {
                teamData = new MatchStatsRR(Integer.valueOf(team),
                        Prefs.getEvent(getApplicationContext(),
                                "Chesapeake Regional"), Integer.valueOf(match),
                        Prefs.getPracticeMatch(getApplicationContext(), false));
            }
            else
            {
                loadData = true;
            }
        }
        else
        {
            teamData = new MatchStatsRR();
        }

        if (loadData)
        {
            showDialog(LOAD_DIALOG);
        }

        currentView = 0;

        loadAuto();
        loadTele();
        loadEndgame();
    }
}
