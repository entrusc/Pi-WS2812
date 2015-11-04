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
import com.sun.jna.Structure;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * A simple Java wrapper around the WS2812 library by
 * Pimoroni (https://github.com/pimoroni/unicorn-hat).
 * <p/>
 * You need to connect the first WS2812 LED on GPIO
 * port #18 (PWM) for this to work.
 * <p/>
 * And no at the moment it is not possible to do the
 * same with Pi4j as it does not allow using the
 * GPIO #18's PWM serialization feature.
 *
 * @author Florian Frankenberger
 */
public final class WS2812 {

    private static WS2812Interface INSTANCE;

    public static class Color_t extends Structure {
        public byte r;
        public byte g;
        public byte b;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] {
                "r", "g", "b"
            });
        }

    }

    public static synchronized WS2812Interface get() {
        if (INSTANCE == null) {
            final File tmpLibraryFile = unpackLibrary();

            INSTANCE = (WS2812Interface) Native.loadLibrary(tmpLibraryFile.getAbsolutePath(), WS2812Interface.class);

            //make sure we clean up before that app exits
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    INSTANCE.clear();
                    INSTANCE.show();
                    INSTANCE.terminate();

                    Native.unregister(WS2812Interface.class);
                    tmpLibraryFile.delete();
                    tmpLibraryFile.getParentFile().delete();
                }

            });
        }
        return INSTANCE;
    }

    public static interface WS2812Interface extends Library {

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
         * changes the brightness of all LEDs. [0 to 1.0], where
         * 0.0 is basically off and 1.0 being the brightest setting.
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

        default void setPixelColor(int pixel, Color color) {
           this.setPixelColor(pixel, (byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }

        default void fill(byte r, byte g, byte b) {
            for (int i = 0; i < numPixels(); ++i) {
                this.setPixelColor(i, r, g, b);
            }
        }

        default void fill(Color color) {
            byte r = (byte) color.getRed();
            byte g = (byte) color.getGreen();
            byte b = (byte) color.getBlue();

            fill(r, g, b);
        }

        /**
         * actually propergates the changes made to the pixels to the
         * real LEDs. This needs to be called everytime when something
         * is changed to actually make it visible.
         */
        void show();

        /**
         * returns the amount of pixels set in init()
         * @see #init(int)
         * @return
         */
        int numPixels();

        /**
         * Should be called when exiting the app. This is done automatically when
         * the virtual machine exists via shutdown hook.
         */
        default void terminate() {
            terminate(0);
        }

        /**
         * Should be called when exiting the app. This is done automatically when
         * the virtual machine exists via shutdown hook.
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
                    OutputStream tmpFileOut = new FileOutputStream(tmpLibraryFile)
            ) {
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
