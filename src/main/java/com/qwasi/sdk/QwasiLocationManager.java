package com.qwasi.sdk;

import android.location.Location;
import android.location.LocationManager;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * todo actually handle locations
 */

//protected static QwasiLocationManager activeManager = null;

public class QwasiLocationManager{
    boolean mdeferred;
    boolean mstarted;
    public LocationManager mmanager = null;
    public HashMap <String, Object> mregionMap;
    protected QwasiLocationManager mactiveManager = null;

    public QwasiLocationManager currentManager(){
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

    public Object initWithLocationManager(LocationManager manager/*, CLAuthStatus status*/){
        //if(this = super()){
        //mrequiredStatus = status;
        //mauthStatus = LocationManager.;
        //mupdateDistance = 100; //100 meter
        //mupdateInterval = 900; //30 minutes
        mregionMap = new HashMap<String, Object>();
        mmanager = manager;
        //
        //for (){

        //}
        return this;
    }

    public void startLocationUpdates(){
        /*switch(LocationManager.)
        case LocationManager.KEY_PROVIDER_ENABLED:
        case LocationManager.KEY_PROXIMITY_ENTERING:
    */}

    public void stopLocationUpdates(){
        //mmanager.removeUpdates();
        //this.stopMonitoringLocation();
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

    }
}