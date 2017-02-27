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
 * A cubic interpolator, simply defined by <code>f(x) = x<sup>5</sup></code>.
 * <p/>
 * This interpolator accelerates faster than the {@link QuarticInterpolator} and decelerates later.
 * <p/>
 * The following curve illustrates the interpolation. </p> <svg style="width:300px;" xmlns="http://www.w3.org/2000/svg"
 * viewBox="-2 -40 124 140"> <line style="stroke: rgb(187, 187, 187); stroke-width: 1px;" y2="60" y1="0" x2="0" x1="0"/>
 * <text style="font-size: 12px; fill: rgb(187, 187, 187);" y="6" x="2">x</text> <line style="stroke: rgb(187, 187,
 * 187); stroke-width: 1px;" y2="60" y1="60" x2="120" x1="0"/> <text style="font-size: 12px; fill: rgb(187, 187, 187);"
 * y="57" x="115">t</text> <path style="fill: rgba(255, 255, 255, 0);stroke: black;stroke-width: 2px;" d="M0,60
 * L1.2,57.1 2.4,54.2 3.6,51.5 4.8,48.9 6.0,46.4 7.2,44.0 8.4,41.7 9.6,39.5 10.8,37.4 12.0,35.4 13.2,33.5 14.4,31.7
 * 15.6,29.9 16.8,28.2 18.0,26.6 19.2,25.1 20.4,23.6 21.6,22.2 22.8,20.9 24.0,19.7 25.2,18.5 26.4,17.3 27.6,16.2
 * 28.8,15.2 30.0,14.2 31.2,13.3 32.4,12.4 33.6,11.6 34.8,10.8 36.0,10.1 37.2,9.4 38.4,8.7 39.6,8.1 40.8,7.5 42.0,7.0
 * 43.2,6.4 44.4,6.0 45.6,5.5 46.8,5.1 48.0,4.7 49.2,4.3 50.4,3.9 51.6,3.6 52.8,3.3 54.0,3.0 55.2,2.8 56.4,2.5 57.6,2.3
 * 58.8,2.1 60.0,1.9 61.2,1.7 62.4,1.5 63.6,1.4 64.8,1.2 66.0,1.1 67.2,1.0 68.4,0.9 69.6,0.8 70.8,0.7 72.0,0.6 73.2,0.5
 * 74.4,0.5 75.6,0.4 76.8,0.4 78.0,0.3 79.2,0.3 80.4,0.2 81.6,0.2 82.8,0.2 84.0,0.1 85.2,0.1 86.4,0.1 87.6,0.1 88.8,0.1
 * 90.0,0.1 91.2,0.0 92.4,0.0 93.6,0.0 94.8,0.0 96.0,0.0 97.2,0.0 98.4,0.0 99.6,0.0 100.8,0.0 102.0,0.0 103.2,0.0
 * 104.4,0.0 105.6,0.0 106.8,0.0 108.0,0.0 109.2,0.0 110.4,0.0 111.6,0.0 112.8,0.0 114.0,0.0 115.2,0.0 116.4,0.0
 * 117.6,0.0 118.8,0.0 120.0,0.0"/> </svg>
 * <p/>
 * The math in this class is taken from <a href="http://www.robertpenner.com/easing/">http://www.robertpenner.com/easing/</a>.
 *
 * @author Christian Schudt
 */
public class QuinticInterpolator extends EasingInterpolator {

	/**
	 * Default constructor. Initializes the interpolator with ease out mode.
	 */
	public QuinticInterpolator() {
		this(EasingMode.EASE_OUT);
	}

	/**
	 * Constructs the interpolator with a specific easing mode.
	 *
	 * @param easingMode The easing mode.
	 */
	public QuinticInterpolator(EasingMode easingMode) {
		super(easingMode);
	}

	@Override
	protected double baseCurve(double v) {
		return Math.pow(v, 5);
	}
}