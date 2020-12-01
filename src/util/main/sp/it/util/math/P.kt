package sp.it.util.math

import kotlin.math.sqrt

/** Mutable point. */
data class P @JvmOverloads constructor(var x: Double = 0.0, var y: Double = 0.0) {

   var xy: P
      get() = this
      set(value) {
         x = value.x
         y = value.y
      }

   /** Mutates [x] and [y] each by the function. Mutable [map]. */
   inline fun modify(f: (Double) -> Double) {
      x = f(x)
      y = f(y)
   }

   /** Mutates [x] and [y] each by the corresponding values from the other point and by the function. Mutable [map]. */
   inline fun modify(p: P, f: (Double, Double) -> Double) {
      x = f(x, p.x)
      y = f(y, p.y)
   }

   /** @return point with [x] and [y] computed from this point by the specified function. */
   inline fun map(f: (Double) -> Double) = P(f(x), f(y))

   /** @return point with [x] and [y] computed from this and the other point by the specified function. */
   inline fun map(p: P, f: (Double, Double) -> Double) = P(f(x, p.x), f(y, p.y))

   infix fun distance(p: P) = distance(p.x, p.y)

   fun distance(x: Double = 0.0, y: Double = 0.0) = sqrt((x - this.x)*(x - this.x) + (y - this.y)*(y - this.y))

   operator fun plus(p: P) = P(x + p.x, y + p.y)

   operator fun plusAssign(p: P) {
      x += p.x
      y += p.y
   }

   operator fun minus(p: P) = P(x - p.x, y - p.y)

   operator fun minusAssign(bounds: P) {
      x -= bounds.x
      y -= bounds.y
   }

   operator fun times(p: P) = P(x*p.x, y*p.y)

   operator fun timesAssign(bounds: P) {
      x *= bounds.x
      y *= bounds.y
   }

   operator fun div(p: P) = P(x/p.x, y/p.y)

   operator fun divAssign(bounds: P) {
      x /= bounds.x
      y /= bounds.y
   }

   operator fun plus(n: Double) = P(x + n, y + n)

   operator fun plusAssign(n: Double) {
      x += n
      y += n
   }

   operator fun minus(n: Double) = P(x - n, y - n)

   operator fun minusAssign(n: Double) {
      x -= n
      y -= n
   }

   operator fun times(n: Double) = P(x*n, y*n)

   operator fun timesAssign(n: Double) {
      x *= n
      y *= n
   }

   operator fun div(p: Double) = P(x/p, y/p)

   operator fun divAssign(n: Double) {
      x /= n
      y /= n
   }

   infix fun max(that: P) = map(that) { a, b -> a max b }

   infix fun min(that: P) = map(that) { a, b -> a min b }

   fun clip(minimum: P, maximum: P) = P(x.clip(minimum.x, maximum.x), y.clip(minimum.y, maximum.y))

}