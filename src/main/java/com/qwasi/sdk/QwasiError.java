/**
 * Created by Ccoulton on 6/11/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiError.java
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

import java.util.HashMap;

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

