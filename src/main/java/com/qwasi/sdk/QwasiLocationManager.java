/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 //QwasiLocationManager.java
 //
 // Copyright (c) 2015-2016, Qwasi Inc (http://www.qwasi.com/)
 // All rights reserved.
 //
 // Redistribution and use in source and binary forms, with or without
 // modification, are permitted provided that the following conditions are met:
 //    * Redistributions of source code must retain the above copyright
 //   notice, this list of conditions and the following disclaimer.
 //    * Redistributions in binary form must reproduce the above copyright
 //   notice, this list of conditions and the following disclaimer in the
 //   documentation and/or other materials provided with the distribution.
 //    * Neither the name of Qwasi nor the
 //   names of its contributors may be used to endorse or promote products
 //   derived from this software without specific prior written permission.
 //
 // THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 // ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 // WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 // DISCLAIMED. IN NO EVENT SHALL QWASI BE LIABLE FOR ANY
 // DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 // (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 // LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 // ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 // (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 // SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qwasi.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.app.Service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.BeaconConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import io.hearty.witness.Witness;

public class QwasiLocationManager //extends Service
        implements GoogleApiClient.ConnectionCallbacks, //google api server callbacks
        GoogleApiClient.OnConnectionFailedListener, //failed connection
        LocationListener{
    private Context mSharedApplication;
    boolean mStarted = false;
    long mUpdateDistance = 100; //10 meter
    long mUpdateInterval =1800000; //30 minutes in milliseconds;
    @Deprecated
    public GoogleApiClient mmanager = null;
    public GoogleApiClient manager = mmanager;
    @Deprecated
    public HashMap<String, QwasiLocation> mregionMap = new HashMap<>();
    public HashMap<String, QwasiLocation> regionMap = mregionMap;
    QwasiLocation mLastLocation = null;
    protected LocationRequest mActiveManager = LocationRequest.create();
    static String TAG = "QwasiLocationManager";
    public QwasiBeacons qwasiBeacons;
    private static QwasiLocationManager mInstance;
    List<String> mLocationsFetched = new ArrayList<>();

    private QwasiLocationManager(){
        mSharedApplication = Qwasi.getContext();
        mActiveManager.setInterval(mUpdateInterval/10) //3 minute updates
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(mUpdateDistance) //how far can the device move
                .setMaxWaitTime(mUpdateInterval); //30 minutes max to get an update
        mInstance = this;
        qwasiBeacons = new QwasiBeacons();

    }

    public static synchronized QwasiLocationManager getInstance(){
        return mInstance == null?new QwasiLocationManager():mInstance;
    }

    public QwasiLocation getLastLocation(){
        if (ContextCompat.checkSelfPermission(Qwasi.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mLastLocation != null) {
                return mLastLocation;
            } else if (LocationServices.FusedLocationApi.getLastLocation(mmanager) != null) {
                mLastLocation = QwasiLocation.initWithLocation(LocationServices.FusedLocationApi.getLastLocation(mmanager));
                return mLastLocation;
            }
        }
        return null;
    }

    public LocationRequest currentManager(){
        return (mActiveManager);
    }

    public LocationRequest foregroundManager(){
        return mActiveManager != null? mActiveManager:null;
    }

    /**
     * todo something about binding to the qwasi object
     * @param
     * @return
     *
    @Override
    public IBinder onBind(Intent incoming){
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId){
        Log.d(TAG, "QwasiLocationMNG starting");
        new QwasiLocationManager();
        return START_NOT_STICKY;
    }//*/

    @Override
    public void onConnectionSuspended(int i) {
        if (mmanager != null) {
            mmanager.reconnect();
            mStarted = false;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()){
            try{//needs main activity
                connectionResult.startResolutionForResult((Activity)mSharedApplication, 1001);
            }
            catch(Exception e){
                manager = mmanager;
                manager.connect();
            }
        }
        else{
            ErrorDialogFragment diagFrag = new ErrorDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt("dialog_error", connectionResult.getErrorCode());
            diagFrag.setArguments(arguments);
        }
        mStarted = false;
    }

    @Override
    public void onConnected(Bundle bundle){
        mStarted = manager.isConnected();
        if (mStarted)
            startLocationUpdates();
        else
            manager.reconnect();
    }

    @Override
    public void onLocationChanged(Location location){
        android.util.Log.d(TAG, "On location changed");
        if (mLastLocation == null){
            mLastLocation = new QwasiLocation(location);
        }
        else if (location.distanceTo(mLastLocation)< mUpdateDistance){  //if it hasn't been 100m
            return;
        }
        else {
            mLastLocation = QwasiLocation.initWithLocation(location);
        }
        Witness.notify(mLastLocation);
    }

    /*QwasiLocationManager backgroundManager(){  //todo is this needed as android doesn't have "background" manager
        /*QwasiLocationManager sharedInstance = null;
        if(mactiveManager != null){
            return(mactiveManager);
        }
        return this;
    }*/

    public Object initWithRequiredAuthorization(){//todo M? granular location?
        return this.init();
    }

    public synchronized Object init(){
        mmanager = new GoogleApiClient.Builder(mSharedApplication)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        manager = mmanager;
        return this;
    }

    public synchronized Object initWithGoogleApi(GoogleApiClient manager){
        mmanager = manager;
        this.manager = mmanager;
        this.manager.registerConnectionCallbacks(this);
        this.manager.registerConnectionFailedListener(this);
        return this;
    }

    public void startLocationUpdates(){
        Log.i(TAG, "Start LocationUpdates");
        if (ContextCompat.checkSelfPermission(Qwasi.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (!mmanager.isConnected()||!manager.isConnected()){
                manager = mmanager;
                manager.connect();
            }
            else
                LocationServices.FusedLocationApi.requestLocationUpdates(mmanager, mActiveManager, this); //foreground

            if (mSharedApplication instanceof BeaconConsumer)
                qwasiBeacons.mBeaconManager.bind((BeaconConsumer) mSharedApplication);

            //mresult = LocationServices.FusedLocationApi.requestLocationUpdates(mmanager,mactiveManager, mintent); //background
        }
    }

    public void stopLocationUpdates(){
        if((manager!=null)&&(mmanager!= null)){
            if((mmanager.isConnected()||manager.isConnected())){
                manager = mmanager;
                LocationServices.FusedLocationApi.removeLocationUpdates(manager, this);
            }
            manager = mmanager;
            manager.disconnect();
            if (mSharedApplication instanceof BeaconConsumer) {
                qwasiBeacons.mBeaconManager.unbind((BeaconConsumer) mSharedApplication);
            }
        }
        mStarted = false;
    }

    void pruneLocations(){ // FIXME:make this remove locations with no app id
        //this is for removing of old locations that didn't come back as valid from the last fetch
        regionMap = mregionMap;
        Iterator<String> stringIterator = regionMap.keySet().iterator();
        while(stringIterator.hasNext() && (mLocationsFetched.size() != regionMap.size())){
            String current = stringIterator.next();
            if(!mLocationsFetched.contains(current)) {
                //if the regionmap has a location key not in the latest fetch
                this.stopMonitoringLocation(regionMap.get(current));
            }
        }
        mLocationsFetched.clear(); //so that next time locations are fetched we don't maintain old ones
    }

    public QwasiErrorCode startMoitoringLocation(QwasiLocation input){
        synchronized (this){
            if (input != null &&(ContextCompat.checkSelfPermission(Qwasi.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)) {
                Witness.notify(input.toString());
                mLocationsFetched.add(input.id);
                mregionMap.put(input.id, input);
                if (input.mType == QwasiLocation.QwasiLocationType.QwasiLocationTypeGeofence) {
                    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                    builder.addGeofence(input.mRegion);

                    if (mmanager.isConnected() || manager.isConnected()) {
                        manager = mmanager;
                        LocationServices.GeofencingApi.addGeofences(manager, builder.build(), getGeoPendingIntent());
                    } else {
                        manager.connect();
                    }
                } else if (input.mType == QwasiLocation.QwasiLocationType.QwasiLocationTypeBeacon) {
                    try {
                        qwasiBeacons.mBeaconManager.startRangingBeaconsInRegion(input.mBeacon);
                        qwasiBeacons.mBeaconManager.startMonitoringBeaconsInRegion(input.mBeacon);
                    } catch (RemoteException e) {
                        Log.e("QwasiError", "Beaconconsumer issue, remoteException");
                    }
                }
                //else if is rfid?
            }
            else{
                return QwasiErrorCode.QwasiErrorLocationMonitoringFailed;
            }
        }
        Witness.notify("Location Start Monitor" + input.toString());
        return QwasiErrorCode.QwasiErrorNone;
    }

    public void stopMonitoringLocation(QwasiLocation location){

        if (location.mType == QwasiLocation.QwasiLocationType.QwasiLocationTypeGeofence) {
            manager = mmanager;
            LocationServices.GeofencingApi.removeGeofences(manager, Collections.singletonList(location.id));
        }

        else {
            try {
                qwasiBeacons.mBeaconManager.stopRangingBeaconsInRegion(location.mBeacon);
                qwasiBeacons.mBeaconManager.stopMonitoringBeaconsInRegion(location.mBeacon);
            }
            catch (RemoteException e){
                Log.e("QwasiError", e.getMessage());
            }
        }
        Witness.notify("Location stop moitoring" + location.toString());
        regionMap = mregionMap;
        regionMap.remove(location.id);
    }

    private PendingIntent getGeoPendingIntent(){
        Intent intent = new Intent(mSharedApplication, QwasiGeofencehandler.class);
        mSharedApplication.getSystemService(Context.LOCATION_SERVICE);
        return PendingIntent.getService(mSharedApplication, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}