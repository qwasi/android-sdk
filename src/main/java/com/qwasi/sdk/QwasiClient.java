/** Created by Charles Coulton on 3/25/16
 // QwasiClient.Java
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

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

abstract public class QwasiClient {
    URL mServer = null;
    private QwasiClient client;
    String TAG = "QwasiClient";
    Boolean isVersion3 = false; //get versioning isn't behaving.
    Reporter Callback = new Reporter() {
        @Override
        public void notifyEvent(Object o) {
            isVersion3 = (Boolean) o;
            Witness.remove(Boolean.class, this);
        }
    };

    public QwasiClient clientWithConfig(final QwasiConfig config, final Qwasi input){
        mServer = config.url;
        checkVersion();
        if (isVersion3) {
            client = new QwasiRestClient().initWithConfig(config, input);
        } else {
            client = new QwasiRPCClient().initWithConfig(config, input);
        }
        return client;
    }

    void checkVersion(){
        Witness.register(Boolean.class, Callback);
        try {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) mServer.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setChunkedStreamingMode(0);
                        connection.setRequestMethod("GET");
                        //OutputStream output = new BufferedOutputStream(connection.getOutputStream());
                        InputStream input = new BufferedInputStream(connection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while((line = reader.readLine()) != null){
                            total.append(line);
                        }
                        connection.disconnect();
                        Log.d(TAG, total.toString());
                        Witness.notify(true);
                        return true;
                    }catch (IOException e) {
                        Witness.notify(false);
                        Log.d(TAG, e.getMessage());
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean result){
                    super.onPostExecute(result);
                    isVersion3 = result;
                }
            }.execute().get();
        }catch (Exception e){
            e.printStackTrace();
        }
        while(isVersion3 == null) {} //stub for waiting the Version is checked
    }

    abstract protected QwasiClient initWithConfig(QwasiConfig config, Qwasi Manager);

    abstract void invokeMethod(final String method,
                               final Map<String, Object> params,
                               final Qwasi.QwasiInterface callback);

    abstract void invokeNotification(final String method,
                                     final Map<String, Object> params,
                                     final Qwasi.QwasiInterface callbacks);
}
