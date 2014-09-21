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
 * A quadratic interpolator, simply defined by <code>f(x) = x<sup>4</sup></code>.
 * <p/>
 * This interpolator accelerates faster than the {@link CubicInterpolator} and decelerates later.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,57.6 2.4,55.3 3.6,53.1 4.8,51.0 6.0,48.9 7.2,46.8 8.4,44.9 9.6,43.0 10.8,41.1 12.0,39.4 13.2,37.6 14.4,36.0 15.6,34.4 16.8,32.8 18.0,31.3 19.2,29.9 20.4,28.5 21.6,27.1 22.8,25.8 24.0,24.6 25.2,23.4 26.4,22.2 27.6,21.1 28.8,20.0 30.0,19.0 31.2,18.0 32.4,17.0 33.6,16.1 34.8,15.2 36.0,14.4 37.2,13.6 38.4,12.8 39.6,12.1 40.8,11.4 42.0,10.7 43.2,10.1 44.4,9.5 45.6,8.9 46.8,8.3 48.0,7.8 49.2,7.3 50.4,6.8 51.6,6.3 52.8,5.9 54.0,5.5 55.2,5.1 56.4,4.7 57.6,4.4 58.8,4.1 60.0,3.8 61.2,3.5 62.4,3.2 63.6,2.9 64.8,2.7 66.0,2.5 67.2,2.2 68.4,2.1 69.6,1.9 70.8,1.7 72.0,1.5 73.2,1.4 74.4,1.3 75.6,1.1 76.8,1.0 78.0,0.9 79.2,0.8 80.4,0.7 81.6,0.6 82.8,0.6 84.0,0.5 85.2,0.4 86.4,0.4 87.6,0.3 88.8,0.3 90.0,0.2 91.2,0.2 92.4,0.2 93.6,0.1 94.8,0.1 96.0,0.1 97.2,0.1 98.4,0.1 99.6,0.1 100.8,0.0 102.0,0.0 103.2,0.0 104.4,0.0 105.6,0.0 106.8,0.0 108.0,0.0 109.2,0.0 110.4,0.0 111.6,0.0 112.8,0.0 114.0,0.0 115.2,0.0 116.4,0.0 117.6,0.0 118.8,0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class QuarticInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public QuarticInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public QuarticInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return Math.pow(v, 4);
    }
}