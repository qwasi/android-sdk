package com.qwasi.sdk;

import android.location.Location;
import android.nfc.tech.NfcBarcode;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.BaseImplementation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.d;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * Created by ccoulton on 6/11/15.
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
    private int longitude;
    private int latitude;
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
    }
    //region
    //beacon

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
