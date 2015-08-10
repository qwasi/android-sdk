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
import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 */
public class QwasiConfig extends HashMap<String, Object> {
    public URL murl = null;
    public String mapplication = null;
    public String mkey = null;
    private Context sharedApplication;
    String TAG = "QwasiConfig";

    public QwasiConfig(Context context){
        sharedApplication = context;
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
                    BufferedReader readBuffer = new BufferedReader(new InputStreamReader(dataIn));
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
                ApplicationInfo applicationInfo = sharedApplication.getPackageManager().getApplicationInfo(sharedApplication.getPackageName(), PackageManager.GET_META_DATA);
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

        if (murl == null){
            try {
                murl = new URL("https://api.qwasi.com/v1:8000");
            }
            catch (MalformedURLException e){
                System.err.println("Malformed URL Exeption: "+e.getMessage());
            }
        }

        mapplication = App;

        if (mapplication == null){
            mapplication = "INVAILD_APP_ID";
        }

        mkey = Key;

        if (mkey == null){
            mkey = "INVAILD_API_KEY";
        }
        return this;
    }

    public boolean isValid(){

        if((murl == null)|| //if url is null
                ((mapplication == null) || //or if application is null, or (it's empty or invalid)
                        (mapplication.equalsIgnoreCase("")|| mapplication.equalsIgnoreCase("INVAILD_APP_ID")))||
                ((mkey == null) || //or if key is null, or it's empty or invalid
                        (mkey.equalsIgnoreCase("")|| mkey.equalsIgnoreCase("INVAILD_API_KEY")))
                ){
            return false;
        }
        return true;
    }
}
