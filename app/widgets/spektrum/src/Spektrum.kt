package spektrum

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HannWindow
import be.tarsos.dsp.util.fft.WindowFunction
import java.util.LinkedList
import java.util.Queue
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.util.Duration.ZERO
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.Double01
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.animation.Loop
import sp.it.util.collections.setTo
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.noUi
import sp.it.util.conf.readOnly
import sp.it.util.conf.values
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.math.P
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.units.version
import sp.it.util.units.year
import spektrum.OctaveGenerator.getHighLimit
import spektrum.OctaveGenerator.getLowLimit
import spektrum.WeightWindow.dBZ

class Spektrum(widget: Widget): SimpleController(widget) {

   val inputDevice by cv("Primary Sound Capture").attach { audioEngine.restartOnNewThread() } // support refresh on audio device add/remove, see https://stackoverflow.com/questions/29667565/jna-detect-audio-device-arrival-remove
      .valuesUnsealed { AudioSystem.getMixerInfo().filter { it.description.contains("Capture") }.map { it.name } }
      .def(name = "Audio input device", info = "")
   val audioFormatChannels by c(1)
      .def(name = "Audio format channels", info = "", editable = NONE).readOnly()
   val audioFormatSigned by c(true)
      .def(name = "Audio format signed", info = "", editable = NONE).readOnly()
   val audioFormatBigEndian by c(false)
      .def(name = "Audio format big endian", info = "", editable = NONE).readOnly()
   val sampleSizeInBits by cv(16).readOnly().attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio sample in bits", info = "", editable = NONE)
   val sampleRate by cv(48000).values(listOf(44100, 48000)).attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio sample rate", info = "")
   val bufferSize by cv(6000).between(32, 24000).attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio buffer size", info = "")
   val bufferOverlap by cv(4976).between(0, 23744).attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio buffer overlap", info = "")
   val maxLevel by cv("RMS").values(listOf("RMS", "Peak"))
      .def(name = "Signal max level", info = "")
   var weight by c(dBZ)
      .def(name = "Signal weighting", info = "")
   val signalAmplification by cv(100).between(0, 250)
      .def(name = "Signal amplification (factor)", info = "")
   val signalThreshold by cv(-28).between(-50, 0)
      .def(name = "Signal threshold (db)", info = "")

   var frequencyStart by c(39).between(1, 24000)
      .def(name = "Frequency range start", info = "")
   var frequencyCenter by c(1000).between(1, 24000)
      .def(name = "Frequency range center", info = "")
   var frequencyEnd by c(16001).between(1, 24000)
      .def(name = "Frequency range end", info = "")
   var octave by c(6).between(1, 24)
      .def(name = "Frequency octave (1/n)", info = "")
   var resolutionHighQuality by c(false)
      .def(name = "Frequency precise resolution", info = "")

   val millisToZero by cv(400).between(0, 1000)
      .def(name = "Animation decay (ms to zero)", info = "")
   val accelerationFactor by cv(5).between(0, 30)
      .def(name = "Animation decay acceleration (1/n)", info = "")
   var timeFilterSize by c(3).between(0, 10)
      .def(name = "Animation smoothness (time)", info = "")
   var smoothnessType by c(FFTTimeFilter.Type.WMA)
      .def(name = "Animation smoothness (time) type", info = "")
   var smoothnessSpaceType by c(FFTSpaceFilter.Type.GAUSS)
      .def(name = "Animation smoothness (position) type", info = "")
   var smoothnessSpaceStrength by c(0.5).between(0.0, 1.0)
      .def(name = "Animation smoothness (position) strength", info = "")

   var barData by c(BarData.FREQUENCY_AMPLITUDES)
      .def(name = "Bar data", info = "Data displayed by the bars")
   val barMinHeight by cv(2.0).min(0.0)
      .def(name = "Bar min height", info = "")
   val barMaxHeight by cv(750.0).min(0.0)
      .def(name = "Bar max height", info = "")
   val barGap       by cv(8).min(0)
      .def(name = "Bar gap", info = "")
   var barAlignment by c(BarShape.CIRCLE_MIDDLE)
      .def(name = "Bars shape", info = "Shape along which the bars are drawn")
   var barStyle     by c(BarStyle.LINE)
      .def(name = "Bars style", info = "Bar drawing style")
   var effectPulse  by c(true)
      .def(name = "Bars effect - pulse", info = "Adjust the lower bar end dynamically by total volume, producing beat effect")
   var effectMirror by c(BarMirror.POINT)
      .def(name = "Bars effect - mirror", info = "")
   var effectShift  by c(0.0).between(-1.0, +1.0)
      .def(name = "Bars effect - shift", info = "Shift bars along the shape to left/right by fraction of the shape length")
   var effectMoving by c(ZERO!!)
      .def(name = "Bars effect - shifting", info = "Keep shifting bars along the shape left/right in the time period ")
   var effectFade   by c(BarFade.NONE)
      .def(name = "Bars effect - fade", info = "Fade bars to 0 towards the edge")
   var barLineCap   by c(StrokeLineCap.ROUND)
      .def(name = "Bar line cap type", info = "")
   var barLineJoin  by c(StrokeLineJoin.ROUND)
      .def(name = "Bar line join type", info = "")

   val spectralColorPosition by cv(180).between(0, 360)
      .def(name = "Color spectral position (degrees)", info = "")
   val spectralColorRange by cv(360).between(0, 360)
      .def(name = "Color spectral range (degrees)", info = "")
   val spectralColorInverted by cv(false)
      .def(name = "Color spectral inverted", info = "")
   val saturation by cv(100).between(0, 100)
      .def(name = "Color saturation (%)", info = "")
   val brightness by cv(100).between(0, 100)
      .def(name = "Color brightness (%)", info = "")
   val baseColor by cv(Color.color(0.1, 0.1, 0.1)!!).noUi()
      .def(name = "Color Base", info = "")

   val spectralFFTService = FrequencyBarsFFTService(this)
   val audioEngine = TarsosAudioEngine(this)
   val spectralView = SpectralView(this, spectralFFTService)

   init {

      root.lay += spectralView.apply {
         heightProperty() syncFrom spektrum.root.heightProperty() on spektrum.onClose
         widthProperty() syncFrom spektrum.root.widthProperty() on spektrum.onClose
      }

      audioEngine.fttListenerList += spectralFFTService

      root.sync1IfInScene {
         onClose += audioEngine::dispose
         audioEngine.start()
         onClose += spectralView::stop
         spectralView.play()
      }
   }

   companion object: WidgetCompanion {
      override val name = "Spektrum"
      override val description = "Spektrum"
      override val descriptionLong = "$description. Shows system audio spectrum"
      override val icon = IconMD.POLL
      override val version = version(1, 3, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO, VISUALISATION)
      override val summaryActions = listOf<Entry>()

      val openLines = ConcurrentHashMap<Mixer, LineUses>()

      override fun dispose() {
         super.dispose()
         openLines.values.forEach { it.line.close() }
      }
   }

   class LineUses(val line: TargetDataLine, count: Int) {
      val count = AtomicInteger(count)
   }

   class SpectralView(val spektrum: Spektrum, val fft: FrequencyBarsFFTService): Canvas() {
      private val settings = spektrum
      private val gc = graphicsContext2D!!
      private val volumeHistory = LinkedList<Double>()
      private val loop = Loop { time ->
         val barsRaw = fft.frequencyBars
         val barsAvg = barsRaw.sumOf { it.height }/barsRaw.count()
         val bars = when (spektrum.barData) {
            BarData.CONSTANT -> barsRaw.map { it.copy(height = 50.0) }
            BarData.CONSTANT_SINE -> barsRaw.mapIndexed { i, b -> b.copy(height = 25.0 * sin(2*PI*i.toDouble()/barsRaw.size).absoluteValue) }
            BarData.VOLUME -> barsRaw.map { it.copy(height = barsAvg) }
            BarData.VOLUME_SINE -> barsRaw.mapIndexed { i, b -> b.copy(height = barsAvg * sin(2*PI*i.toDouble()/barsRaw.size).absoluteValue) }
            BarData.VOLUME_HISTORY -> {
               if (barsRaw.isEmpty()) {
                  barsRaw
               } else {
                  if (volumeHistory.size!=barsRaw.size) volumeHistory setTo barsRaw.map { 0.0 }
                  volumeHistory.removeFirst()
                  volumeHistory.addLast(barsAvg)
                  volumeHistory.mapIndexed { i, v -> barsRaw[i].copy(height = v) }
               }
            }
            BarData.FREQUENCY_AMPLITUDES -> barsRaw
         }
         DrawingData(settings, bars, time).updateBars()
      }

      init {
         spektrum.root.heightProperty() attach { settings.barMaxHeight.value = it.toDouble() }
      }

      fun DrawingData.updateBars() {
         val w = spektrum.root.width
         val h = spektrum.root.height
         val barWg = w/barCount.toDouble()
         val barW = barWg-settings.barGap.value.toDouble() max 1.0
         val pulseEffect = if (settings.effectPulse) avg/15.0 else 0.0

         gc.clearRect(0.0, 0.0, w, h)
         gc.lineCap = settings.barLineCap
         gc.lineJoin = settings.barLineJoin
         gc.save()

         val barPositionsLow = ArrayList<P>(barCount)
         val barPositionsHig = ArrayList<P>(barCount)
         when (settings.barAlignment) {
            BarShape.CENTER -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  barPositionsLow += P(i*barWg, h/2 - f*bar.height/2)
                  barPositionsHig += P(i*barWg, h/2 + f*bar.height/2)
               }
            }
            BarShape.BOTTOM -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  barPositionsLow += P(i*barWg, h)
                  barPositionsHig += P(i*barWg, h - f*bar.height)
               }
            }
            BarShape.TOP -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  barPositionsLow += P(i*barWg, 0.0)
                  barPositionsHig += P(i*barWg, f*bar.height)
               }
            }
            BarShape.CIRCLE_IN, BarShape.CIRCLE_MIDDLE, BarShape.CIRCLE_OUT -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  val barH = f*bar.height*min(h, w)/2.0/settings.barMaxHeight.value
                  val base = min(h, w)/5.0 + pulseEffect
                  val max = base + (if (settings.barAlignment==BarShape.CIRCLE_IN) 0.0 else barH/4.0)
                  val min = base - (if (settings.barAlignment==BarShape.CIRCLE_OUT) 0.0 else barH/8.0)
                  val barCos = cos(2*PI*i/barCount - PI/2)
                  val barSin = sin(2*PI*i/barCount - PI/2)
                  barPositionsLow += P(w/2 + min*barCos, h/2 + min*barSin)
                  barPositionsHig += P(w/2 + max*barCos, h/2 + max*barSin)
               }
            }
         }

         when (settings.barStyle) {
            BarStyle.LINE -> {
               gc.lineWidth = barW
               bars.forEachIndexed { i, bar ->
                  gc.stroke = bar.color
                  gc.strokeLine(barPositionsLow[i].x, barPositionsLow[i].y, barPositionsHig[i].x, barPositionsHig[i].y)
               }
               gc.lineWidth = 0.0
            }

            BarStyle.NET -> {
               gc.lineWidth = 2.0
               bars.forEachIndexed { i, bar ->
                  gc.stroke = bar.color
                  gc.strokeLine(barPositionsLow[i].x, barPositionsLow[i].y, barPositionsHig[i].x, barPositionsHig[i].y)
               }
               gc.stroke = bars.firstOrNull()?.color ?: Color.BLACK
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (barPositionsLow.map { it.x }.toDoubleArray(), barPositionsLow.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(barPositionsLow.map { it.x }.toDoubleArray(), barPositionsLow.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               gc.lineWidth = 0.0
            }
            BarStyle.NET_OUTLINE -> {
               gc.lineWidth = 2.0
               gc.stroke = bars.firstOrNull()?.color ?: Color.BLACK
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (barPositionsLow.map { it.x }.toDoubleArray(), barPositionsLow.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(barPositionsLow.map { it.x }.toDoubleArray(), barPositionsLow.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               gc.lineWidth = 0.0
            }
            BarStyle.OUTLINE -> {
               gc.lineWidth = 2.0
               gc.stroke = bars.firstOrNull()?.color ?: Color.BLACK
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(barPositionsHig.map { it.x }.toDoubleArray(), barPositionsHig.map { it.y }.toDoubleArray(), bars.size)
               gc.lineWidth = 0.0
            }
            BarStyle.ZIGZAG -> {
               gc.lineWidth = 2.0
               gc.stroke = bars.firstOrNull()?.color ?: Color.BLACK
               val pointSelector = { it: Int -> (if (it%2==0) barPositionsLow else barPositionsHig)[it] }
               if (bars.size>1 &&  settings.barAlignment.connect) gc.strokePolygon (DoubleArray(bars.size) { pointSelector(it).x }, DoubleArray(bars.size) { pointSelector(it).y }, bars.size)
               if (bars.size>1 && !settings.barAlignment.connect) gc.strokePolyline(DoubleArray(bars.size) { pointSelector(it).x }, DoubleArray(bars.size) { pointSelector(it).y }, bars.size)
               gc.lineWidth = 0.0
            }
         }
         gc.restore()
      }

      fun play() = loop.start()
      fun stop() = loop.stop()

      data class DrawingData(val settings: Spektrum, val avg: Double, val barCount: Int, val bars: List<FrequencyBar>, val time: Long) {
         constructor(settings: Spektrum, bars: List<FrequencyBar>, time: Long): this(
            settings,
            bars.sumOf { it.height }/bars.size,
            when (settings.effectMirror) {
               BarMirror.NONE -> bars.size
               BarMirror.AXIS, BarMirror.POINT -> bars.size*2
               BarMirror.AXIS_TWICE, BarMirror.POINT_TWICE -> bars.size*4
            },
            when (settings.effectMirror) {
               BarMirror.NONE -> bars
               BarMirror.AXIS -> bars.reversed() + bars
               BarMirror.POINT -> bars + bars
               BarMirror.AXIS_TWICE -> bars + bars + bars + bars
               BarMirror.POINT_TWICE -> bars.reversed() + bars + bars.reversed() + bars
            }.net {
               if (it.isEmpty()) it
               else {
                  val sByRaw = (settings.effectShift.absoluteValue*it.size).toInt() % it.size
                  val sBy = if (settings.effectShift<0.0) it.size-sByRaw else sByRaw
                  val mBy = if (settings.effectMoving.toMillis()==0.0) 0 else {
                     val ms = settings.effectMoving.toMillis().absoluteValue.coerceAtLeast(1.0)
                     val mByRaw = (time%ms/ms*it.size).toInt() % it.size
                     if (settings.effectMoving.toMillis()<0.0) it.size-mByRaw else mByRaw
                  }
                  val by = (sBy + mBy) % it.size
                  if (by==0) it
                  else it.subList(by, it.size) + it.subList(0, by+1)
               }
            },
            time
         )

         fun fade(i: Int): Double {
            val fi = i/barCount.toDouble()
            return when {
               (settings.effectFade==BarFade.IN || settings.effectFade==BarFade.IN_OUT) && fi in 0.0..0.2 -> cos((1.0 - fi*5)*PI)/2.0 + 0.5
               (settings.effectFade==BarFade.OUT || settings.effectFade==BarFade.IN_OUT) && fi in 0.8..1.0 -> cos((fi - 0.8)*5*PI)/2.0 + 0.5
               else -> 1.0
            }
         }
      }
   }

   enum class BarShape(val connect: Boolean) { BOTTOM(false), CENTER(false), TOP(false), CIRCLE_OUT(true), CIRCLE_MIDDLE(true), CIRCLE_IN(true) }

   enum class BarData { CONSTANT, CONSTANT_SINE, FREQUENCY_AMPLITUDES, VOLUME, VOLUME_SINE, VOLUME_HISTORY }

   enum class BarStyle { LINE, NET, NET_OUTLINE, OUTLINE, ZIGZAG }

   enum class BarMirror { NONE, AXIS, AXIS_TWICE, POINT, POINT_TWICE }

   enum class BarFade { NONE, IN, OUT, IN_OUT }

   object GlobalColorCalculator {
      fun getGlobalColor(settings: Spektrum, frequencyBars: List<FrequencyBar>, startHz: Int, endHz: Int, peak: GlobalColorCalculatorPeak): Color {
         if (frequencyBars.isEmpty())
            return Color.BLACK

         var sumIntensity = 0.0
         var maxIntensity = 0.0
         var sumRed = 0.0
         var sumGreen = 0.0
         var sumBlue = 0.0
         var nrBars = 0
         for (frequencyBar in frequencyBars) {
            if (startHz<=frequencyBar.hz && frequencyBar.hz<=endHz) {
               val barHeight = frequencyBar.height
               val barIntensity = barHeight/settings.barMaxHeight.value
               val barColor = frequencyBar.color
               sumRed += barColor.red*barIntensity
               sumGreen += barColor.green*barIntensity
               sumBlue += barColor.blue*barIntensity
               sumIntensity += barIntensity
               if (barIntensity>maxIntensity) {
                  maxIntensity = barIntensity
               }
               nrBars++
            }
         }
         val avgRed = sumRed/nrBars
         val avgGreen = sumGreen/nrBars
         val avgBlue = sumBlue/nrBars
         val avgIntensity = sumIntensity/nrBars
         var intensity = when (peak) {
            GlobalColorCalculatorPeak.AVG -> avgIntensity
            GlobalColorCalculatorPeak.MAX -> maxIntensity
         }
         intensity *= (settings.brightness.value/100.0)
         var color = Color.color(avgRed, avgGreen, avgBlue)
         color = settings.baseColor.value.interpolate(color, maxIntensity)
         color = Color.hsb(color.hue, color.saturation, intensity)
         return color
      }

   }
   enum class GlobalColorCalculatorPeak { AVG, MAX }
   data class FrequencyBar(var hz: Double, var height: Double, var color: Color)

   /**
    * This class holds state information regarding:
    * - timeFiltering a.k.a. smoothness
    * - previous bar heights that are used in bad decay calculation
    */
   class FrequencyBarsFFTService(settings: Spektrum): FFTListener {
      private val settings = settings
      private val oldTime = System.currentTimeMillis()

      // the hzBins and the amplitudes come in pairs and access to them needs to be synchronized
      private val lock = ReentrantLock(true)

      // all the instances have the same input from the audio dispatcher
      private var hzBins: DoubleArray? = null
      private var amplitudes: DoubleArray? = null

      private var hzBinsOld: DoubleArray? = null
      private var amplitudesOld: DoubleArray? = null

      private val fftTimeFilter = FFTTimeFilter(settings)
      private val fftSpaceFilter = FFTSpaceFilter(settings)
      private val barsHeightCalculator = BarsHeightCalculator(settings)

      override fun frame(hzBins: DoubleArray?, normalizedAmplitudes: DoubleArray?) {
         try {
            lock.lock()
            this.hzBins = hzBins
            this.amplitudes = normalizedAmplitudes
         } finally {
            lock.unlock()
         }
      }

      // return empty array
      val frequencyBars: List<FrequencyBar>
         get() {
            val returnBins: DoubleArray?
            var returnAmplitudes: DoubleArray?

            try {
               lock.lock()
               if (hzBins==null) {
                  returnBins = hzBinsOld
                  returnAmplitudes = amplitudesOld
               } else {
                  returnBins = hzBins
                  returnAmplitudes = amplitudes
                  hzBinsOld = hzBins
                  amplitudesOld = amplitudes
                  hzBins = null
                  amplitudes = null
               }
            } finally {
               lock.unlock()
            }

            return if (returnAmplitudes!=null) {
               returnAmplitudes = fftTimeFilter.filter(returnAmplitudes)
               returnAmplitudes = fftSpaceFilter.filter(returnAmplitudes)
               returnAmplitudes = barsHeightCalculator.processAmplitudes(returnAmplitudes)
               createFrequencyBars(returnBins!!, returnAmplitudes!!)
            } else {
               ArrayList()
            }
         }

      fun createFrequencyBars(binsHz: DoubleArray, amplitudes: DoubleArray): List<FrequencyBar> {

         fun setColor(frequencyBars: List<FrequencyBar>, pos: Double, saturation: Double, brightness: Double, i: Int) {
            val color = Color.hsb(pos, saturation, brightness)
            // interpolate opacity based on intensity
            // color = settings.baseColor.value.interpolate(color, frequencyBars[i].height / settings.barMaxHeight.value);
            frequencyBars[i].color = color
         }

         val frequencyBars = ArrayList<FrequencyBar>(binsHz.size)
         for (i in binsHz.indices) {
            frequencyBars.add(FrequencyBar(binsHz[i], amplitudes[i], Color.BLACK))
         }
         var pos = settings.spectralColorPosition.value.toDouble()
         val range = settings.spectralColorRange.value.toDouble()
         val saturation = settings.saturation.value/100.0
         val brightness = settings.brightness.value/100.0
         val inverted = settings.spectralColorInverted.value
         if (!inverted) {
            for (i in binsHz.indices) {
               setColor(frequencyBars, pos, saturation, brightness, i)
               pos += range/binsHz.size
            }
         } else {
            for (i in binsHz.indices.reversed()) {
               setColor(frequencyBars, pos, saturation, brightness, i)
               pos += range/binsHz.size
            }
         }
         return frequencyBars
      }
   }
}

class TarsosAudioEngine(settings: Spektrum) {
   private val settings = settings
   private var dispatcher: AudioDispatcher? = null
   private var audioThread: Thread? = null
   private var mixerRef: Mixer? = null
   val fttListenerList = LinkedList<FFTListener>()

   fun start() {
      runTry {
         obtainMixer().ifNotNull { mixer ->
            mixerRef = mixer
            val audioFormat = AudioFormat(settings.sampleRate.value.toFloat(), settings.sampleSizeInBits.value, settings.audioFormatChannels, settings.audioFormatSigned, settings.audioFormatBigEndian)
            val line = obtainLine(mixer, audioFormat, settings.bufferSize.value)
            val stream = AudioInputStream(line)
            val audioStream = JVMAudioInputStream(stream)
            dispatcher = AudioDispatcher(audioStream, settings.bufferSize.value, settings.bufferOverlap.value).apply {
               addAudioProcessor(FFTAudioProcessor(audioFormat, fttListenerList, settings))
            }
            audioThread = Thread(dispatcher, "Spektrum widget audio-fft").apply {
               isDaemon = true
               start()
            }
         }
      }.ifError {
         // TODO: use logger
         println("Using audio device '${settings.inputDevice.value}' failed")
         it.printStackTrace()
      }
   }

   fun stop() {
      try {
         dispatcher?.stop()
         audioThread?.join((1*1000).toLong()) // wait for audio dispatcher to finish // TODO: remove?
      } catch (e: InterruptedException) {
         // ignore
      }

      mixerRef.ifNotNull {
         val lineUses = Spektrum.openLines[it]
         if (lineUses!=null) {
            if (lineUses.count.decrementAndGet() == 0) {
               Spektrum.openLines -= it
               lineUses.line.stop()
               lineUses.line.close()
            }
         }
      }
   }

   fun restart() {
      stop()
      start()
   }

   @ThreadSafe
   fun restartOnNewThread() {
      val thread = Thread { restart() }
      thread.isDaemon = true
      thread.start()
   }

   fun dispose() {
      stop()
   }

   private fun obtainMixer(): Mixer? = AudioSystem.getMixerInfo()
      .find { m -> settings.inputDevice.value.let { it.isNotBlank() && it in m.name } }
      ?.net { AudioSystem.getMixer(it) }

   private fun obtainLine(mixer: Mixer, audioFormat: AudioFormat, lineBuffer: Int): TargetDataLine {
      val lineUses = Spektrum.openLines[mixer]
      return if (lineUses==null) {
         val lineInfo = mixer.targetLineInfo.firstOrNull()
         val line = mixer.getLine(lineInfo) as TargetDataLine
         line.open(audioFormat, lineBuffer)
         line.start()
         Spektrum.openLines[mixer] = Spektrum.LineUses(line, 1)
         line
      } else {
         lineUses.count.incrementAndGet()
         lineUses.line
      }
   }
}

object OctaveGenerator {
   private val cache = ConcurrentHashMap<OctaveSettings, List<Double>>()

   fun getOctaveFrequencies(centerFrequency_: Int, band: Double, lowerLimit_: Int, upperLimit_: Int): List<Double> {
      val fStart = centerFrequency_.clip(1, 23998).toDouble()
      val fEnd = upperLimit_.clip(centerFrequency_+2, 24000).toDouble()
      val fCenter = lowerLimit_.toDouble().clip(fStart+1.0, fEnd-1.0)
      val octaveSettings = OctaveSettings(fCenter, band, fStart, fEnd)
      return cache.computeIfAbsent(octaveSettings) {
         TreeSet<Double>().apply {
            addLow(this, fCenter, band, fStart)
            addHigh(this, fCenter, band, fEnd)
         }.toList()
      }
   }

   fun getLowLimit(center: Double, band: Double): Double = center/2.0.pow(1.0/(2*band))

   fun getHighLimit(center: Double, band: Double): Double = center*2.0.pow(1.0/(2*band))

   private fun addLow(octave: MutableSet<Double>, center: Double, band: Double, lowerLimit: Double) {
      if (center<lowerLimit) {
         return
      }
      octave.add(center)
      val fl = center/2.0.pow(1.0/band)
      addLow(octave, fl, band, lowerLimit)
   }

   private fun addHigh(octave: MutableSet<Double>, center: Double, band: Double, upperLimit: Double) {
      if (center>upperLimit) {
         return
      }
      octave.add(center)
      val fh = center*2.0.pow(1.0/band)
      addHigh(octave, fh, band, upperLimit)
   }

   private data class OctaveSettings(val centerFrequency: Double, val band: Double, val lowerLimit: Double, val upperLimit: Double)
}

interface FFTListener {
   fun frame(hzBins: DoubleArray?, normalizedAmplitudes: DoubleArray?)
}

@Suppress("UseWithIndex")
class FFTAudioProcessor(audioFormat: AudioFormat, listenerList: List<FFTListener>, settings: Spektrum): AudioProcessor {
   private val audioFormat = audioFormat
   private val listenerList = listenerList
   private val settings = settings
   private val interpolator: UnivariateInterpolator = SplineInterpolator()
   private val windowFunction: WindowFunction = HannWindow()
   private val windowCorrectionFactor = 2.0

   override fun process(audioEvent: AudioEvent): Boolean {
      val audioFloatBuffer = audioEvent.floatBuffer

      // the buffer must be copied into another array for processing otherwise strange behaviour
      // the audioFloatBuffer buffer is reused because of the offset
      // modifying it will create strange issues
      val transformBuffer = FloatArray(audioFloatBuffer.size)
      System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.size)
      val amplitudes = FloatArray(transformBuffer.size/2)

      val fft = FFT(transformBuffer.size, windowFunction)
      fft.forwardTransform(transformBuffer)
      fft.modulus(transformBuffer, amplitudes)

      val bins = DoubleArray(transformBuffer.size/2) { fft.binToHz(it, audioFormat.sampleRate) }
      val doublesAmplitudesFactor = 1.0/amplitudes.size*windowCorrectionFactor   // /amplitudes.size normalizes (n/2), *windowCorrectionFactor applies window correction
      val doublesAmplitudes = DoubleArray(amplitudes.size) { amplitudes[it].toDouble()*doublesAmplitudesFactor }

      val octaveFrequencies = OctaveGenerator.getOctaveFrequencies(settings.frequencyCenter, settings.octave.toDouble(), settings.frequencyStart, settings.frequencyEnd)
      val frequencyBins = DoubleArray(octaveFrequencies.size)
      val frequencyAmplitudes = DoubleArray(octaveFrequencies.size)

      val interpolateFunction = interpolator.interpolate(bins, doublesAmplitudes)

      val octave = settings.octave.toDouble()
      val maxLevel = settings.maxLevel
      val weightWindow = settings.weight.calculateAmplitudeWight
      var m = 0 // m is the position in the frequency vectors
      for (i in octaveFrequencies.indices) {
         // get frequency bin
         frequencyBins[m] = octaveFrequencies[i]

         val highLimit = getHighLimit(octaveFrequencies[i], octave)
         val lowLimit = getLowLimit(octaveFrequencies[i], octave)
         val step = 1.0
         var k = lowLimit

         // group amplitude together in frequency bin
         while (k<highLimit) {
            val amplitude = interpolateFunction.value(k)
            frequencyAmplitudes[m] = frequencyAmplitudes[m] + amplitude.pow(2) // sum up the "normalized window corrected" energy
            k += step
         }

         frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m]) // square root the energy
         frequencyAmplitudes[m] = if (maxLevel.equals("RMS")) sqrt(frequencyAmplitudes[m].pow(2)/2) else frequencyAmplitudes[m] // calculate the RMS of the amplitude
         frequencyAmplitudes[m] = 20*log10(frequencyAmplitudes[m]) // convert to logarithmic scale

         frequencyAmplitudes[m] = frequencyAmplitudes[m] + weightWindow(frequencyBins[m]) // use weight to adjust the spectrum

         m++
      }

      listenerList.forEach { listener: FFTListener -> listener.frame(frequencyBins, frequencyAmplitudes) }
      return true
   }

   override fun processingFinished() {}
}

class FFTTimeFilter(settings: Spektrum) {
   private val settings = settings
   private val historyAmps: Queue<DoubleArray> = LinkedList()

   fun filter(amps: DoubleArray): DoubleArray {
      val timeFilterSize = settings.timeFilterSize
      if (timeFilterSize<2)
         return amps

      if (historyAmps.peek()?.size!=amps.size)
         historyAmps.clear()

      if (historyAmps.size<timeFilterSize) {
         historyAmps.offer(amps)
         return amps
      }

      while (historyAmps.size>timeFilterSize) {
         historyAmps.poll()
      }
      historyAmps.poll()
      historyAmps.offer(amps)
      return when (settings.smoothnessType) {
         Type.WMA -> filterWma(amps)
         Type.EMA -> filterEma(amps)
         Type.SMA -> filterSma(amps)
      }
   }

   private fun filterSma(amps: DoubleArray): DoubleArray {
      val filtered = DoubleArray(amps.size)
      for (i in amps.indices) {
         var sumTimeFilteredAmp = 0.0
         for (currentHistoryAmps in historyAmps) {
            sumTimeFilteredAmp += currentHistoryAmps[i]
         }
         filtered[i] = sumTimeFilteredAmp/historyAmps.size
      }
      return filtered
   }

   private fun filterEma(amps: DoubleArray): DoubleArray {
      val filtered = DoubleArray(amps.size)
      for (i in amps.indices) {
         var nominator = 0.0
         var denominator = 0.0
         var expI = 1
         for (currentHistoryAmps in historyAmps) {
            val exp = expI.toDouble()
            nominator += currentHistoryAmps[i]*exp.pow(exp)
            denominator += exp.pow(exp)
            expI++
         }
         filtered[i] = nominator/denominator
      }
      return filtered
   }

   private fun filterWma(amps: DoubleArray): DoubleArray {
      val filtered = DoubleArray(amps.size)
      for (i in amps.indices) {
         var nominator = 0.0
         var denominator = 0.0
         var weight = 1
         for (currentHistoryAmps in historyAmps) {
            nominator += currentHistoryAmps[i]*weight
            denominator += weight
            weight++
         }
         filtered[i] = nominator/denominator
      }
      return filtered
   }

   enum class Type { SMA, WMA, EMA }
}

class FFTSpaceFilter(val settings: Spektrum) {
   private var lastKernel: DoubleArray? = null
   private var lastAmpSize: Int? = null
   private var lastStrength: Double01? = null

   fun filter(amps: DoubleArray): DoubleArray = when (settings.smoothnessSpaceType) {
      Type.GAUSS -> {
         val strength: Double01 = settings.smoothnessSpaceStrength
         when (strength) {
            0.0 -> amps
            1.0 -> { val avg = amps.average(); DoubleArray(amps.size) { avg } }
            else -> {
               val kernel = if (lastKernel!=null && settings.smoothnessSpaceStrength==lastStrength && amps.size==lastAmpSize) {
                  lastKernel!!
               } else {
                  lastAmpSize = amps.size
                  lastStrength = settings.smoothnessSpaceStrength
                  lastKernel = gaussianKernel1d(amps.size, settings.smoothnessSpaceStrength)
                  lastKernel!!
               }
               DoubleArray(amps.size) { i -> kernel.foldIndexed(0.0) { ki, sum, k -> sum + k*amps[(amps.size+i+ki-kernel.size/2).mod(amps.size)] } }
            }
         }
      }
      Type.NONE -> amps
   }

   fun gaussianKernel1d(items: Int, strength: Double): DoubleArray {
      val sqr2pi = sqrt(2 * Math.PI)
      val sigma = 3.0
      val width = (items*strength / 2).toInt()
      val norm = 1.0 / (sqr2pi * sigma)
      val coefficient = 2 * sigma * sigma
      var total = 0.0
      val kernel = DoubleArray(width * 2 + 1)

      (-width..width).forEach {
         val g = norm * exp(-it * it / coefficient)
         val linearPortion = strength*1
         val gaussPortion = (1-strength)*g
         val v = linearPortion + gaussPortion
         total += v
         kernel[width + it] = v
      }

      kernel.indices.forEach { kernel[it] = kernel[it]/total }

      return kernel
   }

   enum class Type { GAUSS, NONE }
}

class BarsHeightCalculator(settings: Spektrum) {
   private val settings = settings
   private var oldTime = System.nanoTime()
   private var oldAmplitudes: DoubleArray? = null
   private lateinit var oldDecayDecelerationSize: DoubleArray

   fun processAmplitudes(newAmplitudes: DoubleArray): DoubleArray? {
      // init on first run or if number of newAmplitudes has changed
      if (oldAmplitudes==null || oldAmplitudes!!.size!=newAmplitudes.size) {
         oldAmplitudes = newAmplitudes
         oldDecayDecelerationSize = DoubleArray(newAmplitudes.size)
         return convertDbToPixels(newAmplitudes)
      }
      val millisToZero = settings.millisToZero.value
      val millisPassed = millisPassed

      val pixelAmplitudes = convertDbToPixels(newAmplitudes)
      oldAmplitudes = decayPixelsAmplitudes(oldAmplitudes!!, pixelAmplitudes, millisToZero.toDouble(), millisPassed)
      return oldAmplitudes
   }

   private fun convertDbToPixels(dbAmplitude: DoubleArray): DoubleArray {
      val signalThreshold: Int = settings.signalThreshold.value
      val maxBarHeight: Double = settings.barMaxHeight.value
      val signalAmplification: Int = settings.signalAmplification.value

      val pixelsAmplitude = DoubleArray(dbAmplitude.size)

      for (i in pixelsAmplitude.indices) {
         val maxHeight = abs(signalThreshold).toDouble()
         var newHeight = dbAmplitude[i]
         newHeight += abs(signalThreshold)
         // normalizing the bar to the height of the window
         newHeight = newHeight*maxBarHeight/maxHeight
         newHeight *= (signalAmplification/100.0)
         pixelsAmplitude[i] = newHeight
      }

      return pixelsAmplitude
   }

   private fun decayPixelsAmplitudes(oldAmplitudes: DoubleArray, newAmplitudes: DoubleArray, millisToZero: Double, secondsPassed: Double): DoubleArray {
      val processedAmplitudes = DoubleArray(newAmplitudes.size)
      val maxBarHeight: Double = settings.barMaxHeight.value
      val minBarHeight: Double = settings.barMinHeight.value

      for (i in processedAmplitudes.indices) {
         val oldHeight = oldAmplitudes[i]
         val newHeight = newAmplitudes[i]
         val decayRatePixelsPerMilli = maxBarHeight/millisToZero
         val dbPerSecondDecay = decayRatePixelsPerMilli*secondsPassed
         if (newHeight<oldHeight - dbPerSecondDecay) {
            var decaySize = dbPerSecondDecay
            val accelerationStep: Double = 1.0/settings.accelerationFactor.value*decaySize
            if (oldDecayDecelerationSize[i] + accelerationStep<dbPerSecondDecay) {
               oldDecayDecelerationSize[i] = oldDecayDecelerationSize[i] + accelerationStep
               decaySize = oldDecayDecelerationSize[i]
            }
            processedAmplitudes[i] = oldHeight - decaySize
         } else {
            processedAmplitudes[i] = newHeight
            oldDecayDecelerationSize[i] = 0.0
         }

         // apply limits
         if (processedAmplitudes[i]<minBarHeight) {
            // below floor
            processedAmplitudes[i] = minBarHeight
         }
      }

      return processedAmplitudes
   }

   // convert nano to ms to seconds
   private val millisPassed: Double
      get() {
         val newTime = System.nanoTime()
         val deltaTime = newTime - oldTime
         oldTime = newTime
         // convert nano to ms
         return deltaTime/1000000.0
      }
}

@Suppress("EnumEntryName")
enum class WeightWindow(val calculateAmplitudeWight: (Double) -> Double) {
   dBA({ f ->
      val raf = (12194.0.p2*f.p4/((f.p2 + 20.6.p2)*sqrt((f.p2 + 107.7.p2)*(f.p2 + 737.9.p2))*(f.p2 + 12194.0.p2)))
      20*log10(raf) + 2.00
   }),
   dBB({ f ->
      val rbf = (12194.0.p2*f.p3/((f.p2 + 20.6.p2)*sqrt(f.p2 + 158.5.p2)*(f.p2 + 12194.0.p2)))
      20*log10(rbf) + 0.17
   }),
   dBC({ f ->
      val rcf = (12194.0.p2*f.p2/((f.p2 + 20.6.p2)*(f.p2 + 12194.0.p2)))
      20*log10(rcf) + 0.06
   }),
   dBZ({
      0.0
   })
}

val Double.p2: Double get() = pow(2.0)
val Double.p3: Double get() = pow(3.0)
val Double.p4: Double get() = pow(4.0)