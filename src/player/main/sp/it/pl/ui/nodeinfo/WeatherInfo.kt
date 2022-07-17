package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.time.Instant
import java.time.ZoneId
import javafx.geometry.Insets
import javafx.geometry.Side.BOTTOM
import javafx.geometry.VPos.CENTER
import javafx.scene.control.ContextMenu
import javafx.scene.layout.HBox
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconWH
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.Dt
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.ref.LazyR
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.async.IO
import sp.it.util.async.flowTimer
import sp.it.util.async.launch
import sp.it.util.async.runFX
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.dev.fail
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.toSubscription
import sp.it.util.system.browse
import sp.it.util.text.capital
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.times
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.uri

/** Basic display for song album information. */
class WeatherInfo: HBox(15.0) {

   private val caretIcon = Icon(IconFA.CARET_DOWN).onClickDo { openCaret()  }
   private val mainIcon = Icon().apply { isMouseTransparent = true; isFocusTraversable = false }.size(3.em)
   private val tempL = label { styleClass += "h3" }
   private val descL = label { styleClass += "h3p-bottom" }
   private val windIcon = Icon(IconWH.WINDY)
   private val windL = label()
   private val humidityL = label()
   private val dewL = label()
   private val pressureL = label()
   private val uvL = label()
   private val visL = label()
   private val http by lazy { HttpClient(CIO) }
   private val dataKey = "widgets.weather.data.last"

   val latitude = vn<Double>(null)
   val longitude = vn<Double>(null)
   val apiKey = vn<String>(null)
   val units = v(Units.METRIC)
   val data = vn<Data>(null)

   @OptIn(DelicateCoroutinesApi::class)
   private val monitor = Subscribed {
      flowTimer(0, 600*1000).map { refresh() }.flowOn(IO).launchIn(GlobalScope).toSubscription()
   }

   init {
      padding = Insets(10.0)
      lay += vBox {
         alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty()
         prefSize = -1 x -1
         lay += hBox {
            styleClass += "h3p-up"
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * CENTER }
            lay += hBox {
               lay += caretIcon
            }
            lay += mainIcon
            lay += tempL
         }
         lay += descL
         lay += hBox {
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * CENTER }
            lay += Icon(IconWH.WINDY).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += windL
            lay += Icon(IconWH.HUMIDITY).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += humidityL
            lay += Icon(IconFA.CLOCK_ALT).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += dewL
         }
         lay += hBox {
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * CENTER }
            lay += Icon(IconWH.BAROMETER).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += pressureL
            lay += Icon(IconWH.HOT).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += uvL
            lay += Icon(IconFA.EYE).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += visL
         }
      }

      // keep updated content
      sceneProperty().attach { monitor.subscribe(it!=null) } on onNodeDispose

      apiKey attach { IO.launch { refresh() } }
      latitude attach { updateUi() }
      longitude attach { updateUi() }
      units attach { updateUi() }
      data sync { updateUi() }
   }

   private suspend fun refresh() {
      val dataOld = APP.configuration.rawGet(dataKey)?.val1?.parseToJson<Data?>()?.takeIf { it.isActual(this) }
      val dataNew = when {
         latitude.value==null || latitude.value==null || latitude.value==null -> null
         dataOld!=null -> dataOld
         else -> {
            val link = "https://api.openweathermap.org/data/2.5/onecall?lat=${latitude.value}&lon=${longitude.value}&units=${units.value.name.lowercase()}&exclude=minutely,alerts&appid=${apiKey.value}"
            val dataRaw = http.get(link).bodyAsText()
            APP.configuration.rawAdd(dataKey, PropVal1(dataRaw))
            dataRaw.parseToJson<Data>()
         }
      }
      runFX { data.value = dataNew }
   }

   private fun updateUi(): Unit = data.value.net {
      mainIcon.icon(it?.current?.let { it.weather.firstOrNull()?.icon(it.isDay()) } ?: IconWH.NA)
      val l = APP.locale.value
      val d = units.value.d
      val s = units.value.s
      tempL.text = it?.current?.temp?.net { "%.1f%s".format(l, it, d) } ?: "n/a"
      descL.text = it?.current?.net { "Feels like %.1f%s%s".format(l, it.feels_like, d, it.weather.joinToString { ". " + it.description.capital() }) } ?: "n/a"
      windL.text = it?.current?.let { "%d%s %s°".format(l, it.wind_speed.toInt(), s, it.wind_deg.toCD()) } ?: "n/a"
      humidityL.text = it?.current?.humidity?.toInt()?.net { "$it%" } ?: "n/a"
      dewL.text = it?.current?.dew_point?.net { "%.1f%s".format(l, it, d) } ?: "n/a"
      pressureL.text = it?.current?.pressure?.toInt()?.net { "${it}hPa" } ?: "n/a"
      uvL.text = it?.current?.uvi?.toUi() ?: "n/a"
      visL.text = it?.current?.visibility?.toInt()?.net { "%d%s".format(l, if (it>999) it/1000 else it, if (it>999) "km" else "m") } ?: "n/a"
      forecastHourlyPopupContent?.value?.value = computeForecastH()
      forecastHourlyPopupContent?.units?.value = units.value
      forecastDailyPopupContent?.value?.value = computeForecastD()
      forecastDailyPopupContent?.units?.value = units.value
   }

   private fun openSettings() {
      object: ConfigurableBase<Any?>() {
         val latitude by cvn(this@WeatherInfo.latitude.value).def(name = "Latitude", info = "Latitude of the area for weather information")
         val longitude by cvn(this@WeatherInfo.longitude.value).def(name = "Longitude", info = "Longitude of the area for weather information")
         val apiKey by cvn(this@WeatherInfo.apiKey.value).def(name = "ApiKey", info = "API key generated for your account at https://openweathermap.org/")
         val units by cv(this@WeatherInfo.units.value).def(name = "Units", info = "Unit system for ui")
         init {
            this::latitude.getDelegateConfig().addConstraints(ObjectNonNull)
            this::longitude.getDelegateConfig().addConstraints(ObjectNonNull)
            this::apiKey.getDelegateConfig().addConstraints(ObjectNonNull)
         }
      }.configure("Set up weather") {
         latitude.value = it.latitude.value
         longitude.value = it.longitude.value
         apiKey.value = it.apiKey.value
         units.value = it.units.value
         IO.launch { refresh() }
      }
   }


   private var forecastHourlyPopupContent: WeatherInfoForecastHourly? = null
   private var forecastDailyPopupContent: WeatherInfoForecastDaily? = null
   private val forecastHourlyPopup = LazyR {
      forecastHourlyPopupContent = WeatherInfoForecastHourly(units.value, computeForecastH())
      forecastDailyPopupContent = WeatherInfoForecastDaily(units.value, computeForecastD())
      PopWindow().apply {
         title.value = "Weather forecast"
         content.value = vBox {
            styleClass += "weather-info-forecast"
            prefWidth = 800.emScaled

            lay += label("Hourly forecast")
            lay += forecastHourlyPopupContent!!
            lay += label("Daily forecast")
            lay += forecastDailyPopupContent!!
         }
      }
   }

   fun computeForecastH() =
      data.value?.hourly.orEmpty().map {
         WeatherInfoForecastHourly.Cell.Data(
            it.dt.toInstant().atZone(data.value?.timezone),
            it.temp,
            it.feels_like,
            it.clouds, it.rain, it.snow, it.wind_speed, it.wind_gust, it.wind_deg,
            it.weather.firstOrNull()?.icon(it.dt.dt in data.value!!.current.sunrise..data.value!!.current.sunset),
         )
      }

   fun computeForecastD() = data.value?.daily.orEmpty()

   private fun openForecast() {
      val wasShowing = forecastHourlyPopup.orNull?.isShowing==true
      if (wasShowing) forecastHourlyPopup.orNull?.hide()
      else forecastHourlyPopup.get().show(DOWN_CENTER(caretIcon))
   }

   private fun openWindy() {
      val (lat,lon) = latitude.value to longitude.value
      if (lat!=null && lon!=null) uri("https://www.windy.com/%.3f/%.3f".format(lat, lon)).browse()
   }

   private fun openForecastMeteor() {
      PopWindow().apply {
         title.value = "Meteor shower forecast"
         content.value = WeatherInfoForecastMeteors()
      }.show(DOWN_CENTER(caretIcon))
   }

   fun openCaret() {
         ContextMenu().dsl {
            item("Settings") { openSettings() }
            item("Open hourly/daily forecast ()") { openForecast() }
            item("Open meteor shower forecast") { openForecastMeteor() }
            item("Open in windy.com") { openWindy() }
         }.show(
            caretIcon, BOTTOM, 0.0, 0.0
         )
   }

   enum class Units(val d: String, val s: String) { METRIC("°C", "m/s"), IMPERIAL("°F", "mph") }

   /** https://openweathermap.org/api/one-call-api */
   data class Data(
      val lat: Double,
      val lon: Double,
      val timezone: ZoneId,
      val timezone_offset: Long,
      val current: Current,
      val hourly: List<Hourly>,
      val daily: List<Daily>,
   ) {
      fun isActual(info: WeatherInfo) = current.dt.toInstant().isOlderThan1Hour() && lat!=info.latitude.value && lon!=info.longitude.value

      data class Current(
         val dt: Dt,
         val sunrise: Long,
         val sunset: Long,
         val temp: Double,
         val feels_like: Double,
         val pressure: Double,
         val humidity: Double,
         val dew_point: Double,
         val clouds: Double,
         val uvi: Double,
         val visibility: Double,
         val wind_speed: Double,
         val wind_gust: Double?,
         val wind_deg: WindDir,
         val weather: List<WeatherGroup>,
      ) {
         fun isDay() = dt.dt in sunrise..sunset
      }
      data class Hourly(
         val dt: Dt,
         val temp: Double,
         val feels_like: Double,
         val pressure: Double,
         val humidity: Double,
         val dew_point: Double,
         val clouds: Double,
         val visibility: Double,
         val wind_speed: Double,
         val wind_gust: Double?,
         val wind_deg: WindDir,
         val rain: Double?,
         val snow: Double?,
         val weather: List<WeatherGroup>,
      )
      data class Daily(
         val dt: Dt,
         val sunrise: Long,
         val sunset: Long,
         val moonrise: Long,
         val moonset: Long,
         val moon_phase: Double,
         val temp: Temp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: Double,
         val wind_speed: Double,
         val wind_gust: Double?,
         val wind_deg: WindDir,
         val clouds: Double,
         val rain: Double?,
         val snow: Double?,
         val weather: List<WeatherGroup>,

      ) {
         data class Temp(
            val day: Double,
            val min: Double,
            val max: Double,
            val night: Double,
            val eve: Double,
            val morn: Double
         )
      }
      data class WeatherGroup(
         val id: Long,
         val main: String,
         val description: String,
         val icon: String
      ) {
         fun icon(day: Boolean): GlyphIcons = when (id) {
            200L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            201L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            202L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            210L -> if (day) IconWH.DAY_LIGHTNING else IconWH.NIGHT_ALT_LIGHTNING
            211L -> if (day) IconWH.DAY_LIGHTNING else IconWH.NIGHT_ALT_LIGHTNING
            212L -> if (day) IconWH.DAY_LIGHTNING else IconWH.NIGHT_ALT_LIGHTNING
            221L -> if (day) IconWH.DAY_LIGHTNING else IconWH.NIGHT_ALT_LIGHTNING
            230L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            231L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            232L -> if (day) IconWH.DAY_THUNDERSTORM else IconWH.NIGHT_ALT_THUNDERSTORM
            300L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            301L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            302L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            310L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            311L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            312L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            313L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            314L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            321L -> if (day) IconWH.DAY_SPRINKLE else IconWH.NIGHT_ALT_SPRINKLE
            500L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            501L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            502L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            503L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            504L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            511L -> if (day) IconWH.DAY_HAIL else IconWH.NIGHT_ALT_HAIL
            520L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            521L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            522L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            531L -> if (day) IconWH.DAY_RAIN else IconWH.NIGHT_ALT_RAIN
            600L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            601L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            602L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            611L -> if (day) IconWH.DAY_HAIL else IconWH.NIGHT_ALT_HAIL
            612L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            613L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            615L -> if (day) IconWH.DAY_RAIN_MIX else IconWH.NIGHT_ALT_RAIN_MIX
            616L -> if (day) IconWH.DAY_RAIN_MIX else IconWH.NIGHT_ALT_RAIN_MIX
            620L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            621L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            622L -> if (day) IconWH.DAY_SNOW else IconWH.NIGHT_ALT_SNOW
            701L -> if (day) IconWH.DAY_FOG else IconWH.NIGHT_FOG
            711L -> IconWH.SMOKE
            721L -> IconWH.DAY_HAZE
            731L -> IconWH.DUST
            741L -> if (day) IconWH.DAY_FOG else IconWH.NIGHT_FOG
            751L -> IconWH.SANDSTORM
            761L -> IconWH.DUST
            762L -> IconWH.VOLCANO
            771L -> IconWH.WINDY
            781L -> IconWH.TORNADO
            800L -> if (day) IconWH.DAY_SUNNY else IconWH.NIGHT_CLEAR
            801L -> if (day) IconWH.DAY_CLOUDY else IconWH.NIGHT_ALT_CLOUDY
            802L -> if (day) IconWH.DAY_CLOUDY_HIGH else IconWH.NIGHT_ALT_CLOUDY_HIGH
            803L -> IconWH.CLOUDY
            804L -> IconWH.CLOUDY
            else -> IconWH.NA
         }
      }
   }

   companion object {
      fun String.parseToJson() = APP.serializerJson.json.ast(this).orThrow
      inline fun <reified T> String.parseToJson(): T? = APP.serializerJson.json.fromJson<T>(this).orNull()
      fun Instant.isOlderThan1Hour() = Instant.now().isBefore(plusSeconds(3500))

      object Types {
         @JvmInline value class Dt(val dt: Long) {
            fun toInstant(): Instant = Instant.ofEpochMilli(dt*1000)
         }
         @JvmInline value class WindDir(val deg: Double) {
            /** @return cardinal direction of the [deg] value, e.g.: SE */
            fun toCD(): String = when (deg.toInt()) {
               in  78..123 -> "N"
               in  23..77  -> "NE"
               in   0..22  ->  "E"
               in 338..360 ->  "E"
               in 294..337 -> "SE"
               in 249..293 -> "S"
               in 204..248 -> "SW"
               in 169..203 ->  "W"
               in 124..168 -> "NW"
               else -> fail { "invalid wind direction degree value=${deg}" }
            }
         }
      }
   }
}