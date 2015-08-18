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

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
        LocationListener{
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
    List<String> locationsfetched = new ArrayList<>();

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
    }

    private void postToServer() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("lat", mLastLocation.getLatitude());
        data.put("lng", mLastLocation.getLongitude());
        //YYYY-MM-DDTHH:MM:SSS
        data.put("timestamp", System.currentTimeMillis()/1000);
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
        return mactiveManager != null? mactiveManager:null;
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
        mstarted = mmanager.isConnected()?true:false;
        if (mstarted)
            startLocationUpdates();
        else
            mmanager.reconnect();
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
        //beaconManager = BeaconManager.getInstanceForApplication(sharedApplication.getApplicationContext());
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        //beaconManager = BeaconManager.getInstanceForApplication(sharedApplication.getApplicationContext());
        return this;
    }

    public void startLocationUpdates(){
        Log.i(TAG, "Start LocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager, this); //foreground
        if (sharedApplication instanceof BeaconConsumer){
            shared.qwasiBeacons.beaconManager.bind((BeaconConsumer) sharedApplication);
        }
        //mresult = LocationServices.FusedLocationApi.requestLocationUpdates(mmanager,mactiveManager, mintent); //background
    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, this);
        if (sharedApplication instanceof BeaconConsumer) {
            shared.qwasiBeacons.beaconManager.unbind((BeaconConsumer) sharedApplication);
        }
        mstarted = false;
    }

    void pruneLocations(){
        //this is for removing of old locations that didn't come back as valid from the last fetch
        Iterator<String> stringIterator = mregionMap.keySet().iterator();
        while(stringIterator.hasNext() && (locationsfetched.size() != mregionMap.size())){
            String current = stringIterator.next();
            if(!locationsfetched.contains(current)){
            //if the regionmap has a location key not in the latest fetch
                this.stopMonitoringLocation(mregionMap.get(current));
            }
        }
        locationsfetched.clear(); //so that next time locations are fetched we don't maintain old ones
    }

    public QwasiErrorCode startMoitoringLocation(QwasiLocation input){
        Witness.notify(input.toString());
        synchronized (this){
            if (input != null) {
                locationsfetched.add(input.id);
                mregionMap.put(input.id, input);
                if (input.type == QwasiLocation.QwasiLocationType.QwasiLocationTypeGeofence) {
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
                else if(input.type == QwasiLocation.QwasiLocationType.QwasiLocationTypeBeacon){
                    try{
                        //shared.qwasiBeacons.addParsers();
                        shared.qwasiBeacons.beaconManager.startRangingBeaconsInRegion(input.beacon);
                        shared.qwasiBeacons.beaconManager.startMonitoringBeaconsInRegion(input.beacon);
                    }
                    catch (Exception e){
                        Log.e("QwasiError", e.getMessage());
                    }
                }
                //else if is rfid?
            }
            else{
                return QwasiErrorCode.QwasiErrorLocationMonitoringFailed;
            }
        }
        Witness.notify("Location Start Monitor"+ input.toString());
        return QwasiErrorCode.QwasiErrorNone;
    }

    public void stopMonitoringLocation(QwasiLocation location){

        if (location.type == QwasiLocation.QwasiLocationType.QwasiLocationTypeGeofence) {
            LocationServices.GeofencingApi.removeGeofences(mmanager, Arrays.asList(location.id));
        }

        else {
            try {
                shared.qwasiBeacons.beaconManager.stopRangingBeaconsInRegion(location.beacon);
                shared.qwasiBeacons.beaconManager.stopMonitoringBeaconsInRegion(location.beacon);
            }
            catch (RemoteException e){
                Log.e("QwasiError", e.getMessage());
            }
        }
        Witness.notify("Location stop moitoring" + location.toString());
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

    public Context getApplicationContext(){       //#issue 1
        return sharedApplication.getApplicationContext();
    }

    public void setBeaconManager(BeaconManager input){
        synchronized (this){
            beaconManager = input;
        }
    }
}