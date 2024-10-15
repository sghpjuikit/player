package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import java.time.ZonedDateTime
import javafx.scene.Cursor.HAND
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.DoubleSpeed
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.DoubleTemp
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.nodeinfo.WeatherInfo.UnitsDto
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.TextIcon
import sp.it.util.access.V
import sp.it.util.access.Values.next
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.scrollPane

class WeatherInfoForecastHourly(units: UnitsDto, value: List<Cell.Data>): HBox() {
   val units = v(units)
   val value = v(value)
   private val cellNames = CellNames()
   private val cellUnits = CellUnits(this.units)
   private val cellsPane = HBox()

   init {
      styleClass += "weather-info-forecast-hourly"
      lay += cellNames
      lay += cellUnits
      lay += scrollPane {
         vbarPolicy = NEVER
         hbarPolicy = NEVER
         isFitToHeight = true
         content = cellsPane
      }

      (this.units zip this.value) sync { (u, vs) ->
         val cellsOld = cellsPane.children.filterIsInstance<Cell>()
         val cellsNew = vs.mapIndexed { i, v -> cellsOld.getOrNull(i)?.ifNotNull { it.value.value = v; it.units.value = u; it.updateUi() } ?: Cell(v, u) }
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

   class CellUnits(units: V<UnitsDto>): VBox() {
      val hourL = label("h")             { lay += this }
      val icon = Icon(TextIcon("")).also { lay += it }
      val tempL = label                  { lay += this; textProperty() syncFrom units.map { it.temperature.ui    }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(temperature = next(it.temperature)) } } }
      val cloudsL = label("%")           { lay += this }
      val rainL = label()                { lay += this; textProperty() syncFrom units.map { it.precipitation.ui  }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(precipitation = next(it.precipitation)) } } }
      val snowL = label()                { lay += this; textProperty() syncFrom units.map { it.precipitation.ui  }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(precipitation = next(it.precipitation)) } } }
      val windL = label()                { lay += this; textProperty() syncFrom units.map { it.windSpeed.speedUi }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(windSpeed = next(it.windSpeed)) } } }
      val windGustsL = label             { lay += this; textProperty() syncFrom units.map { it.windSpeed.speedUi }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(windSpeed = next(it.windSpeed)) } } }
      val windDirL = label("°")          { lay += this }

      init {
         styleClass += "weather-info-forecast-hourly-units"
      }
   }

   class Cell(value: Data, units: UnitsDto): VBox() {
      val hourL = label { lay += this }
      val icon = Icon().apply { this@Cell.lay += this }
      val tempL = label { lay += this }
      val cloudsL = label { lay += this }
      val rainL = label { lay += this }
      val snowL = label { lay += this }
      val windL = label { lay += this }
      val windGustsL = label { lay += this }
      val windDirL = label { lay += this }
      val value = v(value).initAttachC() { updateUi() }
      val units = v(units).initAttachC() { updateUi() }

      fun updateUi() {
         val v = value.value
         val u = units.value
         val l = APP.locale.value
         hourL.text = " ${"%02d".format(v.at.hour % 12 + 1)}${if (v.at.hour < 12) "AM" else "PM"} "
         icon.icon(v.icon)
         tempL.text = v.temp.toUiValue(u, l)
         cloudsL.text = "${v.clouds.toInt()}%"
         rainL.text = v.rain?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         snowL.text = v.snow?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         windL.text = v.wind?.toUiValue(u, l) ?: ""
         windGustsL.text = v.windGusts?.toUiValue(u, l) ?: ""
         windDirL.text = v.windDir.toCD()
         pseudoClassToggle("first-hour-of-day", v.at.hour==0)
         pseudoClassToggle("last-hour-of-day", v.at.hour==23)
      }

      init {
         styleClass += "weather-info-forecast-hourly-cell"
         updateUi()
      }

      @Suppress("PropertyName")
      data class Data(
         val at: ZonedDateTime,
         val temp: DoubleTemp,
         val feels_like: DoubleTemp,
         val clouds: Double,
         val rain: Double?,
         val snow: Double?,
         val wind: DoubleSpeed?,
         val windGusts: DoubleSpeed?,
         val windDir: WindDir,
         val icon: GlyphIcons?
      )
   }

}