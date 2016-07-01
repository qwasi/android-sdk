package com.qwasi.sdk;

import android.os.Bundle;

/**
 * Created by ccoulton on 1/15/16.
 * Provides a concrete class to communicate w/ QwasiGCMListener
 */
public class QwasiGCMDefault extends QwasiGCMListener{
  @Override
  public void onQwasiMessage(QwasiMessage msg){} //required for overloading custom GCMListener

  @Override
  public void onQwasiBundle(Bundle msg){} //required
}