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

package utilities.Animation.Interpolators;

/**
 * A circular interpolator.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,51.5 2.4,48.1 3.6,45.4 4.8,43.2 6.0,41.3 7.2,39.5 8.4,37.9 9.6,36.5 10.8,35.1 12.0,33.8 13.2,32.6 14.4,31.5 15.6,30.4 16.8,29.4 18.0,28.4 19.2,27.4 20.4,26.5 21.6,25.7 22.8,24.8 24.0,24.0 25.2,23.2 26.4,22.5 27.6,21.7 28.8,21.0 30.0,20.3 31.2,19.6 32.4,19.0 33.6,18.4 34.8,17.7 36.0,17.2 37.2,16.6 38.4,16.0 39.6,15.5 40.8,14.9 42.0,14.4 43.2,13.9 44.4,13.4 45.6,12.9 46.8,12.5 48.0,12.0 49.2,11.6 50.4,11.1 51.6,10.7 52.8,10.3 54.0,9.9 55.2,9.5 56.4,9.1 57.6,8.8 58.8,8.4 60.0,8.0 61.2,7.7 62.4,7.4 63.6,7.0 64.8,6.7 66.0,6.4 67.2,6.1 68.4,5.8 69.6,5.5 70.8,5.3 72.0,5.0 73.2,4.8 74.4,4.5 75.6,4.3 76.8,4.0 78.0,3.8 79.2,3.6 80.4,3.4 81.6,3.2 82.8,3.0 84.0,2.8 85.2,2.6 86.4,2.4 87.6,2.2 88.8,2.1 90.0,1.9 91.2,1.8 92.4,1.6 93.6,1.5 94.8,1.3 96.0,1.2 97.2,1.1 98.4,1.0 99.6,0.9 100.8,0.8 102.0,0.7 103.2,0.6 104.4,0.5 105.6,0.4 106.8,0.4 108.0,0.3 109.2,0.2 110.4,0.2 111.6,0.1 112.8,0.1 114.0,0.1 115.2,0.0 116.4,0.0 117.6,0.0 118.8,0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class CircularInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public CircularInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public CircularInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return -(Math.sqrt(1 - (v * v)) - 1);
    }
}