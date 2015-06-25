package com.qwasi.sdk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

//pulled from dzhuvinov's repos on maven
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;
import java.util.*;
import net.minidev.json.*;

import android.os.AsyncTask;
import android.util.Log;

import org.json.*;

/**
 * Created by ccoulton on 6/11/15.
 */
public class QwasiClient extends AsyncTask{
    private URL server = null;
    private JSONRPC2Session msession = null;
    private JSONRPC2Request mrequest;
    private JSONRPC2Response mresponse;

    public QwasiClient(){
        super();
    }

    public QwasiClient clientWithConfig(QwasiConfig config, Qwasi input) {;
        return this.initWithConfig(config, input);
    }

    private QwasiClient initWithConfig(QwasiConfig config, Qwasi Manager){
        if (config.murl != null){
            try{
                server = new URL(config.murl.toString());
            }
            catch (MalformedURLException e){
                //Already Did this in QwasiConfig....
            }
            msession = new JSONRPC2Session(server);
            msession.setConnectionConfigurator(new QwasiSession(config, Manager));
        }
        return this;
    }

    public JSONRPC2Response invokeMethod(String method, Map<String, Object> parms) throws Throwable {
        Log.d("Debug", "invoking API "+method+", with params"+parms.toString());
        mrequest = new JSONRPC2Request(method, parms, "");
        try {
            mresponse = msession.send(mrequest);
        }
        catch (JSONRPC2SessionException e){
            if(e.getCauseType() == JSONRPC2SessionException.NETWORK_EXCEPTION){
                Log.d("debug", e.toString());
                throw e.getCause();
            }
            else if (e.getCauseType() == JSONRPC2SessionException.BAD_RESPONSE){
            }
            Log.d("WTF", "SessionException");
            throw mresponse.getError();
        }
        return mresponse;
        /*execute(mrequest);  //works but only if we can wait for response
        if (mresponse.indicatesSuccess()){
            return mresponse;
        }
        else {d
            throw mresponse.getError();
        }*/
    }

    @Override
    protected Object doInBackground(Object [] Params) { //works but doesn't wait for return
        try {
            return this.sendRequest();
        }
        catch (Throwable e){
            return e;
        }
    }

    private JSONRPC2Response sendRequest() throws Throwable{
        try {
            mresponse = msession.send(mrequest);
        }
        catch (JSONRPC2SessionException e){
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("result", mresponse.getResult());

        }
        return mresponse;
    }
}