package com.rattapon.navie;

import java.util.HashMap;

/**
 * Created by rattapon on 10/23/2017.
 */

public class WifiPoint {
    public String BSSID;
    public String SSID;
    public int round;
    public int average;
    public int min;
    public int max;
    // how far from different APs, store information about distances (Range)
    // to different APs
    HashMap<String, Double> RangefromAPs;
    // estimated position X,Y
    public double x;
    public double y;
}
