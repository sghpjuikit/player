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
 * This interpolator simulates a bouncing behavior.
 * <p/>
 * The following curve illustrates the interpolation. </p> <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg"
 * viewBox="-2 -40 124 140"> <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text> <line style="stroke: rgb(187, 187,
 * 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/> <text style="font-size: 12px; fill: rgb(187, 187, 187);"
 * y="57" x="115">t</text> <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;" d="M0,60
 * L1.2,60.0 2.4,59.8 3.6,59.6 4.8,59.3 6.0,58.9 7.2,58.4 8.4,57.8 9.6,57.1 10.8,56.3 12.0,55.5 13.2,54.5 14.4,53.5
 * 15.6,52.3 16.8,51.1 18.0,49.8 19.2,48.4 20.4,46.9 21.6,45.3 22.8,43.6 24.0,41.8 25.2,40.0 26.4,38.0 27.6,36.0
 * 28.8,33.9 30.0,31.6 31.2,29.3 32.4,26.9 33.6,24.4 34.8,21.8 36.0,19.2 37.2,16.4 38.4,13.5 39.6,10.6 40.8,7.5 42.0,4.4
 * 43.2,1.2 44.4,1.0 45.6,2.6 46.8,4.0 48.0,5.4 49.2,6.7 50.4,7.9 51.6,9.0 52.8,10.0 54.0,10.9 55.2,11.7 56.4,12.4
 * 57.6,13.1 58.8,13.6 60.0,14.1 61.2,14.4 62.4,14.7 63.6,14.9 64.8,15.0 66.0,15.0 67.2,14.9 68.4,14.7 69.6,14.5
 * 70.8,14.1 72.0,13.7 73.2,13.1 74.4,12.5 75.6,11.8 76.8,10.9 78.0,10.0 79.2,9.0 80.4,8.0 81.6,6.8 82.8,5.5 84.0,4.2
 * 85.2,2.7 86.4,1.2 87.6,0.2 88.8,1.0 90.0,1.6 91.2,2.2 92.4,2.7 93.6,3.1 94.8,3.4 96.0,3.6 97.2,3.7 98.4,3.7 99.6,3.7
 * 100.8,3.5 102.0,3.3 103.2,3.0 104.4,2.5 105.6,2.0 106.8,1.4 108.0,0.7 109.2,0.0 110.4,0.4 111.6,0.7 112.8,0.8
 * 114.0,0.9 115.2,0.9 116.4,0.8 117.6,0.6 118.8,0.4 120.0,0.0"/> </svg>
 * <p/>
 * The math in this class is taken from <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class BounceInterpolator extends EasingInterpolator {

	/**
	 * Default constructor. Initializes the interpolator with ease out mode.
	 */
	public BounceInterpolator() {
		this(EasingMode.EASE_OUT);
	}

	/**
	 * Constructs the interpolator with a specific easing mode.
	 *
	 * @param easingMode The easing mode.
	 */
	public BounceInterpolator(final EasingMode easingMode) {
		super(easingMode);
	}

	@Override
	protected double baseCurve(final double v) {
		for (double a = 0, b = 1; true; a += b, b /= 2) {
			if (v>=(7 - 4*a)/11) {
				return -Math.pow((11 - 6*a - 11*v)/4, 2) + Math.pow(b, 2);
			}
		}
	}
}