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

package org.robobees.recyclerush;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.frc836.database.DB;
import org.frc836.database.FRCScoutingContract.FACT_MATCH_DATA_2015_Entry;
import org.frc836.database.MatchStatsStruct;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchStatsRR extends MatchStatsStruct
{

    private static final int TOTES_IN_STACK = 6;
    private static final int COOP_TOTES_STACK = 4;

    public boolean auto_move;
    public short auto_totes;
    public boolean auto_stack_2;
    public boolean auto_stack_3;
    public short auto_bin;
    public short auto_step_bin;
    public short totes[];
    public short coops[];
    public short bins[];
    public short bin_litter;
    public short landfill_litter;
    public boolean tipped_stack;

    public MatchStatsRR()
    {
        super.init();
        init();
    }

    public MatchStatsRR(int team, String event, int match)
    {
        super(team, event, match);
        init();
    }

    public MatchStatsRR(int team, String event, int match, boolean practice)
    {
        super(team, event, match, practice);
        init();
    }

    public void init()
    {
        auto_move = false;
        auto_totes = 0;
        auto_stack_2 = false;
        auto_stack_3 = false;
        auto_bin = 0;
        auto_step_bin = 0;

        totes = new short[TOTES_IN_STACK];
        coops = new short[COOP_TOTES_STACK];
        bins = new short[TOTES_IN_STACK];

        for (int i = 0; i < TOTES_IN_STACK; i++)
        {
            totes[i] = 0;
        }
        for (int i = 0; i < COOP_TOTES_STACK; i++)
        {
            coops[i] = 0;
        }
        for (int i = 0; i < TOTES_IN_STACK; i++)
        {
            bins[i] = 0;
        }

        bin_litter = 0;
        landfill_litter = 0;
    }

    public ContentValues getValues(DB db, SQLiteDatabase database)
    {
        ContentValues vals = super.getValues(db, database);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_MOVE,
                auto_move ? 1 : 0);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_TOTES, auto_totes);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_2,
                auto_stack_2 ? 1 : 0);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_3,
                auto_stack_3 ? 1 : 0);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_BIN, auto_bin);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STEP_BIN,
                auto_step_bin);

        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            vals.put(
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1.substring(0,
                            FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1
                                    .length() - 1)
                            + i, totes[i - 1]);
        }
        for (int i = 1; i <= COOP_TOTES_STACK; i++)
        {
            vals.put(
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1.substring(0,
                            FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1
                                    .length() - 1)
                            + i, coops[i - 1]);
        }
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            vals.put(
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1.substring(0,
                            FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1
                                    .length() - 1)
                            + i, bins[i - 1]);
        }

        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_LITTER, bin_litter);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_LANDFILL_LITTER,
                landfill_litter);
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TIPPED_STACK,
                tipped_stack ? 1 : 0);
        return vals;
    }

    public void fromCursor(Cursor c, DB db, SQLiteDatabase database)
    {
        super.fromCursor(c, db, database);

        auto_move = c
                .getInt(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_MOVE)) != 0;
        auto_totes = c
                .getShort(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_TOTES));
        auto_stack_2 = c
                .getInt(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_2)) != 0;
        auto_stack_3 = c
                .getInt(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_3)) != 0;
        auto_bin = c
                .getShort(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_BIN));
        auto_step_bin = c
                .getShort(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STEP_BIN));

        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            totes[i - 1] = c
                    .getShort(c.getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1
                            .substring(
                                    0,
                                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1
                                            .length() - 1)
                            + i));
        }
        for (int i = 1; i <= COOP_TOTES_STACK; i++)
        {
            coops[i - 1] = c
                    .getShort(c.getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1
                            .substring(
                                    0,
                                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1
                                            .length() - 1)
                            + i));
        }
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            bins[i - 1] = c
                    .getShort(c.getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1
                            .substring(
                                    0,
                                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1
                                            .length() - 1)
                            + i));
        }

        bin_litter = c
                .getShort(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_LITTER));
        landfill_litter = c
                .getShort(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_LANDFILL_LITTER));
        tipped_stack = c
                .getInt(c
                        .getColumnIndexOrThrow(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TIPPED_STACK)) != 0;

    }

    @Override
    public String[] getProjection()
    {
        String[] projection = super.getProjection();
        List<String> temp = new ArrayList<String>(Arrays.asList(projection));
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_MOVE);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_TOTES);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_2);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_3);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_BIN);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STEP_BIN);
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1.substring(
                    0,
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1.length() - 1)
                    + i);
        }
        for (int i = 1; i <= COOP_TOTES_STACK; i++)
        {
            temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1.substring(0,
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1.length() - 1)
                    + i);
        }
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1.substring(0,
                    FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1.length() - 1)
                    + i);
        }
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_LITTER);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_LANDFILL_LITTER);
        temp.add(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TIPPED_STACK);

        projection = new String[temp.size()];
        return temp.toArray(projection);
    }

    // no text fields in 2015 specific stats
    // @Override
    // public boolean isTextField(String column_name) {
    // return super.isTextField(column_name);
    // }

    @Override
    public ContentValues jsonToCV(JSONObject json) throws JSONException
    {
        ContentValues vals = super.jsonToCV(json);

        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_MOVE,
                json.getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_MOVE));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_TOTES,
                json.getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_TOTES));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_2, json
                .getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_2));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_3, json
                .getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STACK_3));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_BIN,
                json.getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_BIN));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STEP_BIN, json
                .getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_AUTO_STEP_BIN));
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            String col = FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1
                    .substring(0,
                            FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TOTES_1
                                    .length() - 1)
                    + i;
            vals.put(col, json.getInt(col));
        }
        for (int i = 1; i <= COOP_TOTES_STACK; i++)
        {
            String col = FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1
                    .substring(0, FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_COOP_1
                            .length() - 1)
                    + i;
            vals.put(col, json.getInt(col));
        }
        for (int i = 1; i <= TOTES_IN_STACK; i++)
        {
            String col = FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1
                    .substring(0, FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_1
                            .length() - 1)
                    + i;
            vals.put(col, json.getInt(col));
        }
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_LITTER,
                json.getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_BIN_LITTER));
        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_LANDFILL_LITTER, json
                .getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_LANDFILL_LITTER));

        vals.put(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TIPPED_STACK,
                json.getInt(FACT_MATCH_DATA_2015_Entry.COLUMN_NAME_TIPPED_STACK));

        return vals;
    }

}
