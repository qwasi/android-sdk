package com.qwasi.sdk;

import android.app.Activity;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */

public class QwasiLocationManager extends IntentService
        implements
        GoogleApiClient.ConnectionCallbacks, //google api server callbacks
        GoogleApiClient.OnConnectionFailedListener, //failed connection
        LocationListener,
        BeaconConsumer,
        RangeNotifier{
    private Context sharedApplication;
    private Qwasi shared;
    boolean mstarted = false;
    long mupdateDistance; //10 meter
    long mupdateInterval =1800000; //30 minutes in milliseconds;
    public GoogleApiClient mmanager = null;
    public HashMap<String, QwasiLocation> mregionMap = new HashMap<>();
    private QwasiLocation mLastLocation = null;
    protected LocationRequest mactiveManager = LocationRequest.create();
    private static String TAG = "QwasiLocationManager";

    //RangingActivity rangingActivity = new RangingActivity();

    static String eventTag = "com.qwasi.event.location.update";
    public BeaconManager beaconManager;

    public QwasiLocationManager(){
        super(TAG);
        shared = Qwasi.getInstance();
        mupdateDistance = (shared.locationUpdatefilter);
        sharedApplication = shared.getContext();
        mactiveManager.setInterval(mupdateInterval/10) //3 minute updates
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(mupdateDistance) //how far can the device move
                .setMaxWaitTime(mupdateInterval); //30 minutes max to get an update
        //Intent intent = new Intent(sharedApplication, this.getClass());
        //startService(intent);
    }

    private void postToServer() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("lat", mLastLocation.getLatitude());
        data.put("lng", mLastLocation.getLongitude());
        data.put("timestamp", System.currentTimeMillis() / 1000L);
        shared.postEvent(eventTag, data);
        shared.fetchLocationsNear(mLastLocation);

    }

    public QwasiLocation getLastLocation(){
        if (mLastLocation != null) {
            return mLastLocation;
        }
        else if(LocationServices.FusedLocationApi.getLastLocation(mmanager)!=null){
            mLastLocation = QwasiLocation.initWithLocation(LocationServices.FusedLocationApi.getLastLocation(mmanager));
            return mLastLocation;
        }
        else{
            return null;
        }
    }

    public LocationRequest currentManager(){
        return (mactiveManager);
    }

    public LocationRequest foregroundManager(){


        if (mactiveManager != null){
            return(mactiveManager);
        }
        else {
            return null;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mmanager.reconnect();
        mstarted = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()){
            try{//needs main activity
                connectionResult.startResolutionForResult((Activity)sharedApplication, 1001);
            }
            catch(Exception e){
                mmanager.connect();
            }
        }
        else{
            ErrorDialogFragment diagFrag = new ErrorDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt("dialog_error", connectionResult.getErrorCode());
            diagFrag.setArguments(arguments);
        }
        mstarted = false;
    }

    @Override
    public void onConnected(Bundle bundle){
        if (mmanager.isConnected()) {
            this.startLocationUpdates();
            this.mstarted = true;
        }
        else{
            mmanager.reconnect();
            mstarted = false;
        }
    }

    @Override
    public void onLocationChanged(Location location){
        android.util.Log.d(TAG, "On location changed");

        if (mLastLocation == null){
            mLastLocation = new QwasiLocation(location);
        }
        else if (location.distanceTo(mLastLocation)< shared.locationUpdatefilter){  //if it hasn't been 100m
            return;
        }
        else {
            mLastLocation.initWithLocation(location);
        }
        postToServer();
        Witness.notify(mLastLocation);
    }

    private QwasiLocationManager backgroundManager(){
        /*static QwasiLocationManager sharedInstance = null;
        if(mactiveManager != null){
            return(mactiveManager);
        }*/
        return this;
    }

    public Object initWithRequiredAuthorization(/*CLAuthstatus status*/){
        //return this.initWithLocationManager();
        return this;
    }

    public Object init(){

        mmanager = new GoogleApiClient.Builder(sharedApplication)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        beaconManager = BeaconManager.getInstanceForApplication(sharedApplication.getApplicationContext());
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        beaconManager = BeaconManager.getInstanceForApplication(sharedApplication.getApplicationContext());
        return this;
    }

    public void startLocationUpdates(){
        Log.i(TAG, "Start LocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager, this); //foreground
        //mresult = LocationServices.FusedLocationApi.requestLocationUpdates(mmanager,mactiveManager, mintent); //background
    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, this);
        mstarted = false;
    }

    public QwasiErrorCode startMoitoringLocation(QwasiLocation input){
        synchronized (this){
            if (input != null) {
                mregionMap.put(input.id, input);
                if (input.region != null) { //is a geofence
                    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                    //builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
                    builder.addGeofence(input.region);

                    if (mmanager.isConnected()) {
                        LocationServices.GeofencingApi.addGeofences(mmanager, builder.build(), getGeoPendingIntent());
                    }

                    else {
                        mmanager.connect();
                    }
                }
                //beacons handled by onBeaconServiceConnnect()
                //else if is rfid?
            }
            else{
                return QwasiErrorCode.QwasiErrorLocationMonitoringFailed;
            }
        }
        return QwasiErrorCode.QwasiErrorNone;
    }

    public void stopMonitoringLocation(QwasiLocation location){
        LocationServices.GeofencingApi.removeGeofences(mmanager, Arrays.asList(location.id));
        mregionMap.remove(location.id);
    }

    private PendingIntent getGeoPendingIntent(){
        Intent intent = new Intent(sharedApplication, this.getClass());
        sharedApplication.getSystemService(Context.LOCATION_SERVICE);
        return PendingIntent.getService(sharedApplication, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onHandleIntent(Intent input) {
        synchronized (this) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(input);
            Log.i(TAG, "Geofence Intent");
            if (geofencingEvent.hasError()) {
                Log.e("QwasiGeofence", String.valueOf(geofencingEvent.getErrorCode()));
                return;
            }
            HashMap<String, Object> data = new HashMap<>();
            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            //Test that the reported transition was of interest.
            for (Geofence geofence : triggeringGeofences) {
                QwasiLocation temp = Qwasi.getInstance().mlocationManager.mregionMap.get(geofence.getRequestId());
                if (temp != null) {
                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        // Send notification and log the transition details.
                        temp.state = QwasiLocation.QwasiLocationState.QwasiLocationStateInside;
                        Witness.notify(temp);
                        shared.postEvent("com.qwasi.event.location.enter", data);
                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        temp.exit();
                        Witness.notify(temp);
                        shared.postEvent("com.qwasi.event.location.exit", data);
                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        temp.state = QwasiLocation.QwasiLocationState.QwasiLocationStatePending;
                        temp.enter();
                    }
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Set entries = mregionMap.entrySet();
        for(Object entry:  entries){
            QwasiLocation region = (QwasiLocation) entry;
            if (region.beacon != null) {
                try {
                    beaconManager.startRangingBeaconsInRegion(region.beacon);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        beaconManager.setRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Set entries = mregionMap.entrySet();
        for (Object entry : entries) { //get the set of keys
            QwasiLocation location = (QwasiLocation) entry; //for each value in the regionMap get that location
            if (location.beacon == region) { //if the region that triggered this is in the location,
                for (Beacon beacon : beacons) {  //check all the beacons that where returned
                    if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) { //Eddystone-UID frame

                        Identifier namespaceId = beacon.getId1();
                        Identifier instanceId = beacon.getId2();
                        Log.d("QwasiRanging", "Beacon Sighted with namespace id: " + namespaceId
                                + "and instance " + instanceId + " approx " + beacon.getDistance() + "meters away.");
                    } else {
                        Log.d("QwasiRanging", "beacon: " + beacon.getId1().toString() + " dist: " + beacon.getDistance());
                    }
                    location.token = beacon;
                    Witness.notify(location);
                }
            }
        }
    }

    boolean bindBeacon(Activity activity){
        synchronized (this) {
            beaconManager = BeaconManager.getInstanceForApplication(activity.getApplicationContext());
            beaconManager.setForegroundBetweenScanPeriod(2000L);
            /*beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));  Fruity flavored Beacons uncomment at your own risk*/
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19")); //Eddystone UID
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15")); //EddyStone TLM
            /*beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=0203,i:14-19l,d:10-13,p:9-9"));*/  //Placedge private type beacon
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")); //altbeacon
            //try {
                beaconManager.bind(this);  //#issue 1'
            /*} catch (NullPointerException e) {
                Log.wtf(TAG, "Beaconbind borked");
                return false;
            }*/
            return true;
        }
    }

    public Context getApplicationContext(){       //#issue 1
        return sharedApplication.getApplicationContext();
    }

    public void setBeaconManager(BeaconManager input){
        synchronized (this){
            beaconManager = input;
        }
    }
}