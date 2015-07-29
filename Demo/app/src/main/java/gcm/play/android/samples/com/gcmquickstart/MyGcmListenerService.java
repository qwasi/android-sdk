/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gcm.play.android.samples.com.gcmquickstart;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.qwasi.sdk.Qwasi;
import com.qwasi.sdk.QwasiConfig;
import com.qwasi.sdk.QwasiError;
import com.qwasi.sdk.QwasiMessage;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        final Qwasi temp = new Qwasi(this); //todo figure how to get a global or main activity qwasi
        final QwasiConfig config = new QwasiConfig(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    config.configWithURL(new URL("http://192.168.3.10:8000/v1"), "553548681ad966474841bb60", "ec4a9d95ebe5e283822c99b36a9521b50fca7aeb392dcbc24b92cfeda9ceac60");
                    temp.setConfig(config);
                    temp.qwasiWithConfig(config);
                    String qwasi = (String) data.get("qwasi");
                    String[] results = qwasi.split(Pattern.quote("\""));
                    Log.d(TAG, "From: " + results[11]);
                    Log.d(TAG, "Noteifcation: " + results[7]);
                    sendNotification(temp.fetchMessageForNotification(data, false, false));
                }
                catch (QwasiError e){
                    e.printStackTrace();
                }
                catch (Exception e){
                    Log.d(TAG, "bundle issue");
                }
            }
        }).start();
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        //sendNotification((String)data.get("collapse_key"));
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(QwasiMessage message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(getString(R.string.app_name))//app name?
                .setContentText(message.malert)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
