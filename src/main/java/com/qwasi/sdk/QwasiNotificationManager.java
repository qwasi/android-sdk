package com.qwasi.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GcmReceiver;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.FileNotFoundException;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiNotificationManager extends Object{
    private String mpushToken;
    private Boolean mregistering;
    private Context mContext;
    final String TAG = "QwasiNotificationMngr";
    public QwasiNotificationManager(Context app){
        super();
        mregistering = false;
        mpushToken = "";
        mContext = app;
    }

    public Boolean isRegistering(){
        return mregistering;
    }

    public String getPushToken(){
        return mpushToken;
    }

    public void setPushToken(String pushToken){
        mpushToken = pushToken;
    }

    public QwasiErrorCode registerForRemoteNotification(){
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext) != ConnectionResult.SUCCESS) {
            // If we can find google play services, have the user download it?
            //GooglePlayServicesUtil.getErrorDialog();
            return QwasiErrorCode.QwasiErrorPushNotEnabled;
        }
        else {
            try {
                SharedPreferences sharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
                String token;
                token = sharedPreferences.getString("gcm_token", null);
                // We don't have a token so get a new one
                if ((token == null) || token.isEmpty()) {
                    mregistering = true;
                    registerForPushInBackground();
                } else {
                    // check the version of the token
                    int appVersion = sharedPreferences.getInt("AppVersion", 0);
                    int registeredVersion = sharedPreferences.getInt("com.google.android.gms.version", Integer.MIN_VALUE);

                    // Our version is outdated, get a new one
                    if (registeredVersion != appVersion) {
                        registerForPushInBackground();
                    }
                }
                mregistering = false;
            }
            catch (Exception e){
                Log.e("QwasiError", "Problem registering" + e.getMessage());
                return QwasiErrorCode.QwasiErrorPushRegistrationFailed;
            }
        }
        return QwasiErrorCode.QwasiErrorNone;
    }

    private void registerForPushInBackground(/*final OnPushRegistrationListener listener*/) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String token;

                try {
                    ApplicationInfo appinfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    Log.d(TAG, "attempting token");
                    //String senderId = appinfo.metaData.get("gcm_senderid").toString();
                    //Log.d(TAG, senderId);
                    int appVersion = appinfo.metaData.getInt("AppVersion"); //probly null
                    InstanceID iId = InstanceID.getInstance(mContext);
                    token = iId.getToken("335413682000", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    if (!token.isEmpty()) {
                        mpushToken = token;
                    }
                    if (appVersion == 0){
                        appVersion = 210;
                    }
                    // save this token
                    prefEditor.putString("gcm_token", token);
                    prefEditor.putInt("AppVersion", appVersion);
                    prefEditor.commit();
                    //todo save to preferancs not to manifest....
                    //listener.onPushRegistrationSuccess(token);
                    //Log.d("Qwasi", "Register4push");
                    Log.d(TAG, "New GCM token acquired: " + token);


                } catch (Exception e) {
                    Log.d(TAG, "Catch");
                    //Log.e(TAG, e.getMessage());
                    //listener.onPushRegistrationError(e);
                }
            }
        }).start();
    }

}
