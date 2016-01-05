/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiClient.java
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

import android.util.Log;

import com.qwasi.QwasiJson.jsonrpc.JSONRPCException;
import com.qwasi.QwasiJson.jsonrpc.JSONRPCHttpClient;

import org.json.JSONObject;

import java.util.Map;
import java.net.URL;

public class QwasiClient {
    URL mServer = null;
    QwasiSession mQwasiSession;
    private JSONRPCHttpClient mSession = null;
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
            this.mQwasiSession = new QwasiSession(config, Manager);
            mServer = config.murl;
            //connection = (HttpURLConnection) server.openConnection();
            mSession = new JSONRPCHttpClient(mServer.toString(),mQwasiSession.mHeaders);
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
                    JSONObject response = mSession.callJSONObject(method, jsonparms);
                    Log.d(TAG, method + " successful");
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

    void invokeNotification(final String method, final Map<String, Object> parms, final Qwasi.QwasiInterface callbacks){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "invoking API "+method+", with parms: "+parms.toString());
                JSONObject jsonparms = new JSONObject(parms);
                try{
                    mSession.call(method, jsonparms);
                    Log.d(TAG, method + " successful");
                    callbacks.onSuccess(null);
                }
                catch (JSONRPCException e){
                    QwasiError temp = new QwasiError();
                    temp.errorWithCode(QwasiErrorCode.QwasiErrorNone, e.getCause().getMessage());
                    callbacks.onFailure(temp);
                }
            }
        }).start();
    }
}