package com.qwasi.sdk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class Qwasi {
    private Context context = null;
    private Application sharedApplication;
    private SharedPreferences preferences;
    private double locationSyncFilter;
    private double locationUpdatefilter;
    private double locationEventFilter;
    private boolean mregistered;
    QwasiAppManager qwasiAppManager = null; //package accessable
    private QwasiNotificationManager qwasiNotificationManager= null;
    public String mapplicationName = null;
    private String mdeviceToken = null;
    private QwasiClient mclient = null;
    public NetworkInfo networkInfo;
    private HashMap<String, QwasiMessage> mmessageCache;
    public QwasiLocationManager mlocationManager;
    public QwasiConfig mconfig;
    public String muserToken;
    public Boolean mpushEnabled;
    public Boolean mlocationEnabled;
    public Boolean meventsEnabled;
    public Boolean museLocalNotifications; //apple only?
    private static Qwasi instance;

    private QwasiInterface defaultCallback = new QwasiInterface() {
        @Override
        public void onSuccess(Object o) { //should recieve QwasiMessage, or QwasiError
            if(o instanceof QwasiMessage){
                Log.d("QwasiDebug", ((QwasiMessage) o).description());
            } //ignore QwasiError since it's only NoError
        }

        @Override
        public void onFailure(QwasiError e) {
            //e.printStackTrace();
            Log.e("QwasiError", e.message);
        }
    };

    private Qwasi(){
        this.mclient = new QwasiClient();
        instance = this;
    }

    public void initQwasi(Context application){
        if (context != null){
            return;
        }
        context = application;
        sharedApplication = (Application) context.getApplicationContext();
        this.qwasiAppManager = new QwasiAppManager();
        this.networkInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        qwasiNotificationManager= new QwasiNotificationManager(context, this);
        mconfig = new QwasiConfig(application);
        mlocationManager = new QwasiLocationManager();
        mconfig.configWithFile(); //default
        if (mconfig.isValid()){
            instance.initWithConfig(mconfig);
        }
        else{
            Log.d("QwasiDebug", "Config in Manifest not valid; Please init with valid config.");
        }
    }

    public synchronized static Qwasi getInstance(){
        if (instance == null){
            instance = new Qwasi();
        }
        return instance;
    }


    public Context getContext(){return context;}

    public Qwasi qwasiWithConfig(QwasiConfig config) {
        return(this.initWithConfig(config));
    }

    public boolean getRegistrationStatus() {
        return mregistered;
    }

    public String getMdeviceToken(){
        return mdeviceToken;
    }

    private String getVerboseVersionName(){
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
            default:
                return "Android Unknown";   //21&22  12.4%
        }
    }

    private Qwasi initWithConfig(QwasiConfig config){
        if ((config != null)&&(config.isValid())) {
            mconfig = config;
            this.setConfig(config);
        }
        locationUpdatefilter=100.0;
        locationEventFilter = 50.0;
        locationSyncFilter = 200.0;

        mlocationManager.init();
        preferences = context.getSharedPreferences("qwasi_sdk", Context.MODE_PRIVATE);
        mregistered = preferences.getBoolean("registered", false);

        //are localNotifcations set
        museLocalNotifications = preferences.getBoolean("localNote", false);

        //if we have a device token saved already use it.
        mdeviceToken = preferences.getString("qwasi_device_token", "");
        //check if we have a gcm token already so we don't use too much data
        qwasiNotificationManager.setPushToken(preferences.getString("gcm_token", null));

        if (qwasiNotificationManager.getPushToken() == null){
            qwasiNotificationManager.registerForRemoteNotification();
        }

        mmessageCache = new HashMap<>();

        muserToken = preferences.getString("qwasi_user_token","DROIDTOKEN");
        this.sharedApplication.registerActivityLifecycleCallbacks(qwasiAppManager);
        return this;
    }

    public synchronized void setConfig(QwasiConfig config){
        this.mconfig = config;
        mclient = mclient.clientWithConfig(config, this);
        mregistered = false;
    }

    public void setPushEnabled(){
        if(mpushEnabled)
            this.setPushEnabled(defaultCallback);
        else
            this.setPushEnabled(defaultCallback);
    }

    public void setPushEnabled(QwasiInterface callback){
        if (mpushEnabled){
            this.registerForNotifications(callback);
        }
        else{
            this.unregisterForNotifications(callback);
        }
    }
    public synchronized void setLocationEnabled(boolean enabled){
        mlocationEnabled = enabled;
        if (mlocationEnabled){
            QwasiLocation current = mlocationManager.getLastLocation();
            double speed = current.getSpeed();

        }
    }

    private synchronized void mregisterDevice(String deviceToken,
                                       String name,
                                       String userToken,
                                       HashMap<String, Object> userInfo,
                                       final QwasiInterface qwasiInterface) {

        if (deviceToken == null){  //if devicetoken is null set it empty so the server will gen one
            deviceToken = mdeviceToken;
        }

        if (name == null){  //if name is null get it from the phone, or user can give us one
            name = Build.PRODUCT;
        }

        if (userToken == null){  //if we didn't get a usertoken set it to be the phone number
            userToken = muserToken;
        }
        Map<String, Object> info = new HashMap<>();

        if (userInfo != null) {  //make sure that it's init so we don't crash
            if (!userInfo.isEmpty()) { //make sure it's not empty
                info.put("info", userInfo); //put all the recieved info into info
            }
        }

        Map<String, Object> deviceInfo = new HashMap<>();
        if (BuildConfig.DEBUG) { //sets our debug value
            deviceInfo.put("debug", true);
        }
        else {
            deviceInfo.put("debug", false);
        }
        deviceInfo.put("version", String.valueOf(Build.VERSION.RELEASE));  //this is the numerical version
        deviceInfo.put("system", getVerboseVersionName()); //kitkat or w/e the codename is
        deviceInfo.put("model", Build.MANUFACTURER + " " + Build.MODEL);  //Samsung Then the actual device name
        deviceInfo.put("sdkVersion", String.valueOf("2.1.0"));  //set because that's what the sdk is currently
        //since JSONRPC2 on java reads from a Hashmap<string, object> easier to put it here
        //and since the JSONRPC2 doesn't format it how the server expects have to do some playing.
        info.put("info", deviceInfo);
        info.put("name", name);
        info.put("options", null);
        info.put("user_token", userToken); //phonenumber
        info.put("id", deviceToken);
        mclient.invokeMethod("device.register", info, new QwasiInterface() {
            @Override
            public void onSuccess(Object o) {
                SharedPreferences.Editor editor = preferences.edit();
                mregistered = true; //we've now registered
                editor.putBoolean("registered", mregistered);

                Map<String, Object> result = new HashMap<>();  //holder object
                result.put("result", o);  //unpack response object
                result = (Map<String, Object>) result.get("result"); //further unpacking

                //response.hashCode();
                mdeviceToken = result.get("id").toString();  //set our device token from the server
                editor.putString("qwasi_device_token", mdeviceToken);

                //grab the next key and unpack it.
                Map<String, Object> info = (Map<String, Object>) result.get("application");
                mapplicationName = info.get("name").toString();
                editor.putString("qwasi_app_id", info.get("id").toString());

                //get the settings out
                info = (Map<String, Object>) info.get("settings");
                mpushEnabled = (Boolean) info.get("push_enabled");
                mlocationEnabled = (boolean) info.get("location_enabled");
                if (mlocationEnabled) mlocationManager.mmanager.connect();
                meventsEnabled = (Boolean) info.get("events_enabled");

                editor.apply();
                Log.d("QwasiDebug", "Device Successfully Registered");
                Witness.notify(mdeviceToken);
                qwasiInterface.onSuccess(new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
            }

            @Override
            public void onFailure(QwasiError e) {
                Log.e("QwasiError", "Device Failed to Register");
                QwasiError  error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceRegistrationFailed,
                        "Device Registration failed");
                Witness.notify(error);
                qwasiInterface.onFailure(error);
            }
        });
    }

    public void setUserToken(String userToken){
        this.setUserToken(userToken, defaultCallback);
    }

    public synchronized void setUserToken(String userToken, final QwasiInterface callbacks){
        muserToken = userToken;
        if (mregistered){
            Map<String, Object> parms = new HashMap<>();

            parms.put("id", mdeviceToken);
            parms.put("user_token", muserToken);
            mclient.invokeMethod("device.set_user_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    callbacks.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) { //todo handle type of failure
                    Log.e("QwasiError", "Set UserToken Failed");
                    QwasiError error =new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorSetUserTokenFailed,
                                    "Set UserToken Failed");
                    Witness.notify(error);
                    callbacks.onFailure(error);
                }
            });
        }
    }

    public void registerDevice(String deviceToken, String name, String userToken, QwasiInterface qwasiInterface){
        this.mregisterDevice(deviceToken, name, userToken, null, qwasiInterface);
    }

    public void registerDevice(String deviceToken, String name, String userToken, HashMap<String, Object> userInfo, QwasiInterface qwasiInterface) {
        this.mregisterDevice(deviceToken, name, userToken, userInfo, qwasiInterface);}

    public void registerDevice(String deviceToken, String name, QwasiInterface qwasiInterface) {
        this.mregisterDevice(deviceToken, name, null, null, qwasiInterface);
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

    public synchronized void unregisterDevice(String deviceToken, final QwasiInterface qwasiInterface){
        if(mregistered){
            HashMap<String, Object> parm = new HashMap<>();
            if (deviceToken == null){
                parm.put("id", mdeviceToken);
            }
            else {
                parm.put("id", deviceToken);
            }

            mclient.invokeMethod("device.unregister", parm, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    mdeviceToken = "";
                    mregistered = false;
                    mpushEnabled = false;
                    mlocationEnabled = false;
                    Log.d("QwasiDebug", "UnregisterDevice Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.d("Debug", e.getMessage());
                    qwasiInterface.onFailure(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorDeviceUnregisterFailed,
                                    "Qwasi Device Unregister failed"));
                }
            });
        }
        else{
            Log.e("QwasiError", "Device Not Registred");
            QwasiError error = new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                            "Device not registered");
            Witness.notify(error);
            qwasiInterface.onFailure(error);
        }
    }

    private synchronized void registerForNotifications(final QwasiInterface callback){
        if(mregistered){
            while (qwasiNotificationManager.isRegistering()){}
            String pushGCM = qwasiNotificationManager.getPushToken();
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mdeviceToken);
            parms.put("proto", "push.gcm");
            parms.put("token", pushGCM);
            mclient.invokeMethod("device.set_push_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", "Set Push Token success");
                    callback.onSuccess(
                            new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No error"));
                }

                @Override
                public void onFailure(QwasiError e) { //todo handle why network failed
                    Log.e("QwasiError", "Set Push Token failed");
                    callback.onFailure(new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorPushRegistrationFailed,
                            "Push Registration failed"));
                }
            });
            //todo check if remote notes are allowed

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
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mdeviceToken);
            parms.put("proto", "push.poll");
            parms.put("token", "");
            mclient.invokeMethod("device.set_push_token", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    mpushEnabled = false;
                    callback.onSuccess(new QwasiError().
                            errorWithCode(QwasiErrorCode.QwasiErrorNone, "UnSet Push Token success"));
                }

                @Override
                public void onFailure(QwasiError e) { //todo handle why it failed
                    Log.e("QwasiError", "Unregister for Note failed");
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

    public void fetchMessageForNotification(Bundle userInfo) {
        this.fetchMessageForNotification(userInfo, defaultCallback);
    }

    public synchronized void fetchMessageForNotification(Bundle userInfo, final QwasiInterface qwasiInterface) {
        if(mregistered){
            QwasiError error;
            HashMap<String, Object> flags = new HashMap<>();
            HashMap<String, Object> results = new HashMap<>();
            flags.put("opened", qwasiAppManager.isApplicationInForeground());
            String qwasi =(String) userInfo.get("qwasi");
            qwasi = qwasi.replaceAll(Pattern.quote("}"), "").replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("\""), "");
            String[] pairs = qwasi.split(Pattern.quote(","));
            for(String pair :pairs){
                String[] key= pair.split(":");
                results.put(key[0], key[1]);
            }
            final String msgId = results.get("msg_id").toString();
            String appId = results.get("app_id").toString();
            if (!(msgId.isEmpty()) && !(appId.isEmpty())){
                if (appId.equals(preferences.getString("qwasi_app_id", ""))){
                    //cached message
                    if ((mmessageCache.isEmpty())||(!mmessageCache.containsKey(msgId))){
                        HashMap<String, Object> parms = new HashMap<>();
                        parms.put("device", mdeviceToken);
                        parms.put("id", msgId);
                        parms.put("flags", flags);

                        mclient.invokeMethod("message.fetch", parms, new QwasiInterface() {
                            @Override
                            public void onSuccess(Object o) {
                                QwasiMessage temp = new QwasiMessage().messageWithData((HashMap<String, Object>) o);
                                mmessageCache.put(msgId, temp);
                                Witness.notify(temp);
                                qwasiInterface.onSuccess(temp);
                            }

                            @Override
                            public void onFailure(QwasiError e) { //todo handle error types
                                //parse error?
                                if (e.getCause() instanceof  FileNotFoundException)
                                    qwasiInterface.onFailure(new QwasiError()
                                            .errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound, "No messages found"));
                                else
                                    qwasiInterface.onFailure(new QwasiError()
                                            .errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed, "Message fetch failed"));
                            }
                        });
                    }
                    else{
                        qwasiInterface.onSuccess(mmessageCache.get(msgId));
                    }
                }
            }
            else {
                error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorInvaildMessage,
                        "Invalid Message");
                Witness.notify(error);
                qwasiInterface.onFailure(error);
            }
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
        if(mregistered) {
            HashMap<String, Object> parms = new HashMap<>();
            HashMap<String, Object> options = new HashMap<>();
            options.put("fetch", String.valueOf(true));
            parms.put("device", mdeviceToken);
            parms.put("options", options);
            mclient.invokeMethod("message.poll", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", o.toString());
                    QwasiMessage message = new QwasiMessage()
                            .messageWithData((HashMap<String, Object>) o);
                    mmessageCache.put(message.messageId, message);
                    Witness.notify(message);
                    qwasiInterface.onSuccess(message);
                }

                @Override
                public void onFailure(QwasiError e) {
                    QwasiError error;
                    //parse error
                    if (e.getCause() instanceof FileNotFoundException) {
                        error = new QwasiError()
                                .errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound,
                                        "Message not found ");

                    } else {
                        error = new QwasiError()
                                .errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed,
                                        "Message Fetch Failed");
                    }
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

    public synchronized void postEvent(String type, HashMap<String, Object> data, final QwasiInterface qwasiInterface){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mdeviceToken);
            parms.put("type", type);
            parms.put("data", data);
            mclient.invokeMethod("event.post", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", "Event Posted");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {  //todo handle 401, and 404's app not found device not found
                    Exception cause = new Exception(e);
                    Log.d("Debug", cause.getMessage());
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorPostEventFailed, "Event post failed");
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

    public void postEvent(String type, HashMap<String, Object> data){
        this.postEvent(type, data, defaultCallback);
    }

    public void fetchLocationsNear(QwasiLocation place){
       this.fetchLocationsNear(place, defaultCallback);
    }

    public synchronized void fetchLocationsNear(QwasiLocation place, final QwasiInterface qwasiInterface) {
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            HashMap<String, Object> near = new HashMap<>();
            near.put("lng", place.getLongitude());
            near.put("lat", place.getLatitude());
            near.put("radius", locationSyncFilter*10);
            parms.put("near", near);
            near = new HashMap<>();
            near.put("schema", "2.0");
            parms.put("options", near);
            mclient.invokeMethod("location.fetch", parms, new QwasiInterface(){
                @Override
                public void onSuccess(Object o){
                    JSONArray positions;
                    try {
                        positions = new JSONArray(o.toString());
                        JSONObject obj;
                        for (int index=0; index < positions.length(); index++) {
                            obj = positions.getJSONObject(index);
                            //if this location doesn't already exist in the mregionMap add it
                            if (Qwasi.getInstance().mlocationManager.mregionMap.containsKey(obj.getString("id"))) {
                                //for locations in response figure out what type they are i.e. beacons/geofence/
                                if (obj.has("beacon")) {//deal with beacons using altBeacons
                                    Log.d("QwasiDebug", "beacon");
                                }
                                else if (obj.has("geofence")) {
                                    JSONArray latlng = obj.getJSONObject("geofence").
                                            getJSONObject("geometry").
                                            getJSONArray("coordinates");
                                    Integer rad = obj.getJSONObject("geofence").
                                            getJSONObject("properties").
                                            getInt("radius");
                                    Geofence temp = new Geofence.Builder()
                                            .setRequestId(obj.getString("id"))
                                            .setCircularRegion(latlng.getDouble(1), latlng.getDouble(0), rad)
                                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                            .setLoiteringDelay(obj.getJSONObject("properties").getInt("dwell_interval") * 1000)
                                                    //.setNotificationResponsiveness(obj.getJSONObject("properties").getInt("enter_interval")*1000)
                                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                                            .build();
                                    mlocationManager.startMoitoringLocation(temp);
                                }
                                else {//rfid?
                                    Log.d("QwasiDebug", "rfid");
                                }
                            }
                            //else the id is contained in array
                        }
                    }catch (JSONException e){ //todo handle jsonExeceptions?

                    }
                    qwasiInterface.onSuccess(mlocationManager.mregionMap);
                }

                @Override
                public void onFailure(QwasiError e){
                    e.printStackTrace();
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorLocationFetchFailed,
                                    "Location Fetch Failed");
                    Witness.notify(error);
                    qwasiInterface.onFailure(error); //fixme 401/404
                }
            });
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

    public synchronized void subscribeToChannel(String channel, final QwasiInterface qwasiInterface){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mdeviceToken);
            parms.put("channel", channel);
            mclient.invokeMethod("channel.subscribe", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", "subscribe to channel success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone,
                                    "No error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Subscribe to channel Failed");
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorChannelSubscribeFailed,
                                    "Channel Subscribe Failed");
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

    public synchronized void unsubscribeFromChannel(String channel, final QwasiInterface qwasiInterface){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("device", mdeviceToken);
            parms.put("channel", channel);
            mclient.invokeMethod("channel.unsubscribe", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", "Unsubcribe from channel Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Unsubscribe from channel Failed");
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorChannelUnsubscribeFailed,
                                    "Channel Unsubscribe Failed");
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

    public synchronized void setDeviceValue(Object value, String key, final QwasiInterface qwasiInterface){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<>();
            parms.put("id", mdeviceToken);
            parms.put("key", key);
            parms.put("value", value);
            mclient.invokeMethod("device.set_data", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {
                    Log.d("QwasiDebug", "Set data Success");
                    qwasiInterface.onSuccess(new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error"));
                }

                @Override
                public void onFailure(QwasiError e) { //todo 404/401
                    Log.e("QwasiError", "Set data Failed");
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorSetDeviceDataFailed,
                                    "Device Data Set Failed");
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

    public synchronized void deviceValueForKey(final String key, final QwasiInterface qwasiInterface){
        if (mregistered){
            final Map<String, Object> parms = new HashMap<>();
            parms.put("id", mdeviceToken);
            parms.put("key", key);
            mclient.invokeMethod("device.get_data", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object o) {qwasiInterface.onSuccess(o);}

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Get data Failed");
                    QwasiError error = new QwasiError()
                            .errorWithCode(QwasiErrorCode.QwasiErrorGetDeviceDataFailed,
                                    "Error Retriving: " + key + " for device " + mdeviceToken);
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

    public synchronized void sendMessage(QwasiMessage message, String userToken, final QwasiInterface qwasiInterface) {
        if(mregistered) {
            Object payload = message.mpayload;
            if (payload != null){
                if (payload instanceof JSONObject){
                    //todo fix this issue
                }
                else if (payload instanceof String){
                    HashMap<String, Object> encrypted = new HashMap<>();
                    encrypted.put("payload", Base64.encode(((String) payload).getBytes(), Base64.DEFAULT));
                }
                else{
                    payload = null;
                }
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
            mclient.invokeMethod("message.send", parms, new QwasiInterface() {
                @Override
                public void onSuccess(Object responseObject) {
                    Log.d("QwasiDebug", "Message Sent Successfully");
                    qwasiInterface.onSuccess(responseObject);
                }

                @Override
                public void onFailure(QwasiError e) {
                    Log.e("QwasiError", "Message Send Failed");
                    QwasiError error = new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSendMessageFailed,
                            "Send message Failed");
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

    public interface QwasiInterface{
        void onSuccess(Object o);
        void onFailure(QwasiError e);
    }
}
