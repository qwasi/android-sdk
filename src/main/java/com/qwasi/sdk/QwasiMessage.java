package com.qwasi.sdk;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiMessage.java
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
public class QwasiMessage{
    public String malert;
    public Date mtimestamp;
    public String messageId;
    public String application;
    public String mpayloadType;
    public Object mpayload;
    public JSONArray mtags;
    public Boolean selected;
    public Boolean fetched;
    Object mencodedPayload;
    String TAG = "QwasiMessage";

    public QwasiMessage(){
        super();
        mtags = new JSONArray();
    }

    private QwasiMessage initWithData(Object input){
        try {
            JSONObject data = (JSONObject) input;
            messageId = data.has("id")?data.getString("id"):"";
            application = data.has("application")?data.getString("application"):"";
            malert = data.has("text")?data.getString("text"):"";
            selected = QwasiAppManager.getstatus();
            //dateformater = date
            //DateFormat dateFormatter = new DateFormat();
            //mtags = new JSONArray();
            mtimestamp = new Date();

            mpayloadType = data.get("payload_type").toString();
            mtags = data.has("tags")? data.getJSONArray("tags"): new JSONArray();
            fetched = data.has("fetched")&&data.getBoolean("fetched");
            mencodedPayload = data.has("payload")?data.get("payload"):"";
            byte[] temp = Base64.decode(mencodedPayload.toString(), Base64.DEFAULT);
            try {
                if (mpayloadType.equalsIgnoreCase("application/json")) {
                    //error?
                    mpayload = new JSONObject(new String(temp, "UTF-8"));
                } else if (mpayloadType.contains("text")) {
                    mpayload = new String(temp, "UTF-8");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Log.d(TAG, mpayload.toString());
            return this;
        }
        catch (JSONException e){
            Log.wtf(TAG, "Malformed JSONobject" + e.getMessage());
            return null;
        }
    }

    public QwasiMessage messageWithData(JSONObject data){
        return this.initWithData(data);
    }

    private Object initWithCoder(/*nsCoder*/){
        return this;
    }

    public Object initWithAlert(String alert, JSONObject payload, String payloadtype, ArrayList<Object> tags){
        malert = alert;
        mpayload = payload;
        mpayloadType = payloadtype;
        mtags = new JSONArray(tags);
        mtimestamp = new Date();
        if (mpayloadType == null) {
            if (mpayload instanceof JSONObject){
                mpayloadType = "application/json";
            }
            else if(mpayload instanceof String){
                mpayloadType = "text/plain";
            }
        }
        return this;
    }

    public Boolean silent(){
        malert = malert == null? "":malert;
        return (malert.isEmpty());
    }

    public String description(){
        return mpayload != null? mpayload.toString(): "";
    }
}
