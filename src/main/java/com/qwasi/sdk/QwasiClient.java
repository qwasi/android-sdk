package com.qwasi.sdk;

import java.net.MalformedURLException;
import java.net.URL;

//pulled from dzhuvinov's repos on maven
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import java.util.Map;

import android.util.Log;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiClient {
    private URL server = null;
    private JSONRPC2Session msession = null;
    private JSONRPC2Request mrequest;
    private JSONRPC2Response mresponse;

    public QwasiClient(){
        super();
    }

    public QwasiClient clientWithConfig(QwasiConfig config, Qwasi input) {
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

    void invokeMethod(final String method, final Map<String, Object> parms, final Qwasi.QwasiInterface callbacks){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("QwasiDebug", "invoking API "+method+", with params"+parms.toString());
                mrequest = new JSONRPC2Request(method, parms, "");
                try {
                    mresponse = msession.send(mrequest);
                    if (mresponse.indicatesSuccess())
                        callbacks.onSuccess(mresponse.getResult());
                }
                catch (Exception e){
                    QwasiError temp = new QwasiError();
                    temp.setError(e);
                    callbacks.onFailure(temp);
                }
            }
        }).start();

    }

    public JSONRPC2Response invokeMethod(String method, Map<String, Object> parms) throws Throwable {
        Log.d("QwasiDebug", "invoking API "+method+", with params"+parms.toString());
        mrequest = new JSONRPC2Request(method, parms, "");
        try {
            mresponse = msession.send(mrequest);
        }
        catch (JSONRPC2SessionException e){
            if(e.getCauseType() == JSONRPC2SessionException.NETWORK_EXCEPTION){
                throw e.getCause();
            }
            else if (e.getCauseType() == JSONRPC2SessionException.BAD_RESPONSE){
            }
            throw mresponse.getError();
        }
        return mresponse;
    }
}