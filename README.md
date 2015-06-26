Qwasi

The Qwasi Android-SDK provides an convenient method for accessing the Qwasi JSON-RPC API

Usage


Requirements

1. Android SDK's
2. Gradle Build tools

Installation

Qwasi is avaiable as both Ivy and Maven repositories options in the Gradle build file.  To install it, simply add the following lines to your build.gradle:

repository{
	maven(or ivy){
		url "This repo url"
	}
}

and add it to your dependancies like so:

dependancies{
	compile 'com.qwasi.sdk:QwasiSDK:2.1.0'
	}

License

Qwasi is available under the MIT license. See the LICENSE file for more info.

Gradle Dependencies

JSONRPC2-base
JSONRPC2-client

Library initializtion Qwasi

Instantiate a new Qwasi

Qwasi qwasi = new Qwasi(application context);

Library Configuration QwasiConfig

By default any qwasi instance should attempt to grab information from the Androidmanifest.xml.

QwasiConfig config = new QwasiConfig();
config.configWithFile();

Custom Configuration File
You can load a configuration from another file by passing configWithFile a path

config.configWithFile("path to my data here");

configWithURL(URL, String, String)
URL url = new URL("your url here");
QwasiConfig config = new QwasiConfig.configWithURL(url, "your app", "your key");
qwasi.initWithConfig(config);

Device Registration

Device Tokens
Every device that engages with Qwasi will require a unique device token. This token is stored in the Qwasi class and can be retrieved with getMDeviceToken().  RegisterDevice is only needed to be called once per application, unless a configureation is changed.

Register Device is overloaded a number of times, the simpliest is registerDevice(String deviceToken, String userToken)

example:

qwasi.registerDevice("token" "usertoken");
SDK Event - "REGISTER"
SDK Error - QwasiErrorDeviceRegistrationFailed
API Method - device.register



