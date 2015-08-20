package com.qwasi.sdk;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.regex.Pattern;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiNotificationManager extends GcmListenerService{
    private String mpushToken;
    private Boolean mregistering;
    private Context mContext;
    //final private Qwasi qwasi;
    private String senderId;
    static final String TAG = "QwasiNotificationMngr";

    public QwasiNotificationManager(){
        super();
    }
    /**
     * Public constructor to be accessed from Qwasi
     */
    public QwasiNotificationManager(Context app) {
        super();
        synchronized (this) {
            mregistering = false;
            mpushToken = "";
            senderId = "335413682000"; //default
            mContext = app;
        }
    }

    public Boolean isRegistering() {
        return mregistering;
    }

    public String getPushToken() {
        return mpushToken;
    }

    void setPushToken(String pushToken) {
        mpushToken = pushToken;
    }

    void registerForRemoteNotification(final Qwasi.QwasiInterface callbacks) {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext) != ConnectionResult.SUCCESS) {
            // If we can find google play services, have the user download it?
            //GooglePlayServicesUtil.getErrorDialog();
            callbacks.onFailure(new QwasiError()
                    .errorWithCode(QwasiErrorCode.QwasiErrorPushNotEnabled, "Google play not enabled"));
        }
        else {
            try {
                SharedPreferences sharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
                String token;
                token = sharedPreferences.getString("gcm_token", null);
                // We don't have a token so get a new one
                if ((token == null) || token.isEmpty()) {
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
            }
            catch (Exception e) {
                Log.e("QwasiError", "Problem registering" + e.getMessage());
                callbacks.onFailure(new QwasiError()
                        .errorWithCode(QwasiErrorCode.QwasiErrorPushRegistrationFailed, "problem with registering"));
            }
        }
        callbacks.onSuccess(this.getPushToken());
    }

    private void registerForPushInBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String token;
                try {
                    mregistering = true;
                    ApplicationInfo appinfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
                    Log.d(TAG, "attempting token");
                    senderId = appinfo.metaData.containsKey("gcm_senderid")? //if it contains the value
                            appinfo.metaData.getString("gcm_senderid")://set the it to the value
                            senderId; //or the default
                    //Log.d(TAG, senderId);
                    InstanceID iId = InstanceID.getInstance(mContext);
                    token = iId.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    mpushToken = token.isEmpty()?token:null;
                    Log.d(TAG, "New GCM token acquired: " + token);
                }
                catch (PackageManager.NameNotFoundException e){
                    Log.d(TAG, "Name not found");
                }
                catch (IOException e){
                    Log.d(TAG, "IOExecption");
                }
                mregistering = false;
            }
        }).start();
    }

    @Override
    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            String qwasidata = (String) data.get("qwasi");
            String[] results = qwasidata.split(Pattern.quote("\""));
            Log.d(TAG, "From: " + results[11]);
            Log.d(TAG, "Notification: " + results[7]);
            Witness.notify(data);
        }
    }

    /*private void sendNotification(QwasiMessage message) { //todo put me in qwasinotificationhandler

    }*/
}
