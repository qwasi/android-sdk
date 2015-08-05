package com.qwasi.sdk;

import android.location.Location;
import android.nfc.tech.NfcBarcode;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
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
    QwasiLocationType type; //package private
    QwasiLocationState state; //package private
    private double longitude;
    private double latitude;
    public String id;
    private String name;
    private static Double DwellTime = 50.0;
    private long geofenceRadius = 0;
    private JSONObject geometry;
    //private distance;
    private Timer dwellTime;
    private int mdwellInterval;
    private Timer mdwellStart;
    private long mexitDelay;
    boolean mdwell;
    boolean minside;
    private NfcBarcode NFCUUID;

    public QwasiLocation(){
        super(Qwasi.getInstance().mlocationManager.getLastLocation());
    }

    public QwasiLocation(Location l) {
        super(l);
        if (l != null) {
            longitude = l.getLongitude();
            latitude = l.getLatitude();
            type = QwasiLocationType.QwasiLocationTypeCoordinate;
            state = QwasiLocationState.QwasiLocationStateUnknown;
        }
    }

    public double getLongitude(){
        return longitude;
    }

    public  double getLatitude(){
        return latitude;
    }

    public QwasiLocation initWithLocation(Location location){
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.name = location.getProvider();
        type = QwasiLocationType.QwasiLocationTypeCoordinate;
        state = QwasiLocationState.QwasiLocationStateUnknown;
        return this;
    }

    static public QwasiLocation initWithLocationData(JSONObject input) throws JSONException{
        //if this location doesn't already exist in the mregionMap add it
        QwasiLocation location = new QwasiLocation(null);
        location.id = input.getString("id");
        location.name = input.getString("name");
        location.state = QwasiLocationState.QwasiLocationStateUnknown;
        location.mdwellInterval = input
                .getJSONObject("properties").getInt("dwell_interval") * 1000; //ours is s google is ms
        location.mexitDelay = input.getJSONObject("properties")
                .getInt("exit_interval") *1000;
        if (!Qwasi.getInstance().mlocationManager.mregionMap.containsKey(location.id)) {
            //for locations in response figure out what type they are i.e. beacons/geofence/rfid
            if (input.has("beacon")) {//deal with beacons using altBeacons
                location.type = QwasiLocationType.QwasiLocationTypeBeacon;
                int manf = 0x004c; //apple
                String layout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"; //iBeacon layout
                JSONObject props = input.getJSONObject("properties").getJSONObject("beacon");
                Beacon temp = new Beacon.Builder()
                        //id1 iBeacon uuid 16 byte Eddystone 10 byte Altbeacon 16
                        .setId1(props.getString("id"))
                        //id2 iBeacon from 1 to 255 Eddystone 6 byte
                        .setId2(props.getString("maj_ver"))
                        //id3 iBeacon from 1 to 255 Eddystone leave blank
                        .setId3(props.getString("min_ver"))
                        .setManufacturer(manf)
                        //.setTxPower()
                        // .setDataFields(Arrays.asList(new Long[] {0l}))
                        .build();
                Qwasi.getInstance().mlocationManager.beaconManager.getBeaconParsers().add(new BeaconParser()
                        .setBeaconLayout(layout)); //string for UUID etc
            }

            else if (input.has("geofence")) {  //geofence builder
                location.type = QwasiLocationType.QwasiLocationTypeGeofence;
                location.geometry = input.getJSONObject("geofence").getJSONObject("geometry");
                location.latitude = location.geometry.getJSONArray("coordinates").getDouble(1);
                location.longitude = location.geometry.getJSONArray("coordinates").getDouble(0);
                location.geofenceRadius = input.getJSONObject("geofence").getJSONObject("properties")
                        .getInt("radius");
                Qwasi.getInstance().mlocationManager.startMoitoringLocation(new Geofence.Builder()
                        .setRequestId(location.id)
                        .setCircularRegion(location.latitude, location.longitude, location.geofenceRadius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(location.mdwellInterval)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }

            else if (input.has("rfid")){
                location.type = QwasiLocationType.QwasiLocationTypeRFC;
            }
            else{
                location.type = QwasiLocationType.QwasiLocationTypeUnknown;
            }
        }
        return location;
    }

    public QwasiLocationType typeCheck(){
        return type;
    }

    public QwasiLocationState stateCheck(){
        return state;
    }

    public void enter(){
       //todo emit enter events
    }

    public void exit(){
        //todo emit exit event
    }
}
