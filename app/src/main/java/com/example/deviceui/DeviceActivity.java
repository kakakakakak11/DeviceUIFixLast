package com.example.deviceui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;


public class DeviceActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(this);
    if (savedInstanceState == null)
      getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
    else
      onBackStackChanged();
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
