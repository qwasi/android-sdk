package com.qwasi.sdk;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.qwasi.QwasiJson.jsonrpc.JSONRPCException;
import com.qwasi.QwasiJson.jsonrpc.JSONRPCHttpClient;

import java.util.Map;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiClient {
    private URL server = null;
    private QwasiSession session;
    private HttpURLConnection connection;
    private JSONRPCHttpClient msession = null;
    //NetworkTask task;
    String TAG = "QwasiClient";
    public QwasiClient(){
        super();
        //task = new NetworkTask();
    }

    public QwasiClient clientWithConfig(QwasiConfig config, Qwasi input) {
        return this.initWithConfig(config, input);
    }

    private QwasiClient initWithConfig(QwasiConfig config, Qwasi Manager){
        if (config.murl != null){
            try{
                this.session = new QwasiSession(config, Manager);
                server = new URL(config.murl.toString());
                //connection = (HttpURLConnection) server.openConnection();
                msession = new JSONRPCHttpClient(server.toString(),session.headers);
            }
            catch (MalformedURLException e){
                //Already Did this in QwasiConfig....
            }
        }
        return this;
    }

    void invokeMethod(final String method, final Map<String, Object> parms, final Qwasi.QwasiInterface callbacks){
        new Thread (new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "invoking API "+method+", with params: "+parms.toString());
                JSONObject jsonparms = new JSONObject(parms);
                try {
                    JSONObject response = msession.callJSONObject(method, jsonparms);
                    Log.d(TAG, method + " succesful");
                    callbacks.onSuccess(response);
                }
                catch (JSONRPCException e) {
                    QwasiError temp = new QwasiError();
                    temp.errorWithCode(QwasiErrorCode.QwasiErrorNone, e.getCause().getMessage());
                    callbacks.onFailure(temp);
                    //handle when I know what the code is i'll deal with this
                }
            }
        }).start();
    }
}