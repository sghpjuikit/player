package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import javafx.scene.Cursor.HAND
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.main.IconWH
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.DoubleSpeed
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.nodeinfo.WeatherInfo.Data.Daily
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

class WeatherInfoForecastDaily(units: UnitsDto, value: List<Daily>): HBox() {
   val units = v(units)
   val value = v(value)
   private val cellNames = CellNames()
   private val cellUnits = CellUnits(this.units)
   private val cellsPane = HBox()

   init {
      styleClass += "weather-info-forecast-daily"
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

   class CellUnits(units: V<UnitsDto>): VBox() {
      val dayL = label("d")              { lay += this }
      val icon = Icon(TextIcon("")).also { lay += it }
      val tempL = label                  { lay += this; textProperty() syncFrom units.map { it.temperature.ui    }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(temperature = next(it.temperature)) } } }
      val cloudsL = label("%")           { lay += this }
      val rainL = label()                { lay += this; textProperty() syncFrom units.map { it.precipitation.ui  }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(precipitation = next(it.precipitation)) } } }
      val snowL = label()                { lay += this; textProperty() syncFrom units.map { it.precipitation.ui  }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(precipitation = next(it.precipitation)) } } }
      val windL = label()                { lay += this; textProperty() syncFrom units.map { it.windSpeed.speedUi }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(windSpeed = next(it.windSpeed)) } } }
      val windGustsL = label             { lay += this; textProperty() syncFrom units.map { it.windSpeed.speedUi }; cursor = HAND; onEventDown(MOUSE_CLICKED) { units.setValueOf { it.copy(windSpeed = next(it.windSpeed)) } } }
      val windDirL = label("°")          { lay += this }

      init {
         styleClass += "weather-info-forecast-daily-units"
      }
   }

   class Cell(value: Daily, units: UnitsDto): VBox() {
      val dayL = label { lay += this }
      val icon = Icon().apply { this@Cell.lay += this; isFocusTraversable = false }
      val tempL = label { lay += this }
      val cloudsL = label { lay += this }
      val rainL = label { lay += this }
      val snowL = label { lay += this }
      val windL = label { lay += this }
      val windGustsL = label { lay += this }
      val windDirL = label { lay += this }
      val value = v(value).initAttachC { updateUi() }
      val units = v(units).initAttachC { updateUi() }

      fun updateUi() {
         val v = value.value
         val u = units.value
         val l = APP.locale.value
         val at = v.dt.toInstant().atZone(ZoneId.systemDefault())
         dayL.text = at.monthValue.toString() + "." + at.dayOfMonth.toString() + "."
         icon.icon(v.weather.firstOrNull()?.icon(true) ?: IconWH.NA)
         tempL.text = v.temp.max.toUiValue(u, l)
         cloudsL.text = "${v.clouds.toInt()}%"
         rainL.text = v.rain?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         snowL.text = v.snow?.takeIf { it>0.0 }?.net { "%.1f".format(it) } ?: ""
         windL.text = v.wind_speed?.toUiValue(u, l) ?: ""
         windGustsL.text = v.wind_gust?.toUiValue(u, l) ?: ""
         windDirL.text = v.wind_deg.toCD()
         pseudoClassToggle("first-day-of-month", at.dayOfMonth==1)
         pseudoClassToggle("last-day-of-month", at.dayOfMonth==YearMonth.from(at).atEndOfMonth().dayOfMonth)
      }

      init {
         styleClass += "weather-info-forecast-daily-cell"
         updateUi()
      }

      data class Data(
         val at: ZonedDateTime,
         val temp: Double,
         val feels_like: Double,
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