package com.qwasi.sdk;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    String AppID;
    public String id;
    private String name;
    private long geofenceRadius = 0;
    private JSONObject geometry;
    protected double distance;
    private long DwellTime; //in seconds
    private Timer dwellTime;
    private int mdwellInterval;
    private long mdwellStart;
    private int mexitDelay;
    Geofence region;
    Region beacon;
    BeaconParser parser;
    //private NfcBarcode NFCUUID;
    public Beacon token;
    boolean mdwell = false;
    boolean minside =false;
    boolean mexit;
    boolean empty;

    public enum QwasiLocationType{
        QwasiLocationTypeUnknown,
        QwasiLocationTypeCoordinate,
        QwasiLocationTypeGeofence,
        QwasiLocationTypeBeacon,
        QwasiLocationTypeRFC
    }

    public enum QwasiLocationState {
        QwasiLocationStateUnknown,
        QwasiLocationStateOutside,
        QwasiLocationStatePending,
        QwasiLocationStateInside
    }

    public  QwasiLocation(Location location){
        super(location);
        this.AppID = "";
        this.empty = false;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.name = location.getProvider();
        this.mexit = false;
        type = QwasiLocationType.QwasiLocationTypeCoordinate;
        state = QwasiLocationState.QwasiLocationStateUnknown;
        parser = new BeaconParser();
        return;
    }

    public double getDistance(){
        return distance;
    }

    public String getName(){
        return name;
    }

    public double getLongitude(){
        return longitude;
    }

    public  double getLatitude(){
        return latitude;
    }

    static public QwasiLocation initEmpty(){
        QwasiLocation temp = new QwasiLocation(new Location("Qwasi"));
        temp.empty = true;
        return temp;
    }

    static public QwasiLocation initWithLocation(Location location){
        QwasiLocation temp = new QwasiLocation(location);
        return temp;
    }

    // FIXME: 8/19/15 make locations regardless let location manger deal with sorting
    static public QwasiLocation initWithLocationData(JSONObject input) throws JSONException{
        //if this location doesn't already exist in the mregionMap add it
        QwasiLocation location = initEmpty();
        location.id = input.getJSONObject("properties").getString("id");
        location.name = input.getString("name");
        location.state = QwasiLocationState.QwasiLocationStateUnknown;
        location.mdwellInterval = input
                .getJSONObject("properties").getInt("dwell_interval") * 1000; //ours is s google is ms
        if (!QwasiLocationManager.getInstance().mregionMap.containsKey(location.id)) {
            //for locations in response figure out what type they are i.e. beacons/geofence/rfid
            if (input.has("beacon")&&
                    QwasiLocationManager.getInstance().qwasiBeacons.mainAct != null) {//deal with beacons using altBeacons
                location.type = QwasiLocationType.QwasiLocationTypeBeacon;

                JSONObject beaconconfig = input.getJSONObject("beacon");
                String parsestring = beaconconfig.getString("parsestring");
                JSONArray ids = beaconconfig.getJSONArray("id");
                location.parser.setBeaconLayout(parsestring);
                QwasiLocationManager.getInstance().qwasiBeacons.addParser(location.parser);
                List<Identifier> identifierList = new ArrayList<>(3);
                identifierList.add(Identifier.parse(ids.getString(0)));
                /**
                 * ids will be 1 to 3 fields long, of string, int, int or sting string
                 *  or a string, int or a just a string
                 */
                if (ids.length() == 1){ //only has 1 id thus other 2 are null
                    identifierList.add(null);
                    identifierList.add(null);
                }
                else if(ids.length() == 3){ //it's a ibeacon with major and minor
                    identifierList.add(Identifier.fromInt(ids.getInt(1)));
                    identifierList.add(Identifier.fromInt(ids.getInt(2)));
                }
                else { //it's 2 long, and either 2 is int or string
                    if(ids.get(1) instanceof Integer)
                        identifierList.add(Identifier.fromInt(ids.getInt(1)));
                    else
                        identifierList.add(Identifier.parse(ids.getString(1)));
                    identifierList.add(null);
                }
                location.token = new Beacon.Builder().setIdentifiers(identifierList).build();
                location.setBeacon(new Region(location.id, identifierList));
            }

            else if (input.has("geofence")) {  //geofence builder
                location.type = QwasiLocationType.QwasiLocationTypeGeofence;
                location.mexitDelay = input.getJSONObject("properties")
                        .getInt("exit_interval") *1000;
                location.geometry = input.getJSONObject("geofence").getJSONObject("geometry");
                location.latitude = location.geometry.getJSONArray("coordinates").getDouble(1);
                location.longitude = location.geometry.getJSONArray("coordinates").getDouble(0);
                location.geofenceRadius = input.getJSONObject("geofence").getJSONObject("properties")
                        .getInt("radius");
                location.setRegion(new Geofence.Builder()
                        .setRequestId(location.id)
                        .setCircularRegion(location.latitude, location.longitude, location.geofenceRadius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(location.mdwellInterval)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT) //all the events
                        .build());
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

    synchronized void setBeacon(Region input){
        this.beacon = input;
    }

    public synchronized Region getBeacon(){return this.beacon;}

    synchronized void setRegion(Geofence input){
        this.region = input;
    }

    public synchronized Geofence getRegion(){return this.region;}

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
               state = QwasiLocationState.QwasiLocationStatePending;
               minside = true;
               if (!mdwell) {
                   mdwellStart = System.currentTimeMillis()/1000;
                   DwellTime = 0;
                   Witness.notify(this);
               }
               dwell();
           }
           mexit = false;

        }
    }

    void dwell(){
        synchronized (this) {
            if (minside && dwellTime == null) {
                DwellTime = 0;
                dwellTime = new Timer(this.id + "timer", true);
                Log.d("LocationDwell", "timer Start");
                dwellTime.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (minside) {
                            if (mexit) {
                                minside = false;
                                state = QwasiLocationState.QwasiLocationStateOutside;
                            }
                            else if (mdwell){
                                state = QwasiLocationState.QwasiLocationStateInside;
                            }
                            else {
                                mdwell = true;
                            }
                        } else {
                            mdwell = false;
                            this.cancel();
                            dwellTime = null;
                            state = QwasiLocationState.QwasiLocationStateOutside;
                        }
                        DwellTime = (System.currentTimeMillis()/1000) - mdwellStart;
                        Witness.notify(this);
                        Log.d("LocationDwell", "dwellinteval timer happened");

                    } //time to start, and period in milliseconds
                }, new Date(), mdwellInterval);
            }
        }
    }


    public void exit(){
        synchronized (this){
            if (minside){
                distance = -1;
                DwellTime = (System.currentTimeMillis()/1000) - mdwellStart;
                mexit = true;
            }
        }
    }

    public long getDwellTime(){
        return DwellTime;
    }
}
