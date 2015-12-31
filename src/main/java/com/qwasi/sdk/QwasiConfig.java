/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiConfig.java
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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class QwasiConfig{
    @Deprecated
    public URL murl = null;
    public URL url = murl;
    @Deprecated
    public String mapplication = "";
    public String application = mapplication;
    @Deprecated
    public String mkey = "";
    public String key = mkey;
    private Context mSharedApplication;
    String TAG = "QwasiConfig";

    public QwasiConfig(Context context){
        mSharedApplication = context;
    }

    public QwasiConfig configWithFile(){
        return this.configWithFile(null);
    }

    public QwasiConfig configWithFile(String path){
        String apiKey = "";
        String appID  = "";
        URL url = null;
        try {
            if (path != null){
                File inFile = new File(path);
                if (inFile.exists()) {//from stackoverflow.com
                    FileInputStream fileInputStream = new FileInputStream(inFile);
                    DataInputStream dataIn = new DataInputStream(fileInputStream);
                    BufferedReader readBuffer = new BufferedReader(new InputStreamReader(dataIn, Charset.forName("UTF-8")));
                    String line;
                    String splitString[];
                    while ((line = readBuffer.readLine()) != null) {
                        splitString = line.split("\\s");
                        if (splitString[0].equalsIgnoreCase("apiKey")) {
                            apiKey = splitString[1];
                        } else if (splitString[0].equalsIgnoreCase("appID")) {
                            appID = splitString[1];
                        } else if (splitString[0].equalsIgnoreCase("apiUrl")) {
                            url = new URL(splitString[1]);
                        }
                    }
                    readBuffer.close();
                }
            }
            else{ //read from the xml file
                //set up new application info w/  Application.packageManager.getApplicationInfo("application name", get the meta data)
                PackageManager packageManager = mSharedApplication.getPackageManager();
                ApplicationInfo applicationInfo = packageManager
                        .getApplicationInfo(mSharedApplication.getPackageName(), PackageManager.GET_META_DATA);
                if((applicationInfo.metaData != null)&&(!applicationInfo.metaData.isEmpty())){  //make sure we have meta data to parse
                    //start looking for key values
                    if(applicationInfo.metaData.containsKey("apiKey")){  //is and apikey value present
                        apiKey = (String) applicationInfo.metaData.get("apiKey");  //get the value of apiKey from the manifest
                    }

                    if(applicationInfo.metaData.containsKey("appID")){
                        appID  = (String) applicationInfo.metaData.get("appID");
                    }

                    if(applicationInfo.metaData.containsKey("apiUrl")){
                        url = new URL((String) applicationInfo.metaData.get("apiUrl"));
                    }
                }
            }
        }
        catch (FileNotFoundException e){
            Log.e("QwasiError", "File not found "+e.getMessage());
            Log.d(TAG, "Trying with default manifest");
            return configWithFile(null);
        }
        catch (MalformedURLException e){
            Log.e("QwasiError", "Malformed URL in file "+e.getMessage());
            Log.d(TAG, "Passing to default values");
            return configWithURL(null, null, null);
        }
        catch (IOException e) {
            Log.e("QwasiError", "I/O Error reading file at: " + path);
            Log.d(TAG, "Trying with default manifest");
            return configWithFile(null);
        }
        catch (PackageManager.NameNotFoundException e){
            Log.e("QwasiError", "Application name not found " + e.getMessage());
            Log.d(TAG, "Passing to default values");
            return configWithURL(null, null, null);
        }
        return this.configWithURL(url, appID, apiKey);
    }

    public QwasiConfig configWithURL(URL iurl, String iapp, String ikey){
        return this.initWithURL(iurl, iapp, ikey);
    }

    private QwasiConfig initWithURL(URL input, String App, String Key){
        murl = input;
        url = murl;
        if (url == null){
            try {
                url = new URL("https://api.qwasi.com/v1:8000");
            }
            catch (MalformedURLException e){
                System.err.println("Malformed URL Exeption: "+e.getMessage());
            }
        }

        mapplication = App;
        application = mapplication;
        if (application == null){
            application = "INVAILD_APP_ID";
        }

        mkey = Key;
        key = mkey;
        if (key == null){
            key = "INVAILD_API_KEY";
        }
        return this;
    }

    public boolean isValid(){

        return!((url == null)|| //if url is null
                ((application == null) || //or if application is null, or (it's empty or invalid)
                        (application.equalsIgnoreCase("")|| application.equalsIgnoreCase("INVAILD_APP_ID")))||
                ((key == null) || //or if key is null, or it's empty or invalid
                        (key.equalsIgnoreCase("")|| key.equalsIgnoreCase("INVAILD_API_KEY"))));
    }
}
