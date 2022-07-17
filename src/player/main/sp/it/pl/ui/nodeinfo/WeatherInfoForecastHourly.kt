package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import java.time.ZonedDateTime
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
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

class WeatherInfoForecastHourly(units: Units, value: List<Cell.Data>): HBox() {
   private val cellNames = CellNames()
   private val cellUnits = CellUnits(units)
   private val cellsPane = HBox()
   val units = v(units)
   val value = v(value)

   init {
      styleClass += "weather-info-forecast-hourly"
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
         styleClass += "weather-info-forecast-hourly-names"
         lay += label("Time") { minWidth = USE_PREF_SIZE }
         lay += Icon(TextIcon(""))
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
      val hourL = label("h") { lay += this }
      val icon = Icon(TextIcon("")).apply { this@CellUnits.lay += this }
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
         styleClass += "weather-info-forecast-hourly-units"
      }
   }

   class Cell(value: Data): VBox() {
      val hourL = label { lay += this }
      val icon = Icon().apply { this@Cell.lay += this }
      val tempL = label { lay += this }
      val cloudsL = label { lay += this }
      val rainL = label { lay += this }
      val snowL = label { lay += this }
      val windL = label { lay += this }
      val windGustsL = label { lay += this }
      val windDirL = label { lay += this }
      val value = v(value).initSyncC {
         val l = APP.locale.value
         hourL.text = "${((it.at.hour%12) + 11)%12 + 1}${if (it.at.hour<12) "AM" else "PM"}"
         icon.icon(it.icon)
         tempL.text = it.temp.net { "%.1f°".format(l, it) }
         cloudsL.text = "${it.clouds.toInt()}%"
         rainL.text = it.rain?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         snowL.text = it.snow?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         windL.text = "%.1f".format(it.wind)
         windGustsL.text = it.windGusts?.net { "%.1f".format(it) } ?: ""
         windDirL.text = it.windDir.toCD()
         pseudoClassToggle("first-hour-of-day", it.at.hour==0)
         pseudoClassToggle("last-hour-of-day", it.at.hour==23)
      }

      init {
         styleClass += "weather-info-forecast-hourly-cell"
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