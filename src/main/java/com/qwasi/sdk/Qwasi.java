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
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class Qwasi {// implements Plugin{
    private Context context;
    private Application sharedApplication;
    private SharedPreferences preferences;
    private double locationSyncFilter;
    private double locationUpdatefilter;
    private double locationEventFilter;
    private boolean mregistered;
    private QwasiAppManager qwasiAppManager = null;
    private QwasiNotificationManager qwasiNotificationManager= null;
    public String mapplicationName = null;
    private String mdeviceToken = null;
    private QwasiClient mclient = null;
    public NetworkInfo networkInfo;
    private HashMap<Object, Object> mmessageCache;
    public QwasiLocationManager mlocationManager;
    public QwasiConfig mconfig;
    public String muserToken;
    public Boolean mpushEnabled;
    public Boolean mlocationEnabled;
    public Boolean meventsEnabled;
    public Boolean museLocalNotifications; //apple only?
    private static Qwasi instance;

    public Qwasi(Context context){
        super();
        this.context = context;
        sharedApplication = (Application) this.context.getApplicationContext();
        this.qwasiAppManager = new QwasiAppManager(this);
        this.mclient = new QwasiClient();
        this.networkInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        qwasiNotificationManager= new QwasiNotificationManager(context, this);
        mlocationManager = new QwasiLocationManager(context, this);
        instance = this;
    }

    public static Qwasi getInstance(){return instance;}

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
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1: //15 min version
                return "Android Ice_Cream Sandwich";
            case Build.VERSION_CODES.JELLY_BEAN: //16 4.1  14.7%
            case Build.VERSION_CODES.JELLY_BEAN_MR1: //17  17.5%
            case Build.VERSION_CODES.JELLY_BEAN_MR2: //18  5.2%
                return "Android Jellybean";
            case Build.VERSION_CODES.KITKAT: //19 4.4      39.2
            case Build.VERSION_CODES.KITKAT_WATCH: //20
                return "Android KitKat";
            default:
                return "Android Unknown";   //21&22  12.4%
        }
    }

    private Qwasi initWithConfig(QwasiConfig config){
        mconfig = config;
        this.setConfig(config);

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

        mmessageCache = new HashMap<Object, Object>();

        muserToken = preferences.getString("qwasi_user_token","DROIDTOKEN");
        this.sharedApplication.registerActivityLifecycleCallbacks(qwasiAppManager);
        return this;
    }

    public void setConfig(QwasiConfig config){
        this.mconfig = config;
        mclient = mclient.clientWithConfig(config, this);
        mregistered = false;
    }

    public QwasiError setPushEnabled() throws QwasiError{
        if (mpushEnabled){
            return this.registerForNotifications();
        }
        else{
            return this.unregisterForNotifications();
        }
    }

    private QwasiError mregisterDevice(String deviceToken, String name, String userToken, HashMap<String, Object> userInfo, boolean success, boolean failure)
    throws QwasiError{
        if (deviceToken == null){  //if devicetoken is null set it empty so the server will gen one
            deviceToken = mdeviceToken;
        }

        if (name == null){  //if name is null get it from the phone, or user can give us one
            name = Build.PRODUCT;
        }

        if (userToken == null){  //if we didn't get a usertoken set it to be the phone number
            userToken = muserToken;
        }
        Map<String, Object> info = new HashMap<String, Object>();

        if (userInfo != null) {  //make sure that it's init so we don't crash
            if (!userInfo.isEmpty()) { //make sure it's not empty
                info.put("info", userInfo); //put all the recieved info into info
            }
        }

        Map<String, Object> deviceInfo = new HashMap<String, Object>();
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
        JSONRPC2Response response;
        try {
            SharedPreferences.Editor editor = preferences.edit();
            response = mclient.invokeMethod("device.register", info);
            if (response.indicatesSuccess()) {
                mregistered = true; //we've now registered
                editor.putBoolean("registered", true);

                Map<String, Object> result = new HashMap<String, Object>();  //holder object
                result.put("result", response.getResult());  //unpack response object
                result = (Map<String, Object>) result.get("result"); //further unpacking

                //response.hashCode();
                mdeviceToken = result.get("id").toString();  //set our device token from the server
                editor.putString("qwasi_device_token",mdeviceToken);

                //grab the next key and unpack it.
                info = (Map<String, Object>) result.get("application");
                mapplicationName = info.get("name").toString();
                editor.putString("qwasi_app_id", info.get("id").toString());

                //get the settings out
                info = (Map<String, Object>) info.get("settings");
                mpushEnabled = (Boolean) info.get("push_enabled");
                mlocationEnabled = (Boolean) info.get("location_enabled");
                meventsEnabled = (Boolean) info.get("events_enabled");

                editor.apply();
                Log.d("QwasiDebug", "Device Successfully Registered");
                return new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error");
            }
            else {
                Log.e("QwasiError", "Device Failed to Register");
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceRegistrationFailed,
                        "Device Registration failed");
            }
        }
        catch (Throwable e){

            Log.d("Debug", e.getMessage());
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device Not Registered"); //todo handle 404/401?
        }
    }

    public QwasiError setUserToken(String userToken)throws QwasiError{
        muserToken = userToken;
        if (mregistered){
            Map<String, Object> parms = new HashMap<String, Object>();

            parms.put("id", mdeviceToken);
            parms.put("user_token", muserToken);
            try {
                if (mclient.invokeMethod("device.set_user_token", parms).indicatesSuccess()) {
                    return new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error");
                } else {
                    Log.e("QwasiError", "Set UserToken Failed");
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSetUserTokenFailed, "Set UserToken Failed");
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //Todo handle 401/404 error
            }
        }
        Log.e("QwasiError", "Device Not Registered");
        throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered, "Device Not Registred");
    }

    public QwasiError registerDevice(String deviceToken, String name, String userToken, HashMap<String, Object> userInfo, boolean success, boolean failure)
    throws QwasiError{
        return this.mregisterDevice(deviceToken, name, userToken, userInfo, false, false);
    }

    public QwasiError registerDevice(String deviceToken, String name, String userToken, HashMap<String, Object> userInfo, boolean success)
    throws QwasiError{
        return this.registerDevice(deviceToken, name, userToken, userInfo, success, false);
    }

    public QwasiError registerDevice(String deviceToken, String name, String userToken)
    throws QwasiError{
        return this.registerDevice(deviceToken, name, userToken, null, false, false);
    }

    public QwasiError registerDevice(String deviceToken, String userToken)
    throws  QwasiError{
        return this.registerDevice(deviceToken, null, userToken, null, false, false);
    }

    public QwasiError registerDevice(String deviceToken, String userToken, HashMap<String, Object> userInfo )
    throws QwasiError{ //hashmap for nsDictionary
        return this.registerDevice(deviceToken, null, userToken, userInfo, false, false);
    }

    public QwasiError unregisterDevice(String deviceToken)
    throws QwasiError{
        if(mregistered){
            HashMap<String, Object> parm = new HashMap<String, Object>();
            if (deviceToken == null){
                parm.put("id", mdeviceToken);
            }
            else {
                parm.put("id", deviceToken);
            }
            try {
                JSONRPC2Response response = mclient.invokeMethod("device.unregister", parm);
                mdeviceToken = "";
                mregistered = false;
                mpushEnabled = false;
                mlocationEnabled = false;
                Log.d("QwasiDebug", "UnregisterDevice Success");
                return new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error");
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceUnregisterFailed,
                        "Qwasi Device Unregister failed");
            }
        }
        else{
            Log.e("QwasiError", "Device Not Registred");
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered, "Device not registered");
        }
    }

    private QwasiError registerForNotifications() throws  QwasiError{
        if(mregistered){
            while (qwasiNotificationManager.isRegistering()){}
            String pushGCM = qwasiNotificationManager.getPushToken();
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("id", mdeviceToken);
            parms.put("proto", "push.gcm");
            parms.put("token", pushGCM);
            try {
                if (mclient.invokeMethod("device.set_push_token", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "Set Push Token success");
                    return new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error");
                } else {
                    Log.e("QwasiError", "Set Push Token failed");
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorPushRegistrationFailed,
                            "Push Registration failed");
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                throw new QwasiError().errorWithCode(null, e.getMessage()); //todo handle 401/404
            }
        }
        else {
            Log.e("QwasiError", "DeviceNotRegistered");
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device not Registered");
        }
    }

    private QwasiError unregisterForNotifications() throws QwasiError{
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("id", mdeviceToken);
            parms.put("proto", "push.poll");
            parms.put("token", "");
            try {
                if (mclient.invokeMethod("device.set_push_token", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "UnSet Push Token success");
                    mpushEnabled = false;
                    return new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone, "No Error");
                } else {
                    Log.e("QwasiError", "Unregister for Note failed");
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorPushUnregisterFailed,
                            "Push Unregister Failed");
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage()); //todo handle 401/404
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorPushNotEnabled,
                        "Push not Enabled");
            }

        }
        else{
            Log.e("QwasiError", "DeviceNotRegistered");
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                "device not Registered");
        }
    }

    public QwasiMessage fetchMessageForNotification(Bundle userInfo, boolean success, boolean failure)
    throws QwasiError{
        if(mregistered){
            HashMap<String, Object> flags = new HashMap<String, Object>();
            HashMap<String, Object> results = new HashMap<String, Object>();
            flags.put("opened", qwasiAppManager.isApplicationInForeground());
            String qwasi =(String) userInfo.get("qwasi");
            qwasi = qwasi.replaceAll(Pattern.quote("}"), "").replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("\""), "");
            String[] pairs = qwasi.split(Pattern.quote(","));
            for(String pair :pairs){
                String[] key= pair.split(":");
                results.put(key[0], key[1]);
            }
            String msgId = results.get("msg_id").toString();
            String appId = results.get("app_id").toString();
            if (!(msgId.isEmpty()) && !(appId.isEmpty())){
                if (appId.equals(preferences.getString("qwasi_app_id", ""))){
                    if (mmessageCache != null){
                        HashMap<String, Object> parms = new HashMap<String, Object>();
                        parms.put("device", mdeviceToken);
                        parms.put("id", msgId);
                        parms.put("flags", flags);
                        JSONRPC2Response response;
                        try {
                            response = mclient.invokeMethod("message.fetch", parms);
                            if (response.indicatesSuccess()) {
                                QwasiMessage temp = new QwasiMessage().
                                        messageWithData((HashMap<String, Object>)response.getResult());
                                mmessageCache.put(temp.messageId, temp);
                                return temp;
                            } else {
                                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed,
                                        "Message fetch failed");
                            }
                        }
                        catch (FileNotFoundException error){ //404
                            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound,
                                    "No messages found");
                        }
                        catch (Throwable e){ //general
                            throw new QwasiError().errorWithCode(QwasiErrorCode.valueOf(e.getMessage()),
                                    "Unknown see code");
                        }
                    }
                    else{ //todo
                        throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound,
                                "No messages found");
                        //no messages to fetch
                        //QwasiMessage message = nskeyedUnarchiver unarchiver with cachedmessage
                    }
                }
                else{
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorNone,
                            "AppId is incorrect");
                    //wrong appid
                    //if succesful, and there is a message
                }
            }
            else {
                throw  new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorInvaildMessage,
                        "Invalid Message");
            }
        }
        else {
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device Not Registered");
        }
    }

    public QwasiMessage fetchUnreadMessage()throws QwasiError{
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            HashMap<String, Object> options = new HashMap<String, Object>();
            options.put("fetch", String.valueOf(true));
            parms.put("device", mdeviceToken);
            parms.put("options", options);

            try {
                JSONRPC2Response response = mclient.invokeMethod("message.poll", parms);
                Log.d("QwasiDebug", response.toString());
                if (response.indicatesSuccess()) {
                    QwasiMessage message = new QwasiMessage();
                    message.messageWithData((HashMap < String, Object >) response.getResult());
                    return message;
                }
                else{
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorMessageFetchFailed,
                                "Message Fetch Failed");
                    }
                }
            catch (FileNotFoundException e)
            {
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorMessageNotFound,
                        "Message not found ");
            }
            catch (Throwable e) {
                Exception cause = new Exception(e.getMessage(), e);
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorLocationFetchFailed,
                        "Messages fetch failed");
            }
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorDeviceNotRegistered,
                    "Device Not Registered");
        }

    }

    public QwasiErrorCode postEvent(String type, HashMap<String, Object> data, boolean success, boolean failure){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("device", mdeviceToken);
            parms.put("type", type);
            parms.put("data", data);
            try {
                mclient.invokeMethod("event.post", parms);
                return QwasiErrorCode.QwasiErrorNone;
            }
            catch (Throwable e){  //todo handle 401, and 404's app not found device not found
                Exception cause = new Exception(e);
                Log.d("Debug", cause.getMessage());
                return QwasiErrorCode.QwasiErrorPostEventFailed;
            }
        }
        else{
            Log.e("QwasiError", "Device NotRegistered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode postEvent(String type, HashMap<String, Object> data){
        return this.postEvent(type, data, false, false);
    }

    public QwasiErrorCode fetchLocationsNear(QwasiLocation place, boolean success, boolean failure) {
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            HashMap<String, Object> near = new HashMap<String, Object>();
            near.put("lng", place.getLongitude());
            near.put("lat", place.getLatitude());
            near.put("radius", locationSyncFilter*10);
            parms.put("near", near);
            near = new HashMap<String, Object>();
            near.put("schema", "2.0");
            parms.put("options", near);
            try {
                JSONRPC2Response response = mclient.invokeMethod("location.fetch", parms);
                JSONArray positions = new JSONArray(response.getResult().toString());
                JSONObject obj;
                for (int index=0; index < positions.length(); index++) {
                    obj = positions.getJSONObject(index);
                    //if this location doesn't already exist in the mregionMap add it
                    if (!this.mlocationManager.mregionMap.containsKey(obj.getString("id"))) {
                        //for locations in response figure out what type they are i.e. beacons/geofence/
                        if (obj.has("beacon")) {//deal with beacons?  NYI because beacons are primarily apple
                            Log.d("QwasiDebug", "beacons");
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
                                    .setLoiteringDelay(obj.getJSONObject("properties").getInt("dwell_interval")*1000)
                                    //.setNotificationResponsiveness(obj.getJSONObject("properties").getInt("enter_interval")*1000)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                                    .build();
                            mlocationManager.startMoitoringLocation(temp);
                        }
                        else {//rfid?
                            Log.d("QwasiDebug", "rfid");
                        }
                    }
                    else{//key is contained

                    }

                }
                return QwasiErrorCode.QwasiErrorNone;
            }
            catch (Throwable e){
                e.printStackTrace();
                return QwasiErrorCode.QwasiErrorLocationFetchFailed; //fixme 401/404
            }
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode subscribeToChannel(String channel){
        return this.subscribeToChannel(channel, false, false);
    }

    public QwasiErrorCode subscribeToChannel(String channel, boolean success, boolean failure){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("device", mdeviceToken);
            parms.put("channel", channel);
            try {
                if (mclient.invokeMethod("channel.subscribe", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "subscribe to channel success");
                    return QwasiErrorCode.QwasiErrorNone;
                } else {
                    Log.e("QwasiError", "Subscribe to channel Failed");
                    return QwasiErrorCode.QwasiErrorChannelSubscribeFailed;
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //fixme  401/404
            }
        }
        else{
            Log.e("QwasiError", "Device NotRegistered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode unsubscribeFromChannel(String channel){
        return this.unsubscribeFromChannel(channel, false, false);
    }

    public QwasiErrorCode unsubscribeFromChannel(String channel, boolean success, boolean failure){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("device", mdeviceToken);
            parms.put("channel", channel);
            try {
                if (mclient.invokeMethod("channel.unsubscribe", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "Unsubcribe from channel Success");
                    return QwasiErrorCode.QwasiErrorNone;
                } else {
                    Log.e("QwasiError", "Unsubscribe from channel Failed");
                    return QwasiErrorCode.QwasiErrorChannelUnsubscribeFailed;
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //fixme 401/404
            }
        }
        else{
            Log.e("QwasiError", "Device NotRegistered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode setDeviceValue(Object value, String key, boolean success, boolean failure){
        if(mregistered){
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("id", mdeviceToken);
            parms.put("key", key);
            parms.put("value", value);
            try {
                if (mclient.invokeMethod("device.set_data", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "Set data Success");
                    return QwasiErrorCode.QwasiErrorNone;
                } else {
                    Log.e("QwasiError", "Set data Failed");
                    return QwasiErrorCode.QwasiErrorSetDeviceDataFailed;
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //fixme 401/400
            }
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode setDeviceValue(Object value, String key){
        return this.setDeviceValue(value, key, false, false);
    }

    public QwasiErrorCode deviceValueForKey(String key) {
        return this.deviceValueForKey(key, false, false);
    }

    public QwasiErrorCode deviceValueForKey(String key, boolean success, boolean failure){
        if (mregistered){
            Map<String, Object> parms = new HashMap<String, Object>();
            parms.put("id", mdeviceToken);
            parms.put("key", key);
            try {
                JSONRPC2Response response = mclient.invokeMethod("device.get_data", parms);
                if (response.indicatesSuccess()) {
                    parms.put("result", response.getResult());
                    //todo push data to screen?
                    Log.d("Debug", (String) parms.get("result"));
                    Log.d("QwasiDebug", "Get data Success");
                    return QwasiErrorCode.QwasiErrorNone;
                } else {
                    Log.e("QwasiError", "Get data Failed");
                    return QwasiErrorCode.QwasiErrorGetDeviceDataFailed;
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //fixme 400/401
            }
        }
        else{
            Log.e("QwasiError", "Device Not Registered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode sendMessage(QwasiMessage message, String userToken, boolean success, boolean failure)
    throws QwasiError{
        if(mregistered) {
            Object payload = message.mpayload;
            if (payload != null){
                if (payload instanceof JSONObject){
                    //todo fix this issue
                }
                else{
                    HashMap<String, Object> encrypted = new HashMap<String, Object>();
                    encrypted.put("payload", Base64.encode(((String) payload).getBytes(), Base64.DEFAULT));
                    //convert it to a jsonObject?
                }
            //throw an error, get the data if the data is null, or the error isn't print error
            //set payload to the JSONData
            }
            else{
                throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorInvaildMessage,
                        "Invalid Message");
            }//payload == null?
            Map<String, Object> parms = new HashMap<String, Object>();
            HashMap<String, Object> audi = new HashMap<String, Object>();
            audi.put("user_tokens", userToken);
            parms.put("audience", audi); //can be devices, usertokens, channels w/e
            parms.put("payload_type", payload.getClass().toString());
            parms.put("notification", new HashMap<String, Object>().put("text", message.malert));
            parms.put("payload", payload);
            parms.put("tags", message.mtags);
            parms.put("options", (new HashMap<String, Object>().put("encodedPayload",true )));
            try {
                if (mclient.invokeMethod("message.send", parms).indicatesSuccess()) {
                    Log.d("QwasiDebug", "Message Sent Successfully");
                    return QwasiErrorCode.QwasiErrorNone;
                } else {
                    Log.e("QwasiError", "Message Send Failed");
                    throw new QwasiError().errorWithCode(QwasiErrorCode.QwasiErrorSendMessageFailed,
                            "Send message Failed");
                }
            }
            catch (Throwable e){
                Log.d("Debug", e.getMessage());
                return null; //fixme  400/401
            }
        }
        else{
            Log.e("QwasiError", "Device not Registered");
            return QwasiErrorCode.QwasiErrorDeviceNotRegistered;
        }
    }

    public QwasiErrorCode sendMessage(QwasiMessage message, String userToken)
    throws QwasiError{
        return this.sendMessage(message, userToken, false, false);
    }
}
