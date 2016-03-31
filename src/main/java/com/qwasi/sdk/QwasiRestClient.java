package com.qwasi.sdk;

import java.util.Map;

/**
 * Created by Charles on 3/25/16.
 */
public class QwasiRestClient extends QwasiClient{
    String TAG = "QwasiRestClient";

    protected QwasiRestClient initWithConfig(QwasiConfig config, Qwasi manager){
        return this;
    }

    @Override
    void invokeMethod(String method, Map<String, Object> params, Qwasi.QwasiInterface callback) {

    }

    @Override
    void invokeNotification(String method, Map<String, Object> params, Qwasi.QwasiInterface callbacks) {

    }
}
