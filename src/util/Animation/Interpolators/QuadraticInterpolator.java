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

package util.Animation.Interpolators;

/**
 * A quadratic interpolator, simply defined by <code>f(x) = x<sup>2</sup></code>.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,58.8 2.4,57.6 3.6,56.5 4.8,55.3 6.0,54.2 7.2,53.0 8.4,51.9 9.6,50.8 10.8,49.7 12.0,48.6 13.2,47.5 14.4,46.5 15.6,45.4 16.8,44.4 18.0,43.3 19.2,42.3 20.4,41.3 21.6,40.3 22.8,39.4 24.0,38.4 25.2,37.4 26.4,36.5 27.6,35.6 28.8,34.7 30.0,33.8 31.2,32.9 32.4,32.0 33.6,31.1 34.8,30.2 36.0,29.4 37.2,28.6 38.4,27.7 39.6,26.9 40.8,26.1 42.0,25.4 43.2,24.6 44.4,23.8 45.6,23.1 46.8,22.3 48.0,21.6 49.2,20.9 50.4,20.2 51.6,19.5 52.8,18.8 54.0,18.2 55.2,17.5 56.4,16.9 57.6,16.2 58.8,15.6 60.0,15.0 61.2,14.4 62.4,13.8 63.6,13.3 64.8,12.7 66.0,12.1 67.2,11.6 68.4,11.1 69.6,10.6 70.8,10.1 72.0,9.6 73.2,9.1 74.4,8.7 75.6,8.2 76.8,7.8 78.0,7.3 79.2,6.9 80.4,6.5 81.6,6.1 82.8,5.8 84.0,5.4 85.2,5.0 86.4,4.7 87.6,4.4 88.8,4.1 90.0,3.8 91.2,3.5 92.4,3.2 93.6,2.9 94.8,2.6 96.0,2.4 97.2,2.2 98.4,1.9 99.6,1.7 100.8,1.5 102.0,1.4 103.2,1.2 104.4,1.0 105.6,0.9 106.8,0.7 108.0,0.6 109.2,0.5 110.4,0.4 111.6,0.3 112.8,0.2 114.0,0.2 115.2,0.1 116.4,0.1 117.6,0.0 118.8,0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class QuadraticInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public QuadraticInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public QuadraticInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return Math.pow(v, 2);
    }
}