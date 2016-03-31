package com.qwasi.sdk;

import java.net.URL;
import java.util.Map;
/**
 * Created by Charles on 3/25/16.
 */
abstract public class QwasiClient {
    URL mServer = null;
    private QwasiClient client;
    String TAG = "QwasiClient";
    Boolean isVersion3;

    public QwasiClient clientWithConfig(final QwasiConfig config, final Qwasi input){
        mServer = config.url;
        isVersion3 = checkVersion();
        if (isVersion3) client = new QwasiRestClient().initWithConfig(config, input);
        else            client = new QwasiRPCClient().initWithConfig(config, input);
        return client;
    }

    Boolean checkVersion(){
        return false;
    }

    abstract protected QwasiClient initWithConfig(QwasiConfig config, Qwasi Manager);

    abstract void invokeMethod(final String method,
                               final Map<String, Object> params,
                               final Qwasi.QwasiInterface callback);

    abstract void invokeNotification(final String method,
                                     final Map<String, Object> params,
                                     final Qwasi.QwasiInterface callbacks);
}
