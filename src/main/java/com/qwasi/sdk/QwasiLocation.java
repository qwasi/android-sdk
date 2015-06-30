package com.qwasi.sdk;

import android.location.Location;
import android.nfc.tech.NfcBarcode;
import java.util.HashMap;
import java.util.Timer;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
enum QwasiLocationType{
    QwasiLocationTypeUnknown,
    QwasiLocationTypeCoordinate,
    QwasiLocationTypeGeofence,
    QwasiLocationTypeBeacon,
    QwasiLocationTypeRFC
}

enum QwasiLocationState{
    QwasiLocationStateUnknown,
    QwasiLocationStateOutside,
    QwasiLocationStatePending,
    QwasiLocationStateInside
}

public class QwasiLocation extends Location {
    private QwasiLocationType type;
    private QwasiLocationState state;
    private double longitude;
    private double latitude;
    public String id;
    private String name;
    private static Double DwellTime = 50.0;
    //private geofenceRadius;
    private HashMap<String, Object> geometry;
    //private distance;
    private Timer dwellTime;
    private Timer mdwellInterval;
    private Timer mdwellStart;
    private Timer mexitDelay;
    boolean mdwell;
    boolean minside;
    private NfcBarcode NFCUUID;

    public QwasiLocation(Location l) {
        super(l);
        longitude = l.getLongitude();
        latitude = l.getLatitude();
    }

    public QwasiLocation(String provider) {
        super(provider);
    }

    public QwasiLocation initWithLocation(Location location){
        if (super.equals(this)){
            type = QwasiLocationType.QwasiLocationTypeCoordinate;
            //region
            state = QwasiLocationState.QwasiLocationStateUnknown;
        }
        return this;
    }

    public QwasiLocation initWithLocationData(HashMap<String, Object> data){
        Object coord = data.get("geofence.geometry.coordinates");

        return this;
    }

    public QwasiLocationState stateCheck(){

        if(state == QwasiLocationState.QwasiLocationStateInside){
            return state;
        }

        /*else if (dwellTime < DwellTime){
            return QwasiLocationState.QwasiLocationStatePending;
        }*/

        else if (type == QwasiLocationType.QwasiLocationTypeBeacon){
            return QwasiLocationState.QwasiLocationStateOutside;
        }

        else {
            return QwasiLocationState.QwasiLocationStateUnknown;
        }
    }

    public void enter(){

    }

    public void exit(){

    }
}
