package com.example.deviceui;

class Constants {

  // values have to be globally unique
  static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
  static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
  static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

  // values have to be unique within each app
  static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

  private Constants() {}


  public static final String MQTT_BROKER_URL = "tcp://farmer.cloudmqtt.com:14907";
  public static final String PUBLISH_TOPIC = "/topic";
  public static final String CLIENT_ID = "ooxhidbn";

}
