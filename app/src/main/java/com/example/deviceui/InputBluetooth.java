package com.example.deviceui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static android.app.PendingIntent.getActivity;

public class InputBluetooth extends AppCompatActivity implements ServiceConnection, SerialListener {

  // device name "BUFD_BLE-62BE
  private String deviceAddress = "80:1F:12:B9:62:BE";

  // BT
  private enum Connected {False, Pending, True, Disabled}
  private SerialSocket socket;
  private SerialService service;
  private boolean initialStart = true;
  private Connected connected = Connected.False;
  private boolean inputEnabled = false;
  private int countPackages = 0;
  private int countCharts = 0;

  // MQTT
  private MqttAndroidClient client;
  private String TAG = "MainActivity";
  private PahoMqttClient pahoMqttClient;
  boolean connectionMqtt = false;

  private int pBuff = 0;
  private int[] buff = new int[1500];

  // chart
  int minX = 0;
  int maxX = 0;
  private LineChart chart;
  private Typeface tfRegular;
  private Typeface tfLight;
  // limit line
  private LimitLine limitMin;
  private LimitLine limitMax;

  private int amountData = 0;
  private int amountBytes = 0;
  private boolean first = true;

  // amplifier
  int ampMinX = 0;
  int ampMaxX = 0;
  boolean ampEnable = false;
  boolean ampSetting = false;

  int[] lastChartData = new int[1000];


  // enable bluetooth
  private void configureBluetooth() {
    BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter btAdapter = btManager.getAdapter();

    if (btAdapter != null && !btAdapter.isEnabled()) {
      System.out.println(R.string.btdisabled);
      connected = Connected.Disabled;

      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, 1);
    }
  }

  /*
   * Lifecycle
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_input_bluetooth);

    configureBluetooth();


    Button btn_connect = (Button) findViewById(R.id.btn_deviceConnect);
    btn_connect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        connect();
      }
    });

    TextView edit_speedMat = (TextView) findViewById(R.id.edit_speedMat);
    TextView edit_hzs = (TextView) findViewById(R.id.edit_hzs);
    TextView txt_calcResult = (TextView) findViewById(R.id.txt_calcResult);
    Button btn_calc = (Button) findViewById(R.id.btn_calc);
    btn_calc.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO Расчет значений
        // кол. выбранные точек * (1/42 * 10^6) * скорость звука материала
        try {
          float speed = Float.parseFloat(edit_speedMat.getText().toString());
          float hzs = Float.parseFloat(edit_hzs.getText().toString());
          float result = Calculation.calc(minX, maxX, speed, hzs);
          txt_calcResult.setText("" + result + " (mm)");
        } catch (Exception e) {
          System.out.println("ERROR calculation" + e);
        }
      }
    });

//    if(service != null) {
//      System.out.println("onStart -> attach");
//      service.attach(this);
//    }
//    else {
    System.out.println("onStart -> startService");

    this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
//    }

    configMqtt();
    configChart();

    ImageButton btn_amp_enable = (ImageButton) findViewById(R.id.btn_amp_enable);
    btn_amp_enable.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (ampEnable) ampEnable = false;
        else ampEnable = true;

        if (ampEnable) {
          btn_amp_enable.setImageDrawable((getDrawable(R.drawable.ic_chart_amp_enabled)));
        } else {
          btn_amp_enable.setImageDrawable((getDrawable(R.drawable.ic_chart_amp_disabled)));
        }

        updateChartData(lastChartData);
      }
    });

    ImageButton btn_amp_setting = (ImageButton) findViewById(R.id.btn_amp_setting);
    btn_amp_setting.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (ampSetting) ampSetting = false;
        else ampSetting = true;

        if (ampSetting) {
          btn_amp_setting.setImageDrawable((getDrawable(R.drawable.ic_chart_amp_setting)));
        } else {
          btn_amp_setting.setImageDrawable((getDrawable(R.drawable.ic_chart_amp_setting_disabled)));
        }
      }
    });

    int[] init = new int[1000];
    Arrays.fill(init, 1);
    updateChartData(init);
  }

  void configMqtt() {
    pahoMqttClient = new PahoMqttClient();
    final String client_id = "bt_" + (new Random().nextInt(100));
    client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, client_id);
    client.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean b, String s) {
        System.out.println(R.string.connectСompleted);
        connectionMqtt = true;
        updateStatus();
      }

      @Override
      public void connectionLost(Throwable throwable) {
        System.out.println(R.string.connectLost);
        connectionMqtt = false;
        updateStatus();
        try {
          client.connect();
//          connectionMqtt = true;
//          updateStatus();
        } catch (MqttException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
      }
    });
  }

  void updateStatus() {
    Button btn_connection = (Button) findViewById(R.id.btn_connection);
    ImageView iv_bt = (ImageView) findViewById(R.id.iv_bt);
    ImageView iv_mqtt = (ImageView) findViewById(R.id.iv_mqtt);

    // bt
    if (connected == Connected.True) {
      iv_bt.setImageResource(R.drawable.ic_bluetooth_connected);
    }
    if (connected == Connected.False) {
      iv_bt.setImageResource(R.drawable.ic_bluetooth);
    }
    if (connected == Connected.Pending) {
      iv_bt.setImageResource(R.drawable.ic_bluetooth_search);
    }
    if (connected == Connected.Disabled) {
      iv_bt.setImageResource(R.drawable.ic_bluetooth_disabled);
    }

    // mqtt
    if (connectionMqtt) {
      iv_mqtt.setImageResource(R.drawable.ic_cloud_connected);
    } else {
      iv_mqtt.setImageResource(R.drawable.ic_cloud_off);
    }
  }


  private void configChart() {
    RangeSeekBar rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
    rangeSeekBar.setTextAboveThumbsColor(Color.BLACK);
    rangeSeekBar.setRangeValues(0, 999);
    rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
      @Override
      public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        int min = (int) bar.getSelectedMinValue();
        int max = (int) bar.getSelectedMaxValue();

        if (ampSetting) {
          ampMinX = Math.max(min, 1);
          ampMaxX = max;
          limitMin.setLimit(ampMinX);
          limitMax.setLimit(ampMaxX);
          System.out.println("amp minX " + ampMinX + " maxX: " + ampMaxX);
          chart.invalidate();
          updateChartData(lastChartData);
        } else {
          minX = min;
          maxX = max;
          limitMin.setLimit(minX);
          limitMax.setLimit(maxX);
          System.out.println("minX " + minX + " maxX: " + maxX);
          chart.invalidate();
        }
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
      yAxis.setAxisMinimum(0f);
    }

    {   // // Create Limit Lines // //
      LimitLine llXAxis = new LimitLine(9f, "");
      llXAxis.setLineWidth(4f);
//      llXAxis.enableDashedLine(10f, 10f, 0f);
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
      //xAxis.addLimitLine(llXAxis);
    }

    // draw points over time
    chart.animateX(1500);
    // get the legend (only possible after setting data)
    Legend l = chart.getLegend();

//     draw legend entries as lines
    l.setForm(Legend.LegendForm.LINE);

  }

  private void updateChartData(int[] data) {
    System.out.println("~>~~>~>~>~>~>~>~ updateChartData");

    ArrayList<Entry> values = new ArrayList<>();

    int len = data.length;
    for (int i = 0; i < len; i++) {
      values.add(new Entry(i, data[i]));
    }

    ArrayList<Entry> values2 = new ArrayList<>();

    float[] data2;

    if (ampEnable) {
      data2 = Calculation.amplifier(data, ampMinX, ampMaxX);
    } else {
      data2 = new float[ampMaxX];

      for (int i = 0; i < len; i++) {
        if (i > (ampMinX) && i < ampMaxX) data2[i] = 0;
      }
    }

    System.out.println("minX: " + minX + " maxX: " + maxX);

    len = data2.length;

    for (int i = 0; i < len; i++) {
      if (i > (ampMinX) && i < ampMaxX) {
        values2.add(new Entry(i, data2[i]));
      }
    }

    LineDataSet set1;
    LineDataSet set2;

    if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
      set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
      set1.setValues(values);

      set2 = (LineDataSet) chart.getData().getDataSetByIndex(1);
      set2.setValues(values2);

      chart.getData().notifyDataChanged();
      chart.notifyDataSetChanged();
      chart.invalidate();
    } else {
      // create a dataset and give it a type
      set1 = new LineDataSet(values, "-");
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

      // ---
      set2 = new LineDataSet(values2, "--");
      set2.setColor(Color.RED);
      set2.setDrawIcons(false);
      set2.setDrawCircles(false);
      set2.setDrawCircleHole(false);
      set2.setFormLineWidth(1f);
      set2.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
      set2.setFormSize(15.f);
      // text size of values
      set2.setValueTextSize(9f);
      // draw selection line as dashed
      set2.enableDashedHighlightLine(10f, 5f, 0f);
      // ------


      ArrayList<ILineDataSet> dataSets = new ArrayList<>();

      dataSets.add(set2); // add the data sets
      dataSets.add(set1); // add the data sets


      // set data
      chart.setData(new LineData(dataSets));
      chart.invalidate();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    System.out.println("InputBluetooth -> onDestroy");

    if (connected != Connected.False)
      disconnect();

    stopService(new Intent(getApplicationContext(), SerialService.class));

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

      System.out.println(R.string.connectingTo + deviceName + R.string.address + deviceAddress);

      System.out.println("SERVICE: " + service);

      if (service != null) {
        connected = Connected.Pending;
        service.connect(this, R.string.connectingTo + deviceName);
        socket = new SerialSocket();
        socket.connect(getApplicationContext(), service, device);

        updateStatus();

        Toast info = Toast.makeText(getApplicationContext(), R.string.connecting, Toast.LENGTH_SHORT);
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

    if (service != null) service.disconnect();
    if (socket != null) socket.disconnect();

    updateStatus();
    inputEnabled = false;
    pBuff = 0;

    socket = null;
  }


  private void receive(byte[] data) {
    System.out.println("~~~~~~~~~~~> RECIEVE DATA:  " + data);
    System.out.println("~~~~~~~~~~~> RECIEVE DSIZE:  " + data.length);
//    receiveText.append(new String(data));
  }

  // TODO Receive
  private void receive(byte[] data, int[] datai) {
    System.out.println("~~~~~~~~> RECIEVE DATASIZE: " + datai.length);

    int sizePackage = datai.length;

    if (inputEnabled) {
      countPackages++;
      System.out.println("~>~>~>~>~>~>~>~>>~ COUNT PACKEGS: " + countPackages);

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
//      if (pBuff >= 950) {
      if (countPackages >= 7) {
        JSONArray arr = null;
        try {
          int[] tmp = new int[pBuff-1];
          System.arraycopy(buff, 0, tmp, 0, pBuff-1);


          updateChartData(tmp);

          arr = new JSONArray(tmp);
//          System.out.println("<~~~~~~~>    BUFF: " + arr.toString());
          countCharts++;
          System.out.println("~>~>~>~>~>~>~>~>>~~>~>~>~>~>~>~ CHARTS COUNT : " + countCharts);

          pahoMqttClient.publishMessage(client, arr.toString(), 0, Constants.PUBLISH_TOPIC);

        } catch (JSONException e) {
          e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        } catch (MqttException e) {
          e.printStackTrace();
        }

        pBuff = 0;
        countPackages = 0;
      }
    } else {
      if (sizePackage < 100) {  // last package
        System.out.println("~>>>>>~>~>~>~>~>~~>~>~>~>~>~ Size package < 100 ");
        inputEnabled = true;
        countPackages = 0;
        pBuff = 0;
      }
    }



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
    updateStatus();
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



