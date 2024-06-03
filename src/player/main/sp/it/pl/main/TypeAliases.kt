package sp.it.pl.main

import org.jetbrains.annotations.Range

typealias Bool = Boolean

typealias Byte01 = @Range(from = 0L, to = 1L) Byte
typealias Short01 = @Range(from = 0L, to = 1L) Short
typealias Int01 = @Range(from = 0L, to = 1L) Int
typealias Long01 = @Range(from = 0L, to = 1L) Long
typealias Float01 = @Range(from = 0L, to = 1L) Float
typealias Double01 = @Range(from = 0L, to = 1L) Double
typealias UByte01 = @Range(from = 0L, to = 1L) UByte
typealias UShort01 = @Range(from = 0L, to = 1L) UShort
typealias UInt01 = @Range(from = 0L, to = 1L) UInt
typealias ULong01 = @Range(from = 0L, to = 1L) ULong

typealias Glyph  = de.jensd.fx.glyphs.GlyphIcons
typealias IconTx = sp.it.pl.ui.objects.icon.TextIcon
typealias IconUN = sp.it.pl.ui.objects.icon.UnicodeIcon
typealias IconFA = de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
typealias IconMD = de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
typealias IconMA = de.jensd.fx.glyphs.materialicons.MaterialIcon
typealias IconOC = de.jensd.fx.glyphs.octicons.OctIcon
typealias IconWH = de.jensd.fx.glyphs.weathericons.WeatherIcon

typealias Key = javafx.scene.input.KeyCode

typealias KTypeArg = kotlin.reflect.KTypeProjection
typealias KTypeParam = kotlin.reflect.KTypeParameter

typealias F<I,O> = (I) -> O