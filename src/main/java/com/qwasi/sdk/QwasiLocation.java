/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiLocation.java
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

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.hearty.witness.Witness;

public class QwasiLocation extends Location {
  static Creator CREATOR;
  QwasiLocationType mType; //package private
  QwasiLocationState mState; //package private
  private double mLongitude;
  private double mLatitude;
  String mAppId;
  public String id;
  private String mName;
  private long mGeofenceRadius = 0;
  private JSONObject mGeometry;
  protected double mDistance;
  private long mDwellTime; //in seconds
  private Timer mDwellTimer;
  private int mDwellInterval;
  private long mDwellStart;
  //private int mexitDelay;
  Geofence mRegion;
  Region mBeacon;
  BeaconParser mParser;
  //private NfcBarcode NFCUUID;
  public Beacon token;
  static String TAG = "QwasiLocation";
  boolean mDwell = false;
  boolean mInside =false;
  boolean mExit;
  boolean mEmpty;

  public enum QwasiLocationType{
    QwasiLocationTypeUnknown,
    QwasiLocationTypeCoordinate,
    QwasiLocationTypeGeofence,
    QwasiLocationTypeBeacon,
    QwasiLocationTypeRFC
  }

  public enum QwasiLocationState {
    QwasiLocationStateUnknown,
    QwasiLocationStateOutside,
    QwasiLocationStatePending,
    QwasiLocationStateInside
  }

  public  QwasiLocation(Location location){
    super(location);
    mAppId = "";
    mEmpty = false;
    mLatitude = location.getLatitude();
    mLongitude = location.getLongitude();
    mName = location.getProvider();
    mExit = false;
    mType = QwasiLocationType.QwasiLocationTypeCoordinate;
    mState = QwasiLocationState.QwasiLocationStateUnknown;
    mParser = new BeaconParser();
  }

  public double getDistance(){
    return mDistance;
  }

  public String getName(){
    return mName;
  }

  public double getLongitude(){
    return mLongitude;
  }

  public  double getLatitude(){
    return mLatitude;
  }

  static public QwasiLocation initEmpty(){
    QwasiLocation temp = new QwasiLocation(new Location("Qwasi")); //json issue
    temp.mEmpty = true;
    return temp;
  }

  static public QwasiLocation initWithLocation(Location location){
    return new QwasiLocation(location);
  }

  static public QwasiLocation initWithLocationData(JSONObject input){
    //if this location doesn't already exist in the mregionMap add it
    QwasiLocation location = initEmpty();
    try {
      location.id = input.getString("id");
      location.mName = input.getString("name");
      location.mState = QwasiLocationState.QwasiLocationStateUnknown;
      location.mDwellInterval = input.getJSONObject("properties")
          .getInt("dwell_interval") * 1000; //ours is s google is ms
      if (!QwasiLocationManager.getInstance().mregionMap.containsKey(location.id)) {
        //for locations in response figure out what type they are i.e. beacons/geofence/rfid
        if (input.has("beacon") && //deal with beacons using altBeacons
          QwasiLocationManager.getInstance().qwasiBeacons.mMainAct != null) {
          location.mType = QwasiLocationType.QwasiLocationTypeBeacon;

          JSONObject beaconconfig = input.getJSONObject("beacon");
          String parsestring = beaconconfig.getString("parsestring");
          JSONArray ids = beaconconfig.getJSONArray("id");
          location.mParser.setBeaconLayout(parsestring);
          QwasiLocationManager.getInstance().qwasiBeacons.addParser(location.mParser);
          List<Identifier> identifierList = new ArrayList<>(3);
          try {
            identifierList.add(Identifier.parse(ids.getString(0)));
          } catch (JSONException e) {
            Log.e(TAG, "Beacon Parsing issue: " + location.id + location.mName);
          }
          /**
           * ids will be 1 to 3 fields long, of string, int, int or sting string
           *  or a string, int or a just a string
           */
          if (ids.length() == 1) { //only has 1 id thus other 2 are null
            identifierList.add(null);
            identifierList.add(null);
          } else if (ids.length() == 3) { //it's a ibeacon with major and minor
            identifierList.add(Identifier.fromInt(ids.getInt(1)));
            identifierList.add(Identifier.fromInt(ids.getInt(2)));
          } else { //it's 2 long, and either 2 is int or string
            if (ids.get(1) instanceof Integer) {
              identifierList.add(Identifier.fromInt(ids.getInt(1)));
            } else {
              identifierList.add(Identifier.parse(ids.getString(1)));
            }
            identifierList.add(null);
          }
          location.token = new Beacon.Builder().setIdentifiers(identifierList).build();
          location.setBeacon(new Region(location.id, identifierList));
        } else if (input.has("geofence")) {  //geofence builder
          location.mType = QwasiLocationType.QwasiLocationTypeGeofence;
          location.mGeometry = input.getJSONObject("geofence").getJSONObject("geometry");
          location.mLatitude = location.mGeometry.getJSONArray("coordinates").getDouble(1);
          location.mLongitude = location.mGeometry.getJSONArray("coordinates").getDouble(0);
          location.mGeofenceRadius = input.getJSONObject("geofence").getJSONObject("properties")
              .getInt("radius");
          location.setRegion(new Geofence.Builder()
              .setRequestId(location.id)
              .setCircularRegion(location.mLatitude, location.mLongitude, location.mGeofenceRadius)
              .setExpirationDuration(Geofence.NEVER_EXPIRE)
              .setLoiteringDelay(location.mDwellInterval)
              .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                  Geofence.GEOFENCE_TRANSITION_DWELL |
                  Geofence.GEOFENCE_TRANSITION_EXIT) //all the events
              .build());
        } else if (input.has("rfid")) {
            location.mType = QwasiLocationType.QwasiLocationTypeRFC;
        } else {
            location.mType = QwasiLocationType.QwasiLocationTypeUnknown;
        }
        return location;
      } else { //mRegionMap already has this location
          return null;
      }
    }
    catch (JSONException e){
      Log.e(TAG, "Jsonexcetption on Location");
      return null;
    }
  }

  synchronized void setBeacon(Region input){
    this.mBeacon = input;
  }

  public synchronized Region getBeacon(){
    return this.mBeacon;
  }

  synchronized void setRegion(Geofence input){
    this.mRegion = input;
  }

  public synchronized Geofence getRegion(){
    return this.mRegion;
  }

  public boolean isTypeCoordinate(){
    return this.mType == QwasiLocationType.QwasiLocationTypeCoordinate;
  }

  public QwasiLocationType typeCheck(){
    return mType;
  }

  public QwasiLocationState stateCheck(){
      return mState;
  }

  public void enter(){
    //Witness.notify(this);
    synchronized (this) {
     if (!mInside) {
       mState = QwasiLocationState.QwasiLocationStatePending;
       mInside = true;
       if (!mDwell) {
         mDwellStart = System.currentTimeMillis()/1000;
         mDwellTime = 0;
         Witness.notify(this);
       }
       dwell();
     }
      mExit = false;
    }
  }

  void dwell(){
    synchronized (this) {
      if (mInside && mDwellTimer == null) {
        mDwellTime = 0;
        mDwellTimer = new Timer(this.id + "timer", true);
        Log.d("LocationDwell", "timer Start");
        mDwellTimer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run() {
            if (mInside) {
              if (mExit) {
                mInside = false;
                mState = QwasiLocationState.QwasiLocationStateOutside;
              } else if (mDwell){
                mState = QwasiLocationState.QwasiLocationStateInside;
              } else {
                mDwell = true;
              }
            } else {
              mDwell = false;
              this.cancel();
              mDwellTimer = null;
              mState = QwasiLocationState.QwasiLocationStateOutside;
            }
            mDwellTime = (System.currentTimeMillis()/1000) - mDwellStart;
            Witness.notify(this);
            Log.d("LocationDwell", "dwellinteval timer happened");

          } //time to start, and period in milliseconds
        }, new Date(), mDwellInterval);
      }
    }
  }


  public void exit(){
    synchronized (this){
      if (mInside){
        mDistance = -1;
        mDwellTime = (System.currentTimeMillis()/1000) - mDwellStart;
        mExit = true;
      }
    }
  }

  public long getDwellTime(){
    return mDwellTime;
  }
}
