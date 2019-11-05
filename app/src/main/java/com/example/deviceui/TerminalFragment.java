package com.example.deviceui;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private String newline = "\r\n";
    private TextView receiveText;

    // BT
    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    // Graph
    private GraphView graphView;
    private LineGraphSeries<DataPoint> mSeries1;
    private double graph2LastXValue = 1d;
    private int amountData = 0;
    private int amountBytes = 0;
    private int[] lastData;
    private boolean first = true;

    // MQTT
    private MqttAndroidClient client;
    private String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;

    //
    private int minX = 0;
    private int maxX = 999;


    private int pBuff = 0;
    private int[] buff = new int[1000];


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }



    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
//        TextView sendText = view.findViewById(R.id.send_text);
//        View sendBtn = view.findViewById(R.id.send_btn);
//        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));


//        GraphView graph = (GraphView) findViewById(R.id.graph);
        graphView = (GraphView) view.findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<>();
        mSeries1.setColor(Color.BLUE);
//        mSeries2 = new LineGraphSeries<>();
//        mSeries2.setColor(Color.RED);


        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(260);
        graphView.getViewport().setXAxisBoundsManual(false);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(999);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        // enable scaling and scrolling
        graphView.getViewport().setYAxisBoundsManual(false);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);
        graphView.getViewport().setBackgroundColor(Color.WHITE);
        graphView.getGridLabelRenderer().setGridColor(Color.CYAN);
//        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(true);
//        graph.getGridLabelRenderer().setNumVerticalLabels(countVertical);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScalableY(true);
        graphView.getViewport().setScrollableY(true);
        graphView.addSeries(mSeries1);


        // MQTT client
        pahoMqttClient = new PahoMqttClient();

        final String client_id = "bt_device";
//        client = pahoMqttClient.getMqttClient(this.getContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
        client = pahoMqttClient.getMqttClient(this.getContext(), Constants.MQTT_BROKER_URL, client_id);


        TextView edit_speedMat = (TextView) view.findViewById(R.id.edit_speedMat);
        TextView txt_calcResult = (TextView) view.findViewById(R.id.txt_calcResult);
        Button btn_calc = (Button) view.findViewById(R.id.btn_calc);
        btn_calc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Расчет значений
                // кол. выбранные точек * (1/42 * 10^6) * скорость звука материала
                try {
                    float speed = Float.parseFloat(edit_speedMat.getText().toString());
                    float count = (maxX - minX);
                    float result = count * (float)( 1 / (42f * Math.pow(10, 6)) * speed);

                    txt_calcResult.setText("" + result);
                } catch (Exception e) {
                    System.out.println("ERROR calculation" + e);
                }

            }
        });


        // Range Seek bar
        RangeSeekBar rangeSeekBar = (RangeSeekBar) view.findViewById(R.id.rangeSeekBar);
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

                //Toast.makeText(view.getContext(), "Min=" + min + "\n" + "Max=" + max, Toast.LENGTH_SHORT).show();

                Toast info = Toast.makeText(view.getContext(), "[ " + min + " - " + max + " ]"  + "\n" + (max - min), Toast.LENGTH_SHORT);
                info.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 200);
                info.show();
            }
        });


        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id ==R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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

            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connecting to " + deviceName);
            socket.connect(getContext(), service, device);

            Toast info = Toast.makeText(getActivity().getApplicationContext(), "connecting...", Toast.LENGTH_SHORT);
            info.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 300);
            info.show();

        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
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
        receiveText.append(new String(data));
    }

    // TODO Receive
    private void receive(byte[] data, int[] datai) {
        System.out.println("~~~~~~~~~~~> RECIEVE DATA:  " + datai);

        String text = "";
        int sum = 0;

//        for (int i = 0; i < datai.length; i++) {
//            text += " " + datai[i];
//            sum += datai[i];
////            pBuff += i;
////            buff[pBuff + i] = data[i];
//        }

//        receiveText.append(text);

        if (!first) {
            lastData = datai;
        }

        amountData++;

//        System.out.println("!!!!~~~~~~~~~> pBuff" + pBuff);


        int count = 0;
        for (int i = 0; i < datai.length-1; i++) {
            graph2LastXValue += 5d;
            count++;
            buff[pBuff] = datai[i];
            pBuff++;
            mSeries1.appendData(new DataPoint(graph2LastXValue, datai[i]), true, 999);
        }

        amountBytes += amountData + count;

//        receiveText.setText("count: " + amountData + " | bytes: " + amountBytes);
//        System.out.println("~~~~~~~> count: " + amountData + " | bytes: " + amountBytes);
        System.out.println("~~~~~~~_____!!!!!!~~~> COUNT: " + count + "   dataLENGHT " + datai.length);
//        receiveText.setText("" + sum);

        // mqtt send
//        if (pBuff >= 999) {
//          JSONArray arr = null;
//          try {
//            arr = new JSONArray(datai);
//            pahoMqttClient.publishMessage(client, arr.toString(), 1, Constants.PUBLISH_TOPIC);
//          } catch (JSONException e) {
//            e.printStackTrace();
//          } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//          } catch (MqttException e) {
//            e.printStackTrace();
//          }
//
//          pBuff = 0;
//        }
        if (pBuff >= 960) {
            JSONArray arr = null;
            try {
                int[] tmp = new int[pBuff-1];
                System.arraycopy(buff, 0, tmp, 0, pBuff-1);

                // TODO input data
//                System.arraycopy(tmp, 0, inputData, 0, tmp.length);


//          arr = new JSONArray(buff);
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
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);

        Toast info = Toast.makeText(getActivity().getApplicationContext(), "status " + spn, Toast.LENGTH_SHORT);
        info.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 250);
        info.show();
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
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
