package com.qwasi.sdk;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiNotificationManager{
    private String mpushToken = "";
    private Boolean mregistering;
    private Context mContext;
    PendingIntent mIntent;
    //final private Qwasi qwasi;
    private String senderId;
    private static QwasiNotificationManager instance;
    static final String TAG = "QwasiNotificationMngr";

    private QwasiNotificationManager(){
        super();
        mregistering = false;
        mpushToken = "";
        senderId = "335413682000"; //default
        mContext = Qwasi.getContext();
    }

    public static QwasiNotificationManager getInstance(){
        return instance != null?instance:new QwasiNotificationManager();
    }

    /**
     * Public constructor to be accessed from Qwasi
     */

    public Boolean isRegistering() {
        return mregistering;
    }

    public String getPushToken() {
        return mpushToken;
    }

    void setPushToken(String pushToken) {
        mpushToken = pushToken;
    }

    synchronized void registerForRemoteNotification(final Qwasi.QwasiInterface callbacks) {
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
                if ((token == null || token.isEmpty())&& !mregistering) {
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

    private synchronized void registerForPushInBackground() {
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
                    mpushToken =!token.isEmpty()?token:"";
                    Log.d(TAG, "New GCM token acquired: " + token);
                    Witness.notify(mregistering);
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

    void onMessage(PendingIntent intent, Bundle data){
        this.mIntent = intent;
        Witness.notify(data);
    }
}
