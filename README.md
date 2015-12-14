# Qwasi

The Qwasi `Android SDK` provides a convenient method for accessing the Qwasi JSON-RPC API.

## Usage

To run the example project, clone the repo, and run 'gradle build' to make sure it builds correctly.

## Requirements

1. Android Studio
2. Gradle

## Installation

Qwasi is available as a Gradle repo as a mavenCentral repo. To install it, simply add the following lines to your build.gradle file:

```groovy
    repositories{
        mavenCentral()
        jcenter()
    }
```

You must also include the SDK into the dependencies.

```groovy
    dependencies{
        compile 'com.qwasi:QwasiSDK:2.1.0-5'
    }
```

## License

Qwasi is available under the MIT license. See the LICENSE file for more info.

## Gradle Dependencies

```groovy
 'com.google.android.gms:play-services-gcm:7.5.0'
 'com.google.android.gms:play-services-location:7.5.0'
 'com.qwasi:QwasiJSON:1.0.1'  //forces legacy libraries for marshmallow
 'org.altbeacon:android-beacon-library:2.3.5'
```

## Library initialization `Qwasi`
### Allocate a new Qwasi

The Qwasi objects will need to be instantiated with the application Context.  This allows the Qwasi object
to handle setting up all the other objects it relies on.

```java
  Qwasi qwasi = new Qwasi(this);
```

## Library Configuration `QwasiConfig`

By default, the QwasiConfig will attempt to configure with basic information from the androidmanifest.xml tags. If a custom file is desired  it will need to be passed with its path and extension. The Qwasi Object attempts this by default when it is initialized.

```java
  QwasiConfig config = new QwasiConfig(Context);
  config.configWithFile();
```

### Default Configuration

The default configuration file is part of the AndroidManifest.xml. You create and add this to your Android Studio project or add the lines to the existing file

```xml
    <application>
        <meta-data android:name="appID"  android:value="your hex app id here"/>
        <meta-data android:name="apiKey" android:value="your qwasi api key here"/>
        <meta-data android:name="apiUrl" android:value="your qwasi url here"/>
        <meta-data android:name="gcm_senderid" android:value="gcm app id for tokens"/>
        <!--example gcm token would be "\ 335413682000", see note for reason-->
        ...
    </application>
```
Note: GCM senderId's being fully numeric may be confused as Integers by the android system. Adding a "\ " to the front of the number will avoid this; This will default to Qwasi's senderID if not corrected.

The SDK also uses several permissions, include these permissions in order to use the full potential of the SDK

```xml
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"/>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```

Also if you wish to use the default QwasiNotificationManager, QwasiLocationManager, to handle Location and Notifications these will needed to be added to the AndroidManifest as well

```xml
    <application...>
    ...
        <!-- [Start GCMReciver] -->
        <receiver
                    android:name="com.google.android.gms.gcm.GcmReceiver"
                    android:exported="true"
                    android:permission="com.google.android.c2dm.permission.SEND" >
                    <intent-filter>
                        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                    </intent-filter>
                </receiver>
        <!-- [End GCMReciever]-->
        <!-- [Start Geofence Listener] -->
        <service
            android:name="com.qwasi.sdk.QwasiGeofencehandler"
            android:exported="false">
        </service>
        <!-- [End Geofence Listener]-->
        <!-- [START gcm_listener] -->
        <service
            android:name="com.qwasi.sdk.QwasiGCMListener"
            android:exported="false" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
            </intent-filter>
        </service>
        <!-- [END gcm_listener] -->
        <!-- [START Beacon Listener] -->
        <service android:name="com.qwasi.sdk.QwasiBeacons" android:enabled="true"/>
        <!-- [END Beacon Listener] -->
    ...
    </application>
```

### Custom Configuration File

You can load a configuration from another property list by using:

```java
    public QwasiConfig configWithFile(String path);
```

Example:

```java
    QwasiConfig config = new QwasiConfig();
    config.configWithFile("path to text file here");
```

### Runtime Configuration

You can create a runtime configuration object on the fly using:

```java
    public QwasiConfig configWithURL(URL, String, String)
```

Example:

```java
    URL url = new URL("https://sandbox.qwasi.com/v1");
    QwasiConfig config = new QwasiConfig.configWithURL(url, "AppID string", "API String");
    qwasi.qwasiWithConfig(config);
```
**Note: You should always check to make sure that the config that you have is valid with the .isValid() function**

## Event Emitters

The Qwasi library uses Witness library to create node like Emitter events. These events will be caught by the Reporter interface, to register for these events simply use syntax below:
Event emitter registering:

```java
    Witness.register(QwasiMessage.class, Reporter); //messaging events
    Witness.register(QwasiLocation.class, Reporter); //location events
    Witness.register(String.class, Reporter);  //general purpose events
    //will get DeviceToken and PushToken
```

Interface implementation:

```java
    @Override
    public void notifyEvent(Object o){
        //will get all object types registered to the reporter
        //handle threading events based on what you'd like to do.
    }
```

Inline Example:
```java
    Witness.register(QwasiMessage.class, new Reporter(){
        @Override
        public void notifyEvent(Object o){
            //this will only grab QwasiMessage events reduces parsing or if trees
        }
    };
```

**Note: The object types that you register for are the object types that will be returned in the Object for notifyEvent, the QwasiNotificationManager notifies with a QwasiMessage.**

## Interface `QwasiInterface`

All of the methods in the Qwasi Library use a simple interface to handle success and failure callbacks to create a smooth threading experience. While all of the methods can accept a custom QwasiInterface object, a default one is offered in the Library as an example and for convenience. It should be Overloaded in order to handle your needs at any given time.
Most Qwasi functions make use of the default QwasiInterface while performing tasks inside of the SDK.

## Error Handling `QwasiError`

These will be created and passed to the QwasiInterface onFailed(QwasiError Error) method that is passed or the default.
Example:

```java
    qwasi.registerDevice("UserToken", new QwasiInterface({
        @Override
        public void onSuccess(Object o){
            //do success conditions here
        }
        @Override
        public void onFailure(QwasiError e){
            //handle error here
        }
    });
```

## Device Registration
### Device Tokens

Every device that engages with Qwasi SDK requires a unique device token. This token will be stored by the Qwasi object when it is instantiated, and passed to the server when a device is registered or push is enabled.
There are many registerDevice overloads defined in Qwasi.java, the simplest and most useful is:
public void registerDevice(String deviceToken, String userToken), when this function calls it's interface's onSuccess, it also broadcasts an event of the devicetoken as a String.
Example:

```java
    // Get our device token from the defaults
    SharedPreferences preferences =  this.getSharedPreferences(Context.MODE_PRIVATE);
    String deviceToken = preferences.getString("key value", default value);
    qwasi.registerDevice(deviceToken, USER_TOKEN, new QwasiInterface({...
    SharedPreferences.Editor editor = preferences.edit(); 
    editor.putString("key" qwasi.getMDeviceToken);
    editor.apply();
    ...
    }); //this is an asyncrous function.
```

**Note: other registerDevice functions exist for when you have more or less information about the user, or device.**
**Note: Most API method calls are Async meaning use call backs if you are doing a series of them that depend on other functions operations.**

###### SDK EVENT - "REGISTER"
###### SDK Error - `QwasiErrorDeviceRegistrationFailed`
###### API METHOD - `DEVICE.REGISTER`

### User Tokens
User tokens are basically your devices.
Some developers use their customer id or loyalty id number, this allows you to address the devices with this token from the platform.
These do not have to be unique and can be used to group devices under a single user token. The default is "".

You can set the user token either via the `deviceRegister` call, or later via the Qwasi object.

Example:
    qwasi.muserToken = "My User Token";
If the device has not been registered the user token will be updated when registration is called, otherwise it will simply use the device.set_user_token API call.
###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORSETUSERTOKENFAILED`
###### API METHOD - `DEVICE.SET_USER_TOKEN`

### Un-registration

If a device is unnecessary, it can be unregistered using:

```java
    public void qwasi.unregisterDevice(String);
```

Example:

```java
    qwasi.unregisterDevice(qwasi.getMdeviceToken());
```

As a general rule there is very little reason to do so, however if a user chooses to cancel their account or something like that would be the only reason.
###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORDEVICEUNREGISTERFAILED`
###### API METHOD - `DEVICE.UNREGISTER`

## Push Notifications

Qwasi supports a simplified registration for push notifications. Once the device is registered, if mpushEnabled wasn't true set it and then, simply call the method:
 public void setPushEnabled(QwasiInterface), other versions of this method exist.
Example:

```java
    qwasi.mpushEnabled = true;
    qwasi.setPushEnabled();
    //or more prefered as this this is async and if called directly after deviceregister
    qwasi.deviceRegister(null, new QwasiInterface(...
        setPushEnabled(Boolean, QwasiInterface);
        ...
        });
```

setPushEnabled emits the push token as a string, and also returns it on the onSuccess of the QwasiInterface passed to it.  It is recommended you save this to the application's Preferances with the key "gcm_token".

**Note: This function is ASYNC, if the register flag has not been set by the program or by device register it will fail gracefully by putting your device into push.poll rather than gcm.push**

###### SDK EVENT - "PUSHTOKEN"
###### SDK ERROR - `QWASIERRORPUSHREGISTRATIONFAILED`
###### API METHOD - `DEVICE.SET_PUSH_TOKEN`

### Background Fetch

If the user does not permit push notifications, or if the device does not have network access some notification could be missed. If your app has the background fetch permission, you will still continue to get notification periodically, even if push notifications are disabled. The SDK will simulate a push by fetching an unread message, which could be passed to a Notification builder.

### Message Polling

If your app does not support background fetch, you can periodically call:

```java
	public void fetchUnreadMessage(QwasiInterface)
```

Calling this in the UIThread so that you can check for messages.
Example:

```java
protected void onStart(){
    ...
    qwasi.fetchUnreadMessage(new QwasiInterface(){
    @Override
    public void onSuccess(Object QwasiMessage){
        //push message to screen
        }
    public void onFailure(QwasiError Error){
        //error handling
        }
    });
}
```

This method will not generate a notification, if local notifications are not enabled. It will also Send a QwasiMessage Event over Witness.
**NOTE: Applications WILL NOT Recieve notifications if they are force closed!  This method allows you to retrieve messages sent while the user has had the application closed.**

###### SDK EVENT - "MESSAGE" (OPTIONAL)
###### SDK ERROR - `QWASIERRORMESSAGEFETCHFAILED`
###### API METHOD - `MESSAGE.POLL`

### Handling Incoming Messages

Messages come in from GCM and are passed to the GCMListener registered in the Manifest.  They proceed to pass an PendingIntent, and the Bundle to QwasiNotificationManager.onMessage.  At which point the bundle is Emitted to the Qwasi Objects.
Example:

```java
    @Override
    public void onMessageReceived(String from, final Bundle data){
        //build intent and pas
    }
```

By default, messages recieved will be passed to the Notification manager and on to the proper Qwasi Object and posted to the As a Notification.

Notifications generated in this manner will create a PendingIntent that will launch the app, for this functionality to work correctly please set your exported for the launch activity to true, and include the following code into your onCreate

```java
    @Override
    public void onCreate(Bundle ...){
        super.onCreate(...);
        if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)!=0){
            finish();
            return;
        }
    }
```

###### SDK EVENT - "MESSAGE"
###### SDK ERROR - `QWASIERRORMESSAGEFETCHFAILED`
###### API METHOD - N/A

## Message Channels
`Qwasi` AIM supports arbitrary message groups via channels. The API is simple.

### Subscribe to a Channel

```java
    public void subscribeToChannel(String, QwasiInterface)
```

Example:

```java
    qwasi.subscribeToChannel("baseball");
```

###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORCHANNELSUBSCRIBEFAILED`
###### API METHOD - `CHANNEL.SUBSCRIBE`

### Un-subscribe from Channel

public void unsubscribeFromChannel(String, QwasiInterface)
Example:

```java
    qwasi.unsubscribeFromChannel("baseball");
```

###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORCHANNELUNSUBSCRIBEFAILED`
###### API METHOD - `CHANNEL.UNSUBSCRIBE`

## Application Events

The `Qwasi` platform supports triggers on application events, but the events have to be provided. By default, the library will send application state events (open, foreground, background, location). You can send custom events and configure your AIM to act on those as you see fit.

```java
    public void  postEvent:(String, HashMap<String, Object>, QwasiInterface)
```

Example:

```java
    qwasi.postEvent("login", HashMap<String, Object>("username", "bobvila"));
```

## Location

The Qwasi SDK can provide device location and track Geofence, and Beacon events. The Geofences and Beacons must be pre-configured via the AIM or API interfaces.

### Enabling Location

Location is enabled or disabled via the Qwasi instance, once the device has been registered:

```java
    qwasi.mlocationEnabled = true;
    qwasi.setLocationEnabled();
```

### Location Manager
In order to use the LocationManager, you will nee to instantiate it, either with a GoogleApiClient or use the default googleApiClient.

```java
    qwasi.mlocationManager.init();
    //or
    qwasi.mlocationManager.initWithGoogleApiClient(GoogleApiClient);
```

**Note: The connection to the GoogleApiClient will need to be connected and disconnected as the Application opens and closes. **

###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORLOCATIONSYNCFAILED`
###### API METHOD - `LOCATION.FETCH`

### Handling Location Events
####LocationUpdates
Like messages, locations are delivered by listeners which where set in the manifest.
Example:

```java
    public void onLocationChanged(Location location) //location updates
```

####GeoFences
These Locations are emitted when the Geofencehandler receives and handles an Intent from the Google GeoFencing API.  By default all Geofence transistions are tracked by only exit and dwell events emitted by the handler.


####Beacons
The 'Qwasi API' supports beacons with the use of the AltBeacon Android Library.  These are handled by the pre-configured Beacons added though the API.
In order to use beacons, you must implement BeaconConsumer, in your main Activity an example of that is as follows:

```java
    @Override
    public void onBeaconServiceConnect(){
        BeaconManager.getInstanceForApplication(this).setRangeNotifier(qwasi.mlocationManager.qwasiBeacons);
    }
```

The location events from the Beacon Notifiers are sent fairly regularly, but won't be sent if these aren't implemented.

**Note: For best performance RangeNotifier, and MonitorNotifier should be both be set.**

###### SDK EVENT - "LOCATION"
###### SDK ERROR - N/A
###### API METHOD - N/A

## Device Data
Qwasi supports a key value based cloud data storage system. This data stored device specific. The key can be a deep object path using dot-notation.

### Set Device Data

public void setDeviceValue(Object value, String key, QwasiInterface);
public void setDeviceValue(Object value, String key);

###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORSETDEVICEDATAFAILED`
###### API METHOD - `DEVICE.SET_DATA`

### Get Device Data

public void deviceValueForKey(String key);

###### SDK EVENT - N/A
###### SDK ERROR -`QWASIERRORGETDEVICEDATAFAILED`
###### API METHOD - `DEVICE.SET_DATA`

Example:

```java
    qwasi.setDeviceValue("hotrod99", "user.displayname");
    qwasi.deviceValueForKey("user.displayname");
```

## Sending Message

With the Qwasi API and SDK it is possible to send a message to other users, this could facilitate a 2-way communication or chat application. Qwasi does not explicitly support this functionality so much of the implementation is left to the developer. You will need to manage a mapping your own userTokens to some useful data, which can be stored in the device record as described above.
public void  sendMessage(QwasiMessage message, String userToken, QwasiInterface);  //Boolean may change to Methods or Threads
public void sendMessage(QwasiMessage message, String userToken);

###### SDK EVENT - N/A
###### SDK ERROR - `QWASIERRORSENDMESSAGEFAILED`
###### API METHOD - `MESSAGE.SEND`
Example Sender:

```java
    HashMap<String, Object> payload = new Hashmap<String, Object>;
    payload.put("from", "notbob98");
    QwasiMessage welcome = new QwasiMessage().initWithAlert("sup foo",
             payload, null, "chatMessage");
    qwasi.sendMessage(welcome,  "scurry88");
```