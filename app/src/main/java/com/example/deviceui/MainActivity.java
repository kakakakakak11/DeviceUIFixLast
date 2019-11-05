package com.example.deviceui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
  private static final String DEBUG_TAG = "MqttService";
  public static String APP_ID = "DeviceUI";
  private static String HOSTNAME = "hostname";
  private static String TOPIC = "topic";
  private static String USERNAME = "username";
  private static String PASSWORD = "password";
  private static String PORT = "port";
  private static String MESSAGE = "message";

  int minX = 0;
  int maxX = 0;

  private LineChart chart;
  private SeekBar seekBarX, seekBarY;
  private TextView tvX, tvY;
  protected Typeface tfRegular;
  protected Typeface tfLight;

  // limit line
  LimitLine limitMin;
  LimitLine limitMax;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);

    // input data from server
    Button btnInput = (Button) findViewById(R.id.btn_input);

    btnInput.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Intent myIntent = new Intent(view.getContext(), InputCloudActivity.class);
        startActivity(myIntent);
      }
    });

    // bluetooth
    Button btnBT = (Button) findViewById(R.id.btn_bt);
    btnBT.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
//        setData(999, 20);
//        chart.invalidate();
        Intent myIntent = new Intent(view.getContext(), InputBluetooth.class);
        startActivity(myIntent);
      }
    });

    Button btnFind = (Button) findViewById(R.id.btn_find);
    btnFind.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Intent myIntent = new Intent(view.getContext(), DeviceActivity.class);
        startActivity(myIntent);
      }
    });

    configChart();
  }

  private void configChart() {
    RangeSeekBar rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
    rangeSeekBar.setRangeValues(0, 999);
    rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
      @Override
      public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        minX = (int)bar.getSelectedMinValue();
        maxX = (int)bar.getSelectedMaxValue();
        limitMin.setLimit(minX);
        limitMax.setLimit(maxX);
        chart.invalidate();
      }
    });

    tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
    tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

    {   // // Chart Style // //
      chart = findViewById(R.id.chart1);
      // background color
      chart.setBackgroundColor(Color.WHITE);
      // disable description text
      chart.getDescription().setEnabled(false);
      // enable touch gestures
      chart.setTouchEnabled(true);
      // set listeners
//      chart.setOnChartValueSelectedListener(this);
      chart.setDrawGridBackground(false);
      // create marker to display box when values are selected
//      MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
      // Set the marker to the chart
//      mv.setChartView(chart);
//      chart.setMarker(mv);
      // enable scaling and dragging
      chart.setDragEnabled(true);
//      chart.setScaleEnabled(true);
      chart.setScaleXEnabled(true);
      // chart.setScaleYEnabled(true);
      // force pinch zoom along both axis
      chart.setPinchZoom(true);
    }

    XAxis xAxis;
    {   // // X-Axis Style // //
      xAxis = chart.getXAxis();
      // vertical grid lines
      xAxis.enableGridDashedLine(10f, 10f, 0f);
    }

    YAxis yAxis;
    {   // // Y-Axis Style // //
      yAxis = chart.getAxisLeft();
      // disable dual axis (only use LEFT axis)
      chart.getAxisRight().setEnabled(false);
      // horizontal grid lines
      yAxis.enableGridDashedLine(10f, 10f, 0f);
      // axis range
      yAxis.setAxisMaximum(255f);
      yAxis.setAxisMinimum(-30f);
    }


    {   // // Create Limit Lines // //
      LimitLine llXAxis = new LimitLine(9f, "Index 10");
      llXAxis.setLineWidth(4f);
//      llXAxis.enableDashedLine(10f, 10f, 0f);
      llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
      llXAxis.setTextSize(10f);
      llXAxis.setTypeface(tfRegular);

      limitMin = new LimitLine(25f, "Upper Limit");
      limitMin.setLineWidth(2f);
      limitMin.enableDashedLine(10f, 10f, 0);
      limitMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
      limitMin.setTextSize(10f);
      limitMin.setTypeface(tfRegular);

      limitMax = new LimitLine(-30f, "Lower Limit");
      limitMax.setLineWidth(2f);
      limitMax.enableDashedLine(10f, 10f, 10f);
      limitMax.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
      limitMax.setTextSize(10f);
      limitMax.setTypeface(tfRegular);

      // draw limit lines behind data instead of on top
      yAxis.setDrawLimitLinesBehindData(true);
      xAxis.setDrawLimitLinesBehindData(true);

      // add limit lines
      xAxis.addLimitLine(limitMin);
      xAxis.addLimitLine(limitMax);
      //xAxis.addLimitLine(llXAxis);
    }

    // add data
//    seekBarX.setProgress(45);
//    seekBarY.setProgress(180);
    setData(999, 200);
    // draw points over time
    chart.animateY(1700);
    // get the legend (only possible after setting data)
    Legend l = chart.getLegend();
    // draw legend entries as lines
    l.setForm(Legend.LegendForm.LINE);
  }


  private void setData(int count, float range) {

    ArrayList<Entry> values = new ArrayList<>();
    ArrayList<Entry> values2 = new ArrayList<>();

    for (int i = 0; i < count; i++) {

      float val = (float) (Math.random() * range) - 30;
      values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
    }


    if (limitMin != null) limitMin.setLabel("new TEXt");

    if (minX < 10) {
      minX = 10;
    }

    float x = (float)minX;



    values2.add(new Entry(x, 1f, getResources().getDrawable(R.drawable.star)));
    values2.add(new Entry(x+1f, 200f, getResources().getDrawable(R.drawable.star)));


    LineDataSet set1, set2;



    if (chart.getData() != null &&
        chart.getData().getDataSetCount() > 0) {
      set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
      set2 = (LineDataSet) chart.getData().getDataSetByIndex(1);
      set1.setValues(values);
      set2.setValues(values2);
//      set2.notifyDataSetChanged();

//      set1.notifyDataSetChanged();
      chart.getData().notifyDataChanged();
      chart.notifyDataSetChanged();
    } else {
      // create a dataset and give it a type
      set1 = new LineDataSet(values, "DataSet 1");
      set2 = new LineDataSet(values, "DataSet 2");
      set1.setDrawIcons(false);

      // draw dashed line
//      set1.enableDashedLine(10f, 5f, 0f);

      // black lines and points
//      set1.setColor(Color.BLACK);
//      set1.setCircleColor(Color.BLACK);

      // line thickness and point size
//      set1.setLineWidth(1f);
//      set1.setCircleRadius(0f);
      set1.setDrawCircles(false);
      // draw points as solid circles
      set1.setDrawCircleHole(false);

      set2.setDrawCircles(false);

      // customize legend entry
      set1.setFormLineWidth(1f);
      set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
      set1.setFormSize(15.f);

      // text size of values
      set1.setValueTextSize(9f);

      // draw selection line as dashed
      set1.enableDashedHighlightLine(10f, 5f, 0f);

      // set the filled area
//      set1.setDrawFilled(true);
      set1.setFillFormatter(new IFillFormatter() {
        @Override
        public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
          return chart.getAxisLeft().getAxisMinimum();
        }
      });

      // set color of filled area
      if (Utils.getSDKInt() >= 18) {
        // drawables only supported on api level 18 and above
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
        set1.setFillDrawable(drawable);
      } else {
        set1.setFillColor(Color.BLACK);
      }

      ArrayList<ILineDataSet> dataSets = new ArrayList<>();
      dataSets.add(set1); // add the data sets

      dataSets.add(set2); // add the data sets

      // create a data object with the data sets
      LineData data = new LineData(dataSets);

      // set data
      chart.setData(data);
    }
  }


  @Override
  public void onBackStackChanged() {
    getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }
}
