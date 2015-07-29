package com.qwasi.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 * This handles the application's status on the phone allows for checks on if the application is in the fore or background.
 * this also handles when a status event happens.
 */

public class QwasiAppManager implements Application.ActivityLifecycleCallbacks{
    private int resumed;
    private int paused;
    private int started;
    private int stopped;
    final private Qwasi sharedApplication;
    private String event;
    private HashMap<String, Object> data;


    public QwasiAppManager(){
        this.sharedApplication = Qwasi.getInstance();
    }

    private Thread postEvent = new Thread(new Runnable() {
        @Override
        public void run() {
            sharedApplication.postEvent(event, data);
            this.notify();
        }
    });

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState){
    }

    @Override
    public void onActivityDestroyed(Activity activity){}

    @Override
    public void onActivityResumed(Activity activity){
        data = new HashMap<>();
        event = "com.qwasi.event.application.state";
        data.put("", "");
        if (postEvent.getState() == Thread.State.TERMINATED)
            postEvent.start();
        ++resumed;
    }

    @Override
    public void onActivityPaused(Activity activity){
        Log.d("QwasiDebug", "ActivityPaused");
        ++paused;
        android.util.Log.w("test", "application is in foreground: " + (resumed > paused));
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState){
        Log.d("QwasiDebug", "SaveInstanceState");
    }

    @Override
    public void onActivityStarted(Activity activity){
        Log.d("QwasiDebug", "ActivityStarted");
        ++started;
    }

    @Override
    public void onActivityStopped(Activity activity){
        ++stopped;
        data = new HashMap<>();
        event = "com.qwasi.event.application.state";
        data.put("", "");
        if (postEvent.getState() == Thread.State.TERMINATED) {
            postEvent.start();
        }
    }

    public boolean isApplicationStopped(){
        return (started > stopped);
    }

    public boolean isApplicationInForeground(){
        return resumed>paused;
    }
}
