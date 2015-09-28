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
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
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
    private Object mencodedPayload;
    String TAG = "QwasiMessage";

    public QwasiMessage(){
        super();
        mtags = new JSONArray();
    }

    private QwasiMessage initWithData(Object input){
        try {
            JSONObject data = (JSONObject) input;
            messageId = data.has("id")?data.getString("id"):"";
            application = data.has("application")?
                    data.getJSONObject("application").has("id")?
                            data.getJSONObject("application").getString("id"):""
                    :"";
            malert = data.has("text")?data.getString("text"):"";
            selected = QwasiAppManager.getstatus();
            //dateformater = date
            //DateFormat dateFormatter = new DateFormat();
            //mtags = new JSONArray();
            mtimestamp = new Date();

            mpayloadType = data.get("payload_type").toString();
            mtags = data.has("context")?
                    data.getJSONObject("context").has("tags")?
                            data.getJSONObject("context").getJSONArray("tags"): new JSONArray()
                    :new JSONArray();
            fetched = data.has("fetched")? data.getBoolean("fetched"):false;
            mencodedPayload = data.get("payload");
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
        return mpayload.toString();
    }
}
