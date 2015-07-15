package com.qwasi.sdk;

import android.app.Activity;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 * todo actually handle locations
 */

public class QwasiLocationManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    boolean mdeferred;
    private Context sharedApplication;
    final private Qwasi shared;
    boolean mstarted = false;
    private long mupdateDistance = 100; //100 meter
    private long mupdateInterval =1800000; //30 minutes in milliseconds;
    public GoogleApiClient mmanager = null;
    public HashMap<String, Object> mregionMap = null;
    private QwasiLocation mLastLocation = null;
    protected LocationRequest mactiveManager = new LocationRequest().create();
    private static String eventTag = "com.qwasi.event.location.update";

    public QwasiLocationManager(Context application, Qwasi main){
        shared = main;
        sharedApplication = application;
        mactiveManager.setSmallestDisplacement(mupdateDistance);
        mactiveManager.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mactiveManager.setFastestInterval(3000);
        mactiveManager.setInterval(mupdateInterval);
        mactiveManager.setNumUpdates(1000);
    }

    private void postToServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("lat", mLastLocation.getLatitude());
                data.put("lng", mLastLocation.getLongitude());
                data.put("timestamp", System.currentTimeMillis() / 1000L);
                shared.postEvent(eventTag, data);
                shared.fetchLocationsNear(mLastLocation, false, false);
            }
        }).start();
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

    }

    @Override
    public void onConnected(Bundle bundle){
        if (mmanager.isConnected()) {
            this.startLocationUpdates();
            this.mstarted = true;
        }
        else{
            mmanager.reconnect();
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
        mregionMap = new HashMap<String, Object>();
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager, Context sharedApplication){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        mregionMap = new HashMap<String, Object>();
        return this;
    }

    public void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager, this); //foreground
        //mresult = LocationServices.FusedLocationApi.requestLocationUpdates(mmanager,mactiveManager, mintent); //background
    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, this);
        this.stopMonitoringLocation(mLastLocation);
        mstarted = false;
    }

    public void startMoitoringLocation(QwasiLocation location){
        synchronized (this){
            if(mregionMap.containsKey(location.id)){
                mregionMap.put(location.id, location);

            }
        }
    }

    public void stopMonitoringLocation(QwasiLocation location){

        //todo: clear location data?
    }
}