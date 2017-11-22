package sp.it.pl.util.demo.particleanim;

public class Vector2D {

	public double x;
	public double y;

	public Vector2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double magnitude() {
		return Math.sqrt(x*x + y*y);
	}

	public void add(Vector2D v) {
		x += v.x;
		y += v.y;
	}

	public void add(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public void multiply(double n) {
		x *= n;
		y *= n;
	}

	public void div(double n) {
		x /= n;
		y /= n;
	}

	public void normalize() {
		double m = magnitude();
		if (m!=0 && m!=1) {
			div(m);
		}
	}

	public void limit(double max) {
		if (magnitude()>max) {
			normalize();
			multiply(max);
		}
	}

	public double angle() {
		double angle = Math.atan2(-y, x);
		return -1*angle;
	}

	static public Vector2D subtract(Vector2D v1, Vector2D v2) {
		return new Vector2D(v1.x - v2.x, v1.y - v2.y);
	}

}