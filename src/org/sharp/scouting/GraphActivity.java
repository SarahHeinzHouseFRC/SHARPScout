package org.sharp.scouting;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.acra.ACRA;
import org.frc3260.aerialassist.GraphStats;

import java.util.HashMap;
import java.util.Map;

public class GraphActivity extends Activity implements GraphStats.GraphCallback
{
    private String teamId;
    private String event;
    private String XML;

    private GraphicalView mChart;

    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    private ProgressDialog pd;

    private Map<String, Boolean> active = new HashMap<String, Boolean>();

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_view);

        Intent intent = getIntent();

        teamId = intent.getStringExtra("team_id");
        XML = intent.getStringExtra("XML");
        event = Prefs.getEvent(getApplicationContext(), "Buckeye Regional");
        String init = intent.getStringExtra("graph");

        if(init.length() > 0 && init.compareTo("All") != 0)
        {
            active.put(init, true);
        }
        else if(init.compareTo("All") == 0)
        {
            for(String key : GraphStats.graphList)
            {
                active.put(key, true);
            }
        }

        setTitle(event + " " + teamId);
    }

    protected void onResume()
    {
        super.onResume();

        if(mChart == null)
        {
            refreshGraphs();
        }
        else
        {
            mChart.repaint();
        }
    }

    public void refreshGraphs()
    {
        pd = ProgressDialog.show(this, "Busy", "Processing Stats", false);
        pd.setCancelable(true);
        GraphStats s = new GraphStats(event, this);
        s.refresh(XML);
    }

    private void fillDataSets(GraphStats stats)
    {
        int i = 0;
        for(String key : GraphStats.graphList)
        {
            if(active.containsKey(key) && active.get(key))
            {
                mDataset.addSeries(stats.data.get(key));
                XYSeriesRenderer render = new XYSeriesRenderer();
                render.setPointStyle(PointStyle.DIAMOND);
                render.setColor(GraphStats.colorList[i]);
                mRenderer.addSeriesRenderer(render);
                i = i >= GraphStats.colorList.length ? 0 : i + 1;
            }
        }
    }

    public void onResponse(GraphStats stats)
    {
        pd.dismiss();
        fillDataSets(stats);

        LinearLayout layout = (LinearLayout) findViewById(R.id.graph);

        mRenderer.setYAxisMin(0);
        mRenderer.setXAxisMin(1);

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        float val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, metrics);
        mRenderer.setLabelsTextSize(val);
        mRenderer.setLegendTextSize(val);

        mRenderer.setLegendHeight(125);

        val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, metrics);
        mRenderer.setAxisTitleTextSize(val);

        mRenderer.setXTitle("\n\n\n\n\n\n\n\nMatch Number");

        int[] margins = mRenderer.getMargins();
        margins[0] += 25;
        margins[1] += 30;
        margins[2] += 25;
        margins[3] += 30;
        mRenderer.setMargins(margins);

        mRenderer.setBarWidth(mRenderer.getBarWidth() + 10);
        mRenderer.setYLabelsPadding(mRenderer.getYLabelsPadding() + 30);

        mRenderer.setMarginsColor(Color.LTGRAY);
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.LTGRAY);

        mRenderer.setLabelsColor(Color.BLACK);
        mRenderer.setXLabelsColor(Color.BLACK);
        mRenderer.setYLabelsColor(0, Color.BLACK);
        mRenderer.setAxesColor(Color.BLACK);

        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomRate(0.2f);
        mRenderer.setZoomEnabled(false, false);

        mRenderer.setPointSize(mRenderer.getPointSize() + 5);

        SimpleSeriesRenderer[] renderers = mRenderer.getSeriesRenderers();

        for(SimpleSeriesRenderer renderer : renderers)
        {
            XYSeriesRenderer rendererSeries = (XYSeriesRenderer) renderer;

            rendererSeries.setFillPoints(true);
            rendererSeries.setPointStyle(PointStyle.CIRCLE);
        }

        mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.0f);
        layout.addView(mChart);
    }

    public void onError(Exception e)
    {
        Toast.makeText(getApplicationContext(), "Error Processing Graph", Toast.LENGTH_SHORT).show();

        ACRA.getErrorReporter().handleException(e);
    }

    // TODO: add options for selecting which lines to display
}
