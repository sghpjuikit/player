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
 * A cubic interpolator, simply defined by <code>f(x) = x<sup>3</sup></code>.
 * <p/>
 * This interpolator accelerates faster than the {@link QuadraticInterpolator} and decelerates later.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,58.2 2.4,56.5 3.6,54.8 4.8,53.1 6.0,51.4 7.2,49.8 8.4,48.3 9.6,46.7 10.8,45.2 12.0,43.7 13.2,42.3 14.4,40.9 15.6,39.5 16.8,38.2 18.0,36.8 19.2,35.6 20.4,34.3 21.6,33.1 22.8,31.9 24.0,30.7 25.2,29.6 26.4,28.5 27.6,27.4 28.8,26.3 30.0,25.3 31.2,24.3 32.4,23.3 33.6,22.4 34.8,21.5 36.0,20.6 37.2,19.7 38.4,18.9 39.6,18.0 40.8,17.2 42.0,16.5 43.2,15.7 44.4,15.0 45.6,14.3 46.8,13.6 48.0,13.0 49.2,12.3 50.4,11.7 51.6,11.1 52.8,10.5 54.0,10.0 55.2,9.4 56.4,8.9 57.6,8.4 58.8,8.0 60.0,7.5 61.2,7.1 62.4,6.6 63.6,6.2 64.8,5.8 66.0,5.5 67.2,5.1 68.4,4.8 69.6,4.4 70.8,4.1 72.0,3.8 73.2,3.6 74.4,3.3 75.6,3.0 76.8,2.8 78.0,2.6 79.2,2.4 80.4,2.2 81.6,2.0 82.8,1.8 84.0,1.6 85.2,1.5 86.4,1.3 87.6,1.2 88.8,1.1 90.0,0.9 91.2,0.8 92.4,0.7 93.6,0.6 94.8,0.6 96.0,0.5 97.2,0.4 98.4,0.3 99.6,0.3 100.8,0.2 102.0,0.2 103.2,0.2 104.4,0.1 105.6,0.1 106.8,0.1 108.0,0.1 109.2,0.0 110.4,0.0 111.6,0.0 112.8,0.0 114.0,0.0 115.2,0.0 116.4,0.0 117.6,0.0 118.8,0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class CubicInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public CubicInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public CubicInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return Math.pow(v, 3);
    }
}