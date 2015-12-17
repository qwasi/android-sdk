package com.qwasi.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 * This handles the application's status on the phone allows for checks on if the application is in the fore or background.
 * this also handles when a status event happens.  Original source from Stackexchange
 // QwasiAppManager.java
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

public class QwasiAppManager implements Application.ActivityLifecycleCallbacks{
    private int resumed;
    private int paused;
    private int started;
    private int stopped;
    static boolean status;
    final private Qwasi sharedApplication;
    private String event;
    private HashMap<String, Object> data;
    String TAG = "QwasiAppManager";

    public QwasiAppManager(Qwasi qwasi){
        super();
        sharedApplication = qwasi;
    }

    private Thread postEvent = new Thread(new Runnable() {
        @Override
        public void run() {
            sharedApplication.postEvent(event, data, null);
            this.notify();
        }
    });

    static synchronized boolean getstatus(){
        return status;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState){
        QwasiLocationManager.getInstance().qwasiBeacons.setMainAct(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity){
        sharedApplication.mlocationManager.stopLocationUpdates();
    }

    @Override
    public void onActivityResumed(Activity activity){
        data = new HashMap<>();
        event = sharedApplication.kEventApplicationState;
        data.put("", "");
        if (postEvent.getState() == Thread.State.TERMINATED)
            postEvent.start();
        ++resumed;
        if (QwasiLocationManager.getInstance().mmanager !=null) {
            if (!sharedApplication.mlocationManager.mmanager.isConnected())
                sharedApplication.mlocationManager.mmanager.connect();
        }
    }

    @Override
    public void onActivityPaused(Activity activity){
        android.util.Log.d(TAG, "ActivityPaused");
        ++paused;
        sharedApplication.mlocationManager.mmanager.disconnect();
        android.util.Log.w(TAG, "application is in foreground: " + isApplicationInForeground());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState){
        android.util.Log.d(TAG, "SaveInstanceState");
    }

    @Override
    public void onActivityStarted(Activity activity){
        android.util.Log.d(TAG, "ActivityStarted");
        ++started;
    }

    @Override
    public void onActivityStopped(Activity activity){
        ++stopped;
        data = new HashMap<>();
        event = sharedApplication.kEventApplicationState;
        data.put("", "");
        sharedApplication.mlocationManager.mmanager.disconnect();
        if (postEvent.getState() == Thread.State.TERMINATED) {
            postEvent.start();
        }
    }

    public boolean isApplicationStopped(){
        return (started > stopped);
    }

    public boolean isApplicationInForeground(){
        status = resumed>paused;
        return status;
    }
}
