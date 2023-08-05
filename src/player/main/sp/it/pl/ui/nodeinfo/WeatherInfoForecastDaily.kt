package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.main.IconWH
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.nodeinfo.WeatherInfo.Data.Daily
import sp.it.pl.ui.nodeinfo.WeatherInfo.Units
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.TextIcon
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onScrollOnlyScrollHorizontally
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.scrollPane

class WeatherInfoForecastDaily(units: Units, value: List<Daily>): HBox() {
   private val cellNames = CellNames()
   private val cellUnits = CellUnits(units)
   private val cellsPane = HBox()
   val units = v(units)
   val value = v(value)

   init {
      styleClass += "weather-info-forecast-daily"
      lay += cellNames
      lay += cellUnits
      lay += scrollPane {
         vbarPolicy = NEVER
         hbarPolicy = NEVER
         isFitToHeight = true
         onScrollOnlyScrollHorizontally()
         content = cellsPane
      }

      (this.units zip this.value) sync { (u, vs) ->
         cellUnits.units = u
         val cellsOld = cellsPane.children.filterIsInstance<Cell>()
         val cellsNew = vs.mapIndexed { i, v -> cellsOld.getOrNull(i)?.ifNotNull { it.value.value = v } ?: Cell(v) }
         val nonCells = cellsPane.children.filter { it !is Cell }
         cellsPane.children setTo (nonCells + cellsNew)
      }
   }

   class CellNames: VBox() {
      init {
         styleClass += "weather-info-forecast-daily-names"
         lay += label("Date") { minWidth = USE_PREF_SIZE }
         lay += Icon(TextIcon("")).apply { isFocusTraversable = false }
         lay += label("Temperature") { minWidth = USE_PREF_SIZE }
         lay += label("Clouds") { minWidth = USE_PREF_SIZE }
         lay += label("Rain") { minWidth = USE_PREF_SIZE }
         lay += label("Snow") { minWidth = USE_PREF_SIZE }
         lay += label("Wind") { minWidth = USE_PREF_SIZE }
         lay += label("Wind gusts") { minWidth = USE_PREF_SIZE }
         lay += label("Wind direction") { minWidth = USE_PREF_SIZE }
      }
   }

   class CellUnits(units: Units): VBox() {
      val dayL = label("d") { lay += this }
      val icon = Icon(TextIcon("")).apply { this@CellUnits.lay += this; isFocusTraversable = false }
      val tempL = label(units.d) { lay += this }
      val cloudsL = label("%") { lay += this }
      val rainL = label("mm") { lay += this }
      val snowL = label("mm") { lay += this }
      val windL = label(units.s) { lay += this }
      val windGustsL = label(units.s) { lay += this }
      val windDirL = label("°") { lay += this }

      var units = units
         set(value) {
            field = value
            tempL.text = value.d
            windL.text = value.s
            windGustsL.text = value.s
         }

      init {
         styleClass += "weather-info-forecast-daily-units"
      }
   }

   class Cell(value: Daily): VBox() {
      val dayL = label { lay += this }
      val icon = Icon().apply { this@Cell.lay += this; isFocusTraversable = false }
      val tempL = label { lay += this }
      val cloudsL = label { lay += this }
      val rainL = label { lay += this }
      val snowL = label { lay += this }
      val windL = label { lay += this }
      val windGustsL = label { lay += this }
      val windDirL = label { lay += this }
      val value = v(value).initSyncC {
         val l = APP.locale.value
         val at = it.dt.toInstant().atZone(ZoneId.systemDefault())
         dayL.text = at.dayOfMonth.toString()
         icon.icon(it.weather.firstOrNull()?.icon(true) ?: IconWH.NA)
         tempL.text = it.temp.max.net { "%.1f°".format(l, it) }
         cloudsL.text = "${it.clouds.toInt()}%"
         rainL.text = it.rain?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         snowL.text = it.snow?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         windL.text = "%.1f".format(it.wind_speed)
         windGustsL.text = it.wind_gust?.net { "%.1f".format(it) } ?: ""
         windDirL.text = it.wind_deg.toCD()
         pseudoClassToggle("first-day-of-month", at.dayOfMonth==1)
         pseudoClassToggle("last-day-of-month", at.dayOfMonth==YearMonth.from(at).atEndOfMonth().dayOfMonth)
      }

      init {
         styleClass += "weather-info-forecast-daily-cell"
      }

      data class Data(
         val at: ZonedDateTime,
         val temp: Double,
         val feels_like: Double,
         val clouds: Double,
         val rain: Double?,
         val snow: Double?,
         val wind: Double,
         val windGusts: Double?,
         val windDir: WindDir,
         val icon: GlyphIcons?
      )
   }

}