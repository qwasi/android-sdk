package com.qwasi.sdk;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by ccoulton on 8/20/15.
 * as part Qwasi Technogoly for their Android Open Source Project
 * Released under the MIT License.  Hosted on OSSR
 */
public class QwasiGCMListener extends GcmListenerService{
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            Intent intent = (getPackageManager().getLaunchIntentForPackage(getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                    .setContentIntent(pendingIntent);
            QwasiNotificationManager.getInstance().onMessage(noteBuilder, data);
        }
    }
}
