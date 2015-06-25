package com.qwasi.sdk;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 */
enum QwasiErrorCode{
    QwasiErrorNone,
    QwasiErrorDeviceNotRegistered,
    QwasiErrorDeviceRegistrationFailed,
    QwasiErrorDeviceUnregisterFailed,
    QwasiErrorPushRegistrationFailed,
    QwasiErrorPushNotEnabled,
    QwasiErrorPushUnregisterFailed,
    QwasiErrorMessageFetchFailed,
    QwasiErrorLocationFetchFailed,
    QwasiErrorLocationSyncFailed,
    QwasiErrorLocationMonitoringFailed,
    QwasiErrorLocationBeaconRagingFailed,
    QwasiErrorPostEventFailed,
    QwasiErrorChannelSubscribeFailed,
    QwasiErrorChannelUnsubscribeFailed,
    QwasiErrorSetUserTokenFailed,
    QwasiErrorSetDeviceDataFailed,
    QwasiErrorGetDeviceDataFailed,
    QwasiErrorSendMessageFailed,
    QwasiErrorInvaildMessage,
    QwasiErrorLocationAccessDenied,
    QwasiErrorLocationAccessInsufficient,
    QwasiErrorMessageNotFound
}

public class QwasiError extends Exception{
    public QwasiErrorCode errorWithCode(QwasiErrorCode code, String message){
        return this.errorWithCode(code, message, null);
    }

    public QwasiErrorCode errorWithCode(QwasiErrorCode code, String message, Error error){
        HashMap<String, Object> userInfo = new HashMap<String, Object>();

        if (error != null){
            userInfo.put(message + " reason= " + error.getMessage(), null);
            userInfo.put("innerError", error);
        }
        else{
            userInfo.put(message, null);
        }

        return null;
    }

    //todo handle error codes.
}

