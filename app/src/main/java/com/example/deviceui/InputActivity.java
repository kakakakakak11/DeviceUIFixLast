package com.example.deviceui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class InputActivity extends AppCompatActivity {
  private MqttAndroidClient client;
  private String TAG = "MainActivity";
  private PahoMqttClient pahoMqttClient;

  private EditText textMessage, subscribeTopic, unSubscribeTopic;
//  private Button publishMessage, subscribe, unSubscribe;
  private Button subscribe, unSubscribe;

  // graph
  private LineGraphSeries<DataPoint> mGraph;
//  private final Handler mHandler = new Handler();
//  private Runnable mTimer;
  private double graph2LastXValue = 5d;


  //
  private Timer mTimer;
  private MyTimerTask mMyTimerTask;

  private String client_id = "";
  final String TOPIC = "/topic";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_input);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);


    GraphView graph = (GraphView) findViewById(R.id.graph);
    mGraph = new LineGraphSeries<>();
    graph.getViewport().setYAxisBoundsManual(true);
    graph.getViewport().setMinY(0);
    graph.getViewport().setMaxY(260);
    graph.getViewport().setXAxisBoundsManual(true);
    graph.getViewport().setMinX(0);
    graph.getViewport().setMaxX(999);

    // enable scaling and scrolling
    graph.getViewport().setScalable(true);
//    graph.getViewport().setBackgroundColor(Color.LTGRAY);
    graph.getGridLabelRenderer().setGridColor(Color.DKGRAY);

//    graph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
//    graph.getGridLabelRenderer().setNumVerticalLabels(countVertical);

    // activate horizontal scrolling
    graph.getViewport().setScrollable(true);
    // activate horizontal and vertical zooming and scrolling
    graph.getViewport().setScalableY(true);
    graph.getViewport().setScrollableY(true);


    graph.addSeries(mGraph);


    textMessage = (EditText) findViewById(R.id.textMessage);
//    publishMessage = (Button) findViewById(R.id.publishMessage);

    subscribe = (Button) findViewById(R.id.subscribe);
    unSubscribe = (Button) findViewById(R.id.unSubscribe);

    subscribeTopic = (EditText) findViewById(R.id.subscribeTopic);
    unSubscribeTopic = (EditText) findViewById(R.id.unSubscribeTopic);

    // MQTT client
    pahoMqttClient = new PahoMqttClient();

    client_id = Constants.CLIENT_ID + (new Random().nextInt(25));
//    client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
    client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, client_id);



//    mTimer = new Timer();
//    mMyTimerTask = new MyTimerTask();
//    mTimer.schedule(mMyTimerTask, 1000, 300);


    System.out.println("~~~~~~~~> APP CONTEXT" + getApplicationContext());

    client.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean b, String s) {
        System.out.println("connect completed");
      }

      @Override
      public void connectionLost(Throwable throwable) {
        System.out.println("connection lost !!!!");
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
//        System.out.println("~~~~~~> Input: " + s + " Value: " + new String(mqttMessage.getPayload()));
        System.out.println("~ arrived client ID: " + client_id);
//                setMessageNotification(s, new String(mqttMessage.getPayload()));

        String msg = new String(mqttMessage.getPayload());
//
        JSONArray arr = new JSONArray(new String(mqttMessage.getPayload()));
//
//        System.out.println("~~~~~> ARRAY: " + arr);
//        System.out.println(arr);
//
        String message = "";
//
        for (int i = 0; i < arr.length(); i++) {
          message += "|" + arr.getInt(i);
//
          int value = arr.getInt(i);
//
          graph2LastXValue += 5d;
          mGraph.appendData(new DataPoint(graph2LastXValue, value), true, 15984);
        }
//
//
//        System.out.println("~~~~~> ARRAY INPUT : " + message);

      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

      }

    });


//    publishMessage.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        String msg = textMessage.getText().toString().trim();
////        if (!msg.isEmpty()) {
////          try {
////            String message = "";
////            final int size = 155;
////            int[] array = new int[size];
////
////            for (int i = 0; i < size; i++) {
////              int value = (int)(new Random().nextInt(200));
////              message += i + ":" + value + "|";
////              array[i] = value & 0xFF;
////            }
////
////            JSONArray arr = new JSONArray(array);
////            pahoMqttClient.publishMessage(client, arr.toString(), 1, Constants.PUBLISH_TOPIC);
////
////          } catch (MqttException e) {
////            e.printStackTrace();
////          } catch (UnsupportedEncodingException e) {
////            e.printStackTrace();
////          } catch (JSONException e) {
////            e.printStackTrace();
////          }
////        }
//      }
//    });

    subscribe.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
//        String topic = subscribeTopic.getText().toString().trim();
        String topic = TOPIC;
        if (!topic.isEmpty()) {
          try {
            pahoMqttClient.subscribe(client, topic, 1);
          } catch (MqttException e) {
            e.printStackTrace();
          }
        }
      }
    });

    unSubscribe.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
//        String topic = unSubscribeTopic.getText().toString().trim();
        String topic = TOPIC;
        if (!topic.isEmpty()) {
          try {
            pahoMqttClient.unSubscribe(client, topic);
          } catch (MqttException e) {
            e.printStackTrace();
          }
        }
      }
    });

    Intent intent = new Intent(InputActivity.this, MqttMessageService.class);
    startService(intent);

  }

  @Override
  protected void onStop() {
    super.onStop();

    if (mTimer != null) {
      mTimer.cancel();
      mTimer = null;
    }
  }

  class MyTimerTask extends TimerTask {

    @Override
    public void run() {

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          System.out.println("TIMER HANDLER");


          try {

            String message = "";
            final int size = 157;
            int[] array = new int[size];

            for (int i = 0; i < size; i++) {
              int value = (int)(new Random().nextInt(100) + 50);
              message += i + ":" + value + "|";
              array[i] = value & 0xFF;
            }
//
            JSONArray arr = new JSONArray(array);
            pahoMqttClient.publishMessage(client, arr.toString(), 0, Constants.PUBLISH_TOPIC);
//
          } catch (MqttException e) {
            e.printStackTrace();
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    }
  }


}
