package com.qwasi.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 * This handles the application's status on the phone allows for checks on if the application is in the fore or background.
 * this also handles when a status event happens.  Original source from Stackexetange
 */

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
