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

package com.qwasi.sdk;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

public class QwasiMessage{
    @Deprecated
    public String malert;
    public String alert;
    @Deprecated
    public Date mtimestamp;
    public Date timeStamp;
    public String messageId;
    public String application;
    @Deprecated
    public String mpayloadType;
    public String payloadType;
    @Deprecated
    public Object mpayload;
    public Object payload;
    @Deprecated
    public JSONArray mtags;
    public JSONArray tags;
    public Boolean selected;
    public Boolean fetched;
    Object mEncodedPayload;
    Boolean mClosedMessage;
    String TAG = "QwasiMessage";

    public QwasiMessage(){
        super();
        tags = new JSONArray();
        mtags = tags;
        mClosedMessage = false;
    }

    /**
     * Intializes the QwasiMessage object with the data provided
     */
    private QwasiMessage initWithData(Object input){
        try {
            JSONObject data = (JSONObject) input;
            messageId = data.has("id")?data.getString("id"):"";
            application = data.has("application")?data.getString("application"):"";
            alert = data.has("text")?data.getString("text"):"";
            malert = alert;
            selected = QwasiAppManager.getstatus();
            //dateformater = date
            //DateFormat dateFormatter = new DateFormat();
            //mtags = new JSONArray();
            timeStamp = new Date();
            mtimestamp = timeStamp;
            payloadType = data.get("payload_type").toString();
            mpayloadType = payloadType;
            tags = data.has("tags")? data.getJSONArray("tags"): new JSONArray();
            mtags = tags;
            fetched = data.has("fetched")&&data.getBoolean("fetched");
            mEncodedPayload = data.has("payload")?data.get("payload"):"";
            byte[] temp = Base64.decode(mEncodedPayload.toString(), Base64.DEFAULT);
            try {
                if (payloadType.equalsIgnoreCase("application/json")) {
                    //error?
                    payload = new JSONObject(new String(temp, "UTF-8"));

                } else if (payloadType.contains("text")) {
                    payload = new String(temp, "UTF-8");
                }
                mpayload = payload;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.e(TAG, "Payload Encoding not supported");
            }
            return this;
        } catch (JSONException e){
            Log.wtf(TAG, "Malformed JSONobject" + e.getMessage());
            return null;
        }
    }

    /**
     * public initalizing function.
     */
    public QwasiMessage messageWithData(JSONObject data){
        return this.initWithData(data);
    }

    /**
     * public initilizer that has more information mainly for making messages to send with
     */
    public Object initWithAlert(String alert, JSONObject payload, String payloadtype, ArrayList<Object> tags){
        malert = alert;
        this.alert = malert;

        mpayload = payload;
        this.payload = mpayload;

        mpayloadType = payloadtype;
        this.payloadType = mpayloadType;

        this.tags = new JSONArray(tags);
        mtags = this.tags;

        timeStamp = new Date();
        mtimestamp = timeStamp;
        if (payloadType == null) {
            if (this.payload instanceof JSONObject){
                payloadType = "application/json";
            }
            else if(this.payload instanceof String){
                payloadType = "text/plain";
            }
        }
        return this;
    }

    /**
     * checks to see if the message is silent, i.e. has no notification such that it doesn't get
     * displayed to the end User.
     * @return
     */
    public Boolean silent(){
        alert = alert == null? "": alert.contains("do_not_collapse")?"":alert;
        return (alert.isEmpty());
    }

    /**
     * returns the payload as a string rather than a Object
     * @return
     */
    public String description(){
        return payload != null? payload.toString(): "";
    }

    /**
     * allows access to the package level closedMessage, such that custom configurations can know
     * if the message was received on a closed state.
     * @return
     */
    public Boolean getmClosedMessage(){return  mClosedMessage;}
}
