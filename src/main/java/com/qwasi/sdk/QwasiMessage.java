package com.qwasi.sdk;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Base64;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;

/**
 * Created by ccoulton on 6/11/15.
 */
public class QwasiMessage extends Object{
    public String malert;
    public Timer mtimestamp;
    public String messageId;
    public String application;
    public String mpayloadType;
    public Object mpayload;
    public ArrayList<String> mtags;
    public Boolean silent;
    public Boolean selected;
    public Boolean fetched;
    private Object mencodedPayload;

    public QwasiMessage(){
        super();
    }

    private QwasiMessage initWithData(HashMap<String, Object> data){
        messageId = data.get("id").toString();
        application = data.get("application.id").toString();
        malert = data.get("text").toString();
        //todo check appmanager for ap status
        //dateformater = date
        DateFormat dateFormatter = new DateFormat();

        Date timestamp = new Date();

        mpayloadType = data.get("payload_type").getClass().toString();
        mtags.add(data.get("context.tags").toString());
        fetched = Boolean.getBoolean(data.get("flags.fetched").toString());

        mencodedPayload = data.get("payload");

        if (mpayloadType.equalsIgnoreCase("application/json")){
            //error?
            //JSONObject payload = new JSONObject(new HashMap<String, Object>().put());
        }

        else if (mpayloadType.contains("text")){

        }

        return this;
    }

    public QwasiMessage messageWithData(HashMap<String, Object> data){
        return this.initWithData(data);
    }

    private Object initWithCoder(/*nsCoder*/){
        return this;
    }

    public Object initWithAlert(String alert, JSONObject payload, String payloadtype, ArrayList<String> tags){
        malert = alert;
        mpayload = payload;
        mpayloadType = payloadtype;
        mtags = tags;
        mtimestamp = new Timer();
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

    public void encodeWithCoder(){

    }

    public Boolean silent(){
        return (malert == null);
    }

    public String description(){
        return mpayload.getClass().toString();
    }
}
