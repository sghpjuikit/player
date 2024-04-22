package sp.it.util.ui.image

/** Fitting of an image with original aspect ratio into an arbitrary rectangular area */
enum class FitFrom {
   /** Fits entire image into the area, image touching area from inside, resulting in potential empty space. */
   INSIDE,
   /** Cover entire area, image touching area from outside, resulting in potential parts of an image outside area. */
   OUTSIDE,
}