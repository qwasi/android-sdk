package com.qwasi.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.HashMap;
import java.util.List;

import io.hearty.witness.Witness;

/**
 * Created by ccoulton on 8/20/15.
 */
public class QwasiGeofencehandler extends IntentService {
    String TAG = "QwasiGeofence";

    public QwasiGeofencehandler(){
        super("QwasiGeofence");
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onHandleIntent(Intent input) {
        synchronized (this) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(input);
            Log.i(TAG, "Geofence Intent");
            if (geofencingEvent.hasError()) {
                Log.e("QwasiGeofence", String.valueOf(geofencingEvent.getErrorCode()));
                return;
            }
            HashMap<String, Object> data = new HashMap<>();
            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            //Test that the reported transition was of interest.
            for (Geofence geofence : triggeringGeofences) {
                QwasiLocation temp = QwasiLocationManager.getInstance().
                        mregionMap.get(geofence.getRequestId());
                if (temp != null) {
                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        // Send notification and log the transition details.
                        temp.state = QwasiLocation.QwasiLocationState.QwasiLocationStateInside;
                        Witness.notify(temp);
                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        temp.exit();
                        Witness.notify(temp);
                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        temp.state = QwasiLocation.QwasiLocationState.QwasiLocationStatePending;
                        temp.enter();
                    }
                }
            }
        }
    }
}
