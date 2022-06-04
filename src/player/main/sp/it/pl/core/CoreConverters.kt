package sp.it.pl.core

import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Paint
import javafx.scene.paint.RadialGradient
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.transform.Transform
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString
import sp.it.util.text.split2
import sp.it.util.text.split3
import sp.it.util.text.splitTrimmed

object ConverterColor: ConverterString<Color> {
   override fun toS(o: Color) = o.toString()
   override fun ofS(s: String) = runTry { Color.valueOf(s) }.orMessage()
}

object ConverterRadialGradient: ConverterString<RadialGradient> {
   override fun toS(o: RadialGradient) = o.toString()
   override fun ofS(s: String) = runTry { RadialGradient.valueOf(s) }.orMessage()
}

object ConverterLinearGradient: ConverterString<LinearGradient> {
   override fun toS(o: LinearGradient) = o.toString()
   override fun ofS(s: String) = runTry { LinearGradient.valueOf(s) }.orMessage()
}

object ConverterImagePattern: ConverterString<ImagePattern> {
   override fun toS(o: ImagePattern) = "image(${o.image.url} ${o.x} ${o.y} ${o.width} ${o.height} ${o.isProportional})"
   override fun ofS(s: String) = runTry {
      val values = s.substringAfter("image(").substringBeforeLast(")").split(" ")
      ImagePattern(Image(values[0], false), values[1].toDouble(), values[2].toDouble(), values[3].toDouble(), values[4].toDouble(), values[5].toBooleanStrict())
   }.orMessage()
}

object ConverterPaint: ConverterString<Paint> {
   override fun toS(o: Paint) = when (o) {
      is Color -> ConverterColor.toS(o)
      is RadialGradient -> ConverterRadialGradient.toS(o)
      is LinearGradient -> ConverterLinearGradient.toS(o)
      is ImagePattern -> ConverterImagePattern.toS(o)
      else -> ConverterColor.toS(Color.TRANSPARENT)
   }
   override fun ofS(s: String) = when {
      s.startsWith("linear-gradient(") -> ConverterLinearGradient.ofS(s)
      s.startsWith("radial-gradient(") -> ConverterRadialGradient.ofS(s)
      s.startsWith("image(") -> ConverterImagePattern.ofS(s)
      else -> ConverterColor.ofS(s)
   }
}

object ConverterFont: ConverterString<Font> {
   override fun toS(o: Font) = "${o.family}, ${o.style}, ${o.size}"
   override fun ofS(s: String) = runTry {
      val i = s.indexOf(',')
      val name = s.substring(0, i)
      val style = if (s.lowercase().contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
      val weight = if (s.lowercase().contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
      val size = s.substringAfterLast(",").trim().toDoubleOrNull() ?: fail { "Unrecognized font size" }
      val f = Font.font(name, weight, style, size)
      if (f.family==name) f else fail { "Unrecognized font family" }
   }.orMessage()
}

object ConverterBoundingBox: ConverterString<BoundingBox> {
   override fun toS(o: BoundingBox) = "[${o.minX}, ${o.minY}, ${o.minZ}, ${o.width}, ${o.height}, ${o.depth}, ${o.maxX}, ${o.maxY}, ${o.maxZ}]"
   override fun ofS(s: String) = runTry { s.substringAfter("[").substringBefore("]").split(",").map { it.trim().toDouble() }.let { BoundingBox(it[0], it[1], it[2], it[3], it[4], it[5]) } }.orMessage()
}

object ConverterBounds: ConverterString<Bounds> {
   override fun toS(o: Bounds) = "[${o.minX}, ${o.minY}, ${o.minZ}, ${o.width}, ${o.height}, ${o.depth}, ${o.maxX}, ${o.maxY}, ${o.maxZ}]"
   override fun ofS(s: String) = runTry { s.substringAfter("[").substringBefore("]").split(",").map { it.trim().toDouble() }.let { BoundingBox(it[0], it[1], it[2], it[3], it[4], it[5]) } }.orMessage()
}

object ConverterPoint2D: ConverterString<Point2D> {
   override fun toS(o: Point2D) = "[${o.x}, ${o.y}]"
   override fun ofS(s: String) = runTry { s.substringAfter("[").substringBefore("]").split2(",").let { (x, y) -> Point2D(x.trim().toDouble(), y.trim().toDouble()) } }.orMessage()
}

object ConverterPoint3D: ConverterString<Point3D> {
   override fun toS(o: Point3D) = "[${o.x}, ${o.y}, ${o.z}]"
   override fun ofS(s: String) = runTry { s.substringAfter("[").substringBefore("]").split3(",").let { (x, y, z) -> Point3D(x.trim().toDouble(), y.trim().toDouble(), z.trim().toDouble()) } }.orMessage()
}

object ConverterInsets: ConverterString<Insets> {
   override fun toS(o: Insets) = if (o.top==o.right && o.top==o.bottom && o.top==o.left) "${o.top}" else "${o.top} ${o.right} ${o.bottom} ${o.left}"
   override fun ofS(s: String) = null
      ?: s.toDoubleOrNull()?.net { Try.ok(Insets(it)) }
      ?: s.splitTrimmed(" ").mapNotNull { it.toDoubleOrNull() }.takeIf { it.size==4 }?.net { Try.ok(Insets(it[0], it[1], it[2], it[3])) }
      ?: Try.error("'$s' is not a valid padding value")
}