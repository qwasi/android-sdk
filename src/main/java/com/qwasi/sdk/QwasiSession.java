/**
 * Created by ccoulton on 6/16/15.
 * For Qwasi Inc. for the Open source Android SDK example
 // QwasiSession.java
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
 ** This file uses examples from JSONRPC2 website on how to build custom headers
 */

package com.qwasi.sdk;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;

@SuppressWarnings("deprecation")
public class QwasiSession {
  private Qwasi mSharedApp;
  private QwasiConfig mConfig;
  Header[] mHeaders;
  List[] mRequestHeaders;

  public QwasiSession(QwasiConfig config, Qwasi main) {
    mSharedApp = main;
    mConfig = config;
    mHeaders = new Header[4];  //convert to list?
    mRequestHeaders = new List[4];
    this.configure();
  }

  public void configure() {
    this.applyHeaders();
  }

  private void applyHeaders() {
    mHeaders[0] = new BasicHeader("x-qwasi-api-key", mConfig.mkey);
    mHeaders[1] = new BasicHeader("x-qwasi-app-id", mConfig.mapplication);
    mHeaders[2] = new BasicHeader("x-qwasi-device-id", mSharedApp.getMdeviceToken()); //is This needed?
    mHeaders[3] = new BasicHeader("accept-version", "2.1.0");
  }
}

