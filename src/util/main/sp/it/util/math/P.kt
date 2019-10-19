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

   fun distance(p: P) = distance(p.x, p.y)

   @JvmOverloads
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

   infix fun max(that: P) = P(x max that.x, y max that.y)

   infix fun min(that: P) = P(x min that.x, y min that.y)

   fun clip(minimum: P, maximum: P) = P(x.clip(minimum.x, maximum.x), y.clip(minimum.y, maximum.y))

}