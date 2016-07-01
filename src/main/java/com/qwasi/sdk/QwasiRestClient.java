/**
 // QwasiRestClient.Java
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class QwasiRestClient extends QwasiClient{
  final String TAG = "QwasiRestClient";
  HttpURLConnection session;
  final String UTF_8 = "UTF-8";

  protected QwasiRestClient initWithConfig(QwasiConfig config, Qwasi manager){
    try {
      session = (HttpURLConnection) mServer.openConnection();
      session.setDoOutput(true);
      session.setDoInput(true);
    }catch (IOException e ){
      e.printStackTrace();
      Log.e(TAG, "Server URL incorrect");
    }
    return this;
  }

  private String parseMethod(String method){
    String[] methods = method.split("\\.");
    switch(methods[0]){
      case "device":
        switch (methods[1]){
          case "register":
            return  "POST";
          case "set_user_token":
            //fallthough
          case "set_push_token":
            //fallthough
          case "set_data":
            return  "PUT";
          case "unregister":
            return  "DELETE";
          case "get_data":
            return "GET";
        }//device setup
      case "message":
        switch (methods[1]){
          case "fetch":
            //fallthough
          case "poll":
            return "GET";
          case "send":
            return "POST";
        }
      case "event":
        return "PUT";//new event or update?
        //return "POST";
      case "location":
        return "GET";
      case "channel":
        return "PUT";
      case "member":
        switch (methods[1]) {
          case "set":
            //fallthough
          case "set_auth":
            return "PUT";
          case "get":
            return "GET";
          case "auth":
            return "POST";
        }
      default: //will never happen
        return null;
    }
  }

  @Override
  void invokeMethod(String method, Map<String, Object> params, Qwasi.QwasiInterface callback) {
    callRestMethod(method, params, callback);
  }

  @Override
  void invokeNotification(String method, Map<String, Object> params, Qwasi.QwasiInterface callbacks) {
    callRestMethod(method, params, callbacks);
  }

  private String mapToURIQueryString(Map<String, Object>params){
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Object> entry: params.entrySet()){
      if (builder.length() > 0) {
        builder.append('&');
      }
      try {
        builder.append(URLEncoder.encode(entry.getKey(), UTF_8))
            .append('=')
            .append(URLEncoder.encode(entry.getValue().toString(), UTF_8));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    return builder.toString();
  }

  private void callRestMethod(String method, Map<String, Object> params, Qwasi.QwasiInterface callbacks){
    String restMethod = parseMethod(method);
    if (restMethod == null)
      return;
    try {
      session.setRequestMethod(restMethod);
      OutputStream output = session.getOutputStream();
      String query = mapToURIQueryString(params);
      //output.
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
