package com.qwasi.sdk;

import java.util.HashMap;

/**
 * Created by Ccoulton on 6/11/15.
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
    QwasiErrorSetMemberDataFailed,
    QwasiErrorGetMemberDataFailed,
    QwasiErrorSendMessageFailed,
    QwasiErrorInvaildMessage,
    QwasiErrorLocationAccessDenied,
    QwasiErrorLocationAccessInsufficient,
    QwasiErrorMessageNotFound,
    QwasiErrorSetMemberAuthFailed,
    QwasiErrorAuthMemberFailed
}

public class QwasiError extends Throwable{
    String message;
    Exception error;
    QwasiErrorCode code;

    public QwasiError errorWithCode(QwasiErrorCode code, String message){
        return this.errorWithCode(code, message, null);
    }

    public QwasiError errorWithCode(QwasiErrorCode code, String message, Error error){
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("code", code);
        if (error != null){
            userInfo.put(message + " reason= " + error.getMessage(), null);
            userInfo.put("innerError", error);
        }
        else{
            userInfo.put(message, null);
        }
        this.message = message;
        this.code = code;
        return this;
    }

    void setError(Exception e) {
        this.error = e;
    }

    @Override
    public String getMessage(){
        return this.message;
    }
}

