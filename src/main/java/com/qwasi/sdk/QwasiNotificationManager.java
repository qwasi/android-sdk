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
    final private Qwasi qwasi;
    private String senderId;
    final String TAG = "QwasiNotificationMngr";

    public QwasiNotificationManager() {
        super();
        qwasi = Qwasi.getInstance();
        mContext = qwasi.getContext();
    }

    public QwasiNotificationManager(Context app) {
        synchronized (this) {
            mregistering = false;
            mpushToken = "";
            mContext = app;
            qwasi = Qwasi.getInstance();
            senderId = "335413682000"; //default
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

    public QwasiErrorCode registerForRemoteNotification() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext) != ConnectionResult.SUCCESS) {
            // If we can find google play services, have the user download it?
            //GooglePlayServicesUtil.getErrorDialog();
            return QwasiErrorCode.QwasiErrorPushNotEnabled;
        } else {
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
            } catch (Exception e) {
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
                    if (appinfo.metaData.containsKey("gcm_senderid")) {
                        senderId = appinfo.metaData.get("gcm_senderid").toString();

                    }
                    Log.d(TAG, senderId);
                    int appVersion = appinfo.metaData.getInt("AppVersion"); //most likely null
                    InstanceID iId = InstanceID.getInstance(mContext);
                    token = iId.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    if (!token.isEmpty()) {
                        mpushToken = token;
                    }
                    if (appVersion == 0) {
                        appVersion = 210;
                    }
                    // save this token
                    prefEditor.putString("gcm_token", token);
                    prefEditor.putInt("AppVersion", appVersion);
                    prefEditor.commit();
                    Log.d(TAG, "New GCM token acquired: " + token);
                } catch (Exception e) {
                    Log.d(TAG, "Catch");
                }
            }
        }).start();
    }

    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String qwasidata = (String) data.get("qwasi");
                    String[] results = qwasidata.split(Pattern.quote("\""));
                    Log.d(TAG, "From: " + results[11]);
                    Log.d(TAG, "Notification: " + results[7]);
                    qwasi.fetchMessageForNotification(data, new Qwasi.QwasiInterface() {
                        @Override
                        public void onSuccess(Object o) {
                            sendNotification((QwasiMessage) o);
                        }

                        @Override
                        public void onFailure(QwasiError e) {
                            e.printStackTrace();
                        }
                    });
                }
            }).start();
        }
    }

    private void sendNotification(QwasiMessage message) {
        Intent intent = new Intent(this, mContext.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSoundUri;
        if (!message.silent()) {
            defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        else defaultSoundUri = null;
        String appName = mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo()).toString();
        NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(mContext.getApplicationInfo().icon)
                .setContentTitle(appName)
                .setContentText(message.malert)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL) //default sound and vibrate
                .setSound(defaultSoundUri) //default sound
                .setContentIntent(pendingIntent);

        //configure expanded action
        if (message.mpayloadType.contains("text")){ //text
            noteBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message.description()));
            //allows stuff when expanded.  BigTextStyle, BigPictureStyle, and InboxStyle
        }
        else if(message.mpayloadType.contains("image")){ //image
            Log.d(TAG, "Image");
            //noteBuilder.setStyle(new NotificationCompat.BigPictureStyle().b);
        }
        else if(message.mpayloadType.contains("json")){//application
            Log.d(TAG, "App context");
        }

        NotificationManager noteMng = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        noteMng.notify(message.messageId.hashCode(), noteBuilder.build());
        //Witness.notify(message);
    }
}
