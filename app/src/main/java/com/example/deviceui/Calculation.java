package com.example.deviceui;

public class Calculation {


  public static float calc(float minX, float maxX, float speed, float hzs) {
    try {
      float r = (float)((((1 / (37.8f * Math.pow(10, 6))) * (maxX - minX)) / 2) * speed);
      float te = (float)(20f * Math.pow(10, -6));
      float t = (float) ((1 / (37.8f * Math.pow(10, 6))) * (maxX - minX));

//      float tzs = Math.abs(t - te);
//      float hzs = tzs * speed;
//      float result = r - hzs;

      float result = (r * 1000) - hzs;
      return result;
    } catch (Exception e) {
      return 0;
    }
  }

  public static float[] amplifier(int[] data, int minX, int maxX) {
    int len = data.length;
    float[] res = new float[len];

    for (int i = 0; i < data.length; i++) {
      res[i] = data[i];
    }

//    int center = data[minX];

    float last = data[minX + 1];
    int amp = 5;
    float dist = 0;

    InputCloudActivity.Dir dir = InputCloudActivity.Dir.NONE;

    for (int i = 0; i < len; i++) {
      if (i > (minX + 1) && i < maxX)  {

        switch (dir) {
          case UP:
            res[i] = last + (amp * (dist));
            break;
          case DOWN:
            res[i] = last - (amp * dist);
            break;
          case EQUAL:
//            System.out.println("====================== EQUAL COMPUTE ========================= + " + i);
            res[i] = res[i-1];
            break;
        }

        last = res[i];

        if (data[i] > data[i+1]) {    // down
          dir = InputCloudActivity.Dir.DOWN;
          dist = Math.abs(data[i] - data[i+1]);
        }

        if (data[i] < data[i+1]) {    // up
          dir = InputCloudActivity.Dir.UP;
          dist = Math.abs(data[i] - data[i+1]);
        }

        if (data[i] == data[i+1]) {
          dir = InputCloudActivity.Dir.EQUAL;
        }

      }
    }

    ///////////////////// OFFSET BY Y
    float p = 0;

    if (minX > 0 && maxX > 0) {
      float d = Math.abs(data[maxX-1] - res[maxX-1]);
//      System.out.println("DISTANCE maxX " + d);

      if (data[maxX-1] < res[maxX-1]) {
        p = -d;
      } else {
        p = d;
      }
    }

    for (int i = 0; i < len; i++) {
      if (i > (minX + 1) && i < maxX) {
        res[i] = res[i] + p;
      }
    }

    return res;
  }

}
