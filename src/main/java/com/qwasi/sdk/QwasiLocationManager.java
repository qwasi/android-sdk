package com.qwasi.sdk;

import android.app.Activity;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.List;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */

public class QwasiLocationManager extends IntentService
        implements
        ResultCallback<Status>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private Context sharedApplication;
    private Qwasi shared;
    boolean mstarted = false;
    private long mupdateDistance = 100; //100 meter
    private long mupdateInterval =1800000; //30 minutes in milliseconds;
    public GoogleApiClient mmanager = null;
    public HashMap<String, Object> mregionMap = null;
    private QwasiLocation mLastLocation = null;
    protected LocationRequest mactiveManager = new LocationRequest().create();
    private static String TAG = "QwasiLocationManager";
    private static String eventTag = "com.qwasi.event.location.update";

    protected QwasiLocationManager(){
        super(TAG);
        shared = Qwasi.getInstance();
        sharedApplication = shared.getContext();
        mactiveManager.setSmallestDisplacement(mupdateDistance);
        mactiveManager.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mactiveManager.setFastestInterval(30000);
        mactiveManager.setInterval(mupdateInterval);
        mactiveManager.setNumUpdates(1000);
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
            mLastLocation = new QwasiLocation(LocationServices.FusedLocationApi.getLastLocation(mmanager));
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
        android.util.Log.d("QwasiDebug", "On location changed");
        if (mLastLocation == null){
            mLastLocation = new QwasiLocation(location);
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
        mregionMap = new HashMap<>();
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        mregionMap = new HashMap<>();
        return this;
    }

    public void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager, this).setResultCallback(this); //foreground
        //mresult = LocationServices.FusedLocationApi.requestLocationUpdates(mmanager,mactiveManager, mintent); //background
    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, this);
        this.stopMonitoringLocation(mLastLocation);
        mstarted = false;
    }

    public void startMoitoringLocation(Geofence input){
        synchronized (this){
            mregionMap.put("id", input);
            if (mregionMap != null){
                GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
                builder.addGeofence(input);
                if (mmanager.isConnected()) {
                    LocationServices.GeofencingApi.addGeofences(mmanager, builder.build(), getGeoPendingIntent()).setResultCallback(this);
                }
                else{
                    mmanager.connect();
                }
            }
        }
    }

    public void stopMonitoringLocation(QwasiLocation location){ //fixme?
        LocationServices.GeofencingApi.removeGeofences(mmanager, getGeoPendingIntent());
        mregionMap.remove(location.id);
    }

    private PendingIntent getGeoPendingIntent(){
        Intent intent = new Intent(sharedApplication, this.getClass());
        return PendingIntent.getService(sharedApplication, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onHandleIntent(Intent input){
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(input);
        if (geofencingEvent.hasError()) {
            //String errorMessage = GeofenceErrorMessages.getErrorString(this,
              //      geofencingEvent.getErrorCode());
            Log.e("QwasiGeofence", String.valueOf(geofencingEvent.getErrorCode()));
            return;
        }
        HashMap<String, Object> data = new HashMap<>();
        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
            //data.put("", );
            // Send notification and log the transition details.
            QwasiLocation temp = new QwasiLocation(geofencingEvent.getTriggeringLocation());
            temp.state = QwasiLocationState.QwasiLocationStateInside;
            temp.type = QwasiLocationType.QwasiLocationTypeGeofence;
            Witness.notify(temp);
            shared.postEvent("com.qwasi.event.location.enter", data);
        }
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            Witness.notify("Geofence.exit");
            //data.put("", );
            Witness.notify(new QwasiLocation(geofencingEvent.getTriggeringLocation()));
            shared.postEvent("com.qwasi.event.location.exit", data);

        }
        else {
            // Log the error.
            Log.e("QwasiError", "invalid transition type");
        }
    }
    @Override
    public void onResult(Status status){
        if (status.isSuccess()){
            Log.d(TAG, "Geofence Success");
        }
        else{
            Log.e(TAG, "Geofence failed status code: "+getString(status.getStatusCode()));
        }
    }
}