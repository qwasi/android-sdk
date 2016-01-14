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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Pattern;

import io.hearty.witness.Witness;

public class QwasiService extends Service {
    private Qwasi mQwasi;
    static Class<?> mCustomListener;
    static Method mOnQwasiMessage;
    static private String mListenerName;
    static private String DEFAULT_GCM = "com.qwasi.sdk.QwasiGCMListener";
    static String TAG = "QwasiService";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            String qwasi = intent.getStringExtra("qwasi");
            if (mQwasi.config.isValid()){
                //todo move this back to Qwasi?
                HashMap<String, Object> results = new HashMap<>();
                qwasi = qwasi.replaceAll(Pattern.quote("}"), "")
                        .replaceAll(Pattern.quote("{"), "")
                        .replaceAll(Pattern.quote("\""), "");
                String[] pairs = qwasi.split(Pattern.quote(","));
                for (String pair : pairs) {
                    String[] key = pair.split(Pattern.quote(":"), 2);
                    results.put(key[0], key[1]);
                }
                final String msgId = results.get("msg_id").toString();
                String appId = results.get("app_id").toString();
                if (!(msgId.isEmpty()) && !(appId.isEmpty())) {
                    if (appId.equals(mQwasi.config.application)||appId.equals(mQwasi.mconfig.mapplication)) {
                        if ((mQwasi.mMessageCache.isEmpty()) || (!mQwasi.mMessageCache.containsKey(msgId))) {
                            mQwasi.fetchMessageForNotification(msgId, new Qwasi.QwasiInterface() {
                                @Override
                                public void onSuccess(Object o) {
                                    QwasiMessage message = (QwasiMessage) o;
                                    if (mQwasi.mQwasiAppManager.isApplicationStopped()) {
                                        mQwasi.mHasClosedUnread = true;
                                        message.mClosedMessage = true;
                                    }
                                    mQwasi.useLocalNotifications = mQwasi.museLocalNotifications;
                                    Witness.notify(message);
                                    if (mQwasi.useLocalNotifications) new QwasiGCMListener().sendNotification(intent.getBundleExtra("data"));
                                    else SendtoCustom(message);
                                }

                                @Override
                                public void onFailure(QwasiError e) {

                                }
                            });
                        } else {
                            QwasiMessage message = mQwasi.mMessageCache.get(msgId);
                            mQwasi.useLocalNotifications = mQwasi.museLocalNotifications;
                            Witness.notify(message); //when app is open
                            if (mQwasi.useLocalNotifications) new QwasiGCMListener().sendNotification(intent.getBundleExtra("data"));
                            else SendtoCustom(message);
                        }
                    }
                }
            }
        }
    };

    public static void SendtoCustom(QwasiMessage message){
        try {
            mOnQwasiMessage.invoke(mCustomListener.newInstance(), message);
        }catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access Exception, constuctor not public");
        }catch (InstantiationException e){
            Log.e(TAG, "Trouble instantiation of: "+mListenerName);
        }catch (InvocationTargetException e){
            Log.e(TAG, "Trouble calling onQwasiMessage of: " + mListenerName);
        }
    }

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
        }catch (PackageManager.NameNotFoundException e){
            Log.e(TAG, "Packagename "+getPackageName()+" not found");
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            Log.e(TAG, "Custom GCMListener with Classname: "+mListenerName+" Not found");
            e.printStackTrace();
        }catch (NoSuchMethodException e){
            Log.e(TAG, "Custom GCMListener has no method onQwasiMessage");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if (mQwasi.mQwasiAppManager.isApplicationStopped()){
            Log.e(TAG, "closed destroyed");
        }
    }
}
