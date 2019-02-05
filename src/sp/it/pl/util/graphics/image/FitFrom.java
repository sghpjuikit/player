package sp.it.pl.util.graphics.image;

/** Fitting of an image with original aspect ration into an arbitrary rectangular area. */
public enum FitFrom {
	/** Fits entire image into the area, image touching area from inside, resulting in potential empty space. */
	INSIDE,
	/** Cover entire area, image touching area from outside, resulting in potential parts of an image outside area. */
	OUTSIDE
}