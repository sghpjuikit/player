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
 * A sinus based interpolator.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,59.1 2.4,58.1 3.6,57.2 4.8,56.2 6.0,55.3 7.2,54.4 8.4,53.4 9.6,52.5 10.8,51.5 12.0,50.6 13.2,49.7 14.4,48.8 15.6,47.8 16.8,46.9 18.0,46.0 19.2,45.1 20.4,44.2 21.6,43.3 22.8,42.4 24.0,41.5 25.2,40.6 26.4,39.7 27.6,38.8 28.8,37.9 30.0,37.0 31.2,36.2 32.4,35.3 33.6,34.5 34.8,33.6 36.0,32.8 37.2,31.9 38.4,31.1 39.6,30.3 40.8,29.5 42.0,28.7 43.2,27.9 44.4,27.1 45.6,26.3 46.8,25.5 48.0,24.7 49.2,24.0 50.4,23.2 51.6,22.5 52.8,21.8 54.0,21.0 55.2,20.3 56.4,19.6 57.6,18.9 58.8,18.2 60.0,17.6 61.2,16.9 62.4,16.3 63.6,15.6 64.8,15.0 66.0,14.4 67.2,13.8 68.4,13.2 69.6,12.6 70.8,12.0 72.0,11.5 73.2,10.9 74.4,10.4 75.6,9.9 76.8,9.3 78.0,8.8 79.2,8.4 80.4,7.9 81.6,7.4 82.8,7.0 84.0,6.5 85.2,6.1 86.4,5.7 87.6,5.3 88.8,4.9 90.0,4.6 91.2,4.2 92.4,3.9 93.6,3.5 94.8,3.2 96.0,2.9 97.2,2.7 98.4,2.4 99.6,2.1 100.8,1.9 102.0,1.7 103.2,1.4 104.4,1.2 105.6,1.1 106.8,0.9 108.0,0.7 109.2,0.6 110.4,0.5 111.6,0.4 112.8,0.3 114.0,0.2 115.2,0.1 116.4,0.1 117.6,0.0 118.8,0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class SineInterpolator extends EasingInterpolator {

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public SineInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public SineInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    protected double baseCurve(double v) {
        return -Math.cos(v * (Math.PI / 2)) + 1;
    }
}