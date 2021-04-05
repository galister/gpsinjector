# GPS Injector

This is a personal project that aims to allow a ChromeOS tablet to use an Android phone as a remote GPS source. It works on any two Dndroid devices.

**ChromeOS does not need to be in developer mode, but you will need to enable ADB.**

Due to Android apps on ChromeOS running inside an isolate container, you'll have no luck trying to use a serial GPS module via USB or Bluetooth. I've also had no luck with 'GPS tether' apps as they expect your devices to be on the same L2 network.

Be warned: I'm not an Android dev, the code may be of the great spaghetti kind.

## Before first use

You will need to do these steps on the **receiving device**.

1. Install GPS Injector
2. Lanunch the app, under ⋅⋅⋅ -> Settings, turn on Mock Location
3. Developer Settings should be enabled. [How to enable?](https://developer.android.com/studio/debug/dev-options?hl=en-419)
4. Under Developer Settings, set "Mock Location App" to "GPS Injector"

If you do not have "Mock Location App" under Developer Settings, you will need to do send these commands via ADB:

```
adb shell settings put secure mock_location 1
adb shell appops set li.gravio.gpsinjector android:mock_location allow
```

### How to access ADB on a Chromebook

1. Under settings, install _Linux Development Environment_
2. Under the _Linux Development Environment_ > _Develop Android Apps_ section of settings, turn on _ADB debugging_
3. Restart your device. You'll need to confirm to enable _ADB debugging_ once it reboots.
4. Open the _Terminal_ app and type in the 2 `adb` commands above

### How to access ADB on other Android devices?

See the XDA developers forum on how to do this for your specific model. This generally involves connecting to a computer via USB and downloading some drivers.

## Running the app

Client "A" is configured to use the default hotspot address (192.168.43.1), and so if you run the server on the hotspot device, client A will work out of the box.

On the GPS-capable Android device / sender:
1. Enable Wifi Hotspot
2. Launch GPS Injector and tap "Start Server"
   
On receiving device, after setup:
1. Connect to the sender device's Wifi Hotspot
2. Launch app and tap "Start Client A" 

At this point you can open other apps; GPS Injector will run in the background.\
To stop, click the ⋅⋅⋅ icon on the top right and select Exit.

## What is Client A and B?

There are two server addresses you can specify in the settings.

_Start Client A_ will connect to _Server Address A_ and _Start Client B_ will connect to _Server Address B_.

This is for the sake of convenience, in case you need to use the app with multiple devices, or on different networks.

## How does it work?

The server:
- Runs a TCP listener
- Reads NMEA sentences from the Android system
- Relays NMEA sentences to all clients connected to the TCP listener

The client:
- Connects to a remote TCP listener
- Receives NMEA sentences via TCP socket
- Publishes mock locations generated from NMEA sentences

## Used Works

Big thanks to the creators of the following works:

https://github.com/petr-s/android-nmea-parser

https://github.com/mcastillof/FakeTraveler

https://en.wikipedia.org/wiki/File:Pictogram_VORTAC.svg
