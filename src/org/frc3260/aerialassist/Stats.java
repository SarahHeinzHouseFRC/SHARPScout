package org.frc3260.aerialassist;

import java.util.List;
import java.util.Map;

public abstract class Stats
{
    public static float getAvgScore(List<Map<String, String>> table)
    {
        float score = 0;
        for(Map<String, String> row : table)
        {
            score += Integer.valueOf(row.get("auto_high")) * 15;
            score += Integer.valueOf(row.get("auto_high_hot")) * 20;
            score += Integer.valueOf(row.get("auto_low_hot")) * 11;
            score += Integer.valueOf(row.get("auto_low")) * 6;
            score += Integer.valueOf(row.get("assist_points"));
            score += Integer.valueOf(row.get("auto_low")) * 6;
            score += Integer.valueOf(row.get("auto_mobile")) * 5;
            score += Integer.valueOf(row.get("high")) * 10;
            score += Integer.valueOf(row.get("low"));
            score += Integer.valueOf(row.get("truss")) * 10;
            score += Integer.valueOf(row.get("caught")) * 10;
        }
        score /= table.size();
        return round(score);
    }

    public static int getMatchScore(Map<String, String> row)
    {
        int score = 0;
        score += Integer.valueOf(row.get("auto_high")) * 15;
        score += Integer.valueOf(row.get("auto_high_hot")) * 20;
        score += Integer.valueOf(row.get("auto_low_hot")) * 11;
        score += Integer.valueOf(row.get("auto_low")) * 6;
        score += Integer.valueOf(row.get("assist_points"));
        score += Integer.valueOf(row.get("auto_mobile")) * 5;
        score += Integer.valueOf(row.get("high")) * 10;
        score += Integer.valueOf(row.get("low"));
        score += Integer.valueOf(row.get("truss")) * 10;
        score += Integer.valueOf(row.get("caught")) * 10;
        return score;
    }

    public static float getAvgAutoScore(List<Map<String, String>> table)
    {
        float score = 0;

        for(Map<String, String> row : table)
        {
            score += Integer.valueOf(row.get("auto_high")) * 15;
            score += Integer.valueOf(row.get("auto_high_hot")) * 20;
            score += Integer.valueOf(row.get("auto_low_hot")) * 11;
            score += Integer.valueOf(row.get("auto_low")) * 6;
            score += Integer.valueOf(row.get("auto_mobile")) * 5;
        }
        score /= table.size();
        return round(score);
    }

    public static int getMatchAutoScore(Map<String, String> row)
    {
        int score = 0;
        score += Integer.valueOf(row.get("auto_high")) * 15;
        score += Integer.valueOf(row.get("auto_high_hot")) * 20;
        score += Integer.valueOf(row.get("auto_low_hot")) * 11;
        score += Integer.valueOf(row.get("auto_low")) * 6;
        score += Integer.valueOf(row.get("auto_mobile")) * 5;
        return score;
    }

    public static float getTotalAssistPoints(List<Map<String, String>> table)
    {
        int assistPoints = 0;

        for(Map<String, String> row : table)
        {
            assistPoints += Integer.valueOf(row.get("assist_points"));
        }

        return assistPoints;
    }

    public static float getAvgAccuracy(List<Map<String, String>> table)
    {
        int scores = 0;
        int attempts = 0;
        for(Map<String, String> row : table)
        {
            scores += Integer.valueOf(row.get("auto_high"));
            scores += Integer.valueOf(row.get("auto_low"));
            scores += Integer.valueOf(row.get("auto_high_hot"));
            scores += Integer.valueOf(row.get("auto_low_hot"));
            scores += Integer.valueOf(row.get("high"));
            scores += Integer.valueOf(row.get("low"));
            attempts = scores;
            attempts += Integer.valueOf(row.get("misses"));
        }
        float accuracy = ((float) scores) / ((float) attempts);
        return roundPercent(accuracy);
    }

    public static int getTotalAttempts(List<Map<String, String>> table)
    {
        int scores = 0;
        int attempts = 0;

        for(Map<String, String> row : table)
        {
            scores += Integer.valueOf(row.get("auto_high"));
            scores += Integer.valueOf(row.get("auto_low"));
            scores += Integer.valueOf(row.get("auto_high_hot"));
            scores += Integer.valueOf(row.get("auto_low_hot"));
            scores += Integer.valueOf(row.get("high"));
            scores += Integer.valueOf(row.get("low"));
            attempts = scores;
            attempts += Integer.valueOf(row.get("misses"));
        }

        return attempts;
    }

    public static int getTotalCatchPoints(List<Map<String, String>> table)
    {
        int score = 0;
        for(Map<String, String> row : table)
        {
            score += Integer.valueOf(row.get("caught")) * 10;
        }
        return score;
    }

    public static float getMatchAccuracy(Map<String, String> row)
    {
        int scores = 0;
        int attempts = 0;
        scores += Integer.valueOf(row.get("auto_high"));
        scores += Integer.valueOf(row.get("auto_low"));
        scores += Integer.valueOf(row.get("auto_high_hot"));
        scores += Integer.valueOf(row.get("auto_low_hot"));
        scores += Integer.valueOf(row.get("high"));
        scores += Integer.valueOf(row.get("low"));
        attempts = scores;
        attempts += Integer.valueOf(row.get("misses"));
        float accuracy = ((float) scores) / ((float) attempts);
        return roundPercent(accuracy);
    }

    public static float round(float num)
    {
        int per = (int) (num * 10);
        float cent = ((float) per) / 10.0f;
        return cent;
    }

    public static float roundPercent(float num)
    {
        int per = (int) (num * 1000);
        float cent = ((float) per) / 10.0f;
        return cent;
    }
}
