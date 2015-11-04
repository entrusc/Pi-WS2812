Pi WS2812 LED Driver
====================

A Java library to drive the popular WS2812 RGB LED from a Raspberry Pi.
E.g. the unicorn-hat is a 8x8 array of those WS2812 RGB LEDs
(https://shop.pimoroni.com/products/unicorn-hat)

This is a simple wrapper around Pimoroni's C library using JNA to
relay the calls. The original C library is here:

https://github.com/pimoroni/unicorn-hat

For the moment this was just tested with Raspberry
Pi B+ and not Raspberry Pi2. The enclosed c library is compiled with
the GPIO memory location of model 1, so this most likely won't work
with model 2 yet.

how to use?
============
First you need to clone and compile the library using Maven and
then you can use the library in your maven projects like this:

    <dependency>
        <groupId>de.pi3g.pi</groupId>
        <artifactId>pi-ws2812</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

The hardware needs to be connected to GPIO port #18 which has
hardware PWM (which is needed here as the WS2812 has singal lengths
of about 1.25us)

Then you can use the library like this:

    WS2812.get().init(64); //init a chain of 64 LEDs
    WS2812.get().clear();    
    WS2812.get().setPixelColor(0, Color.RED); //sets the color of the fist LED to red
    WS2812.get().show();

Note that you always need to call show() after you changed the color of any LED
to publish the changes to the hardware.

how to build?
=============

The entire project is build with maven. Just clone the master branch, open the directory in NetBeans and hit run. Or if
you prefer the command line: 

    mvn install

should build everything correctly. 