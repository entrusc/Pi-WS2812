/*
 * Copyright (C) 2015 Florian Frankenberger.
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package de.pi3g.pi.ws2812;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * A simple Java wrapper around the WS2812 library by Pimoroni
 * (https://github.com/pimoroni/unicorn-hat).
 * <p/>
 * You need to connect the first WS2812 LED on GPIO port #18 (PWM) for this to
 * work.
 * <p/>
 * And no at the moment it is not possible to do the same with Pi4j as it does
 * not allow using the GPIO #18's PWM serialization feature.
 *
 * @author Florian Frankenberger
 */
public final class WS2812 {

    private static WS2812 INSTANCE;
    private final WS2812Interface iface;

    private WS2812(WS2812Interface iface) {
        this.iface = iface;
    }

    /**
     * initializes the led array with the given number of pixels (=LEDs)
     *
     * @param numPixels
     */
    public void init(int numPixels) {
        iface.init(numPixels);
    }

    /**
     * clears all LEDs by setting them to color (0, 0, 0)
     */
    public void clear() {
        iface.clear();
    }

    /**
     * changes the brightness of all LEDs. [0 to 1.0], where 0.0 is basically
     * off and 1.0 being the brightest setting.
     *
     * @param brightness
     */
    public void setBrightness(float brightness) {
        iface.setBrightness(brightness);
    }

    /**
     * sets the color of a specific LED
     *
     * @param pixel
     * @param r
     * @param g
     * @param b
     */
    public void setPixelColor(int pixel, byte r, byte g, byte b) {
        iface.setPixelColor(pixel, r, g, b);
    }

    public  void setPixelColor(int pixel, Color color) {
        this.setPixelColor(pixel, (byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
    }

    public void fill(byte r, byte g, byte b) {
        for (int i = 0; i < numPixels(); ++i) {
            this.setPixelColor(i, r, g, b);
        }
    }

    public void fill(Color color) {
        byte r = (byte) color.getRed();
        byte g = (byte) color.getGreen();
        byte b = (byte) color.getBlue();

        fill(r, g, b);
    }

    /**
     * returns the current color of the given pixel/LED
     * @param index
     * @return
     */
    public Color getPixelColor(int index) {
        Color_t color = iface.getPixelColor(index);
        if (color != null) {
            return new Color(color.r & 0xFF, color.g & 0xFF, color.b & 0xFF);
        } else {
            return Color.BLACK;
        }
    }

    /**
     * actually propergates the changes made to the pixels to the real LEDs.
     * This needs to be called everytime when something is changed to actually
     * make it visible.
     */
    public void show() {
        iface.show();
    }

    /**
     * returns the amount of pixels set in init()
     *
     * @see #init(int)
     * @return
     */
    public int numPixels() {
        return iface.numPixels();
    }

    /**
     * Should be called when exiting the app. This is done automatically when
     * the virtual machine exists via shutdown hook.
     */
    public void terminate() {
        iface.terminate(0);
    }


    public static synchronized WS2812 get() {
        if (INSTANCE == null) {
            final File tmpLibraryFile = unpackLibrary();

            final WS2812Interface iface = (WS2812Interface) Native.loadLibrary(tmpLibraryFile.getAbsolutePath(), WS2812Interface.class);
            INSTANCE = new WS2812(iface);

            //make sure we clean up before that app exits
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    iface.clear();
                    iface.show();
                    iface.terminate(0);

                    Native.unregister(WS2812Interface.class);
                    tmpLibraryFile.delete();
                    tmpLibraryFile.getParentFile().delete();
                }

            });
        }
        return INSTANCE;
    }

    private static interface WS2812Interface extends Library {

        /**
         * initializes the led array with the given number of pixels (=LEDs)
         *
         * @param numPixels
         */
        void init(int numPixels);

        /**
         * clears all LEDs by setting them to color (0, 0, 0)
         */
        void clear();

        /**
         * changes the brightness of all LEDs. [0 to 1.0], where 0.0 is
         * basically off and 1.0 being the brightest setting.
         *
         * @param brightness
         * @return
         */
        byte setBrightness(float brightness);

        /**
         * sets the color of a specific LED
         *
         * @param pixel
         * @param r
         * @param g
         * @param b
         * @return
         */
        byte setPixelColor(int pixel, byte r, byte g, byte b);

        Color_t.ByValue getPixelColor(int index);

        /**
         * actually propergates the changes made to the pixels to the real LEDs.
         * This needs to be called everytime when something is changed to
         * actually make it visible.
         */
        void show();

        /**
         * returns the amount of pixels set in init()
         *
         * @see #init(int)
         * @return
         */
        int numPixels();

        /**
         * Should be called when exiting the app. This is done automatically
         * when the virtual machine exists via shutdown hook.
         *
         * @param dummy
         */
        void terminate(int dummy);

        /**
         * debug: dump PWM register status of the BCM2835
         */
        void dumpPWMStatus();

        /**
         * debug: dumps the led buffer array of the underlying library
         */
        void dumpLEDBuffer();

    }

    private static final String LIBRARY_FILE_NAME = "libws2812-RPi.so";

    private static File unpackLibrary() {
        try {
            final File tmpDirectory = Files.createTempDirectory("library").toFile();
            final File tmpLibraryFile = new File(tmpDirectory, LIBRARY_FILE_NAME);

            tmpDirectory.deleteOnExit();
            tmpLibraryFile.deleteOnExit();

            //TODO: add library for Raspi v2
            byte[] buffer = new byte[512];
            try (
                    InputStream libraryIn = WS2812.class.getResourceAsStream("/" + LIBRARY_FILE_NAME);
                    OutputStream tmpFileOut = new FileOutputStream(tmpLibraryFile)) {
                int read = 0;
                while ((read = libraryIn.read(buffer)) >= 0) {
                    tmpFileOut.write(buffer, 0, read);
                }
            }

            return tmpLibraryFile;
        } catch (IOException e) {
            throw new IllegalStateException("Could not unpack native library", e);
        }
    }

}
