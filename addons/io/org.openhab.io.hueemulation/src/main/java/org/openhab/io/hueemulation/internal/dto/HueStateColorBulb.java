/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.hueemulation.internal.dto;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * Hue API state object
 *
 * @author Dan Cunningham - Initial contribution
 * @author David Graeff - "extended color light bulbs" support
 * @author Florian Lentz - added xy support
 *
 */
public class HueStateColorBulb extends HueStateBulb {
    public static int MAX_HUE = 65535; // For extended color light bulbs
    public int hue = 0;
    public static int MAX_SAT = 254;
    public int sat = 0;

    // color as array of xy-coordinates
    public double[] xy = { 0, 0 };

    public String effect = "none";

    /** time for transition in centiseconds. */
    public int transitiontime;

    public static enum ColorMode {
        ct,
        hs,
        xy
    }

    public ColorMode colormode = ColorMode.ct;

    protected HueStateColorBulb() {
    }

    public HueStateColorBulb(boolean on) {
        super(on);
        this.bri = on ? MAX_BRI : 0;
        colormode = ColorMode.ct;
    }

    /**
     * Create a hue state with the given brightness percentage
     *
     * @param brightness Brightness percentage
     * @param on On value
     */
    public HueStateColorBulb(PercentType brightness, boolean on) {
        super(brightness, on);
        colormode = ColorMode.ct;
    }

    /**
     * Creates a hue state with the given color information
     *
     * @param hsb Color information. Sets the hue state to "on" if brightness is > 0.
     */
    public HueStateColorBulb(HSBType hsb) {
        super(hsb.getBrightness().intValue() > 0);
        this.hue = hsb.getHue().intValue() * MAX_HUE / 360;
        this.sat = hsb.getSaturation().intValue() * MAX_SAT / 100;
        this.bri = hsb.getBrightness().intValue() * MAX_BRI / 100;
        colormode = this.sat > 0 ? ColorMode.hs : ColorMode.ct;
    }

    /**
     * Converts this HueState to a HSBType
     */
    public HSBType toHSBType() {
        if (colormode == ColorMode.xy) {
            int i;
            double d = this.xy[0];
            d = this.xy[1];
            double y = ((double) this.bri) / 100.0d;
            double x = (y / this.xy[1]) * this.xy[0];
            double z = (y / this.xy[1]) * ((1.0d - this.xy[0]) - this.xy[1]);
            int r = (int) (Math.abs(((1.4628067016601562d * x) - (0.18406230211257935d * y)) - (0.2743605971336365d * z)) * 255.0d);
            int g = (int) (Math.abs((((-x) * 0.5217933058738708d) + (1.4472380876541138d * y)) + (0.06772270053625107d * z)) * 255.0d);
            int b = (int) (Math.abs(((0.03493420034646988d * x) - (0.09689299762248993d * y)) + (1.288409948348999d * z)) * 255.0d);
            if (r < g) {
                i = r;
            } else {
                i = g;
            }
            double minValue = i;
            if (minValue >= b) {
                minValue = b;
            }
            double maxValue = (r > g ? r : g);
            if (maxValue <= b) {
                maxValue = b;
            }
            double delta = maxValue - minValue;
            if (maxValue <= 0.0d) {
                return new HSBType(new DecimalType(0), new PercentType(100), new PercentType((this.bri * 100) / MAX_BRI));
            }
            double h;
            if (((double) r) >= maxValue) {
                h = ((double) (g - b)) / delta;
            } else if (((double) g) >= maxValue) {
                h = 2.0d + (((double) (b - r)) / delta);
            } else {
                h = 4.0d + (((double) (r - g)) / delta);
            }
            h *= 60.0d;
            if (h < 0.0d) {
                h += 360.0d;
            }
            double hueSat = Math.floor((delta / maxValue) * 254.0d);
            int percentSat = (int) ((100.0d * hueSat) / ((double) MAX_SAT));
            if (!this.on) {
                this.bri = 0;
            }
            return new HSBType(new DecimalType((Math.floor(182.04d * h) * 360.0d) / ((double) MAX_HUE)), new PercentType(percentSat), new PercentType((this.bri * 100) / MAX_BRI));
       
        } else {
            int bri = this.bri * 100 / MAX_BRI;
            int sat = this.sat * 100 / MAX_SAT;
            int hue = this.hue * 360 / MAX_HUE;
   
            if (!this.on) {
                bri = 0;
            }
            if (colormode == ColorMode.ct) {
                sat = 0;
            }
            return new HSBType(new DecimalType(hue), new PercentType(sat), new PercentType(bri));
        }
    }

    @Override
    public String toString() {
        String xyString = "{";
        for (double d : xy) {
            xyString += d + " ";
        }
        xyString += "}";
        return "on: " + on + ", brightness: " + bri + ", hue: " + hue + ", sat: " + sat + ", xy: " + xyString + ", ct: "
                + ct + ", alert: " + alert + ", effect: " + effect + ", colormode: " + colormode + ", reachable: "
                + reachable;
    }
}
