/**
 // Qwasi.Java
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.Manifest;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Qwasi{
    static final float LOCATION_EVENT_FILTER = 50.0f;
    static final float LOCATION_UPDATE_FILTER = 100.0f;
    static final float LOCATION_SYNC_FILTER = 200.0f;
    static final float PED_FILTER = 10.0f;
    //static Activity sMainActivity;
    static private Context sContext = null;
    SharedPreferences mPreferences;
    private float mLocationSyncFilter;
    private boolean mRegistered;
    String mAppId; //android code style
    float mLocationUpdateFilter;
    float mLocationEventFilter;
    QwasiAppManager mQwasiAppManager = null;
    private QwasiNotificationManager mQwasiNotificationManager = null;
    public String deviceName = null;
    @Deprecated
    public String mapplicationName = null;
    public String applicationName = null;
    private String mDeviceToken = null;
    private QwasiClient mClient = null;
    public NetworkInfo networkInfo;
    private Map<String, Void> mChannels;
    private HashMap<String, QwasiMessage> mMessageCache;
    @Deprecated
    public QwasiLocationManager mlocationManager;
    public QwasiLocationManager locationManager;
    @Deprecated
    public QwasiConfig mconfig;
    public QwasiConfig config;
    protected String muserToken;
    @Deprecated
    public Boolean mpushEnabled = false;
    public Boolean pushEnabled;
    @Deprecated
    public Boolean mlocationEnabled = false;
    public Boolean locationEnabled;
    @Deprecated
    public Boolean meventsEnabled = false;
    public Boolean eventsEnabled;
    @Deprecated
    public Boolean museLocalNotifications; //apple only?
    public Boolean useLocalNotifications;
    String TAG = "Qwasi";

    //event tags
    final String kEventApplicationState = "com.qwasi.event.application.state";
    final String kEventLocationUpdate   = "com.qwasi.event.location.update";
    final String kEventLocationEnter    = "com.qwasi.event.location.enter";
    final String kEventLocationDwell    = "com.qwasi.event.location.dwell";
    final String kEventLocationExit     = "com.qwasi.event.location.exit";
    private QwasiLocation mlastLocationEvent, mlastLocationUpdate, mlastLocationSync = null;
    Reporter mQwasiNotificationHandler;
    Reporter mQwasiLocationHandler;
    Reporter mPushTokenCallback;
    String mVersion;

    private QwasiInterface defaultCallback = new QwasiInterface() {
        @Override
        public void onSuccess(Object o) { //should recieve QwasiMessage, or QwasiError
            if(o instanceof QwasiMessage) Log.i("DefaultCallback", ((QwasiMessage) o).description());//ignore QwasiError since it's only NoError
        }

        @Override
        public void onFailure(QwasiError e) {
            //e.printStackTrace();
            Log.e("DefaultCallback", e.message);
        }
    };

    public Qwasi(Activity application)/*public constructor iOS 46*/ {
        this.mClient = new QwasiClient();
        //sMainActivity = application;
        mChannels = new HashMap<>();
        sContext = application.getApplicationContext();
        this.mQwasiAppManager = new QwasiAppManager(this);
        this.networkInfo = ((ConnectivityManager) sContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        mQwasiNotificationManager = QwasiNotificationManager.getInstance();
        mconfig = new QwasiConfig(sContext);
        config = mconfig;
        mlocationManager = QwasiLocationManager.getInstance();
        locationManager = mlocationManager;
        //mlocationManager.init();
        application.getApplication().registerActivityLifecycleCallbacks(mQwasiAppManager);
        config.configWithFile(); //default
        if (config.isValid()) this.initWithConfig(config, "");
        else Log.e(TAG, "Config in Manifest not valid; Please init with valid config.");
    }

   // static public Activity getMainActivity(){return sMainActivity;}

    static public Context getContext(){ return sContext; } //return application context

    public Qwasi qwasiWithConfig(QwasiConfig config, String deviceToken){
        return(initWithConfig(config, deviceToken));
    }

    public Qwasi qwasiWithConfig(QwasiConfig config) /*ios 57*/{
        return(initWithConfig(config, ""));
    }

    public boolean getRegistrationStatus() {
        return mRegistered;
    }

    public String version()/*iOS 61*/{
        return mVersion;
    }

    public String getMdeviceToken(){
        return mDeviceToken;
    }

    String getVerboseVersionName()/*Android Verbose Version *Android only**/{
        switch(Build.VERSION.SDK_INT){
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1: //15 min version 2.0.3
                return "Android Ice_Cream Sandwich";  //5.1
            case Build.VERSION_CODES.JELLY_BEAN: //16 4.1  14.7%
            case Build.VERSION_CODES.JELLY_BEAN_MR1: //17  17.5%
            case Build.VERSION_CODES.JELLY_BEAN_MR2: //18  5.2%
                return "Android Jellybean";
            case Build.VERSION_CODES.KITKAT: //19 4.4      39.2 min version 2.1
            case Build.VERSION_CODES.KITKAT_WATCH: //20
                return "Android KitKat";
            case Build.VERSION_CODES.LOLLIPOP:  //21    5.0 11.6
            case Build.VERSION_CODES.LOLLIPOP_MR1: //22 5.1 .8
                return "Android Lollipop";
            case Build.VERSION_CODES.M:
                return "Android Marshmellow";
            default:
                return "Android Unknown";   //21&22  12.4%
        }
    }

    private synchronized Qwasi initWithConfig(QwasiConfig config, String deviceToken) /*ios*/ {
        if ((config != null)&&(config.isValid())) {
            mconfig = config;
            this.config = mconfig;
            this.setConfig(config);
        }
        mLocationUpdateFilter= LOCATION_UPDATE_FILTER;
        mLocationEventFilter = LOCATION_EVENT_FILTER;
        mLocationSyncFilter = LOCATION_SYNC_FILTER;
        mDeviceToken = deviceToken;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        mRegistered = mPreferences.getBoolean("registered", false);

        //are localNotifcations set
        museLocalNotifications = mPreferences.getBoolean("localNote", true);
        useLocalNotifications = museLocalNotifications;

        mMessageCache = new HashMap<>();

        //if we have a device token saved already use it.
        //check if we have a gcm token already so we don't use too much data
        mQwasiNotificationManager.setPushToken(mPreferences.getString("gcm_token", null));

        if (mQwasiNotificationManager.getPushToken() == null) mQwasiNotificationManager.registerForRemoteNotification(defaultCallback);
        String test;
        if ((ContextCompat.checkSelfPermission(sContext, Manifest.permission.GET_ACCOUNTS)&
                ContextCompat.checkSelfPermission(sContext, Manifest.permission.READ_PHONE_STATE))
                == PackageManager.PERMISSION_GRANTED) {
            Account[] accounts = AccountManager.get(sContext).getAccountsByType("com.google");

            deviceName = accounts.length > 0 ?
                    accounts[0].name.substring(0, accounts[0].name.lastIndexOf("@")) : null;

            test = mPreferences.getString("qwasi_user_token", ((TelephonyManager) sContext
                    .getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
        } else{
            deviceName = "";
            test = null;
        }

        muserToken = test != null?test:"DROIDTOKEN";
        return this;
    }

    public synchronized void setConfig(QwasiConfig config)/*iOS 95*/{
        this.mconfig = config;
        mAppId = config.mapplication;
        mClient = mClient.clientWithConfig(config, this);
        mRegistered = false;
    }

    public void setPushEnabled(Boolean pushEnabled)/*iOS 101*/{
        setPushEnabled(pushEnabled, defaultCallback);
    }

    @Deprecated
    public void setPushEnabled(final QwasiInterface callbacks){
        setPushEnabled((mpushEnabled||pushEnabled), callbacks);
    }

    public void setPushEnabled(Boolean pushEnabled, final QwasiInterface callbacks){
        if (pushEnabled) registerForNotifications(callbacks);
        else unregisterForNotifications(callbacks);
    }

    public synchronized void setLocationEnabled(boolean enabled)/*iOS 111*/{
        mlocationEnabled = enabled;
        locationEnabled = enabled;
        Log.d(TAG, "setLocationEnabled"+locationEnabled.toString());
        if (enabled){
            locationManager = locationManager == null? QwasiLocationManager.getInstance():locationManager;
            mQwasiLocationHandler = new Reporter() {
                @Override
                public void notifyEvent(Object o) {
                    Log.d(TAG, "LocationHandler");
                    QwasiLocation location = (QwasiLocation) o;
                    HashMap<String, Object> data = new HashMap<>();
                    float speed = location.getSpeed();
                    // onLocationChanged filter out locations too close
                    if (mlastLocationEvent == null || location.distanceTo(mlastLocationEvent)
                           > MAX(LOCATION_EVENT_FILTER, UPDATE_FILTER(speed, mLocationEventFilter))){
                        data.put("lat", location.getLatitude());
                        data.put("lng", location.getLongitude());
                        postEvent(kEventLocationUpdate, data, true);
                        mlastLocationEvent = location;
                    }
                    if(mlastLocationUpdate == null || location.distanceTo(mlastLocationUpdate)>
                            UPDATE_FILTER(speed, location.distanceTo(mlastLocationUpdate))) {
                        mlastLocationUpdate = location;
                    }
                    if (mlastLocationSync == null || location.distanceTo(mlastLocationSync)
                            > MAX(LOCATION_SYNC_FILTER, UPDATE_FILTER(speed, mLocationSyncFilter))){
                        fetchLocationsNear(location);
                        mlastLocationSync = location;
                    }
                    data.put("id", location.id);
                    data.put("name", location.getName());
                    data.put("lng", location.getLongitude());
                    data.put("lat", location.getLatitude());
                    data.put("dwellTime", location.getDwellTime());
                    if (location.isTypeCoordinate()){
                        QwasiLocation mLastLocation = locationManager.getLastLocation();
                        data.put("timestamp", System.currentTimeMillis() / 1000);
                        postEvent(kEventLocationUpdate, data, false);
                        fetchLocationsNear(mLastLocation);
                    }
                    else if (location.mAppId.equals(mAppId)){
                        if(location.mState == QwasiLocation.QwasiLocationState.QwasiLocationStateInside){
                            postEvent(kEventLocationDwell, data, false); //post inside event;
                        }
                    }
                }
            };
            Witness.register(QwasiLocation.class, mQwasiLocationHandler);
        }
        else{
            Witness.remove(QwasiLocation.class, mQwasiLocationHandler);
        }
    }

    private synchronized void mregisterDevice(String deviceToken,
                                       String name,
                                       String userToken,
                                       HashMap<String, Object> userInfo,
                                       final QwasiInterface qwasiInterface)/*iOS 301*/ {

        //if devicetoken is null set it empty so the server will gen one
        deviceToken = deviceToken == null? mDeviceToken:deviceToken;

        //if name is null get it from the phone, or user can give us one
        name = name == null? deviceName:name;

         //if we didn't get a usertoken set it to be the phone number
        userToken = userToken== null?muserToken:userToken;
        muserToken = userToken;

        Map<String, Object> info = new HashMap<>();

        if (userInfo != null) {  //make sure that it's init so we don't crash
            if (!userInfo.isEmpty()) { //make sure it's not empty
                info.put("info", userInfo); //put all the recieved info into info
            }
        }

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("debug", BuildConfig.DEBUG);
        deviceInfo.put("version", String.valueOf(Build.VERSION.RELEASE));  //systemversion
        deviceInfo.put("system", getVerboseVersionName()); //kitkat or w/e the codename is
        deviceInfo.put("model", Build.MANUFACTURER + " " + Build.MODEL);  //Samsung Then the actual device name
        deviceInfo.put("sdkVersion", mVersion);  //set because that's what the sdk is currently
        //since JSONRPC2 on java reads from a Hashmap<string, object> easier to put it here
        //and since the JSONRPC2 doesn't format it how the server expects have to do some playing.
        info.put("info", deviceInfo);
        info.put("name", name);
        info.put("user_token", userToken); //phonenumber
        info.put("id", deviceToken);
        mClient.invokeMethod("device.register", info, new QwasiInterface() {
            @Override
            public void onSuccess(Object o) {
                try{
                    mRegistered = true; //we've now registered
                    JSONObject result = (JSONObject) o;
                    mDeviceToken = result.getString("id");  //set our device token from the server

                    JSONObject info = result.getJSONObject("application");
                    mapplicationName = info.get("name").toString();
                    applicationName = mapplicationName;

                    //ActivityCompat.requestPermissions(mainActivity, new String[]{});
                    Log.i(TAG, "Device Successfully Registered");
                    Witness.notify(mDeviceToken);
                    qwasiInterface.onSuccess(mDeviceToken);
                }
                catch (JSONException e){
                    Log.wtf(TAG, "Malformed JsonObject response " + e.getMessage());
                }
            }

            @Override
            public void onFailure(QwasiError e) {
                Log.e("QwasiError", "Device Failed to Register: "+ e.getMessage());
                QwasiError  error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceRegistrationFailed,
                        "Device Registration failed" +e.getMessage());
                Witness.notify(error);
                qwasiInterface.onFailure(error);
            }
        });
    }

    public void registerDevice(String deviceToken, String name, String userToken, QwasiInterface qwasiInterface){
        this.mregisterDevice(deviceToken, name, userToken, null, qwasiInterface);
    }

    public void registerDevice(String deviceToken, String name, String userToken, HashMap<String, Object> userInfo, QwasiInterface qwasiInterface) {
        this.mregisterDevice(deviceToken, name, userToken, userInfo, qwasiInterface);}

    public void registerDeviceWithName(String deviceToken, String name, QwasiInterface qwasiInterface) {
        this.mregisterDevice(deviceToken, name, null, null, qwasiInterface);
    }

    public void registerDevice(String deviceName, String userToken, QwasiInterface qwasiInterface){
        this.mregisterDevice(deviceName, null, userToken, null, qwasiInterface);
    }

    public void registerDevice(String deviceToken, QwasiInterface qwasiInterface){
        this.mregisterDevice(deviceToken, null, null, null, qwasiInterface);
    }

    public void registerDevice(String deviceToken, String userToken){
        this.mregisterDevice(deviceToken, null, userToken, null, defaultCallback);
    }

    public void registerDevice(String deviceToken){
        this.mregisterDevice(deviceToken, null, null, null, defaultCallback);
    }

    public void unregisterDevice(String deviceToken){
        this.unregisterDevice(deviceToken, defaultCallback);
    }

    public synchronized String getUserToken(){
        return muserToken;
    }

    public synchronized void setUserToken(String userToken)/*iOS 381*/{
        muserToken = userToken;
        if (mRegistered){
            Map<String, Object> parms = new HashMap<>();

            parms.put("id", mDeviceToken);
            parms.put("user_token", muserToken);
            mClient.invokeNotification("device.set_user_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Device Token Set");
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Set UserToken Failed: " + e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorSetUserTokenFailed,
                                    "Set UserToken Failed " + e.getMessage());
                    Witness.notify(error);
                    //callbacks.onFailure(error);
                }
            });
        }
    }

    public synchronized void unregisterDevice(String deviceToken, final QwasiInterface qwasiInterface)/*iOS 402*/{
        if(mRegistered){
            HashMap<String, Object> parm = new HashMap<>();
            parm.put("id", deviceToken==null?mDeviceToken:deviceToken);

            mClient.invokeNotification("device.unregister", parm, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    mDeviceToken = "";
                    mRegistered = false;
                    mpushEnabled = false;
                    pushEnabled = false;
                    mlocationEnabled = false;
                    locationEnabled = mlocationEnabled;
                    setLocationEnabled(locationEnabled);
                    setPushEnabled(pushEnabled);
                    Log.i(TAG, "UnregisterDevice Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", e.getMessage());
                    qwasiInterface.onFailure(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorDeviceUnregisterFailed,
                                    "Qwasi Device Unregister failed: " + e.getMessage()));
                }
            });
        }
        else{ //404 device not found
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                            "Device not registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    private synchronized void registerForNotifications(final QwasiInterface callback)/*iOS 433*/{
        String test = mQwasiNotificationManager.getPushToken();
        if (test == null || test.isEmpty())  {
            mPushTokenCallback =new Reporter() {
                @Override
                public void notifyEvent(Object o) {
                    registerForNotifications(callback);
                }
            };
            Witness.register(Boolean.class, mPushTokenCallback);
        } else if (mRegistered){
            Witness.remove(Boolean.class, mPushTokenCallback);
            String pushGCM = mQwasiNotificationManager.getPushToken();
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mDeviceToken);
            parms.put("proto", "push.gcm");
            parms.put("token", pushGCM);
            mClient.invokeMethod("device.set_push_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Set Push Token success");
                    mQwasiNotificationHandler = new Reporter() {
                        @Override
                        public void notifyEvent(Object o) {
                            HashMap<String, Object> results = new HashMap<>();
                            String qwasi = ((Bundle) o).getString("qwasi", "");

                            qwasi = qwasi.replaceAll(Pattern.quote("}"), "")
                                    .replaceAll(Pattern.quote("{"), "")
                                    .replaceAll(Pattern.quote("\""), "");

                            String[] pairs = qwasi.split(Pattern.quote(","));
                            for (String pair : pairs) {
                                String[] key = pair.split(Pattern.quote(":"), 2);
                                results.put(key[0], key[1]);
                            }
                            String msgId = results.get("msg_id").toString();
                            String appId = results.get("app_id").toString();
                            if (!(msgId.isEmpty()) && !(appId.isEmpty())) {
                                if (appId.equals(config.mapplication)||appId.equals(mconfig.mapplication)) {
                                    if ((mMessageCache.isEmpty()) || (!mMessageCache.containsKey(msgId))) {
                                        fetchMessageForNotification(msgId, new QwasiInterface() {
                                            @Override
                                            public void onSuccess(Object o) {
                                                useLocalNotifications = museLocalNotifications;
                                                if (useLocalNotifications) {
                                                    sendNotification((QwasiMessage) o);
                                                }
                                                else
                                                    Witness.notify(o);
                                            }

                                            @Override
                                            public void onFailure(QwasiError e) {

                                            }
                                        });
                                    } else {
                                        QwasiMessage message = mMessageCache.get(msgId);
                                        useLocalNotifications = museLocalNotifications;
                                        if (useLocalNotifications) sendNotification(message);
                                        else Witness.notify(message);
                                    }
                                }
                            } else {
                                callback.onFailure(new QwasiError()
                                        .errorWithCode(QwasiErrorCode.QwasiErrorInvaildMessage, "Message is Invalid"));
                            }
                        }
                    };
                    mpushEnabled = true;
                    pushEnabled = mpushEnabled;
                    Witness.register(Bundle.class, mQwasiNotificationHandler);
                    callback.onSuccess(mQwasiNotificationManager.getPushToken());
                    Witness.notify(mQwasiNotificationManager.getPushToken());
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Set Push Token failed: " + e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorPushRegistrationFailed,
                                    "Push Registration failed: " + e.getMessage());
                    Witness.notify(error);
                    callback.onFailure(error);
                }
            });
            //todo M Remote notification allowed?
        }
        else {
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device not Registered");
            Witness.notify(error);
            callback.onFailure(error);
        }
    }

    private synchronized void unregisterForNotifications(final QwasiInterface callback){
        if(mRegistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mDeviceToken);
            parms.put("proto", "push.poll");
            parms.put("token", "");
            mClient.invokeNotification("device.set_push_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d(TAG, "Device unregistered for push success");
                    mpushEnabled = false;
                    pushEnabled = mpushEnabled;
                    Witness.remove(Bundle.class, mQwasiNotificationHandler);
                    callback.onSuccess(new QwasiError().
                            errorWithCode(QwasiErrorCode.QwasiErrorNone, "UnSet Push Token success"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Unregister for Note failed: " + e.getMessage());
                    callback.onFailure(new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorPushUnregisterFailed,
                            "Push Unregister Failed"));
                }
            });
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "device not Registered");
            Witness.notify(error);
            callback.onFailure(error);
        }
    }

    public void fetchMessageForNotification(String userInfo) {
        this.fetchMessageForNotification(userInfo, defaultCallback);
    }

    public synchronized void fetchMessageForNotification(final String msgId, final QwasiInterface qwasiInterface) {
        if(mRegistered){
            HashMap<String, Object> flags = new HashMap<>();
            flags.put("opened", mQwasiAppManager.isApplicationInForeground());
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mDeviceToken);
            parms.put("id", msgId);
            parms.put("flags", flags);

            mClient.invokeMethod("message.fetch", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    QwasiMessage temp = new QwasiMessage().messageWithData((JSONObject) o);
                    mMessageCache.put(msgId, temp);
                    qwasiInterface.onSuccess(temp);
                }

                @Override
                public void onFailure(QwasiError e) {
                    if (e.getMessage().contains("404")) {
                        qwasiInterface.onFailure(new QwasiError()
                                .errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound, "No messages found"));
                    } else
                        qwasiInterface.onFailure(new QwasiError()
                                .errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed, "Message fetch failed"));
                }
            });
        }
        else {
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public void fetchUnreadMessage(){
        this.fetchUnreadMessage(defaultCallback);}

    public synchronized void fetchUnreadMessage(final QwasiInterface qwasiInterface){
        if(mRegistered) {
            HashMap<String, Object> parms = new HashMap<>();
            HashMap<String, Object> options = new HashMap<>();
            options.put("fetch", String.valueOf(true));
            parms.put("device", mDeviceToken);
            parms.put("options", options);
            mClient.invokeMethod("message.poll", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    //Log.i(TAG, o.toString());
                    QwasiMessage message = new QwasiMessage();
                    message.messageWithData((JSONObject) o);
                    mMessageCache.put(message.messageId, message);
                    if (useLocalNotifications||museLocalNotifications) sendNotification(message);
                    else Witness.notify(message);
                    qwasiInterface.onSuccess(message);
                }

                @Override
                public void onFailure(QwasiError e) {
                    QwasiError error;
                    //parse error
                    error = e.getMessage().contains("404") ?
                            new QwasiError()
                                    .errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound,
                                            "No messages for Device") :
                            new QwasiError()
                                    .errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed,
                                            "Message Fetch Failed");

                    qwasiInterface.onFailure(error);
                }
            });
        }
        else {
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered, "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void postEvent(String type, HashMap<String, Object> data, Boolean retry, final QwasiInterface qwasiInterface){
        if(mRegistered){
            HashMap<String, Object> parms = new HashMap<>();
            data = data == null?new HashMap<String, Object>():data;
            parms.put("device", mDeviceToken);
            parms.put("type", type);
            parms.put("data", data);
            mClient.invokeNotification("event.post", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Event Posted");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorPostEventFailed, "Event post failed " + e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else {
            Log.e("QwasiError", "Device NotRegistered");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered, "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public void postEvent(String type, HashMap<String, Object> data, Boolean retry){
        Boolean Retry = retry == null?true:retry;
        this.postEvent(type, data, Retry, defaultCallback);
    }

    public void tryPostEvent(String event, HashMap<String, Object> data) {
        this.postEvent(event, data, false);
    }

    public void fetchLocationsNear(QwasiLocation place){
       this.fetchLocationsNear(place, defaultCallback);
    }

    public synchronized void fetchLocationsNear(QwasiLocation place, final QwasiInterface qwasiInterface) {
        if(mRegistered) {
            //if (mlocationEnabled) {
            place = place == null? QwasiLocation.initEmpty() : place;
            HashMap<String, Object> parms = new HashMap<>();
            HashMap<String, Object> near = new HashMap<>();
            if (!place.mEmpty) {
                near.put("lng", place.getLongitude());
                near.put("lat", place.getLatitude());
                near.put("radius", mLocationSyncFilter * 10);
                parms.put("near", near);
                near = new HashMap<>();
                near.put("schema", "2.0");
                parms.put("options", near);
            }
            mClient.invokeMethod("location.fetch", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    JSONArray positions;
                    try {
                        positions = ((JSONObject) o).getJSONArray("value");
                        for (int index = 0; index < positions.length(); index++) {
                            locationManager.startMoitoringLocation(
                                    QwasiLocation.initWithLocationData(positions.getJSONObject(index)));
                        }
                        //mlocationManager.pruneLocations();
                    } catch (JSONException e) {
                        Log.wtf(TAG, "malformed JSONArray");
                        e.printStackTrace();
                    }
                    qwasiInterface.onSuccess(locationManager.mregionMap);
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorLocationFetchFailed,
                                    "Location Fetch Failed: " + e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
            /*}
            //todo for M: Handle location permissions
            else{
                Log.e("QwasiError", "Locations not enabled");
                QwasiError error = new QwasiError()
                        .errorWithCode(QwasiErrorCode.QwasiErrorLocationAccessDenied,
                                "Location Access is Disabled");
                Witness.notify(error);
                qwasiInterface.onFailure(error);
            }*/
        }
        else {
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device not registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public void subscribeToChannel(String channel){
        this.subscribeToChannel(channel, defaultCallback);
    }

    public synchronized void subscribeToChannel(final String channel, final QwasiInterface qwasiInterface){
        if(mRegistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mDeviceToken);
            parms.put("channel", channel);
            mClient.invokeNotification("channel.subscribe", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "subscribe to channel success");
                    if (!mChannels.containsKey(channel))
                        mChannels.put(channel, null);
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone,
                                    "No error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Subscribe to channel Failed: "+e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorChannelSubscribeFailed,
                                    "Channel Subscribe Failed "+e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else {
            Log.e("QwasiError", "Device NotRegistered");
            qwasiInterface.onFailure(new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device Not Registered"));
        }
    }

    public void unsubscribeFromChannel(String channel){
        this.unsubscribeFromChannel(channel, defaultCallback);
    }

    public synchronized void unsubscribeFromChannel(final String channel, final QwasiInterface qwasiInterface){
        if(mRegistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mDeviceToken);
            parms.put("channel", channel);
            mClient.invokeNotification("channel.unsubscribe", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Unsubcribe from channel Success");
                    if (mChannels.containsKey(channel)) mChannels.remove(channel);
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Unsubscribe from channel Failed: "+e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorChannelUnsubscribeFailed,
                                    "Channel Unsubscribe Failed "+e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else {
            Log.e("QwasiError", "Device NotRegistered");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered, "Device not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    //device set
    public synchronized void setDeviceValue(Object value, String key, final QwasiInterface qwasiInterface){
        if(mRegistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mDeviceToken);
            parms.put("key", key);
            parms.put("value", value);
            mClient.invokeNotification("device.set_data", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Set data Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Set data Failed");
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorSetDeviceDataFailed,
                                    "Device Data Set Failed: "+e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            qwasiInterface.onFailure(new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                            "Device not Registered"));
        }
    }

    public void setDeviceValue(Object value, String key){
        this.setDeviceValue(value, key, defaultCallback);
    }

    public void deviceValueForKey(String key) {
        this.deviceValueForKey(key, defaultCallback); //default
    }
    //device.get
    public synchronized void deviceValueForKey(final String key, final QwasiInterface qwasiInterface){
        if (mRegistered){
            final Map<String, Object> parms = new HashMap<>();
            parms.put("id", mDeviceToken);
            parms.put("key", key);
            mClient.invokeMethod("device.get_data", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    qwasiInterface.onSuccess(o);
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Get data Failed: " + e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorGetDeviceDataFailed,
                                    "Error Retriving: " + key + " for device " + mDeviceToken);
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                            "Device not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void setMemberValue(Object value, String key){
        setMemberValue(value, key, defaultCallback);
    }

    public synchronized void setMemberValue(Object value, String key, final QwasiInterface qwasiInterface){
        if (mRegistered){
            Map<String, Object> parms = new HashMap<>();
            parms.put("id", muserToken);
            parms.put("key",key);
            parms.put("value", value);
            mClient.invokeMethod("member.set", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onFailure(QwasiError e) {

                }
            });
        }
        else{
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorSetMemberDataFailed, "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void memberValueForKey(String key){
        memberValueForKey(key, defaultCallback);
    }

    public synchronized void memberValueForKey(String key, final QwasiInterface qwasiInterface){
        if (mRegistered){
            Map<String, Object> parms = new HashMap<>();
            parms.put("id", muserToken);
            parms.put("key", key);
            mClient.invokeMethod("member.get", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Member Value retrieval");
                    qwasiInterface.onSuccess(o);
                }

                @Override
                public void onFailure(QwasiError e) {

                }
            });
        }
        else {
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorSetMemberDataFailed, "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void memberSetUserName(String userName, String password, String currentPass){
        this.memberSetUserName(userName, password, currentPass,defaultCallback);
    }

    public synchronized void memberSetUserName(String userName, String password, String currentPass, final QwasiInterface qwasiInterface){
        if (mRegistered){
            final Map<String, Object> parms = new HashMap<>();
            parms.put("id", muserToken);
            parms.put("username", userName);
            parms.put("password", password);
            parms.put("current", currentPass);
            mClient.invokeMethod("member.set_auth", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Member Auth Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e(TAG, "Member Auth Failed: "+e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorSetMemberAuthFailed,
                                    "Member Set Auth Failed");
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });

        }
        else{
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSetMemberAuthFailed, "Device Not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void memberAuthUser(String userName, String password){
        this.memberAuthUser(userName, password, defaultCallback);
    }

    public synchronized void memberAuthUser(String userName, String password, final QwasiInterface qwasiInterface){
        if (mRegistered){
            final Map<String, Object> parms = new HashMap<>();
            parms.put("username", userName);
            parms.put("password", password);
            mClient.invokeMethod("member.auth", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.i(TAG, "Member Auth Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e(TAG, "Member Auth Failed: "+e.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorAuthMemberFailed,
                                    "Member Auth Failed");
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else{
            QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSetMemberAuthFailed, "Device not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public synchronized void sendMessage(QwasiMessage message, String userToken, final QwasiInterface qwasiInterface){
        if(mRegistered) {
            Object payload = message.mpayload;
            //HashMap<String, Object> encrypted = new HashMap<>();
            String encrypted;
            if (payload != null){
                if (payload instanceof JSONObject) payload = Base64.encode(payload.toString().getBytes(), Base64.DEFAULT);
                else if (payload instanceof String) {  //payload is plaintext
                    try {
                        encrypted = URLDecoder.decode((String) payload, "UTF-8");
                        payload = Base64.encode(encrypted.getBytes(), Base64.DEFAULT);
                    }
                    catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                        Log.e(TAG, "Decoding failed");
                    }
                }
                else payload = ""; //message is silent
            //throw an error, get the data if the data is null, or the error isn't print error
            //set payload to the JSONData
            }
            else{
                qwasiInterface.onFailure(new QwasiError()
                        .errorWithCode(QwasiErrorCode.QwasiErrorInvaildMessage, "Invalid Message"));
            }
            Map<String, Object> parms = new HashMap<>();
            HashMap<String, Object> audi = new HashMap<>();
            audi.put("user_tokens", userToken);
            parms.put("audience", audi); //can be devices, usertokens, channels w/e
            parms.put("payload_type", message.mpayloadType);
            parms.put("notification", new HashMap<String, Object>().put("text", message.malert));
            parms.put("payload", payload);
            parms.put("tags", message.mtags);
            parms.put("options", (new HashMap<String, Object>().put("encodedPayload", true)));
            mClient.invokeMethod("message.send", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object responseObject) {
                    Log.i(TAG, "Message Sent Successfully");
                    qwasiInterface.onSuccess(responseObject);
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Message Send Failed: "+e.getMessage());
                    QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSendMessageFailed,
                            "Send message Failed " + e.getMessage());
                    Witness.notify(error);
                    qwasiInterface.onFailure(error);
                }
            });
        }
        else{
            Log.e("QwasiError", "Device not Registered");
            QwasiError  error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device not Registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    public void  sendMessage(QwasiMessage message, String userToken){
        this.sendMessage(message, userToken, defaultCallback);
    }

    public synchronized Map<String, Void> channels(){
        return (new HashMap<>(mChannels));
    }

    public interface QwasiInterface{
        void onSuccess(Object o);
        void onFailure(QwasiError e);
    }

    private void sendNotification(QwasiMessage message)/*android default notification builder*/{
        if (message != null) {
            Uri defaultSoundUri = message.silent() ?
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) :
                    null;

            String appName = sContext.getPackageManager().getApplicationLabel(sContext.getApplicationInfo()).toString();
            if (mQwasiNotificationManager.mNoteBuilder == null) new QwasiGCMListener().onMessagePolled();
            NotificationCompat.Builder noteBuilder = mQwasiNotificationManager.mNoteBuilder
                    .setSmallIcon(sContext.getApplicationInfo().icon)
                    .setContentTitle(appName)
                    .setContentText(message.malert)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL) //default sound and vibrate
                    .setSound(defaultSoundUri); //default sound;
            //configure expanded action
            if (message.mpayloadType.contains("text")) noteBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message.description()));
            //allows stuff when expanded.  BigTextStyle, BigPictureStyle, and InboxStyle
            else if (message.mpayloadType.contains("image")) Log.d(TAG, "Image");
            //noteBuilder.setStyle(new NotificationCompat.BigPictureStyle().b);
            else if (message.mpayloadType.contains("json")) Log.d(TAG, "App context");
            NotificationManager noteMng = (NotificationManager) sContext.getSystemService(Context.NOTIFICATION_SERVICE);
            noteMng.notify(0, noteBuilder.build());
            Witness.notify(message);
        }
    }

    static float UPDATE_FILTER(float Speed, float Filter)/*iOS inline function*/{return (Speed/PED_FILTER)*Filter;}
    static float MAX(float number_1, float number_2)/*iOS inline function*/{return number_1>number_2?number_1:number_2;}

    public boolean isInForground(){
        return mQwasiAppManager.isApplicationInForeground();
    }
}