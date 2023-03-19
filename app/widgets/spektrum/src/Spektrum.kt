@file:Suppress("UsePropertyAccessSyntax")

package spektrum

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.MultichannelToMono
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
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.util.Duration.ZERO
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.withLock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import mu.KLogging
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.Double01
import sp.it.pl.main.IconMD
import sp.it.pl.main.Ui.FPS
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
import sp.it.util.conf.uiInfoConverter
import sp.it.util.conf.values
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.getOr
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.math.P
import sp.it.util.math.abs
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.ui.canvas
import sp.it.util.ui.lay
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year
import spektrum.OctaveGenerator.computeHighLimit
import spektrum.OctaveGenerator.computeLowLimit
import spektrum.OctaveGenerator.computeOctaveFrequencies
import spektrum.Spektrum.FrequencyBarsProcessor
import spektrum.WeightWindow.dBZ

// based on https://github.com/89iuv/visualizer
// for intro into audio processing see https://towardsdatascience.com/understanding-audio-data-fourier-transform-fft-spectrogram-and-speech-recognition-a4072d228520
class Spektrum(widget: Widget): SimpleController(widget) {

   val inputDevice by cv("Primary Sound Capture").attach { audioEngine.restartOnNewThread() } // support refresh on audio device add/remove, see https://stackoverflow.com/questions/29667565/jna-detect-audio-device-arrival-remove
      .valuesUnsealed { AudioSystem.getMixerInfo().filter { it.description.contains("Capture") }.map { it.name } }
      .def(name = "Audio input device", info = "")
   val audioFormatChannels by c(2)
      .def(name = "Audio format channels", info = "Information regarding the format the audio is read for FFT", editable = NONE).readOnly()
   val audioFormatSigned by c(true)
      .def(name = "Audio format signed", info = "Information regarding the format the audio is read for FFT", editable = NONE).readOnly()
   val audioFormatBigEndian by c(false)
      .def(name = "Audio format big endian", info = "Information regarding the format the audio is read for FFT", editable = NONE).readOnly()
   val sampleSizeInBits by cv(16).readOnly().attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio sample in bits", info = "Information regarding the format the audio is read for FFT", editable = NONE)
   val sampleRate by cv(48000).values(listOf(44100, 48000)).attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio sample rate", info = "Information regarding the format the audio is read for FFT")
   val bufferSize by cv(50).between(25, 5000).readOnly().attach { audioEngine.restartOnNewThread() }
      .def(name = "Audio buffer size (ms)", info = "Audio buffer for the FFT. Buffer is necessary and bigger size improves accuracy, but introduces delay")
   val maxLevel by cv("RMS").values(listOf("RMS", "Peak"))
      .def(name = "Signal max level", info = "How amplitude of the bar representing frequency range is computed. Peak takes maximum value, RMS (root mean square) average")
   var weight by c(dBZ).uiInfoConverter { it.infoUi }
      .def(name = "Signal weighting", info = "Sound signal weighting involves adjusting the amplitude of different frequency components of a sound signal to reflect the sensitivity of the human ear to different frequencies")
   val signalAmplification by cv(100).between(0, 250)
      .def(name = "Signal amplification (%)", info = "")
   val signalThreshold by cv(-28).between(-100, 0)
      .def(name = "Signal threshold (db)", info = "")

   var frequencyStart by c(20).between(1, 24000)
      .def(name = "Frequency range start", info = "")
   var frequencyCenter by c(1000).between(1, 24000)
      .def(name = "Frequency range center", info = "")
   var frequencyEnd by c(16000).between(1, 24000)
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
      .def(name = "Bars effect - shifting", info = "Keep shifting bars along the shape to left/right by shape length per the time period ")
   var effectFade   by c(BarFade.NONE)
      .def(name = "Bars effect - fade", info = "Fade bars to 0 towards the edge")
   var barLineCap   by c(StrokeLineCap.ROUND)
      .def(name = "Bar line cap type", info = "")
   var barLineJoin  by c(StrokeLineJoin.ROUND)
      .def(name = "Bar line join type", info = "")

   val spectralColorPosition by cv(180).between(0, 360)
      .def(name = "Color spectral position (degrees)", info = "")
   val spectralColorRange by cv(180).between(-360, +360)
      .def(name = "Color spectral range (degrees)", info = "")
   val saturation by cv(100).between(0, 100)
      .def(name = "Color saturation (%)", info = "")
   val brightness by cv(100).between(0, 100)
      .def(name = "Color brightness (%)", info = "")
   val baseColor by cv(Color.color(0.1, 0.1, 0.1)!!).noUi()
      .def(name = "Color Base", info = "")

   val spectralFbProcessor = FrequencyBarsProcessor(this)
   val audioEngine = TarsosAudioEngine(this, spectralFbProcessor)
   val spectralView = SpectralView(this, spectralFbProcessor)

   init {
      root.lay += spectralView.canvas
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

   class SpectralView(val spektrum: Spektrum, val fft: FrequencyBarsProcessor) {
      private val settings = spektrum
              val canvas = canvas({})
      private val gc = canvas.graphicsContext2D!!
      private val volumeHistory = LinkedList<Double>()
      private val loop = Loop { time ->
         val barsRaw = fft.frequencyBars
         val barsAvg = barsRaw.sumOf { it.height }/barsRaw.count()
         val bars = when (spektrum.barData) {
            BarData.CONSTANT -> barsRaw.map { it.copy(height = 0.5) }
            BarData.CONSTANT_SINE -> barsRaw.mapIndexed { i, b -> b.copy(height = 0.5 * sin(2*PI*i.toDouble()/barsRaw.size).abs) }
            BarData.VOLUME -> barsRaw.map { it.copy(height = barsAvg) }
            BarData.VOLUME_SINE -> barsRaw.mapIndexed { i, b -> b.copy(height = barsAvg * sin(2*PI*i.toDouble()/barsRaw.size).abs) }
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
         DrawingData(settings, bars, time/1000000).updateBars()
      }

      fun DrawingData.updateBars() {
         val w = spektrum.root.width
         val h = spektrum.root.height
         val w2 = w/2
         val wh = w min h
         val wh2 = wh/2
         val barWg = w/barCount.toDouble()
         val barWg2 = barWg/2
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
                  barPositionsLow += P(barWg2+i*barWg, h/2 - f*bar.height*wh2)
                  barPositionsHig += P(barWg2+i*barWg, h/2 + f*bar.height*wh2)
               }
            }
            BarShape.BOTTOM -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  barPositionsLow += P(barWg2+i*barWg, h)
                  barPositionsHig += P(barWg2+i*barWg, h - f*bar.height*wh)
               }
            }
            BarShape.TOP -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  barPositionsLow += P(barWg2+i*barWg, 0.0)
                  barPositionsHig += P(barWg2+i*barWg, f*bar.height*wh)
               }
            }
            BarShape.CIRCLE_IN, BarShape.CIRCLE_MIDDLE, BarShape.CIRCLE_OUT -> {
               bars.forEachIndexed { i, bar ->
                  val f = fade(i)
                  val barH = f*bar.height*wh/2.0
                  val base = wh/5.0 + pulseEffect
                  val max = base + (if (settings.barAlignment==BarShape.CIRCLE_IN) 0.0 else barH/4.0)
                  val min = base - (if (settings.barAlignment==BarShape.CIRCLE_OUT) 0.0 else barH/8.0)
                  val barCos = cos(2*PI*i/barCount - PI/2)
                  val barSin = sin(2*PI*i/barCount - PI/2)
                  barPositionsLow += P(w2 + min*barCos, h/2 + min*barSin)
                  barPositionsHig += P(w2 + max*barCos, h/2 + max*barSin)

                  // fade effect, can we generalize this?
                  // gc.lineWidth = barW
                  // gc.stroke = LinearGradient(barPositionsLow[i].x, barPositionsLow[i].y, w/2 + max*1.5*barCos, h/2 + max*1.5*barSin, false, CycleMethod.NO_CYCLE, Stop(0.0, bar.color), Stop(1.0, Color.TRANSPARENT))
                  // gc.strokeLine(barPositionsLow[i].x, barPositionsLow[i].y, w/2 + max*1.5*barCos, h/2 + max*1.5*barSin)
                  // gc.lineWidth = 0.0
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
                  val sByRaw = (settings.effectShift.abs*it.size).toInt() % it.size
                  val sBy = if (settings.effectShift<0.0) it.size-sByRaw else sByRaw
                  val mBy = if (settings.effectMoving.toMillis()==0.0) 0 else {
                     val ms = settings.effectMoving.toMillis().abs.coerceAtLeast(1.0)
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

   data class FrequencyBar(var hz: Double, var height: Double, var color: Color)

   /**
    * This class holds state information regarding:
    * - timeFiltering a.k.a. smoothness
    * - previous bar heights that are used in bad decay calculation
    */
   class FrequencyBarsProcessor(settings: Spektrum) {
      private val settings = settings
      private val oldTime = System.currentTimeMillis()

      // the hzBins and the amplitudes come in pairs and access to them needs to be synchronized
      private val lock = ReentrantLock(true)

      // all the instances have the same input from the audio dispatcher
      private var frequencies: DoubleArray? = null
      private var amplitudes: DoubleArray? = null

      private var frequenciesOld: DoubleArray? = null
      private var amplitudesOld: DoubleArray? = null

      private val fftTimeFilter = FFTTimeFilter(settings)
      private val fftSpaceFilter = FFTSpaceFilter(settings)
      private val barsHeightCalculator = BarsHeightCalculator(settings)

      fun frame(frequencies: DoubleArray?, normalizedAmplitudes: DoubleArray?) {
         lock.withLock {
            this.frequencies = frequencies
            this.amplitudes = normalizedAmplitudes
         }
      }

      val frequencyBars: List<FrequencyBar>
         get() {
            var returnFrequencies: DoubleArray?
            var returnAmplitudes: DoubleArray?

            lock.withLock {
               if (frequencies==null) {
                  returnFrequencies = frequenciesOld
                  returnAmplitudes = amplitudesOld
               } else {
                  returnFrequencies = frequencies
                  returnAmplitudes = amplitudes
                  frequenciesOld = frequencies
                  amplitudesOld = amplitudes
                  frequencies = null
                  amplitudes = null
               }
            }

            return if (returnAmplitudes!=null) {
               returnAmplitudes = fftTimeFilter.filter(returnAmplitudes!!)
               returnAmplitudes = fftSpaceFilter.filter(returnAmplitudes!!)
               returnAmplitudes = barsHeightCalculator.processAmplitudes(returnAmplitudes!!)
               createFrequencyBars(returnFrequencies!!, returnAmplitudes!!)
            } else {
               ArrayList()
            }
         }

      fun createFrequencyBars(binsHz: DoubleArray, amplitudes: DoubleArray): List<FrequencyBar> {
         fun color(pos: Double, saturation: Double, brightness: Double): Color =
            Color.hsb(pos, saturation, brightness)
            // interpolate opacity based on intensity
            // .let { color -> settings.baseColor.value.interpolate(color, frequencyBars[i].height / settings.barMaxHeight.value) }

         val saturation = settings.saturation.value/100.0
         val brightness = settings.brightness.value/100.0
         var pos = settings.spectralColorPosition.value.toDouble()
         val posConnect = settings.barAlignment.connect
         val posBy = settings.spectralColorRange.value.toDouble()/binsHz.size * (if (posConnect) 2.0 else 1.0)
         return binsHz.indices.map {
            FrequencyBar(binsHz[it], amplitudes[it], color(pos, saturation, brightness)).apply {
               pos += posBy * (if (posConnect && it>binsHz.size/2) -1.0 else +1.0)
            }
         }
      }
   }
}

class TarsosAudioEngine(val settings: Spektrum, val fft: FrequencyBarsProcessor) {
   private var dispatcher: AudioDispatcher? = null
   private var audioThread: Thread? = null
   private var mixerRef: Mixer? = null

   fun start() {
      runTry {
         obtainMixer().ifNotNull { mixer ->
            mixerRef = mixer
            val audioFormat = AudioFormat(settings.sampleRate.value.toFloat(), settings.sampleSizeInBits.value, settings.audioFormatChannels, settings.audioFormatSigned, settings.audioFormatBigEndian)
            val uiFrameFrameSize = (1.seconds.toSeconds()/FPS*audioFormat.sampleRate).roundToInt()*audioFormat.frameSize//*audioFormat.channels
            val bufferSize = 2*uiFrameFrameSize // roughly 50ms
            val bufferOverlap = 1*uiFrameFrameSize // the remaining
            val line = obtainLine(mixer, audioFormat, bufferSize)
            val audioStream = JVMAudioInputStream(AudioInputStream(line))
            dispatcher = AudioDispatcher(audioStream, bufferSize, bufferOverlap).apply {
               addAudioProcessor(MultichannelToMono(audioFormat.channels, true))
               addAudioProcessor(FFTAudioProcessor(audioFormat, fft, settings))
            }
            audioThread = audioThreadFactory.start(dispatcher)
         }
      }.ifError {
         logger.warn(it) { "Using audio device '${settings.inputDevice.value}' failed" }
      }
   }

   fun stop() {
      try {
         dispatcher?.stop()
         audioThread?.join((1*1000).toLong()) // wait for audio dispatcher to finish
      } catch (e: InterruptedException) {
         logger.trace { "Thread=${Thread.currentThread().name} interrupted" }
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
      Thread.startVirtualThread {
         restart()
      }
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

   companion object: KLogging() {
      val audioThreadFactory = Thread.ofVirtual().name("spektrum-widget-audio-fft", 0)!!
   }

}

object OctaveGenerator {
   private val cache = ConcurrentHashMap<OctaveSettings, DoubleArray>()

   fun computeOctaveFrequencies(centerFrequency: Int, band: Double, lowerLimit: Int, upperLimit: Int): DoubleArray {
      val fStart = centerFrequency.clip(1, 23998).toDouble()
      val fEnd = upperLimit.clip(centerFrequency+2, 24000).toDouble()
      val fCenter = lowerLimit.toDouble().clip(fStart+1.0, fEnd-1.0)
      val octaveSettings = OctaveSettings(fCenter, band, fStart, fEnd)
      return cache.computeIfAbsent(octaveSettings) {
         TreeSet<Double>().apply {
            addLow(this, fCenter, band, fStart)
            addHigh(this, fCenter, band, fEnd)
         }.toList().toDoubleArray()
      }
   }

   fun computeLowLimit(center: Double, band: Double): Double = center/2.0.pow(1.0/(2*band))

   fun computeHighLimit(center: Double, band: Double): Double = center*2.0.pow(1.0/(2*band))

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

class FFTAudioProcessor(val audioFormat: AudioFormat, val onProcess: FrequencyBarsProcessor, val settings: Spektrum): AudioProcessor {
   private val interpolator: UnivariateInterpolator = SplineInterpolator()
   private val windowFunction: WindowFunction = HannWindow()
   private val windowCorrectionFactor = 2.0

   override fun process(e: AudioEvent): Boolean {
      // the buffer must be copied into another array for processing otherwise strange behaviour
      // the audioFloatBuffer buffer is reused because of the offset
      // modifying it will create strange issues
      val transformBuffer = FloatArray(e.floatBuffer.size)
      System.arraycopy(e.floatBuffer, 0, transformBuffer, 0, e.floatBuffer.size)
      val amplitudes = FloatArray(transformBuffer.size/2)

      val fft = FFT(transformBuffer.size, windowFunction)
      fft.forwardTransform(transformBuffer)
      fft.modulus(transformBuffer, amplitudes)

      val bins = DoubleArray(transformBuffer.size/2) { fft.binToHz(it, audioFormat.sampleRate) }
      val doublesAmplitudesFactor = 1.0/amplitudes.size*windowCorrectionFactor   // /amplitudes.size normalizes (n/2), *windowCorrectionFactor applies window correction
      val doublesAmplitudes = DoubleArray(amplitudes.size) { amplitudes[it].toDouble()*doublesAmplitudesFactor }

      val frequencies = computeOctaveFrequencies(settings.frequencyCenter, settings.octave.toDouble(), settings.frequencyStart, settings.frequencyEnd)
      val interpolateFunction = interpolator.interpolate(bins, doublesAmplitudes)

      val octave = settings.octave.toDouble()
      val maxLevel = settings.maxLevel
      val weightWindow = settings.weight.calculateAmplitudeWight

      val frequencyAmplitudes = DoubleArray(frequencies.size) {
         val frequency = frequencies[it]
         val frequencyMin = floor(computeLowLimit(frequency, octave)).toInt()
         val frequencyMax = floor(computeHighLimit(frequency, octave)).toInt()
         val frequencyRange = frequencyMin..frequencyMax
         var amplitude = frequencyRange.sumOf { runTry { interpolateFunction.value(it.toDouble()) }.getOr(0.0).pow(2) } // sum up range's individual "normalized window corrected" energies
         amplitude = if (maxLevel.value=="RMS") sqrt(amplitude/2) else sqrt(amplitude) // calculate the RMS of the amplitude
         amplitude = 20*log10(amplitude) // convert to logarithmic scale
         amplitude += weightWindow(frequency) // use weight to adjust the spectrum
         amplitude
      }

      onProcess.frame(frequencies, frequencyAmplitudes)
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

      val pixelAmplitudes = convertDbToPixels(newAmplitudes)
      oldAmplitudes = decayPixelsAmplitudes(oldAmplitudes!!, pixelAmplitudes, settings.millisToZero.value.toDouble(), millisPassed)
      return oldAmplitudes
   }

   private fun convertDbToPixels(dbAmplitude: DoubleArray): DoubleArray {
      val signalThreshold = settings.signalThreshold.value.abs.toDouble()
      val signalAmplification = settings.signalAmplification.value/100.0
      return DoubleArray(dbAmplitude.size) { signalAmplification*(dbAmplitude[it]+signalThreshold)/signalThreshold }
   }

   private fun decayPixelsAmplitudes(oldAmplitudes: DoubleArray, newAmplitudes: DoubleArray, millisToZero: Double, secondsPassed: Double): DoubleArray {
      val processedAmplitudes = DoubleArray(newAmplitudes.size)

      for (i in processedAmplitudes.indices) {
         val oldHeight = oldAmplitudes[i]
         val newHeight = newAmplitudes[i]
         val dbPerSecondDecay = secondsPassed/millisToZero
         if (newHeight<oldHeight - dbPerSecondDecay) {
            var decaySize = dbPerSecondDecay
            val accelerationStep: Double = 1.0/settings.accelerationFactor.value*decaySize
            if (oldDecayDecelerationSize[i] + accelerationStep<dbPerSecondDecay) {
               oldDecayDecelerationSize[i] = oldDecayDecelerationSize[i] + accelerationStep
               decaySize = oldDecayDecelerationSize[i]
            }
            processedAmplitudes[i] = (oldHeight - decaySize).clip(0.0, 1.0)
         } else {
            processedAmplitudes[i] = newHeight.clip(0.0, 1.0)
            oldDecayDecelerationSize[i] = 0.0
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
enum class WeightWindow(val infoUi: String, val calculateAmplitudeWight: (Double) -> Double) {
   dBA(
   "A-weighted scale, which is most commonly used for assessing environmental and occupational noise exposure, as well as assessing sound levels from audio equipment and consumer products like headphones and speakers",
   { f ->
      val raf = (12194.0.p2*f.p4/((f.p2 + 20.6.p2)*sqrt((f.p2 + 107.7.p2)*(f.p2 + 737.9.p2))*(f.p2 + 12194.0.p2)))
      20*log10(raf) + 2.00
   }),
   dBB(
   "B-weighted scale, which is rarely used today",
   { f ->
      val rbf = (12194.0.p2*f.p3/((f.p2 + 20.6.p2)*sqrt(f.p2 + 158.5.p2)*(f.p2 + 12194.0.p2)))
      20*log10(rbf) + 0.17
   }),
   dBC(
   "C-weighted scale, which has a flatter frequency response than A-weighting and is used for measuring very high sound pressure levels, such as from explosions or jet engines",
   { f ->
      val rcf = (12194.0.p2*f.p2/((f.p2 + 20.6.p2)*(f.p2 + 12194.0.p2)))
      20*log10(rcf) + 0.06
   }),
   dBZ(
   "Zero-weighting scale, which does not apply any frequency weighting to the sound signal and represents the total sound energy at all frequencies",
   {
      0.0
   })
}

val Double.p2: Double get() = pow(2.0)
val Double.p3: Double get() = pow(3.0)
val Double.p4: Double get() = pow(4.0)