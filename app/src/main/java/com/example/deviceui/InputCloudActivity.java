package com.example.deviceui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Random;


public class InputCloudActivity extends AppCompatActivity {
  private MqttAndroidClient client;
  private String TAG = "MainActivity";
  private PahoMqttClient pahoMqttClient;

  //  private Button subscribe, unSubscribe;
  Button connection;
  ImageView iv_status;

  int minX = 0;
  int maxX = 0;
  private LineChart chart;
  protected Typeface tfRegular;
  protected Typeface tfLight;
  // limit line
  LimitLine limitMin;
  LimitLine limitMax;

  private String client_id = "";
  final String TOPIC = "/topic";
  boolean connectionMqtt = false;
  boolean download = false;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    System.out.println("onCreate");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_input_cloud);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    iv_status = (ImageView) findViewById(R.id.iv_status);

    // MQTT client
    pahoMqttClient = new PahoMqttClient();
    client_id = Constants.CLIENT_ID + (new Random().nextInt(32));
    client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, client_id);

    client.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean b, String s) {
        System.out.println("connect completed");
        connectionMqtt = true;
        updateStatus();
      }

      @Override
      public void connectionLost(Throwable throwable) {
        System.out.println("connection lost !!!!");
        connectionMqtt = false;
        updateStatus();
        try {
          pahoMqttClient.subscribe(client, TOPIC, 1);
          updateStatus();
        } catch (MqttException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
//        System.out.println("~~~~~~> Input: " + s + " Value: " + new String(mqttMessage.getPayload()));
        System.out.println("~ arrived client ID: " + client_id);
        String msg = new String(mqttMessage.getPayload());
        JSONArray arr = new JSONArray(new String(mqttMessage.getPayload()));
        String message = "";
        int[] chartData = new int[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
          message += "|" + arr.getInt(i);
          int value = arr.getInt(i);
          chartData[i] = value;
        }
        updateChartData(chartData);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
      }
    });


    TextView edit_speedMat = (TextView) findViewById(R.id.edit_speedMat);
    TextView txt_calcResult = (TextView) findViewById(R.id.txt_calcResult);
    Button btn_calc = (Button) findViewById(R.id.btn_calc);
    btn_calc.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO Расчет значений
        // кол. выбранные точек * (1/42 * 10^6) * скорость звука материала
        try {
          float speed = Float.parseFloat(edit_speedMat.getText().toString());
          float count = (maxX - minX);
          float result = count * (float) (1 / (42f * Math.pow(10, 6)) * speed);
          txt_calcResult.setText("" + result);
        } catch (Exception e) {
          System.out.println("ERROR calculation" + e);
        }

      }
    });

    // Range Seek bar
    RangeSeekBar rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
    rangeSeekBar.setRangeValues(0, 999);

    rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
      @Override
      public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        minX = (int) bar.getSelectedMinValue();
        maxX = (int) bar.getSelectedMaxValue();
        limitMin.setLimit(minX);
        limitMax.setLimit(maxX);
        chart.invalidate();
      }
    });

    configChart();

    Button btn_connection = (Button) findViewById(R.id.btn_connection);
    btn_connection.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
//        connect();
        onConnectionHandler();
      }
    });


//    subscribe.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        String topic = TOPIC;
//        if (!topic.isEmpty()) {
//          try {
//            pahoMqttClient.subscribe(client, topic, 1);
//          } catch (MqttException e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    });
//
//    unSubscribe.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
////        String topic = unSubscribeTopic.getText().toString().trim();
//        String topic = TOPIC;
//        if (!topic.isEmpty()) {
//          try {
//            pahoMqttClient.unSubscribe(client, topic);
//          } catch (MqttException e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    });

    Intent intent = new Intent(InputCloudActivity.this, MqttMessageService.class);
    startService(intent);
  }


  // mqtt connection
  void onConnectionHandler() {
    if (download) {   // disconnect
      try {
        pahoMqttClient.unSubscribe(client, TOPIC);
        download = false;
        updateStatus();
      } catch (MqttException e) {
        e.printStackTrace();
      }
    } else {  // connect
      if (client.isConnected()) {
        try {
          pahoMqttClient.subscribe(client, TOPIC, 1);
          download = true;
          updateStatus();
        } catch (MqttException e) {
          e.printStackTrace();
        }
      } else {  // reconnect
        try {
          client.connect();
          pahoMqttClient.subscribe(client, TOPIC, 1);
          download = true;
          updateStatus();
        } catch (MqttException e) {
          e.printStackTrace();
        }
      }
    }
  }


  void disconnect() {
    try {
      pahoMqttClient.unSubscribe(client, TOPIC);
      connectionMqtt = false;
//      download = false;
      updateStatus();
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }


  void updateStatus() {
    Button btn_connection = (Button) findViewById(R.id.btn_connection);
    ImageView iv_download = (ImageView) findViewById(R.id.iv_download);

    if (download) {
      btn_connection.setText("Остановить");
      if (connectionMqtt) {
        iv_download.setImageResource(R.drawable.ic_cloud_download);
      } else {
        iv_download.setImageResource(R.drawable.ic_download_pause);
      }
    } else {
      btn_connection.setText("Прием");
      iv_download.setImageResource(R.drawable.ic_download_pause);
    }

    if (iv_status != null) {
      if (connectionMqtt) {
        iv_status.setImageResource(R.drawable.ic_cloud_connected);
      } else {
        iv_status.setImageResource(R.drawable.ic_cloud_off);
      }
    }
  }


  @Override
  protected void onResume() {
    System.out.println("onResume");
    super.onResume();
  }

  void configChart() {
    tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
    tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

    {   // Chart Style //
      chart = findViewById(R.id.chart1);
      chart.setBackgroundColor(Color.WHITE);
      chart.getDescription().setEnabled(false);
      // enable touch gestures
      chart.setTouchEnabled(true);
      // set listeners
//      chart.setOnChartValueSelectedListener(this);
      chart.setDrawGridBackground(false);
      chart.setDragEnabled(true);
      chart.setScaleXEnabled(true);
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
      llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
      llXAxis.setTextSize(10f);
      llXAxis.setTypeface(tfRegular);

      limitMin = new LimitLine(0, "min");
      limitMin.setLineWidth(2f);
      limitMin.enableDashedLine(10f, 10f, 0f);
      limitMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
      limitMin.setTextSize(10f);
      limitMin.setTypeface(tfRegular);

      limitMax = new LimitLine(999f, "max");
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
    }

    // get the legend (only possible after setting data)
    Legend l = chart.getLegend();
    // draw legend entries as lines
    l.setForm(Legend.LegendForm.LINE);
  }

  private void updateChartData(int[] data) {
    System.out.println("~>~~>~>~>~>~>~>~ updateChartData");

    ArrayList<Entry> values = new ArrayList<>();

    int len = data.length;
    for (int i = 0; i < len; i++) {
      values.add(new Entry(i, data[i]));
    }

    LineDataSet set1;

    if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
      set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
      set1.setValues(values);
      chart.getData().notifyDataChanged();
      chart.notifyDataSetChanged();
      chart.invalidate();
    } else {
      // create a dataset and give it a type
      set1 = new LineDataSet(values, "");
      set1.setColor(Color.GREEN);
      set1.setDrawIcons(false);
      // draw dashed line
      set1.setDrawCircles(false);
      set1.setDrawCircleHole(false);
      // customize legend entry
      set1.setFormLineWidth(1f);
      set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
      set1.setFormSize(15.f);
      // text size of values
      set1.setValueTextSize(9f);
      // draw selection line as dashed
      set1.enableDashedHighlightLine(10f, 5f, 0f);

      ArrayList<ILineDataSet> dataSets = new ArrayList<>();
      dataSets.add(set1); // add the data sets
      // set data
      chart.setData(new LineData(dataSets));
      chart.invalidate();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    if (client != null) {
      disconnect();
    }

  }

}
