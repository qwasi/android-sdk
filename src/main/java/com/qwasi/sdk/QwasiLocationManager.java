package com.qwasi.sdk;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.BaseImplementation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.c;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by ccoulton on 6/11/15.
 * todo actually handle locations
 */

public class QwasiLocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    boolean mdeferred;
    boolean mstarted = false;
    private double mupdateInterval;
    public GoogleApiClient mmanager = null;
    //public Geofence mregionMap;
    public HashMap<String, Object> mregionMap = null;
    private QwasiLocation mLastLocation = null;
    protected LocationRequest mactiveManager = null;
    private LocationListener mlocationListener;

    public LocationRequest currentManager(){
        return (mactiveManager);
    }

    public QwasiLocationManager foregroundManager(){
        //QwasiLocationManager sharedInstance = null;

        //if (mactiveManager != null){
          //  return(mactiveManager);
        //}
        //sharedInstance = new
        return this;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(Bundle bundle){
        mLastLocation.initWithLocation(LocationServices.FusedLocationApi.getLastLocation(mmanager));
        if (mLastLocation != null){
            if (!mstarted){
                startLocationUpdates();
            }
        }
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

    public Object init(Context sharedApplication){
        mmanager = new GoogleApiClient.Builder(sharedApplication)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager, Context sharedApplication){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        //if(this = super()){
        //mrequiredStatus = status;
        //mauthStatus = LocationManager.;
        //mupdateDistance = 100; //100 meter
        mupdateInterval = 1800000; //30 minutes in milliseconds
        mregionMap = new HashMap<String, Object>();
        //
        //for (){

        //}
        return this;
    }

    public void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager,mlocationListener);
    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, mlocationListener);
        this.stopMonitoringLocation(mLastLocation);
        mstarted = false;
    }

    public void startMoitoringLocation(QwasiLocation location){
        synchronized (this){
            if(mregionMap.containsKey(location.id)){
                mregionMap.put(location.id, location);
                //mmanager.requestLocationUpdates( );
            }
        }
    }

    public void stopMonitoringLocation(QwasiLocation location){
        //todo: clear location data?
    }
}