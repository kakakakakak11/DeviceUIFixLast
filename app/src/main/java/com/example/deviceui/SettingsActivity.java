package com.example.deviceui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity
{
  private static String APP_ID = "MqttAdviseClient";
  private static String HOSTNAME = "hostname";
  private static String USERNAME = "username";
  private static String PASSWORD = "password";
  private static String PORT = "port";

  private SharedPreferences mqttSettings;
  EditText editURL;
  EditText editPort;
  EditText editUsername;
  EditText editPassword;
  //Button btnSave;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
//    setContentView(R.layout.activity_settings);

//    editURL = (EditText) findViewById(R.id.editHostname);
//    editPort = (EditText) findViewById(R.id.editPort);
//    editUsername = (EditText) findViewById(R.id.editUsername);
//    editPassword = (EditText) findViewById(R.id.editPassword);

    mqttSettings = getSharedPreferences(APP_ID, MODE_PRIVATE);
    editURL.setText(mqttSettings.getString(HOSTNAME, ""));
    editPort.setText(mqttSettings.getString(PORT, ""));
    editUsername.setText(mqttSettings.getString(USERNAME, ""));
    editPassword.setText(mqttSettings.getString(PASSWORD, ""));

//    btnSave = (Button) findViewById(R.id.btnSave);
//    btnSave.setOnClickListener(new View.OnClickListener()
//    {
//      @Override
//      public void onClick(View v)
//      {
//        saveSettings();
//        finish();
//      }
//    });
  }

  private void saveSettings()
  {
    mqttSettings = getSharedPreferences(APP_ID, MODE_PRIVATE);
    //SharedPreferences.Editor editor = mqttSettings.edit();
    //editor.putString("broker", "192.168.0.128");
    //editor.putString("broker", "mosquitto.org")
    SharedPreferences.Editor editor = mqttSettings.edit();
    editor.putString(HOSTNAME, editURL.getText().toString());
    editor.putString(USERNAME, editUsername.getText().toString());
    editor.putString(PASSWORD, editPassword.getText().toString());
    editor.putString(PORT, editPort.getText().toString());
    editor.commit();

    Toast toast = Toast.makeText(getApplicationContext(), R.string.savedPreferences, Toast.LENGTH_SHORT);
    toast.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.activity_settings, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings_ok)
    {
      saveSettings();
      finish();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
