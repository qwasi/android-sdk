package com.qwasi.sdk;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 8/13/15.
 * Service to interface with the beacon library
 */
public class QwasiBeacons extends Service
    implements BeaconConsumer,
        RangeNotifier,
        MonitorNotifier{
    private static long FOCUSSCANPERIOD = 2000;
    HashMap<String, QwasiLocation> map;
    List<BeaconParser> parsers;
    BeaconManager beaconManager;
    private static String TAG = "QwasiBeacon";
    BeaconConsumer mainAct;
    private Context context;

    public QwasiBeacons() {
        super();
        context = Qwasi.getContext();
        mainAct = Qwasi.getMainActivity() instanceof  BeaconConsumer? (BeaconConsumer) Qwasi.mainActivity:null;
        map = QwasiLocationManager.getInstance().mregionMap;
        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.setForegroundBetweenScanPeriod(FOCUSSCANPERIOD);
        parsers = beaconManager.getBeaconParsers();
        //beaconManager.setBackgroundBetweenScanPeriod(FOCUSSCANPERIOD * 10);
    }

    synchronized void setMainAct(Activity main){
        Log.d(TAG, "SetMain");
        mainAct = BeaconConsumer.class.isAssignableFrom(main.getClass())?(BeaconConsumer) main:null;
    }

    synchronized void addParser(BeaconParser input) {
        if (!parsers.contains(input))
            parsers.add(input);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(this);
        beaconManager.setMonitorNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        if(!collection.isEmpty()){
            for (Beacon beacon: collection){
                Log.d(TAG, "beacon found: "+beacon.getId1()+
                        " dist away"+beacon.getDistance()+
                        " QwasiID "+region.getUniqueId());
                map.get(region.getUniqueId()).distance = beacon.getDistance();
                Witness.notify(map.get(region.getUniqueId()));
            }
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.wtf(TAG, "Enter region");
        map.get(region.getUniqueId()).enter();
    }

    @Override
    public void didExitRegion(Region region) {
        Log.wtf(TAG, "Exit region");
        map.get(region.getUniqueId()).exit();
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.wtf(TAG, "StateDet");
    }
}
