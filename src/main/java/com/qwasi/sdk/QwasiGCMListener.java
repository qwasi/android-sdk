package com.qwasi.sdk;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by ccoulton on 8/20/15.
 */
public class QwasiGCMListener extends GcmListenerService{
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        synchronized (this) {
            Intent intent = new Intent(this, Qwasi.getContext().getClass()).addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            QwasiNotificationManager.getInstance().onMessage(intent, data);

        }
    }
}
