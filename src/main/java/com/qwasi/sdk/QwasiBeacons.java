package com.qwasi.sdk;

import android.app.Activity;
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
 // QwasiBeacons.java
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

public class QwasiBeacons extends Service
    implements BeaconConsumer,
        RangeNotifier,
        MonitorNotifier{
    final static long FOCUSSCANPERIOD = 2000;
    HashMap<String, QwasiLocation> map;
    List<BeaconParser> parsers;
    BeaconManager beaconManager;
    private static String TAG = "QwasiBeacon";
    BeaconConsumer mainAct;
    Context context;

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
