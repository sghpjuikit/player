package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javafx.geometry.Insets
import javafx.geometry.Side.BOTTOM
import javafx.geometry.VPos.CENTER
import javafx.scene.control.ContextMenu
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import mu.KLogger
import mu.KLogging
import sp.it.pl.core.bodyAsJs
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconWH
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.DoubleSpeed
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.DoubleTemp
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.Dt
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.Dts
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.nodeinfo.WeatherInfo.Units2.DistUnit.IMPERIAL
import sp.it.pl.ui.nodeinfo.WeatherInfo.Units2.DistUnit.METRIC
import sp.it.pl.ui.nodeinfo.WeatherInfo.Units2.TemperatureUnit.CELSIUS
import sp.it.pl.ui.nodeinfo.WeatherInfo.Units2.TemperatureUnit.FAHRENHEIT
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.async.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.flowTimer
import sp.it.util.async.coroutine.launch
import sp.it.util.async.coroutine.toSubscription
import sp.it.util.async.invoke
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.cNest
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.conf.noPersist
import sp.it.util.conf.noUi
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.Json
import sp.it.util.file.json.toCompactS
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncFrom
import sp.it.util.system.browse
import sp.it.util.text.capital
import sp.it.util.time.isYoungerThanFx
import sp.it.util.type.type
import sp.it.util.ui.displayed
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.times
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.hours
import sp.it.util.units.uri
import sp.it.util.units.version
import sp.it.util.units.year

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
   private val http = APP.http.client
   private val dataKey = "widgets.weather.data.last"
   private var dataPersistable: JsValue?
      get() = APP.configuration.rawGet(dataKey)
      set(value) = APP.configuration.rawAdd(dataKey, value!!).toUnit()

   val latitude = vn<Double>(null)
   val longitude = vn<Double>(null)
   val apiKey = vn<String>(null)
   val units = v(UnitsDto())
   val data = vn<Data>(null)

   private val monitor = Subscribed {
      launch(VT) { flowTimer(0, 10*60*1000).collect { refresh() } }.toSubscription()
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

         lay += stackPane {
            this@vBox.heightProperty() map { it.toDouble() > forecastMinRequiredHeight } attach {
               if (it && lay.isEmpty()) lay += forecastContent
               else if (!it && !lay.isEmpty()) lay.clear()
            }
         }
      }

      displayed sync { monitor.subscribe(it) } on onNodeDispose
      onNodeDispose += { monitor.unsubscribe() }

      apiKey attach {  refresh() }
      latitude attach { updateUi() }
      longitude attach { updateUi() }
      units attach { updateUi() }
      APP.locale attach { updateUi() }
      data sync { updateUi() }
   }

   private fun refresh() {
      launch(VT) {
         val dataOld = dataPersistable?.asJson<Data?>()?.takeIf { it.isActual(this) }
         val dataNew = when {
            latitude.value==null || latitude.value==null || apiKey.value==null -> null
            dataOld!=null -> dataOld
            else -> {
               @Suppress("UNUSED_VARIABLE")
               val linkOpenweathermap = "https://api.openweathermap.org/data/2.5/onecall?lat=${latitude.value}&lon=${longitude.value}&units=metric&exclude=minutely,alerts&appid=${apiKey.value}"
               val linkVisualcrossing = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/${latitude.value},${longitude.value}?unitGroup=metric&include=days%2Chours%2Ccurrent&iconSet=icons2&key=${apiKey.value}&contentType=json"
               val link = linkVisualcrossing
               logger.info { "Obtaining weather data $link" }
               val res = runTry { http.get(link) }
               dataPersistable = when (res) {
                  is Try.Error -> {
                     logger.error(res.value) { "Failed to obtain weather data" }
                     null
                  }
                  is Try.Ok -> when {
                     res.value.status.isSuccess() ->
                        APP.serializerJson.json.toJsonValue<Data>(res.value.bodyAsJs().asJson<DataImplVisualCrossing>()?.convert())
                     else -> {
                        logger.error { "Failed to obtain weather data, status=${res.value.status}" }
                        null
                     }
                  }
               }
               dataPersistable?.asJson<Data>()
            }
         }
         FX {
            data.value = dataNew
         }
      }
   }

   private fun updateUi(): Unit = data.value.net {
      mainIcon.icon(it?.current?.let { it.weather.firstOrNull()?.icon(it.isDay()) } ?: IconWH.NA)
      val l = APP.locale.value
      val u = units.value
      tempL.text = it?.current?.temp?.toUi(u, l) ?: "n/a"
      descL.text = it?.current?.net { "Feels like %s%s".format(it.feels_like.toUi(u, l), it.weather.joinToString { ". " + it.description.capital() }) } ?: "n/a"
      windL.text = it?.current?.let { "%s %s°".format(l, it.wind_speed?.toUi(u, l) ?: "", it.wind_deg.toCD()) } ?: "n/a"
      humidityL.text = it?.current?.humidity?.toInt()?.net { "$it%" } ?: "n/a"
      dewL.text = it?.current?.dew_point?.toUi(u, l) ?: "n/a"
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
         val apiKey by cvn(this@WeatherInfo.apiKey.value).def(name = "ApiKey", info = "API key generated for your account at https://www.visualcrossing.com/account#")

         val unitTemperature by cv(this@WeatherInfo.units.value.temperature).noUi()
         val unitWindSpeed by cv(this@WeatherInfo.units.value.windSpeed).noUi()
         val unitPrecipitation by cv(this@WeatherInfo.units.value.precipitation).noUi()
         val unitPressure by cv(this@WeatherInfo.units.value.pressure).noUi()
         val unitHumidity by cv(this@WeatherInfo.units.value.humidity).noUi()
         val unitVisibility by cv(this@WeatherInfo.units.value.visibility).noUi()
         val units by cNest(
            ListConfigurable.heterogeneous(
               ::unitTemperature.getDelegateConfig(),
               ::unitWindSpeed.getDelegateConfig(),
               ::unitPrecipitation.getDelegateConfig(),
               ::unitPressure.getDelegateConfig(),
               ::unitHumidity.getDelegateConfig(),
               ::unitVisibility.getDelegateConfig(),
            )
         ).noPersist().def(
            name = "Units"
         )

         init {
            this::latitude.getDelegateConfig().addConstraints(ObjectNonNull)
            this::longitude.getDelegateConfig().addConstraints(ObjectNonNull)
            this::apiKey.getDelegateConfig().addConstraints(ObjectNonNull)
         }
      }.configure("Set up weather") {
         latitude.value = it.latitude.value
         longitude.value = it.longitude.value
         apiKey.value = it.apiKey.value
         units.value = UnitsDto(
            it.unitTemperature.value,
            it.unitWindSpeed.value,
            it.unitPrecipitation.value,
            it.unitPressure.value,
            it.unitHumidity.value,
            it.unitVisibility.value,
         )
         refresh()
      }
   }


   private val forecastMinRequiredHeight = 800
   private var forecastHourlyPopupContent: WeatherInfoForecastHourly? = null
   private var forecastDailyPopupContent: WeatherInfoForecastDaily? = null
   private val forecastContent by lazy {
      forecastHourlyPopupContent = WeatherInfoForecastHourly(units.value, computeForecastH()).apply { units syncBiFrom this@WeatherInfo.units }
      forecastDailyPopupContent = WeatherInfoForecastDaily(units.value, computeForecastD()).apply { units syncBiFrom this@WeatherInfo.units }
      vBox {
         styleClass += "weather-info-forecast"
         prefWidth = 800.emScaled

         lay += label("Hourly forecast")
         lay += forecastHourlyPopupContent!!
         lay += label("Daily forecast")
         lay += forecastDailyPopupContent!!
      }
   }
   private fun computeForecastPopup(): PopWindow = popWindow {
      title.value = "Weather forecast"
      content.value = forecastContent
   }

   private fun computeForecastH() =
      data.value?.hourly.orEmpty().map {
         WeatherInfoForecastHourly.Cell.Data(
            it.dt.toInstant().atZone(data.value?.timezone),
            it.temp,
            it.feels_like,
            it.clouds, it.rain?.`1h`, it.snow?.`1h`, it.wind_speed, it.wind_gust, it.wind_deg,
            it.weather.firstOrNull()?.icon(it.dt.dt in data.value!!.current.sunrise.dt..data.value!!.current.sunset.dt),
         )
      }

   private fun computeForecastD() = data.value?.daily.orEmpty()

   private fun openForecast() {
      val wasShowing = forecastHourlyPopupContent?.scene?.window?.isShowing==true
      if (wasShowing) forecastHourlyPopupContent?.scene?.window?.requestFocus()
      else showForecast()
   }

   private fun showForecast() {
      val r = children.getOrNull(0)?.asIf<VBox>()
      if (r?.height ?: 0.0 < forecastMinRequiredHeight) computeForecastPopup().show(DOWN_CENTER(caretIcon))
   }

   private fun openWindy() {
      val (lat,lon) = latitude.value to longitude.value
      if (lat!=null && lon!=null) uri("https://www.windy.com/%.3f/%.3f".format(lat, lon)).browse()
   }

   private fun openForecastMeteor() {
      popWindow {
         title.value = "Meteor shower forecast"
         content.value = WeatherInfoForecastMeteors()
      }.show(DOWN_CENTER(caretIcon))
   }

   fun openCaret() {
         ContextMenu().dsl {
            item("Settings") { openSettings() }
            item("Open hourly/daily forecast") { openForecast() }
            item("Open meteor shower forecast") { openForecastMeteor() }
            item("Open in windy.com") { openWindy() }
         }.show(
            caretIcon, BOTTOM, 0.0, 0.0
         )
   }

   enum class Units(val d: String, val s: String) { METRIC("°C", "m/s"), IMPERIAL("°F", "mph") }
   object Units2 {
      enum class TemperatureUnit(val ui: String) { CELSIUS("°C"), FAHRENHEIT("°F") }
      enum class DistUnit(val dUi: String, val speedUi: String) { METRIC("m", "m/s"), IMPERIAL("mile", "mph") }
      enum class PrecipitationUnit(val ui: String) { MILLIMETERS("mm"), INCHES("″") }
      enum class PressureUnit { HECTOPASCALS, INCHES_OF_MERCURY }
      enum class HumidityUnit { PERCENTAGE }
   }

   data class UnitsDto(
      val temperature: Units2.TemperatureUnit = CELSIUS,
      val windSpeed: Units2.DistUnit = METRIC,
      val precipitation: Units2.PrecipitationUnit = Units2.PrecipitationUnit.MILLIMETERS,
      val pressure: Units2.PressureUnit = Units2.PressureUnit.HECTOPASCALS,
      val humidity: Units2.HumidityUnit = Units2.HumidityUnit.PERCENTAGE,
      val visibility: Units2.DistUnit = METRIC,
   )

   /** https://openweathermap.org/api/one-call-api */
   @Suppress("PropertyName", "SpellCheckingInspection")
   data class Data(
      val lat: Double,
      val lon: Double,
      val timezone: ZoneId,
      val current: Current,
      val hourly: List<Hourly>,
      val daily: List<Daily>,
   ) {
      fun isActual(info: WeatherInfo) = current.dt.toInstant().isYoungerThanFx(1.hours) && lat!=info.latitude.value && lon!=info.longitude.value

      data class Current(
         val dt: Dt,
         val sunrise: Dt,
         val sunset: Dt,
         val temp: DoubleTemp,
         val feels_like: DoubleTemp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: DoubleTemp,
         val clouds: Double,
         val uvi: Double,
         val visibility: Double?,
         val wind_speed: DoubleSpeed?,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val weather: List<WeatherGroup>,
      ) {
         fun isDay() = dt.dt in sunrise.dt..sunset.dt
      }
      data class Hourly(
         val dt: Dt,
         val temp: DoubleTemp,
         val feels_like: DoubleTemp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: DoubleTemp,
         val clouds: Double,
         val visibility: Double?,
         val wind_speed: DoubleSpeed?,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val rain: Hourly1h?,
         val snow: Hourly1h?,
         val weather: List<WeatherGroup>,
      ) {
         data class Hourly1h(
            val `1h`: Double?
         )
      }
      data class Daily(
         val dt: Dt,
         val sunrise: Dt,
         val sunset: Dt,
         val moonrise: Dt?,
         val moonset: Dt?,
         val moon_phase: Double,
         val temp: Temp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: DoubleTemp,
         val wind_speed: DoubleSpeed?,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val clouds: Double,
         val rain: Double?,
         val snow: Double?,
         val weather: List<WeatherGroup>,
      ) {
         data class Temp(
            val day: DoubleTemp,
            val min: DoubleTemp,
            val max: DoubleTemp,
            val night: DoubleTemp?,
            val eve: DoubleTemp?,
            val morn: DoubleTemp?
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
            901L -> IconWH.STRONG_WIND
            else -> IconWH.NA
         }
      }
   }

   /** https://openweathermap.org/api/one-call-api */
   @Suppress("PropertyName", "SpellCheckingInspection")
   data class DataImplOpenWeatherMap(
      val lat: Double,
      val lon: Double,
      val timezone: ZoneId,
      val timezone_offset: Long,
      val current: Current,
      val hourly: List<Hourly>,
      val daily: List<Daily>,
   ) {
      fun isActual(info: WeatherInfo) = current.dt.toInstant().isYoungerThanFx(1.hours) && lat!=info.latitude.value && lon!=info.longitude.value

      fun convert(data: WeatherInfo.DataImplOpenWeatherMap): Data {
         val j = Json()
         return j.fromJsonValue<Data>(j.toJsonValue(type<WeatherInfo.DataImplOpenWeatherMap>(), data)).orThrow
      }

      data class Current(
         val dt: Dt,
         val sunrise: Long,
         val sunset: Long,
         val temp: DoubleTemp,
         val feels_like: DoubleTemp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: DoubleTemp,
         val clouds: Double,
         val uvi: Double,
         val visibility: Double?,
         val wind_speed: DoubleSpeed,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val weather: List<WeatherGroup>,
      ) {
         fun isDay() = dt.dt in sunrise..sunset
      }
      data class Hourly(
         val dt: Dt,
         val temp: DoubleTemp,
         val feels_like: DoubleTemp,
         val pressure: Double,
         val humidity: Double,
         val dew_point: DoubleTemp,
         val clouds: Double,
         val visibility: Double?,
         val wind_speed: DoubleSpeed,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val rain: Hourly1h?,
         val snow: Hourly1h?,
         val weather: List<WeatherGroup>,
      ) {
         data class Hourly1h(
            val `1h`: Double?
         )
      }
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
         val dew_point: DoubleTemp,
         val wind_speed: DoubleSpeed,
         val wind_gust: DoubleSpeed?,
         val wind_deg: WindDir,
         val clouds: Double,
         val rain: Double?,
         val snow: Double?,
         val weather: List<WeatherGroup>,

      ) {
         data class Temp(
            val day: DoubleTemp,
            val min: DoubleTemp,
            val max: DoubleTemp,
            val night: DoubleTemp,
            val eve: DoubleTemp,
            val morn: DoubleTemp
         )
      }
      data class WeatherGroup(
         val id: Long,
         val main: String,
         val description: String,
         val icon: String
      )
   }

   /** https://www.visualcrossing.com/weather/weather-data-services */
   data class DataImplVisualCrossing(
      val latitude: Double,
      val longitude: Double,
      val resolvedAddress: String,
      val address: String,
      val timezone: ZoneId,
      val tzoffset: Int,
      val description: String?,
      val days: List<Day>,
      val alerts: List<Alert>?,
      val currentConditions: CurrentConditions
   ) {
      fun convert(): Data {
         return Data(
            latitude,
            longitude,
            timezone,
            Data.Current(
               currentConditions.datetimeEpoch,
               currentConditions.sunriseEpoch!!,
               currentConditions.sunsetEpoch!!,
               currentConditions.temp,
               currentConditions.feelslike,
               currentConditions.pressure!!,
               currentConditions.humidity!!.toDouble(),
               currentConditions.dew!!,
               currentConditions.cloudcover!!.toDouble(),
               currentConditions.uvindex!!.toDouble(),
               currentConditions.visibility!!.toDouble()*1000,
               currentConditions.windspeed,
               currentConditions.windgust,
               WindDir(currentConditions.winddir!!.toDouble()),
               listOf(Data.WeatherGroup(convertIcon(currentConditions.icon), "", "", ""))
            ),
            days.asSequence().sortedBy { it.datetimeEpoch.dt }.flatMap { it.hours ?: listOf() }.take(24).toList().map {
               Data.Hourly(
                  it.datetimeEpoch,
                  it.temp,
                  it.feelslike,
                  it.pressure!!,
                  it.humidity!!.toDouble(),
                  it.dew!!,
                  it.cloudcover?.toDouble() ?: 0.0,
                  it.visibility?.net { it*1000 },
                  it.windspeed,
                  it.windgust,
                  WindDir(it.winddir!!.toDouble()),
                  Data.Hourly.Hourly1h(it.precip),
                  Data.Hourly.Hourly1h(it.snow),
                  listOf(Data.WeatherGroup(convertIcon(it.icon), "", "", ""))
               )
            },
            days.sortedBy { it.datetimeEpoch.dt }.map {
               Data.Daily(
                  it.datetimeEpoch,
                  it.sunriseEpoch!!,
                  it.sunsetEpoch!!,
                  null,
                  null,
                  it.moonphase!!,
                  Data.Daily.Temp(
                     it.temp, it.tempmin!!, it.tempmax!!, null, null, null
                  ),
                  it.pressure!!,
                  it.humidity!!.toDouble(),
                  it.dew!!,
                  it.windspeed,
                  it.windgust,
                  WindDir(it.winddir!!.toDouble()),
                  it.cloudcover!!.toDouble(),
                  it.precip,
                  it.snow,
                  listOf(Data.WeatherGroup(convertIcon(it.icon), "", "", ""))
               )
            },
         )
      }

      companion object {
         fun convertIcon(id: String?): Long = when (id) {
            // Amount of snow is greater than zero
            "snow" -> 601
            // Periods of snow during the day
            "snow-showers-day" -> 621
            // Periods of snow during the night
            "snow-showers-night" -> 621
            // Thunderstorms throughout the day or night
            "thunder-rain" -> 201
            // Possible thunderstorms throughout the day
            "thunder-showers-day" -> 201
            // Possible thunderstorms throughout the night
            "thunder-showers-night" -> 201
            // Amount of rainfall is greater than zero
            "rain" -> 501
            // Rain showers during the day
            "showers-day" -> 521
            // Rain showers during the night
            "showers-night" -> 521
            // Visibility is low (lower than one kilometer or mile)
            "fog" -> 741
            // Wind speed is high (greater than 30 kph or mph)
            "wind" -> 901
            // Cloud cover is greater than 90% cover
            "cloudy" -> 804
            // Cloud cover is greater than 20% cover during day time.
            "partly-cloudy-day" -> 802
            // Cloud cover is greater than 20% cover during night time.
            "partly-cloudy-night" -> 802
            // Cloud cover is less than 20% cover during day time
            "clear-day" -> 800
            // Cloud cover is less than 20% cover during night time
            "clear-night" -> 800
            else -> 1000
         }
      }

      data class Day(
         val datetime: String,
         val datetimeEpoch: Dt,
         val temp: DoubleTemp,
         val tempmax: DoubleTemp?,
         val tempmin: DoubleTemp?,
         val feelslike: DoubleTemp,
         val cloudcover: Int?,
         val conditions: String?,
         val dew: DoubleTemp?,
         val humidity: Int?,
         val icon: String?,
         val moonphase: Double?,
         val precip: Double?,
         val preciptype: List<String>?,
         val pressure: Double?,
         val snow: Double?,
         val snowdepth: Double?,
         val source: String,
         val sunrise: String?,
         val sunriseEpoch: Dt?,
         val sunset: String?,
         val sunsetEpoch: Dt?,
         val uvindex: Int?,
         val visibility: Double?,
         val winddir: Int?,
         val windgust: DoubleSpeed?,
         val windspeed: DoubleSpeed?,
         val windspeedmax: DoubleSpeed?,
         val windspeedmean: DoubleSpeed?,
         val windspeedmin: DoubleSpeed?,
         val solarradiation: Double?,
         val solarenergy: Double?,
         val severerisk: Int?,
         val cape: Double?,
         val cin: Double?,
         val degreedays: Degreedays?,
         val hours: List<Hour>?
      ) {
         data class Hour(
            val datetime: String,
            val datetimeEpoch: Dt,
            val temp: DoubleTemp,
            val tempmax: DoubleTemp?,
            val tempmin: DoubleTemp?,
            val feelslike: DoubleTemp,
            val cloudcover: Int?,
            val conditions: String?,
            val dew: DoubleTemp?,
            val humidity: Int?,
            val icon: String?,
            val moonphase: Double?,
            val precip: Double?,
            val preciptype: List<String>?,
            val pressure: Double?,
            val snow: Double?,
            val snowdepth: Double?,
            val source: String,
            val sunrise: String?,
            val sunriseEpoch: Dt?,
            val sunset: String?,
            val sunsetEpoch: Dt?,
            val uvindex: Int?,
            val visibility: Double?,
            val winddir: Int?,
            val windgust: DoubleSpeed?,
            val windspeed: DoubleSpeed?,
            val windspeedmax: DoubleSpeed?,
            val windspeedmean: DoubleSpeed?,
            val windspeedmin: DoubleSpeed?,
            val solarradiation: Double?,
            val solarenergy: Double?,
            val severerisk: Int?,
            val cape: Double?,
            val cin: Double?
         )
      }
      data class Alert(
         val event: String,
         val description: String
      )
      data class CurrentConditions(
         val datetime: String,
         val datetimeEpoch: Dt,
         val temp: DoubleTemp,
         val tempmax: DoubleTemp?,
         val tempmin: DoubleTemp?,
         val feelslike: DoubleTemp,
         val cloudcover: Int?,
         val conditions: String?,
         val dew: DoubleTemp?,
         val humidity: Int?,
         val icon: String?,
         val moonphase: Double?,
         val precip: Double?,
         val preciptype: List<String>?,
         val pressure: Double?,
         val snow: Double?,
         val snowdepth: Double?,
         val source: String,
         val sunrise: String?,
         val sunriseEpoch: Dt?,
         val sunset: String?,
         val sunsetEpoch: Dt?,
         val uvindex: Int?,
         val visibility: Double?,
         val winddir: Int?,
         val windgust: DoubleSpeed?,
         val windspeed: DoubleSpeed?,
         val windspeedmax: DoubleSpeed?,
         val windspeedmean: DoubleSpeed?,
         val windspeedmin: DoubleSpeed?,
         val solarradiation: Double?,
         val solarenergy: Double?,
         val severerisk: Int?,
         val cape: Double?,
         val cin: Double?
      )
      data class Degreedays(
         val normal: List<Double>,
         val value: Double,
         val accValue: Double
      )
   }

   companion object: KLogging(), WidgetCompanion {
      override val name = "Weather Info"
      override val description = "Displays current weather data and weather forecast"
      override val descriptionLong = "$description."
      override val icon = IconWH.YAHOO_44
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2022)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf<ShortcutPane.Entry>()

      private inline fun <reified T> JsValue.asJson(): T? = APP.serializerJson.json.fromJsonValue<T>(this).orThrow

      object Types {
         @JvmInline value class Dt(val dt: Long) {
            fun toInstant(): Instant = Instant.ofEpochMilli(dt*1000)
         }
         @JvmInline value class Dts(val dt: Long) {
            fun toDt(): Dt = Dt(dt*1000)
         }
         @JvmInline value class DoubleTemp(val t: Double) {
            fun toUi(u: UnitsDto, l: Locale): String = "%s%s".format(l, toUiValue(u, l), u.temperature.ui)
            fun toUiValue(u: UnitsDto, l: Locale): String = "%.1f".format(l, when (u.temperature) { CELSIUS -> t; FAHRENHEIT -> t*1.8+32 })
         }
         @JvmInline value class DoubleSpeed(val t: Double) {
            fun toUi(u: UnitsDto, l: Locale): String = "%s%s".format(l, toUiValue(u, l), u.windSpeed.speedUi)
            fun toUiValue(u: UnitsDto, l: Locale): String = "%.1f".format(l, when (u.windSpeed) { METRIC -> t; IMPERIAL -> t*2.23694 })
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