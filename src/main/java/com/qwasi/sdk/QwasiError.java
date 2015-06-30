package com.qwasi.sdk;

import java.util.HashMap;

/**
 * Created by ccoulton on 6/11/15.
 * For Qwasi Inc. for their Open source Android SDK example
 * Released under the MIT Licence
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

public class QwasiError extends Throwable{

    public QwasiError errorWithCode(QwasiErrorCode code, String message) throws QwasiError{
        return this.errorWithCode(code, message, null);
    }

    public QwasiError errorWithCode(QwasiErrorCode code, String message, Error error) throws QwasiError{
        HashMap<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.put("code", code);
        if (error != null){
            userInfo.put(message + " reason= " + error.getMessage(), null);
            userInfo.put("innerError", error);
        }
        else{
            userInfo.put(message, null);
        }

        return null;
    }
}

