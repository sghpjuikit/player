package sp.it.pl.core

import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Paint
import javafx.scene.paint.RadialGradient
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import sp.it.util.dev.fail
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString

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
      val size = s.substringAfterLast(",").trim().toDoubleOrNull() ?: Font.getDefault().size
      val f = Font.font(name, weight, style, size)
      if (f.family==name) f else fail { "Not recognized font" }
   }.orMessage()
}