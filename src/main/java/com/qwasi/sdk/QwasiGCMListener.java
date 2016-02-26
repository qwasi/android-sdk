/**
 * Created by ccoulton on 8/20/15.
 // QwasiGCMListener.java
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
 **/

package com.qwasi.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

abstract public class QwasiGCMListener extends GcmListenerService{
    private Context mBaseContext;
    private PackageManager mPM;
    private PendingIntent mDefaultPendingIntent;
    private Uri mDefaultSoundUri;
    private NotificationManager mNoteMng;

    public QwasiGCMListener() {
        this.onCreate();
    }

    @Override
    public void onCreate(){ //Droid-48
        try {
            mBaseContext = this.getBaseContext();
            mBaseContext = mBaseContext != null ? mBaseContext : Qwasi.getContext();
            mPM = mBaseContext.getPackageManager();
            Intent mDefaultIntent;
            mDefaultIntent = mPM.getLaunchIntentForPackage(mBaseContext.getPackageName());
            mDefaultIntent
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            mDefaultPendingIntent = PendingIntent.getActivity(mBaseContext, 0, mDefaultIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            mDefaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } catch (NullPointerException e){
            Log.e("QwasiGCMListener", "null pointer thrown, most likely basecontext");
        }
    }

    /**
     * creates a broadcast for the QwasiService to handle
     */
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            this.sendBroadcast(new Intent("com.qwasi.sdk.QwasiService.RECEIVE").putExtra("qwasi", data.getString("qwasi")).putExtra("from", from).putExtra("data",data));
        }
    }

    /**
     * Override this function to dictate logic when useLocalNotifications is set false
     * @param msg
     */
    abstract protected void onQwasiMessage(QwasiMessage msg);

    /**
     * Optional method for client override on the fetch message failure.
     */
    protected void onQwasiBundle(Bundle data){}

    /**
     * Notification builder when the fetch failed but we still have the bundle
     * @param data
     */
    /*package*/void sendNotification(final Bundle data){
        String alert = data.getString("collapse_key", "");
        if (!alert.contains("do_not_collapse")){
            NotificationCompat.Builder builder = noteBuilder(alert);
            mNoteMng = (NotificationManager) mBaseContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNoteMng.notify(1, builder.build());
        }
    }

    /**
     * Used as a default when the message is fetched.  Can be used to keep the default
     * Notification by client overloaded projects.
     * @param message
     */
    protected void sendNotification(final QwasiMessage message){
        String alert = message.alert;
        if (!message.silent()){
            NotificationCompat.Builder builder = noteBuilder(alert);
            Bitmap bitmap = BitmapFactory.decodeResource(mBaseContext.getResources(),
                    mBaseContext.getApplicationInfo().logo);
            builder.setLargeIcon(bitmap);
            mNoteMng = (NotificationManager) mBaseContext.getSystemService(Context.NOTIFICATION_SERVICE);
            //allows stuff when expanded.  BigTextStyle, BigPictureStyle, and InboxStyle
            if (message.payloadType.contains("text")) {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message.description()));
            }
            else if (message.payloadType.contains("image")){
                Log.d("QwasiGCMListener", "Image");
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
            }
            else if (message.payloadType.contains("json")){
                Log.d("QwasiGCMListener", "App context");
                builder.setStyle(new NotificationCompat.InboxStyle()
                        .addLine("")
                        .setBigContentTitle(message.description())
                        .setSummaryText("Testingthings"));
            }
            mNoteMng.notify(1, builder.build());
        }
        stopSelf();
    }

    private NotificationCompat.Builder noteBuilder(String alert){
        String appName = mPM.getApplicationLabel(mBaseContext.getApplicationInfo()).toString();
        return new NotificationCompat.Builder(mBaseContext)
                .setSmallIcon(mBaseContext.getApplicationInfo().icon)
                .setContentIntent(mDefaultPendingIntent)
                .setContentTitle(appName)
                .setContentText(alert)
                .setAutoCancel(true)
                .setSound(mDefaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL);
    }
}
