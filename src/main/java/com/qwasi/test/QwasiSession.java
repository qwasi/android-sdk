package com.qwasi.sdk;


import com.thetransactioncompany.jsonrpc2.client.ConnectionConfigurator;


import java.io.IOException;

import java.net.HttpURLConnection;

/**
 * Created by ccoulton on 6/16/15.
 * This file uses examples from JSONRPC2 website on how to build custom headers
 * Was created for Qwasi Tech. using the Apache Software Licence.
 */

public class QwasiSession implements ConnectionConfigurator{
    private Qwasi sharedApp;
    private QwasiConfig mconfig;
    public QwasiSession(QwasiConfig config, Qwasi main){
        sharedApp = main;
        mconfig = config;
        this.configure(new HttpURLConnection(mconfig.murl) {
            @Override
            public void disconnect() {}

            @Override
            public boolean usingProxy() {
                return false;
            }

            @Override
            public void connect() throws IOException {}
        });
    }

    public void configure(HttpURLConnection con){
        this.applyHeaders(con);
    }

    private void applyHeaders(HttpURLConnection con){
        con.setRequestProperty("x-qwasi-api-key"    , mconfig.mkey);
        con.setRequestProperty("x-qwasi-app-id"     , mconfig.mapplication);
        con.setRequestProperty("x-qwasi-device-id"  , sharedApp.getMdeviceToken()); //is This needed?
        con.setRequestProperty("x-qwasi-user-token" , sharedApp.muserToken);
    }

}

