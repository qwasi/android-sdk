package com.qwasi.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

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

    public QwasiNotificationManager(Context app){
        super();
        mregistering = false;
        mpushToken = "";
        mContext = app;
    }

    public Boolean isRegistering(){
        return mregistering;
    }

    public  String getPushToken(){
        return mpushToken;
    }

    private void load(){
        return;
    }

    public QwasiErrorCode registerForRemoteNotification(){
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext) != ConnectionResult.SUCCESS) {
            // If we can find google play services, fail.
            return QwasiErrorCode.QwasiErrorPushNotEnabled;
        }
        else {
            try {
                ApplicationInfo appinfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);

                String token = (String) appinfo.metaData.get("gcm_token");

                // We don't have a token so get a new one
                if ((token == null)||token.isEmpty()) {
                    mregistering = true;
                    registerForPushInBackground();
                } else {
                    // check the version of the token
                /*int appVersion = Helpers.getVersionFromPackageInfo(mContext);
                int registeredVersion = cm.getInt(ConfigurationManager.GCM_TOKEN_VER, Integer.MIN_VALUE);

                // Our version is outdated, get a new one
                if (registeredVersion != appVersion) {
                    registerForPushInBackground(listener);
                } else {

                }*/
                }
                mregistering = false;
            }
            catch (PackageManager.NameNotFoundException e){
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
                    String senderId = appinfo.metaData.get("gcm_senderid").toString();
                    int appVersion = appinfo.metaData.getInt("AppVersion"); //probly null
                    //InstanceID iId = InstanceID.getInstance(mContext);
                    //token = iId.getToken(getString(senderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    token = GoogleCloudMessaging.getInstance(mContext).register(senderId);
                    if (!token.isEmpty()) {
                        mpushToken = token;
                    }
                    // save this token
                    appinfo.metaData.putString("gcm_token", token);
                    appinfo.metaData.putInt("AppVersion", appVersion);
                    //todo save to preferancs not to manifest....

                    //listener.onPushRegistrationSuccess(token);

                    Log.d("QwasiDebug", "New GCM token acquired: " + token);


                } catch (Exception e) {
                    Log.d("QwasiDebug", e.getMessage());
                    //listener.onPushRegistrationError(e);
                }
            }
        }).start();
    }

}
