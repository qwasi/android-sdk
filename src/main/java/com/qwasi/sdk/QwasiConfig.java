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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.CharEncoding;

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
    private SharedPreferences mSharedPreferences;
    String TAG = "QwasiConfig";

    public QwasiConfig(Context context){
        mSharedApplication = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @return default overloading constructor making path nullable.
     */
    public QwasiConfig configWithFile(){
        return this.configWithFile(null);
    }

    /**
     * @param path full pathname to file containing settings for a qwasi configuration.
     * @return A QwasiConfig using 1 of 3 options, file if provided;
     * from the default sharedPreferences if saved from a custom config;
     * or from the manifest as a default for most applications.
     */
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
                    BufferedReader readBuffer = new BufferedReader(new InputStreamReader(dataIn, CharEncoding.UTF_8));
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
            } else if((mSharedPreferences.contains("QwasiApiKey"))&&
                    (mSharedPreferences.contains("QwasiAppId"))&&
                    (mSharedPreferences.contains("QwasiUrl"))){

                appID = mSharedPreferences.getString("QwasiAppId", "");
                apiKey= mSharedPreferences.getString("QwasiApiKey", "");
                url = new URL(mSharedPreferences.getString("QwasiUrl",""));
            } else{ //read from the Manifest
                //set up new application info w/  Application.packageManager.getApplicationInfo("application name", get the meta data)
                PackageManager packageManager = mSharedApplication.getPackageManager();
                String packageName = mSharedApplication.getPackageName();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                Bundle metaData = applicationInfo.metaData;
                if((metaData != null)&&(!metaData.isEmpty())){  //make sure we have meta data to parse
                    //start looking for key values
                    if((metaData.containsKey("apiKey"))&&
                            (metaData.containsKey("appID"))&&
                            (metaData.containsKey("apiUrl"))){

                        apiKey = metaData.getString("apiKey", "");
                        appID  = metaData.getString("appID", "");
                        url = new URL(metaData.getString("apiUrl", ""));
                    }
                }
            }
        }
        catch (FileNotFoundException e){ //file path given was not found
            Log.e("QwasiError", "File not found "+e.getMessage());
            Log.d(TAG, "Trying with default manifest");
            return configWithFile(null); //try again w/o the file
        }
        catch (MalformedURLException e){ //if the url provided was fubar
            Log.e("QwasiError", "Malformed URL in file "+e.getMessage());
            Log.d(TAG, "Passing to default values");
            return configWithURL(null, null, null); //try defaults
        }
        catch (IOException e) { //if the file failed at reading
            Log.e("QwasiError", "I/O Error reading file at: " + path);
            Log.d(TAG, "Trying with default manifest");
            return configWithFile(null); //try again w/o the file
        }
        catch (PackageManager.NameNotFoundException e){ //application was not found
            Log.e("QwasiError", "Application name not found " + e.getMessage());
            Log.d(TAG, "Passing to default values");
            return configWithURL(null, null, null);
        }
        return initWithURL(url, appID, apiKey);
    }

    /**
     * public interface for initWithUrl
     * @param iurl url from user or from configwithfile
     * @param iapp appId
     * @param ikey
     * @return either a valid config or an config w/ invalid values in it.
     */
    public QwasiConfig configWithURL(URL iurl, String iapp, String ikey){
        return initWithURL(iurl, iapp, ikey);
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
        } else mSharedPreferences.edit().putString("QwasiUrl", url.getHost());

        mapplication = App;
        application = mapplication;
        if (application == null){
            application = "INVAILD_APP_ID";
        }else mSharedPreferences.edit().putString("QwasiAppId", application);

        mkey = Key;
        key = mkey;
        if (key == null){
            key = "INVAILD_API_KEY";
        } else mSharedPreferences.edit().putString("QwasiApiKey", key);
        mSharedPreferences.edit().apply();
        return this;
    }

    /**
     * checks to see if the configuration is valid, I.E. did it get initialized without an issue.
     * @return
     */
    public boolean isValid(){

        return!((url == null)|| //if url is null
                ((application == null) || //or if application is null, or (it's empty or invalid)
                        (application.equalsIgnoreCase("")|| application.equalsIgnoreCase("INVAILD_APP_ID")))||
                ((key == null) || //or if key is null, or it's empty or invalid
                        (key.equalsIgnoreCase("")|| key.equalsIgnoreCase("INVAILD_API_KEY"))));
    }
}
