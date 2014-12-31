# huggiAndroid

Android application for the Hugginess project.

### What is Hugginess
Physical contact has an important role in human well-being. Hugginess is a concept of interactive system that encourages people to hug by augmenting this gesture with digital information
exchange. 

The idea is to develop smart t-shirts able to detect, through conductive fibers and pressure detectors, when a hug occurs and to exchange some piece of information. 
The t-shirts should alors be able to communicate with an app in the user's smartphone using a bluetooth connection.

Since the objective of Hugginess is promoting physical touch, we strived to build a system that reflects the user’s need of physical contact. While existing projects that exchange information with a handshaking rely onwireless communication, we designed smart t-shirts that need physical contact in order to communicate properly. Moreover, Hugginess t-shirts can modulate the amount of information exchanged according to the length of the hug, but also to the social closeness of hugged person. 

 
### What is this project
This project represents the first version of an Android application able to communicate with a _Huggi-Shirt_. 
The interface of the _Huggi-Shirt_ consists of an [Arduino LilyPad](http://arduino.cc/en/Main/arduinoBoardLilyPad) as well as a [BlueSMIRF silver Bluetooth module](https://www.sparkfun.com/products/12577).

This application is currently quite simple. It provides a specific bluetooth service as well as a sample application interface. The functionalities are:

* communication with the t-shirt
* storage of the hugs into a local database
* a terminal view, mostly for debugging
* a display of the list of hugs
* a "statistics" view, with the total number of hugs and the top three huggers
* some basic settings to configure the t-shirt
*  ...


More information to come...
