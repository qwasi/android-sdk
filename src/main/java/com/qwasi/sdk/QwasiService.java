/**
 * Created by ccoulton on 1/6/16.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiService.java
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
package com.qwasi.sdk;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Pattern;

import io.hearty.witness.Witness;

public class QwasiService extends Service {
    static private Qwasi mQwasi;
    static Class<?> mCustomListener;
    static Method mOnQwasiMessage, mOnQwasiBundle;
    static private String mListenerName;
    static String DEFAULT_GCM = "com.qwasi.sdk.QwasiGCMDefault";
    static String TAG = "QwasiService";

    /**
     * Generic broadcastReceiver for this service used to handle messages delivered during
     * operation of application, both opened and closed.
     */
    protected final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final Bundle data = intent.hasExtra("qwasi")? intent.getBundleExtra("qwasi"):new Bundle();
            String from = intent.hasExtra("from")? intent.getStringExtra("from"):""; //senderID
            if (data.isEmpty()) return;
            try {
                JSONObject results = new JSONObject(data.getString("qwasi"));
                if (mQwasi.config.isValid()) {
                    String msgId = results.getString("msg_id");
                    String appId = results.getString("app_id");
                    JSONObject msgContext = results.has("context")?
                            results.getJSONObject("context"):
                            new JSONObject();
                    if (!(msgId.isEmpty()) && !(appId.isEmpty()) && (mQwasi.pSenderId.equals(from))) {
                        if (appId.equals(mQwasi.config.application) || appId.equals(mQwasi.mconfig.mapplication)) {
                            if ((mQwasi.mMessageCache.isEmpty()) || (!mQwasi.mMessageCache.containsKey(msgId))) {
                                mQwasi.postDlr("delivered", msgId, msgContext);
                                if (mQwasi.isInForeground()) mQwasi.postDlr("opened", msgId, msgContext);
                                mQwasi.fetchMessageForNotification(msgId, new Qwasi.QwasiInterface() {
                                    @Override
                                    public void onSuccess(Object o) {
                                        QwasiMessage message = (QwasiMessage) o;
                                        if (QwasiAppManager.isApplicationStopped()) {
                                            mQwasi.mHasClosedUnread = true;
                                            message.mClosedMessage = true;
                                        }
                                        mQwasi.useLocalNotifications = mQwasi.museLocalNotifications;
                                        Witness.notify(message);
                                        SendNotification(message);
                                    }

                                    //message not fetched but still want to build a notification w/bundle
                                    @Override
                                    public void onFailure(QwasiError e) {
                                        Log.e(TAG, "Fetch Message failed");
                                        if (mQwasi.useLocalNotifications) new QwasiGCMDefault()
                                                .sendNotification(data);
                                    }
                                });
                            } else {
                                QwasiMessage message = mQwasi.mMessageCache.get(msgId);
                                mQwasi.useLocalNotifications = mQwasi.museLocalNotifications;
                                Witness.notify(message); //when app is open
                                SendNotification(message);
                                if (mQwasi.useLocalNotifications)
                                    new QwasiGCMDefault().sendNotification(message);
                            }
                        }
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    };

    /**
     * Sends a QwasiMessage to the custom notification builder
     * @param message
     */
    public static void SendNotification(QwasiMessage message){
        try {
            if (mQwasi.useLocalNotifications){
                new QwasiGCMDefault().sendNotification(message);
            }
            else mOnQwasiMessage.invoke(mCustomListener.newInstance(), message);
        }catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access Exception, Constructor, or onQwasiMessage protection level" +
                    " too high");
        }catch (InstantiationException e){
            Log.e(TAG, "Trouble instantiation of: "+mListenerName);
        }catch (InvocationTargetException e){
            Log.e(TAG, "Trouble calling onQwasiMessage of: " + mListenerName);
        }
    }

    /**
     * Bound service stub Required to subclass a service if it's a bound service or not.
     * @param intent the intent to bind from another object
     */
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public int onStartCommand(Intent input, int flag, int stuff){
        mQwasi = Qwasi.getInstance(getApplication());
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.qwasi.sdk.QwasiService.RECEIVE");
        registerReceiver(receiver, filter);
        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            mListenerName = metaData != null? //if not null
                    metaData.containsKey("GCMListener")? //and contains the key we want
                            metaData.getString("GCMListener") //get the key
                            :DEFAULT_GCM
                    :DEFAULT_GCM; //or set to default
            mCustomListener = Class.forName(mListenerName);
            mOnQwasiMessage = mCustomListener.getMethod("onQwasiMessage", QwasiMessage.class);
            //mOnQwasiBundle  = mCustomListener.getMethod("onQwasiBundle", Bundle.class);
        }catch (PackageManager.NameNotFoundException e){
            Log.e(TAG, "Packagename " + getPackageName() + " not found");
        }catch (ClassNotFoundException e) {
            Log.e(TAG, "Custom GCMListener with Classname: " + mListenerName + " Not found");
        }
        catch (NoSuchMethodException e){
            Log.e(TAG, "Custom GCMListener has no method onQwasiMessage, make sure to extend " +
                    "QwasiGCMListener");
        }
        catch (NullPointerException e){
            Log.e(TAG, "Some Object in GCMListener set up was null");
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(receiver);
        if (QwasiAppManager.isApplicationStopped()){
            Log.e(TAG, "closed destroyed");
        }
    }
}
