package com.example.deviceui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.jjoe64.graphview.series.DataPoint;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static android.app.PendingIntent.getActivity;

public class InputBluetooth extends AppCompatActivity implements ServiceConnection, SerialListener {

  boolean connectionBt = false;
  boolean connectionServer = false;


  // device name "BUFD_BLE-62BE
  private String deviceAddress = "80:1F:12:B9:62:BE";

  // BT
  private enum Connected { False, Pending, True }
  private SerialSocket socket;
  private SerialService service;
  private boolean initialStart = true;
  private Connected connected = Connected.False;

  // MQTT
  private MqttAndroidClient client;
  private String TAG = "MainActivity";
  private PahoMqttClient pahoMqttClient;
  boolean connectionMqtt = false;


  private int pBuff = 0;
  private int[] buff = new int[1000];

  // chart
  int minX = 0;
  int maxX = 0;
  private LineChart chart;
  private SeekBar seekBarX, seekBarY;
  private TextView tvX, tvY;
  private Typeface tfRegular;
  private Typeface tfLight;
  // limit line
  private LimitLine limitMin;
  private LimitLine limitMax;

  private int amountData = 0;
  private int amountBytes = 0;
  private int[] lastData;
  private boolean first = true;

  /*
   * Lifecycle
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_input_bluetooth);


    Button btn_connect = (Button) findViewById(R.id.btn_deviceConnect);
    btn_connect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        connect();
      }
    });

//    if(service != null) {
//      System.out.println("onStart -> attach");
//      service.attach(this);
//    }
//    else {
      System.out.println("onStart -> startService");

      Intent intent = new Intent(this, SerialService.class);
      startService(intent);
//      this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
//    }

    // MQTT client
    pahoMqttClient = new PahoMqttClient();

    final String client_id = "bt_device";
//        client = pahoMqttClient.getMqttClient(this.getContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
    client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, client_id);



    configChart();

  }

  private void configChart() {
    limitMin = new LimitLine(25f, "Upper Limit");
    limitMin.setLineWidth(4f);
    limitMin.enableDashedLine(20f, 10f, 0f);
    limitMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
    limitMin.setTextSize(10f);
    limitMin.setTypeface(tfRegular);

    RangeSeekBar rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
    rangeSeekBar.setRangeValues(0, 999);
    rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
      @Override
      public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
//        Number min_value = bar.getSelectedMinValue();
//        Number max_value = bar.getSelectedMaxValue();

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

      limitMax = new LimitLine(-30f, "Lower Limit");
      limitMax.setLineWidth(4f);
      limitMax.enableDashedLine(10f, 10f, 0f);
      limitMax.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
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

    // draw points over time
    chart.animateX(1500);

    // get the legend (only possible after setting data)
    Legend l = chart.getLegend();

//     draw legend entries as lines
    l.setForm(Legend.LegendForm.LINE);

  }

  private void setChartData(int count, float range) {
    ArrayList<Entry> values = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      float val = (float) (Math.random() * range) - 30;
      values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
    }

    LineDataSet set1;

    if (chart.getData() != null &&
        chart.getData().getDataSetCount() > 0) {
      set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
      set1.setValues(values);
//      set2.notifyDataSetChanged();

//      set1.notifyDataSetChanged();
      chart.getData().notifyDataChanged();
      chart.notifyDataSetChanged();
    } else {
      // create a dataset and give it a type
      set1 = new LineDataSet(values, "DataSet 1");
      set1.setColor(Color.RED);
      set1.setDrawIcons(false);
      // draw dashed line
//      set1.enableDashedLine(10f, 5f, 0f);
      set1.setDrawCircles(false);
      // draw points as solid circles
      set1.setDrawCircleHole(false);


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
//      set1.setFillFormatter(new IFillFormatter() {
//        @Override
//        public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
//          return chart.getAxisLeft().getAxisMinimum();
//        }
//      });

      // set color of filled area
//      if (Utils.getSDKInt() >= 18) {
//        // drawables only supported on api level 18 and above
//        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
//        set1.setFillDrawable(drawable);
//      } else {
//        set1.setFillColor(Color.BLACK);
//      }
      ArrayList<ILineDataSet> dataSets = new ArrayList<>();
      dataSets.add(set1); // add the data sets
      // create a data object with the data sets
      LineData data = new LineData(dataSets);

      // set data
      chart.setData(data);
    }
  }

  private void updateChartData(int[] data) {
    System.out.println("~~>~~>~>~>~>~>~>~ updateChartData");

    ArrayList<Entry> values = new ArrayList<>();

    int len = data.length;
    for (int i = 0; i < len; i++) {
      values.add(new Entry(i, data[i]));
    }

    LineDataSet set1;

    if (chart.getData() != null &&
        chart.getData().getDataSetCount() > 0) {
      set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
      set1.setValues(values);
      chart.getData().notifyDataChanged();
      chart.notifyDataSetChanged();
      chart.invalidate();
    } else {
      // create a dataset and give it a type
      set1 = new LineDataSet(values, "DataSet 1");
      set1.setColor(Color.GREEN);
      set1.setDrawIcons(false);
      // draw dashed line
      set1.setDrawCircles(false);
      // draw points as solid circles
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
      // create a data object with the data sets
//      LineData data = ;
      // set data
      chart.setData(new LineData(dataSets));
      chart.invalidate();
    }
  }

  @Override
  public void onDestroy() {
    System.out.println("InputBluetooth -> onDestroy");

    if (connected != Connected.False)
      disconnect();
    stopService(new Intent(getApplicationContext(), SerialService.class));
    super.onDestroy();
  }

  @Override
  public void onStart() {
    System.out.println("onStart");
    super.onStart();

    if(service != null) {
      System.out.println("onStart -> attach");
      service.attach(this);
    }
    else {
      System.out.println("onStart -> startService");

      Intent intent = new Intent(this, SerialService.class);
      startService(intent);
//      this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

  }

  @Override
  public void onStop() {
    System.out.println("onStop");
    if(service != null && !this.isChangingConfigurations()) {
      try { unbindService(this); } catch(Exception ignored) {}
      service.detach();
      try { unbindService(this); } catch(Exception ignored) {}
    }

    super.onStop();
  }

//  @Override
//  public void onAttach(Activity activity) {
////    super.onAttach(activity);
//    this.bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
//  }
//
//  @Override
//  public void onDetach() {
//    try { this.unbindService(this); } catch(Exception ignored) {}
//    super.onDetach();
//  }

  @Override
  public void onResume() {
    super.onResume();
    if(initialStart && service !=null) {
      initialStart = false;
      this.runOnUiThread(this::connect);
    }
  }




  ////////////////////////////////
  @Override
  public void onServiceConnected(ComponentName name, IBinder binder) {
    System.out.println("onServiceConnected");

    service = ((SerialService.SerialBinder) binder).getService();
//    if(initialStart && isResumed()) {
    if(initialStart) {

      System.out.println("initial start false");

      initialStart = false;
      this.runOnUiThread(this::connect);

    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    System.out.println("onServiceDisconnected");
    service = null;
  }

  /*
   * Serial + UI
   */
  private void connect() {
    try {
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
      String deviceName = device.getName() != null ? device.getName() : device.getAddress();
      status("connecting...");

      System.out.println("Connecting to " + deviceName + " | address " + deviceAddress);

      System.out.println("SERVICE: " + service);

      if (service != null) {
        connected = Connected.Pending;
        service.connect(this, "Connecting to " + deviceName);
        socket = new SerialSocket();
        socket.connect(getApplicationContext(), service, device);

        Toast info = Toast.makeText(getApplicationContext(), "connecting...", Toast.LENGTH_SHORT);
        info.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 300);
        info.show();
      } else {
        System.out.println("InputBluetooth > connect. Service is null");
        bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
      }



    } catch (Exception e) {
      onSerialConnectError(e);
    }
  }

  private void disconnect() {
    System.out.println("disconnect()");
    connected = Connected.False;
    service.disconnect();
    socket.disconnect();


    socket = null;
  }

  private void send(String str) {
//        if(connected != Connected.True) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
//            byte[] data = (str + newline).getBytes();
//            socket.write(data);
//        } catch (Exception e) {
//            onSerialIoError(e);
//        }
//        String msg = str.getText().toString().trim();
    String msg = str.trim();
//        if (!msg.isEmpty()) {
    try {
      pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);
    } catch (MqttException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
//        }
  }



  private void receive(byte[] data) {
    System.out.println("~~~~~~~~~~~> RECIEVE DATA:  " + data);
    System.out.println("~~~~~~~~~~~> RECIEVE DSIZE:  " + data.length);
//    receiveText.append(new String(data));
  }

  // TODO Receive
  private void receive(byte[] data, int[] datai) {
    System.out.println("~~~~~~~~> RECIEVE DATASIZE: " + datai.length);

    amountData++;
//        System.out.println("!!!!~~~~~~~~~> pBuff" + pBuff);

    int count = 0;
    for (int i = 0; i < datai.length-1; i++) {
      count++;
      buff[pBuff] = datai[i];
      pBuff++;
    }

    amountBytes += amountData + count;

//        receiveText.setText("count: " + amountData + " | bytes: " + amountBytes);
        System.out.println("~~~~~~~> count: " + amountData + " | bytes: " + amountBytes);
//    System.out.println("~~~~~~~_____!!!!!!~~~> COUNT: " + count + "   dataLENGHT " + datai.length);
//        receiveText.setText("" + sum);

    // mqtt send
    if (pBuff >= 950) {
      JSONArray arr = null;
      try {
        int[] tmp = new int[pBuff-1];
        System.arraycopy(buff, 0, tmp, 0, pBuff-1);

        updateChartData(tmp);

        arr = new JSONArray(tmp);
        System.out.println("<~~~~~~~>    BUFF: " + arr.toString());
        pahoMqttClient.publishMessage(client, arr.toString(), 0, Constants.PUBLISH_TOPIC);
      } catch (JSONException e) {
        e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (MqttException e) {
        e.printStackTrace();
      }

      pBuff = 0;
    }

    first = false;
  }

  private void status(String str) {
    SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//    spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//    receiveText.append(spn);

    System.out.println("Status: " + spn);

    Toast info = Toast.makeText(getApplicationContext(), "status " + spn, Toast.LENGTH_SHORT);
    info.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 250);
    info.show();
  }

  /*
   * SerialListener
   */
  @Override
  public void onSerialConnect() {
    System.out.println("onSerialConnect");
    status("connected");
    connected = Connected.True;
  }

  @Override
  public void onSerialConnectError(Exception e) {
    System.out.println("Serial ERROR -> connection failed: " + e.getMessage());
    System.out.println("Serial ERROR > " + e);
    status("connection failed: " + e.getMessage());
    disconnect();
  }

  @Override
  public void onSerialRead(byte[] data) {
    receive(data);
  }

  @Override
  public void onSerialRead(byte[] data, int[] datai) {
    receive(data, datai);
  }

  @Override
  public void onSerialIoError(Exception e) {
    status("connection lost: " + e.getMessage());
    disconnect();
  }

}



