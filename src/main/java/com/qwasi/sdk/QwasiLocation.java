package com.qwasi.sdk;

import android.location.Location;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */

public class QwasiLocation extends Location {
    QwasiLocationType type; //package private
    QwasiLocationState state; //package private
    private double longitude;
    private double latitude;
    public String id;
    private String name;
    private long geofenceRadius = 0;
    private JSONObject geometry;
    //private long distance;
    private long DwellTime; //in seconds
    private Timer dwellTime;
    private int mdwellInterval;
    private Date mdwellStart;
    private long mexitDelay;
    public static Geofence region;
    public static Region beacon;
    //private NfcBarcode NFCUUID;
    public Beacon token;
    boolean mdwell = false;
    boolean minside =false;
    boolean mexit;

    public enum QwasiLocationType{
        QwasiLocationTypeUnknown,
        QwasiLocationTypeCoordinate,
        QwasiLocationTypeGeofence,
        QwasiLocationTypeBeacon,
        QwasiLocationTypeRFC
    }

    public enum QwasiLocationState{
        QwasiLocationStateUnknown,
        QwasiLocationStateOutside,
        QwasiLocationStatePending,
        QwasiLocationStateInside
    }



    public QwasiLocation(){
        super(Qwasi.getInstance().mlocationManager.getLastLocation());
    }

    public  QwasiLocation(Location location){
        super(location);
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.name = location.getProvider();
        this.mexit = false;
        type = QwasiLocationType.QwasiLocationTypeCoordinate;
        state = QwasiLocationState.QwasiLocationStateUnknown;
        return;
    }

    public String getName(){return name;}

    public double getLongitude(){
        return longitude;
    }

    public  double getLatitude(){
        return latitude;
    }

    static public QwasiLocation initWithLocation(Location location){
        QwasiLocation temp = new QwasiLocation(location);
        return temp;
    }

    static public QwasiLocation initWithLocationData(JSONObject input) throws JSONException{
        //if this location doesn't already exist in the mregionMap add it
        QwasiLocation location = new QwasiLocation();
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
                JSONObject props = input.getJSONObject("properties").getJSONObject("beacon");
                location.beacon = new Region(location.id,
                        Identifier.parse(props.getString("id")),
                        Identifier.parse(props.getString("maj_ver")),
                        Identifier.parse(props.getString("min_ver")));
            }

            else if (input.has("geofence")) {  //geofence builder
                location.type = QwasiLocationType.QwasiLocationTypeGeofence;
                location.geometry = input.getJSONObject("geofence").getJSONObject("geometry");
                location.latitude = location.geometry.getJSONArray("coordinates").getDouble(1);
                location.longitude = location.geometry.getJSONArray("coordinates").getDouble(0);
                location.geofenceRadius = input.getJSONObject("geofence").getJSONObject("properties")
                        .getInt("radius");
                location.region = new Geofence.Builder()
                        .setRequestId(location.id)
                        .setCircularRegion(location.latitude, location.longitude, location.geofenceRadius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(location.mdwellInterval)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT) //all the events
                        .build();
            }

            else if (input.has("rfid")){
                location.type = QwasiLocationType.QwasiLocationTypeRFC;
            }
            else{
                location.type = QwasiLocationType.QwasiLocationTypeUnknown;
            }
            return location;
        }
        else { //mRegionMap already has this location
            return null;
        }
    }

    public boolean isTypeCoordinate(){
        if (this.type == QwasiLocationType.QwasiLocationTypeCoordinate) return true;
        return false;
    }

    public QwasiLocationType typeCheck(){
        return type;
    }

    public QwasiLocationState stateCheck(){
        return state;
    }

    public void enter(){
        //Witness.notify(this);
        synchronized (this) {
           if (!minside) {
               minside = true;
               if (!mdwell) {
                   mdwellStart = new Date();
                   DwellTime = 0;
                   Witness.notify(this);
               }
               dwell();
           }
           mexit = false;

        }
    }

    synchronized void dwell(){

            if (minside && dwellTime == null){
                DwellTime = 0;
                dwellTime = new Timer(this.id+"timer", true); //make a 1 second timer to update the ui thread
                    dwellTime.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (this) {
                                if (minside) {
                                    if (mexit) {
                                        minside = false;
                                    } else {
                                        mdwell = true;
                                        state = QwasiLocationState.QwasiLocationStatePending;
                                        DwellTime = new Date().compareTo(mdwellStart);
                                    }
                                } else {
                                    this.cancel();
                                    mdwell = false;
                                    dwellTime = null;
                                    state = QwasiLocationState.QwasiLocationStateOutside;
                                }
                            }
                        } //time to start, and period in milliseconds
                    }, new Date(), mdwellInterval * 1000);
                    Witness.notify(this);
            }


    }


    public void exit(){
        synchronized (this){
            if (minside){
                DwellTime = new Date().compareTo(mdwellStart);
                mexit = true;
            }
        }
    }

    public long getDwellTime(){
        return new Date().compareTo(mdwellStart);
    }
}
