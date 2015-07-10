package com.qwasi.sdk;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.Geofence;
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
    boolean mstarted = false;
    private long mupdateDistance = 100; //100 meter
    private long mupdateInterval =1800000; //30 minutes in milliseconds;
    public GoogleApiClient mmanager = null;
    //public Geofence[] mregionMap;
    public HashMap<String, Object> mregionMap = null;
    private QwasiLocation mLastLocation = null;
    protected LocationRequest mactiveManager = new LocationRequest().create();
    private LocationListener mlocationListener;

    public QwasiLocationManager(Context application){
        sharedApplication = application;
        //SharedPreferences pref = sharedApplication.getSharedPreferences(sharedApplication.getPackageName(), Context.MODE_PRIVATE);
        mactiveManager.setSmallestDisplacement(mupdateDistance);
        mactiveManager.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mactiveManager.setFastestInterval(3000);
        mactiveManager.setInterval(mupdateInterval);
    }

    public QwasiLocation getLastLocation(){
        if (mLastLocation != null) {
            return mLastLocation;
        }
        else {
            mLastLocation = new QwasiLocation(LocationServices.FusedLocationApi.getLastLocation(mmanager));
            return mLastLocation;
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
            //diagFrag.show(getSupportFragmentManager(), "errordialog");
        }

    }

    @Override
    public void onConnected(Bundle bundle){
        startLocationUpdates();
        mstarted = true;
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
        if (mmanager != null) {
            mmanager.connect();
        }
        return this;
    }

    public Object initWithGoogleApi(GoogleApiClient manager, Context sharedApplication){
        mmanager = manager;
        mmanager.registerConnectionCallbacks(this);
        mmanager.registerConnectionFailedListener(this);
        //if(this = super()){
        //mrequiredStatus = status;
        //mauthStatus = LocationManager.;
        mregionMap = new HashMap<String, Object>();
        LocationRequest request = new LocationRequest();

        return this;
    }

    public void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mactiveManager, this);

    }

    public void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mmanager, this);
        //for mregionMap
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