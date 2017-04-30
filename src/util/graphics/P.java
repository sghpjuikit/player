package util.graphics;

import static java.lang.Math.sqrt;

/**
 * Mutable point.
 */
public class P {
	public double x = 0, y = 0;

	public P() {
		x = y = 0;
	}

	public P(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setXY(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distance(P p) {
		return distance(p.x, p.y);
	}

	public double distance(double x, double y) {
		return sqrt((x - this.x)*(x - this.x) + (y - this.y)*(y - this.y));
	}
}