package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.time.Instant
import javafx.geometry.Insets
import javafx.geometry.VPos
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
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.Icon
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
import sp.it.util.conf.getDelegateConfig
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.toSubscription
import sp.it.util.text.capital
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.times
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.em

/** Basic display for song album information. */
class WeatherInfo: HBox(15.0) {

   private val confIcon = Icon(IconFA.COG).onClickDo { configure() }
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
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * VPos.CENTER }
            lay += confIcon
            lay += mainIcon
            lay += tempL
         }
         lay += descL
         lay += hBox {
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * VPos.CENTER }
            lay += Icon(IconWH.WINDY).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += windL
            lay += Icon(IconWH.HUMIDITY).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += humidityL
            lay += Icon(IconFA.CLOCK_ALT).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += dewL
         }
         lay += hBox {
            alignmentProperty() syncFrom this@WeatherInfo.alignmentProperty().map { it.hpos * VPos.CENTER }
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

      data.sync {
         mainIcon.icon(it?.current?.let { it.weather.firstOrNull()?.icon(it.isDay()) } ?: IconWH.NA)
         val l = APP.locale.value
         val d = units.value.d
         val s = units.value.s
         tempL.text = it?.current?.temp?.net { "%.1f%s".format(l, it, d) } ?: "n/a"
         descL.text = it?.current?.net { "Feels like %.1f%s%s".format(l, it.feels_like, d, it.weather.joinToString { ". " + it.description.capital() }) } ?: "n/a"
         windL.text = it?.current?.let { "%d%s %s°".format(l, it.wind_speed.toInt(), s, it.windDir()) } ?: "n/a"
         humidityL.text = it?.current?.humidity?.toInt()?.net { "$it%" } ?: "n/a"
         dewL.text = it?.current?.dew_point?.net { "%.1f%s".format(l, it, d) } ?: "n/a"
         pressureL.text = it?.current?.pressure?.toInt()?.net { "${it}hPa" } ?: "n/a"
         uvL.text = it?.current?.uvi?.toUi() ?: "n/a"
         visL.text = it?.current?.visibility?.toInt()?.net { "%d%s".format(l, if (it>999) it/1000 else it, if (it>999) "km" else "m") } ?: "n/a"
      }
   }

   private suspend fun refresh() {
      val dataOld = APP.configuration.rawGet(dataKey)?.val1?.parseToJson<Data?>()?.takeIf { it.isActual(this) }
      val dataNew = when {
         latitude.value==null || latitude.value==null || latitude.value==null -> null
         dataOld!=null -> dataOld
         else -> {
            val link = "https://api.openweathermap.org/data/2.5/onecall?lat=${latitude.value}&lon=${longitude.value}&units=${units.value.name.lowercase()}&exclude=minutely,alerts&appid=${apiKey.value}"
            val dataRaw = http.get(link).bodyAsText()
            dataRaw.printIt()
            APP.configuration.rawAdd(dataKey, PropVal1(dataRaw))
            dataRaw.parseToJson<Data>()
         }
      }
      runFX { data.value = dataNew }
   }

   private fun configure() {
      object: ConfigurableBase<Any?>() {
         val latitude by cvn(this@WeatherInfo.latitude)
         val longitude by cvn(this@WeatherInfo.longitude)
         val apiKey by cvn(this@WeatherInfo.apiKey)
         val units by cv(this@WeatherInfo.units)
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

   enum class Units(val d: String, val s: String) { METRIC("°C", "m/s"), IMPERIAL("°F", "mph") }

   /** https://openweathermap.org/api/one-call-api */
   data class Data(
      val lat: Double,
      val lon: Double,
      val timezone: java.time.ZoneId,
      val timezone_offset: Long,
      val current: Current,
      val hourly: List<Hourly>,
      val daily: List<Daily>,
   ) {
      fun isActual(info: WeatherInfo) = current.dt.toInstant().isOlderThan1Hour() && lat!=info.latitude.value && lon!=info.longitude.value

      data class Current(
         val dt: Long,
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
         val wind_deg: Double,
         val weather: List<WeatherGroup>,
      ) {
         fun isDay() = dt in sunrise..sunset

         fun windDir(): String = when (wind_deg.toInt()) {
            in  78..123 -> "N"
            in  23.. 77 -> "NE"
            in   0..22  -> "E"
            in 338..360 -> "E"
            in 294..337 -> "SE"
            in 249..293 -> "S"
            in 204..248 -> "SW"
            in 169..203 -> "W"
            in 124..168 -> "NW"
            else -> fail { "invalid wind direction degree value=${wind_deg}" }
         }
      }
      data class Hourly(
         val temp: Double,
         val feels_like: Double,
         val weather: List<WeatherGroup>,
      )
      data class Daily(
         val sunrise: Long,
         val sunset: Long,
         val moonrise: Long,
         val moonset: Long,
         val weather: List<WeatherGroup>,
      )
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
      fun Long.toInstant(): Instant = Instant.ofEpochMilli(this*1000)
      fun String.parseToJson() = APP.serializerJson.json.ast(this).orThrow
      inline fun <reified T> String.parseToJson(): T? = APP.serializerJson.json.fromJson<T>(this).ifError { it.printStackTrace() }.orNull()
      fun Instant.isOlderThan1Hour() = Instant.now().isBefore(plusSeconds(3500))
   }
}