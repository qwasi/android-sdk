/**
 * Created by ccoulton on 8/20/15.
 * for Qwasi Technology's Android Open source project
 * QwasiGeofencehandler.java
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

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.HashMap;
import java.util.List;

import io.hearty.witness.Witness;

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
