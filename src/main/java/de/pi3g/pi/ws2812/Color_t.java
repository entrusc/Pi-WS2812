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

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Florian Frankenberger
 */
public class Color_t extends Structure {

    public static class ByValue extends Color_t implements Structure.ByValue {}

    public byte r;
    public byte g;
    public byte b;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(new String[]{"r", "g", "b"});
    }

}
