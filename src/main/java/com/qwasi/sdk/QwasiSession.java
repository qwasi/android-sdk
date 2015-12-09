package com.qwasi.sdk;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;


/**
 * Created by ccoulton on 6/16/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
 * This file uses examples from JSONRPC2 website on how to build custom headers
 */
@SuppressWarnings("deprecation")
public class QwasiSession {
    private Qwasi sharedApp;
    private QwasiConfig mconfig;
    Header[] headers;
    List[] requestHeaders;

    public QwasiSession(QwasiConfig config, Qwasi main) {
        sharedApp = main;
        mconfig = config;
        headers = new Header[4];  //convert to list?
        requestHeaders = new List[4];
        this.configure();
    }

    public void configure() {
        this.applyHeaders();
    }

    private void applyHeaders() {
        headers[0] = new BasicHeader("x-qwasi-api-key", mconfig.mkey);
        headers[1] = new BasicHeader("x-qwasi-app-id", mconfig.mapplication);
        headers[2] = new BasicHeader("x-qwasi-device-id", sharedApp.getMdeviceToken()); //is This needed?
        headers[3] = new BasicHeader("accept-version", "2.1.0");
    }

}

