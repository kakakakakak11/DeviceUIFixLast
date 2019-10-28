package com.example.deviceui;

interface SerialListener {
  void onSerialConnect();
  void onSerialConnectError (Exception e);
  void onSerialRead(byte[] data);
  void onSerialRead(byte[] data, int[] datai);
  void onSerialIoError      (Exception e);
}

