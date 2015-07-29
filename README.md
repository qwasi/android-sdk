# Qwasi

The Qwasi `Android SDK` provides a convenient method for accessing the Qwasi JSON-RPC API.

## Usage

To run the example project, clone the repo, and run 'gradle build' to make sure it builds correctly.

## Requirements

1. Android Studio
2. Gradle

## Installation

Qwasi is available as a gradle repo as mavenCentral repo. To install
it, simply add the following lines to your build.gradle file:

```groovy
repositories{
	mavenCentral(){
		url 'repo url'
	}
}
```

You must also include the sdk into the dependancies.

```groovy
dependencies{
	compile 'com.qwasi.sdk:QwasiSDK:2.1.0'
}
```

## License

Qwasi is available under the MIT license. See the LICENSE file for more info.

## Gradle Dependencies

```groovy
 'Org.json'
 'JSONRPC2-base'
 'JSONRPC2-client'

```

## Library initialization `Qwasi`
### Allocate a new Qwasi
The Qwasi object is created as a Singleton object; in order to access it and the constructor simply call the static function getInstance anywhere in the program.
If the object hasn't been initialized that will need to be done afterwords with the application Context.

```java
	Qwasi qwasi = new Qwasi.getInstance();
	qwasi.initQwasi(this);
```

## Library Configuration `QwasiConfig`
By default the Qwasi config will attempt to initialized with basic values or pull information from the androidmanifest.xml <meta-data> tags.  If a custom file is desired, or pulling from the manifest is desired this needs to be followed.
The Qwasi Object attempts this by default when it is initialized.

```java
	QwasiConfig config = new QwasiConfig(Context);
	config.configWithFile();
```

### Default Configuration

The default configuration file is part of the `AndroidManifest.xml`. You create and add this to your Android Studio project or add the lines to the existing file

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="your package name">
    <meta-data android:name="appID"  android:value="your hex app id here"/>
    <meta-data android:name="apiKey" android:value="your qwasi api key here"/>
    <meta-data android:name="apiUrl" android:value="your qwasi url here"/>
    <meta-data android:name="gcm_senderid" android:value="gcm app id for tokens"/>
</manifest>
```

Also if you wish to use the default QwasiNotificationManager, QwasiLocationManager, to handle Location and Notifications these will needed to be added to the AndroidManifest as well

```xml
	<application....>
		<!-- [Start Geofence Listener] -->
        <service
            android:name="com.qwasi.sdk.QwasiLocationManager"
            android:exported="false">
        </service>
        <!-- [End Geofence Listener]-->
        <!-- [START gcm_listener] -->
		<service
			android:name="com.qwasi.sdk.QwasiNotificationManager"
			android:exported="false" >
			<intent-filter>
				<action android:name="com.google.android.c2dm.intent.RECEIVE" />
			</intent-filter>
		</service>
		<!-- [END gcm_listener] -->
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

**Note:  you should include the file extention type when passing it to this method**

### Runtime Configuration
You can create a runtime configuration object on the fly using:

```java
	public QwasiConfig configWithURL(URL, String, String)
```
Example:

```java
	URL url = new URL("https://sandbox.qwasi.com/v1");
	QwasiConfig config = new QwasiConfig.configWithURL(url, "AppID srting", "API String");
	qwasi.qwasiWithConfig(config);
```

**Note: You should always check to make sure that the config that you have is valid with the .isValid() function**

## Event Emitters NYI
The Qwasi library uses Witness library to create node like Emitter events.  These events will be caught by the Reporter interface, to register for these events
simply use syntax below:

Event emitter registering:
```java
Witness.register(QwasiNotificationManager.class, this); //messaging events
Witness.register(QwasiLocationManager.class, this); //location events
Witness.register(Qwasi.class, this);  //general purpose events
```

Interface implementation:
```java
@Override
public void notifyEvent(Object o){
	//handle threading events based on what you'd like to do.
}
```


## Interface  `QwasiInterface`
All of the methods in the Qwasi Library use a simple interface to handle success and failure callbacks to create a smooth threading experience.
While all of the methods can accept a custom QwasiInterface object, a default one is offered in the Library as an example and for convenience.
It should be Overloaded in order to handle your needs at any given time.

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
Every device that engages with Qwasi SDK requires a unique device token.  This token will be stored by the Qwasi object when it is instaniated, and passed to
the server when a device is registered or push is enabled.

There are many `registerDevice` overloads defined in `Qwasi.java`, the simplest and most useful is:

```java
public void registerDevice(String deviceToken, String userToken)
```

Example:

```java
    // Get our device token from the defaults
    SharedPreferences preferences =  this.getSharedPreferences("app preferances", Context.MODE_PRIVATE);
    String deviceToken = preferences.getString("key value", default value);
    qwasi.registerDevice(deviceToken, USER_TOKEN);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString("key" qwasi.getMDeviceToken);
    editor.apply();
```
Note other 'registerDevice functions exist for when you have more or less information about the user, or device.

###### SDK Event - "register"
##### SDK Error - `QwasiErrorDeviceRegistrationFailed`
###### API Method - `device.register`

### User Tokens
User tokens are basically your vendor identifier for this device. Some developers use their customer id or loyalty id number, this allow you to address the devices with this token from the platform. These do not have to be unique and can be used to group devices under a single user token. The default is "".

You can set the user token either via the `deviceRegister` call, or later via the qwasi object.

Example:

```java
	qwasi.muserToken = "My User Token";
```

If the device has not been registered the user token will be updated when registration is called, otherwise it will simply use the 	`device.set_user_token` API call.

###### SDK Event - N/A
###### SDK Error - `QwasiErrorSetUserTokenFailed`
###### API Method - `device.set_user_token`

### Unregistration
If a device is unnecessary, it can be unregistered using:

```java
	qwasi.unregisterDevice(String);
```
Example:

```java
	qwasi.unregisterDevice(qwasi.getMdeviceToken());
```

As a general rule there is very little reason to do so, however if a user chooses to cancel their account or somthing like that would be the only reason.

###### SDK Event - N/A
###### SDK Error - `QwasiErrorDeviceUnregisterFailed`
###### API Method - `device.unregister`

## Push Notifications
Qwasi supports a simplified registration for push notifications. Once the device is registered, if mpushEnabled wasn't
 true set it and then, simply call the method:

```java
 public void setPushEnabled(QwasiInterface)
```

Example:

```java
	qwasi.mpushEnabled = true;
	qwasi.setPushEnabled();
```

###### SDK Event - "pushToken"
###### SDK Error - `QwasiErrorPushRegistrationFailed`
###### API Method - `device.set_push_token`

### Background Fetch
If the user does not permit push notifications, or if the device does not have network access some notification could be missed.
If your app has the backgroud fetch permission, you will still continue to get notification periodically, even if push is disabled.
 The SDK will simluate a push by fetching an unread message, which could be passed to a Notification builder.

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
This method will not generate a notification.  But if one is desired an example of how to create a notification can be seen at
at the QwasiNotificationManager onRecieve for an example.

###### SDK Event - "message" (optional)
###### SDK Error - `QwasiErrorMessageFetchFailed`
###### API Method - `message.poll`

### Handling Incoming Messages
You receive message via the GCMListener registered in the AndroidManifest.xml, which can be .QwasiNotificationManager or some other class that you've defined.

Example:

```java
	@Override
	public void onMessageReceived(String from, final Bundle data){
		qwasi.fetchMessageForNotification(data, QwasiInterface);
		//or Qwasi.fetchMessageForNotification(data);
	}
```

While this effective again without a sendNotification method that builds the notification to send to the UIThread, post it to the onSuccess defined or the default if none is defined.
###### SDK Event - "message"
###### SDK Error - `QwasiErrorMessageFetchFailed`
###### API Method - N/A

## Message Channels
`Qwasi` AIM supports arbitraty message groups via channels. The API is simple.

### Subscribe to a Channel

```java
public void subscribeToChannel(String, QwasiInterface)
```

Example:

```java
	qwasi.subscribeToChannel("baseball");
```
###### SDK Event - N/A
###### SDK Error - `QwasiErrorChannelSubscribeFailed`
###### API Method - `channel.subscribe`

### Unsubscribe from Channel

```java
public void unsubscribeFromChannel(String, QwasiInterface)
```

Example:

```java
	qwasi.unsubscribeFromChannel("baseball");
```
###### SDK Event - N/A
###### SDK Error - `QwasiErrorChannelUnsubscribeFailed`
###### API Method - `channel.unsubscribe`


## Application Events
The `Qwasi` platform supports triggers on application events, but the events have to be provided. By default the library will send application state events (open, foreground, background, location).
You can send custom events and configure your AIM to act on those as you see fit

```java
public void  postEvent:(String, HashMap<String, Object>, QwasiInterface)
```

Example:

```java
	qwasi.postEvent("login", HashMap<String, Object>("username", "bobvila"));
```

## Location
The `Qwasi` SDK can provide device location and track geofence events. The geofences  must be preconfigured via the AIM or API interfaces.
(Geofences are still experimental)
### Enabling Location
Location is enabled or disabled via the qwasi instance, once the device has been registered:

```java
	qwasi.mlocationEnabled = true;
```

### Location Manager
In order to use the LocationManager you will nee to instancate it, either with a GoogleApiClient, or use the default googleApiClient.

```java
	qwasi.mlocationManager.init();
	//or
	qwasi.mlocationManager.initWithGoogleApiClient(GoogleApiClient);
```

**Note: The connection to the GoogleApiClient will need to be connnected and disconnected as the Application opens and closes. ***

###### SDK Event - N/A
###### SDK Error - `QwasiErrorLocationSyncFailed`
###### API Method - `location.fetch`

### Handling Location Events
Like messages locations are delivered by listeners.

Example:

```java
	public void onLocationChanged(Location location)
```
###### SDK Event - "location"
###### SDK Error - N/A
###### API Method - N/A

## Device Data
Qwasi supports a key value based cloud data storage system. This data stored device specific. The key can be a deep object path using dot-notication.

### Set Device Data

```java
public void setDeviceValue(Object value, String key, QwasiInterface);
public void setDeviceValue(Object value, String key);
```
###### SDK Event - N/A
###### SDK Error - `QwasiErrorSetDeviceDataFailed`
###### API Method - `device.set_data`

### Get Device Data

```java
public void deviceValueForKey(String key);
```
###### SDK Event - N/A
###### SDK Error - `QwasiErrorGetDeviceDataFailed`
###### API Method - `device.set_data`

Example:

```java
qwasi.setDeviceValue("hotrod99", "user.displayname");
qwasi.deviceValueForKey("user.displayname");
```
## Sending Message
With the Qwasi API and SDK it is possible to send message to other users, this could facilitate a 2-way communication or chat application. Qwasi does not explictly support this functionality so much of the implementation is left to the developer. You will need to manage mapping your own userTokens to some useful data, which can be stored in the device record as described above.

```java
public void  sendMessage(QwasiMessage message, String userToken, QwasiInterface);  //Boolean may change to Methods or Threads
public void sendMessage(QwasiMessage message, String userToken);
```
###### SDK Event - N/A
###### SDK Error - `QwasiErrorSendMessageFailed`
###### API Method - `message.send`

Example Sender:

```java
	HashMap<String, Object> payload = new Hashmap<String, Object>;
	payload.put("from", "notbob98");
	QwasiMessage welcome = new QwasiMessage().initWithAlert("sup foo",
		     payload, null, "chatMessage");
	qwasi.sendMessage(welcome,  "scurry88");
```