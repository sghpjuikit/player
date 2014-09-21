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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * An interpolator which is also known as the "bow" function. It interpolates shortly below 0 or above 1 and then bows back.
 * <p/>
 * The following curve illustrates the interpolation.
 * </p>
 * <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg" viewBox="-2 -40 124 140">
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text>
 * <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="57" x="115">t</text>
 * <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;"
 * d="M0,60 L1.2,57.2 2.4,54.5 3.6,51.9 4.8,49.3 6.0,46.8 7.2,44.4 8.4,42.1 9.6,39.8 10.8,37.6 12.0,35.5 13.2,33.4 14.4,31.4 15.6,29.5 16.8,27.6 18.0,25.8 19.2,24.0 20.4,22.4 21.6,20.7 22.8,19.2 24.0,17.7 25.2,16.2 26.4,14.8 27.6,13.5 28.8,12.2 30.0,11.0 31.2,9.8 32.4,8.7 33.6,7.6 34.8,6.5 36.0,5.6 37.2,4.6 38.4,3.8 39.6,2.9 40.8,2.1 42.0,1.4 43.2,0.7 44.4,0.0 45.6,-0.6 46.8,-1.2 48.0,-1.7 49.2,-2.2 50.4,-2.7 51.6,-3.2 52.8,-3.6 54.0,-3.9 55.2,-4.2 56.4,-4.5 57.6,-4.8 58.8,-5.1 60.0,-5.3 61.2,-5.4 62.4,-5.6 63.6,-5.7 64.8,-5.8 66.0,-5.9 67.2,-6.0 68.4,-6.0 69.6,-6.0 70.8,-6.0 72.0,-6.0 73.2,-5.9 74.4,-5.8 75.6,-5.8 76.8,-5.7 78.0,-5.6 79.2,-5.4 80.4,-5.3 81.6,-5.1 82.8,-5.0 84.0,-4.8 85.2,-4.6 86.4,-4.4 87.6,-4.3 88.8,-4.1 90.0,-3.8 91.2,-3.6 92.4,-3.4 93.6,-3.2 94.8,-3.0 96.0,-2.8 97.2,-2.6 98.4,-2.4 99.6,-2.2 100.8,-1.9 102.0,-1.8 103.2,-1.6 104.4,-1.4 105.6,-1.2 106.8,-1.0 108.0,-0.9 109.2,-0.7 110.4,-0.6 111.6,-0.4 112.8,-0.3 114.0,-0.2 115.2,-0.2 116.4,-0.1 117.6,-0.0 118.8,-0.0 120.0,0.0"/>
 * </svg>
 * <p/>
 * The math in this class is taken from
 * <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class BackInterpolator extends EasingInterpolator {

    private DoubleProperty amplitude = new SimpleDoubleProperty(this, "amplitude", 1.70158);

    /**
     * Default constructor. Initializes the interpolator with ease out mode.
     */
    public BackInterpolator() {
        this(EasingMode.EASE_OUT);
    }

    /**
     * Constructs the interpolator with a specific easing mode.
     *
     * @param easingMode The easing mode.
     */
    public BackInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    /**
     * Constructs the interpolator with a specific easing mode and an amplitude.
     *
     * @param easingMode The easing mode.
     * @param amplitude  The amplitude.
     */
    public BackInterpolator(EasingMode easingMode, double amplitude) {
        super(easingMode);
        this.amplitude.set(amplitude);
    }

    /**
     * Gets the amplitude. The default value is 1.70158.
     *
     * @return The property.
     * @see #getAmplitude()
     * @see #setAmplitude(double)
     */
    public DoubleProperty amplitudeProperty() {
        return amplitude;
    }

    /**
     * Gets the amplitude.
     *
     * @return The property.
     * @see #amplitudeProperty()
     */
    public double getAmplitude() {
        return amplitude.get();
    }

    /**
     * Sets the amplitude.
     *
     * @param amplitude The amplitude.
     * @see #amplitudeProperty()
     */
    public void setAmplitude(final double amplitude) {
        this.amplitude.set(amplitude);
    }

    @Override
    protected double baseCurve(double v) {
        double s = amplitude.get();
        return v * v * ((s + 1) * v - s);
    }
}