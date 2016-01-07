/**
 * Created by ccoulton on 8/20/15.
 * as part Qwasi Technogoly for their Android Open Source Project
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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.HashMap;

import io.hearty.witness.Witness;

public class QwasiGCMListener extends GcmListenerService{
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            Intent intent = (getPackageManager().getLaunchIntentForPackage(getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                    .setContentIntent(pendingIntent);
            //QwasiNotificationManager.getInstance().onMessage(noteBuilder, data);
            sendNotification(pendingIntent, data);
            //Witness.notify(data);
            this.sendBroadcast(new Intent("com.qwasi.sdk.QwasiService.RECEIVE").putExtra("qwasi", data.getString("qwasi")).putExtra("from", from));
        }
    }

    public void onMessagePolled(){
        synchronized (this) {
            Context baseContext = Qwasi.getContext();
            PackageManager manager = baseContext.getPackageManager();
            Intent intent = manager.getLaunchIntentForPackage(baseContext.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(baseContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(baseContext)
                    .setContentIntent(pendingIntent);
            QwasiNotificationManager.getInstance().onMessage(noteBuilder);
        }
    }

    private void onQwasiMessage(QwasiMessage msg) {

    }

    private void sendNotification(PendingIntent pendingIntent, final Bundle data){
        String alert = data.getString("collapse_key","");
        if (!alert.contains("do_not_collapse")){
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            String appName = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();
            NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(getApplicationInfo().icon)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(appName)
                    .setContentText(alert)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSound(defaultSoundUri);
            NotificationManager noteMng = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            noteMng.notify(1, noteBuilder.build());
        }
    }
}
