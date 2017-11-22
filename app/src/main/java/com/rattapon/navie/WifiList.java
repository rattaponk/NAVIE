package com.rattapon.navie;

import java.util.ArrayList;

/**
 * Created by rattapon on 10/23/2017.
 */

public class WifiList {
    public ArrayList<WifiPoint> List;
    public int round;

    public WifiList() {
        List = new ArrayList<WifiPoint>();
        round = 0;
    }

    public int isAvailable(String BSSID) {
        for (int i = 0; i < List.size(); i++) {
            if (List.get(i).BSSID.equals(BSSID))
                return i;
        }
        return -1;
    }

    public void updateAt(int index, int level) {
        WifiPoint tempPoint = List.get(index);

        if (tempPoint.min > level) {
            tempPoint.min = level;
        }
        if (tempPoint.max < level) {
            tempPoint.max = level;
        }
        if(tempPoint.round <= 5) {
            tempPoint.average = ((tempPoint.average * tempPoint.round) + level) / (tempPoint.round + 1);
        }
        tempPoint.round = tempPoint.round + 1;
        List.set(index, tempPoint);
    }

    public void insertNew(String BSSID, String SSID, int level) {
        WifiPoint tempPoint = new WifiPoint();
        tempPoint.round = 1;
        tempPoint.average = level;
        tempPoint.min = level;
        tempPoint.max = level;
        tempPoint.BSSID = BSSID;
        tempPoint.SSID = SSID;
        List.add(tempPoint);
    }
}
