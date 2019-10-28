package com.example.deviceui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.Random;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
  private static final String DEBUG_TAG = "MqttService";
  public static String APP_ID = "MqttAdviseClient";
  private static String HOSTNAME = "hostname";
  private static String TOPIC = "topic";
  private static String USERNAME = "username";
  private static String PASSWORD = "password";
  private static String PORT = "port";
  private static String MESSAGE = "message";


  int minX = 0;
  int maxX = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);

    // input data from server
    Button btnInput = (Button) findViewById(R.id.btn_input);

    btnInput.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Intent myIntent = new Intent(view.getContext(), InputActivity.class);
        startActivity(myIntent);
      }
    });

    // bluetooth
    Button btnBT = (Button) findViewById(R.id.btn_bt);
    btnBT.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Intent myIntent = new Intent(view.getContext(), DeviceActivity.class);
        startActivity(myIntent);
      }
    });


    GraphView graph = (GraphView) findViewById(R.id.graph);
    LineGraphSeries<DataPoint> mGraph = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> mGraph2 = new LineGraphSeries<>();


    graph.getViewport().setYAxisBoundsManual(true);
    graph.getViewport().setMinY(0);
    graph.getViewport().setMaxY(260);

    graph.getViewport().setXAxisBoundsManual(true);
    graph.getViewport().setMinX(0);
    graph.getViewport().setMaxX(1080);

    // enable scaling and scrolling
    graph.getViewport().setScalable(true);
//    graph.getViewport().setBackgroundColor(Color.LTGRAY);
    graph.getGridLabelRenderer().setGridColor(Color.DKGRAY);

//    graph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
//    graph.getGridLabelRenderer().setNumVerticalLabels(countVertical);

    // activate horizontal scrolling
    graph.getViewport().setScrollable(true);
    // activate horizontal and vertical zooming and scrolling
//    graph.getViewport().setScalableY(true);
//    graph.getViewport().setScrollableY(true);
//    graph.getSecondScale().setVerticalAxisTitleTextSize();
    graph.addSeries(mGraph);
    graph.addSeries(mGraph2);
    mGraph2.setColor(Color.CYAN);

    DataPoint point = new DataPoint(100, 20);
    DataPoint point2 = new DataPoint(100, 200);


    mGraph2.appendData(point, false, 999);
    mGraph2.appendData(point2, false, 999);

    double graph2LastXValue = 5d;
    final int size= 999;

    for (int i = 0; i < size; i++) {

      int value = (int)(new Random().nextInt(200));

      graph2LastXValue += 1d;
      mGraph.appendData(new DataPoint(graph2LastXValue, value), true, 999);
    }


    // Range Seek Bar
    RangeSeekBar rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
    rangeSeekBar.setRangeValues(0, 999);
    rangeSeekBar.setNotifyWhileDragging(true);


    rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
      @Override
      public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        Number min_value = bar.getSelectedMinValue();
        Number max_value = bar.getSelectedMaxValue();

        int min = (int)min_value;
        int max = (int)max_value;

        minX = min;
        maxX = max;

        Toast.makeText(getApplicationContext(), "Min=" + min + "\n" + "Max=" + max, Toast.LENGTH_SHORT).show();
      }
    });


    TextView edit_speedMat = (TextView) findViewById(R.id.edit_speedMat);
    Button btn_calc = (Button) findViewById(R.id.btn_calc);
    btn_calc.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO Расчет значений
        // кол. выбранные точек * (1/42 * 10^6) * скорость звука материала
        try {
          float speed = Float.parseFloat(edit_speedMat.getText().toString());
          float count = (maxX - minX);
          float result = count * (float)( 1 / (42f * Math.pow(10, 6)) * speed);

          Toast.makeText(getApplicationContext(), "RESULT" + result, Toast.LENGTH_SHORT).show();

//          txt_calcResult.setText("" + result);
        } catch (Exception e) {
          System.out.println("ERROR calculation" + e);
        }

      }
    });



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
