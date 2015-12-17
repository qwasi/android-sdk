package com.qwasi.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiNotificatonManager.java
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
public class QwasiNotificationManager{
    private String mpushToken = "";
    private Boolean mregistering;
    private Context mContext;
    //PendingIntent mIntent;
    NotificationCompat.Builder noteBuilder;
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
        instance = this;
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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                String token;
                token = sharedPreferences.getString("gcm_token", "");
                // We don't have a token so get a new one
                if (token.isEmpty()&& !mregistering) {
                    mregistering = !mregistering;
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
                    Log.d(TAG, "Attempting to Aquire new Token");
                    //Device Registering issue 11-4-15
                    senderId = appinfo.metaData.containsKey("gcm_senderid")? //has senderid in manifest
                            appinfo.metaData.getString("gcm_senderid", "335413682000"): //get it
                            senderId;  //or set to default, default also included in case android munges it
                    Log.d(TAG, "Using SenderID: "+senderId);
                    InstanceID iId = InstanceID.getInstance(mContext);
                    token = iId.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
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

    void onMessage(NotificationCompat.Builder builder, Bundle data){
        this.noteBuilder = builder;
        Witness.notify(data);
    }

    void onMessage(NotificationCompat.Builder builder){
        this.noteBuilder = builder;
    }
}
