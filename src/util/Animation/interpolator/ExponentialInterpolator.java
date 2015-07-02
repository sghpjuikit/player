/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013, Christian Schudt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package util.animation.interpolator;

/**
 * An exponential interpolator.
 * <p/>
 * This interpolator accelerates very fast and decelerates very late.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,56.0 2.4,52.2 3.6,48.7 4.8,45.5 6.0,42.4 7.2,39.6 8.4,36.9 9.6,34.5 10.8,32.2 12.0,30.0 13.2,28.0 14.4,26.1 15.6,24.4 16.8,22.7 18.0,21.2 19.2,19.8 20.4,18.5 21.6,17.2 22.8,16.1 24.0,15.0 25.2,14.0 26.4,13.1 27.6,12.2 28.8,11.4 30.0,10.6 31.2,9.9 32.4,9.2 33.6,8.6 34.8,8.0 36.0,7.5 37.2,7.0 38.4,6.5 39.6,6.1 40.8,5.7 42.0,5.3 43.2,4.9 44.4,4.6 45.6,4.3 46.8,4.0 48.0,3.8 49.2,3.5 50.4,3.3 51.6,3.0 52.8,2.8 54.0,2.7 55.2,2.5 56.4,2.3 57.6,2.2 58.8,2.0 60.0,1.9 61.2,1.7 62.4,1.6 63.6,1.5 64.8,1.4 66.0,1.3 67.2,1.2 68.4,1.2 69.6,1.1 70.8,1.0 72.0,0.9 73.2,0.9 74.4,0.8 75.6,0.8 76.8,0.7 78.0,0.7 79.2,0.6 80.4,0.6 81.6,0.5 82.8,0.5 84.0,0.5 85.2,0.4 86.4,0.4 87.6,0.4 88.8,0.4 90.0,0.3 91.2,0.3 92.4,0.3 93.6,0.3 94.8,0.3 96.0,0.2 97.2,0.2 98.4,0.2 99.6,0.2 100.8,0.2 102.0,0.2 103.2,0.2 104.4,0.1 105.6,0.1 106.8,0.1 108.0,0.1 109.2,0.1 110.4,0.1 111.6,0.1 112.8,0.1 114.0,0.1 115.2,0.1 116.4,0.1 117.6,0.1 118.8,0.1 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class ExponentialInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public ExponentialInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public ExponentialInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return Math.pow(2, 10 * (v - 1));
    }
}