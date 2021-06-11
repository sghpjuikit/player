package spektrum

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HannWindow
import be.tarsos.dsp.util.fft.WindowFunction
import java.util.LinkedList
import java.util.Objects
import java.util.Queue
import java.util.TreeSet
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.IntStream
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator
import sp.it.pl.layout.widget.Widget
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconMD
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.animation.Loop
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.max
import sp.it.util.conf.min
import sp.it.util.conf.values
import sp.it.util.conf.valuesIn
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.units.version
import sp.it.util.units.year
import spektrum.AmplitudeWeightCalculator.WeightWindow
import spektrum.AmplitudeWeightCalculator.WeightWindow.dBZ
import spektrum.Spektrum.BarPos

class Spektrum(widget: Widget): SimpleController(widget) {

   val inputDevice by cv(AppConfig.inputDevice).sync { AppConfig.inputDevice = it }.valuesIn { AudioSystem.getMixerInfo().asSequence().filter { it.description.contains("Capture") }.map { it.name } }
      .def(name = "Audio input device", info = "")
   val sampleRate by cv(AppConfig.sampleRate).sync { AppConfig.sampleRate = it }.values(listOf(44100, 48000))
      .def(name = "Audio sample rate", info = "")
   val bufferSize by cv(AppConfig.bufferSize).sync { AppConfig.bufferSize = it }.between(32, 24000)
      .def(name = "Audio buffer size", info = "")
   val bufferOverlap by cv(AppConfig.bufferOverlap).sync { AppConfig.bufferOverlap = it }.between(0, 23744)
      .def(name = "Audio buffer overlap", info = "")
   val zeroPadding by cv(AppConfig.zeroPadding).sync { AppConfig.zeroPadding = it }.between(0, 12256)
      .def(name = "Audio fft zero padding", info = "")
   val maxLevel by cv(AppConfig.maxLevel).sync { AppConfig.maxLevel = it }.values(listOf("RMS", "Peak"))
      .def(name = "Signal max level", info = "")
   val weight by cv(AppConfig.weight).sync { AppConfig.weight = it }
      .def(name = "Signal weighting", info = "")
   val signalAmplification by cv(AppConfig.signalAmplification).sync { AppConfig.signalAmplification = it }.between(0, 250)
      .def(name = "Signal amplification (factor)", info = "")
   val signalThreshold by cv(AppConfig.signalThreshold).sync { AppConfig.signalThreshold = it }.between(-90, 0)
      .def(name = "Signal threshold (db)", info = "")

   val frequencyStart by cv(AppConfig.frequencyStart).sync { AppConfig.frequencyStart = it }.between(0, 24000)
      .def(name = "Frequency start", info = "")
   val frequencyCenter by cv(AppConfig.frequencyCenter).sync { AppConfig.frequencyCenter = it }.between(0, 24000)
      .def(name = "Frequency center", info = "")
   val frequencyEnd by cv(AppConfig.frequencyEnd).sync { AppConfig.frequencyEnd = it }.between(0, 24000)
      .def(name = "Frequency end", info = "")
   val octave by cv(AppConfig.octave).sync { AppConfig.octave = it }.min(0).max(48)
      .def(name = "Octave (1/n)", info = "")
   val resolutionHighQuality by cv(AppConfig.resolutionHighQuality).sync { AppConfig.resolutionHighQuality = it }
      .def(name = "Frequency precise resolution", info = "")

   val pixelsPerSecondDecay by cv(AppConfig.pixelsPerSecondDecay).sync { AppConfig.pixelsPerSecondDecay = it }.between(0, 2000)
      .def(name = "Animation decay (pixels/s)", info = "")
   val accelerationFactor by cv(AppConfig.accelerationFactor).sync { AppConfig.accelerationFactor = it }.between(0, 50)
      .def(name = "Animation decay acceleration (1/n)", info = "")
   val timeFilterSize by cv(AppConfig.timeFilterSize).sync { AppConfig.timeFilterSize = it }.between(0, 100)
      .def(name = "Animation smoothness", info = "")

   val minBarHeight by cv(AppConfig.minBarHeight).sync { AppConfig.minBarHeight = it }.min(0)
      .def(name = "Bar min height", info = "")
   val maxBarHeight by cv(AppConfig.maxBarHeight).sync { AppConfig.maxBarHeight = it }.min(0)
      .def(name = "Bar max height", info = "")
   val barGap       by cv(AppConfig.barGap).sync { AppConfig.barGap = it }.min(0)
      .def(name = "Bar gap", info = "")
   val barAlignment by cv(AppConfig.barAlignment).sync { AppConfig.barAlignment = it }
      .def(name = "Bar alignment", info = "")

   val spectralColorPosition by cv(AppConfig.spectralColorPosition).sync { AppConfig.spectralColorPosition = it }.between(0, 360)
      .def(name = "Color spectral position (degrees)", info = "")
   val spectralColorRange by cv(AppConfig.spectralColorRange).sync { AppConfig.spectralColorRange = it }.between(0, 360)
      .def(name = "Color spectral range (degrees)", info = "")
   val spectralColorInverted by cv(AppConfig.spectralColorInverted).sync { AppConfig.spectralColorInverted = it }
      .def(name = "Color spectral inverted", info = "")
   val saturation by cv(AppConfig.saturation).sync { AppConfig.saturation = it }.between(0, 100)
      .def(name = "Color saturation (%)", info = "")
   val brightness by cv(AppConfig.brightness).sync { AppConfig.brightness = it }.between(0, 100)
      .def(name = "Color brightness (%)", info = "")
   val baseColor by cv(AppConfig.baseColor).sync { AppConfig.baseColor = it }
      .def(name = "Color Base", info = "")

   init {
      val spectralFFTService = FrequencyBarsFFTService()
      val audioEngine = TarsosAudioEngine()
      val spectralView = SpectralView(this, spectralFFTService)

      root.lay += spectralView.apply {
         heightProperty() syncFrom spektrum.root.heightProperty() on spektrum.onClose
         widthProperty() syncFrom spektrum.root.widthProperty() on spektrum.onClose
      }

      audioEngine.fttListenerList += spectralFFTService

      root.sync1IfInScene {
         onClose += audioEngine::stop
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
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO, VISUALISATION)
      override val summaryActions = listOf<Entry>()
   }

   class SpectralView(val spektrum: Spektrum, val spectralFFTService: FrequencyBarsFFTService): Canvas() {
      private val loop = Loop({ _ -> updateBars(spectralFFTService.frequencyBarList) })
      private val gc = graphicsContext2D!!

      init {
         spektrum.root.heightProperty() attach { AppConfig.maxBarHeight = it.toDouble() }
      }

      fun updateBars(frequencyBarList: List<FrequencyBar>) {
         val w = spektrum.root.width
         val h = spektrum.root.height
         val barCount = frequencyBarList.size
         val barW = w/barCount.toDouble()
         gc.clearRect(0.0, 0.0, w, h)

         gc.save()
         frequencyBarList.forEachIndexed { i, bar ->
            val barH = when (AppConfig.barAlignment) {
               BarPos.CIRCLE_IN, BarPos.CIRCLE_MIDDLE, BarPos.CIRCLE_OUT -> bar.height*min(h, w)/2.0/AppConfig.maxBarHeight
               else -> bar.height
            }
            gc.fill = bar.color
            gc.stroke = bar.color
            when (AppConfig.barAlignment) {
               BarPos.CENTER -> gc.fillRect(i*(barW+AppConfig.barGap), (h-barH)/2, barW, barH)
               BarPos.BOTTOM -> gc.fillRect(i*(barW+AppConfig.barGap), h-barH, barW, barH)
               BarPos.TOP -> gc.fillRect(i*(barW+AppConfig.barGap), 0.0, barW, barH)
               BarPos.CIRCLE_IN, BarPos.CIRCLE_MIDDLE, BarPos.CIRCLE_OUT -> {
                  gc.lineWidth = (barW - AppConfig.barGap) max 1.0
                  val max = min(h, w)/4.0*1.5 + (if (AppConfig.barAlignment==BarPos.CIRCLE_IN) 0.0 else barH/4.0)
                  val min = min(h, w)/4.0*1.5 - (if (AppConfig.barAlignment==BarPos.CIRCLE_OUT) 0.0 else barH/8.0)
                  val barCos = cos(i*2*PI/barCount)
                  val barSin = sin(i*2*PI/barCount)
                  gc.strokeLine(w/2 + max*barCos, h/2 + max*barSin, w/2 + min*barCos, h/2 + min*barSin)
                  gc.lineWidth = 0.0
               }
            }
         }
         gc.restore()
      }

      fun play() = loop.start()
      fun stop() = loop.stop()
   }

   enum class BarPos {
      BOTTOM, CENTER, TOP, CIRCLE_OUT, CIRCLE_MIDDLE, CIRCLE_IN
   }
}

class TarsosAudioEngine {
   private var dispatcher: AudioDispatcher? = null
   private var audioThread: Thread? = null
   val fttListenerList = LinkedList<FFTListener>()

   fun start() {
      runTry {
         val mixer = mixer
         val audioFormat = AudioFormat(AppConfig.sampleRate.toFloat(), AppConfig.sampleSizeInBits, AppConfig.channels, AppConfig.signed, AppConfig.bigEndian)
         val line = getLine(mixer, audioFormat, AppConfig.bufferSize)
         val stream = AudioInputStream(line)
         val audioStream = JVMAudioInputStream(stream)
         dispatcher = AudioDispatcher(audioStream, AppConfig.bufferSize, AppConfig.bufferOverlap).apply {
            addAudioProcessor(AudioEngineRestartProcessor(this@TarsosAudioEngine))
            addAudioProcessor(FFTAudioProcessor(audioFormat, fttListenerList))
         }
         audioThread = Thread(dispatcher, "Audio dispatching").apply {
            isDaemon = true
            start()
         }
      }
   }

   fun stop() {
      try {
         dispatcher?.stop()
         audioThread?.join((1*1000).toLong()) // wait for audio dispatcher to finish // TODO: remove?
      } catch (e: InterruptedException) {
      }
   }

   fun restart() {
      stop()
      start()
   }

   private val mixer: Mixer
      get() = AudioSystem.getMixerInfo().find { AppConfig.inputDevice in it.name }
         ?.net { info: Mixer.Info? -> AudioSystem.getMixer(info) }!!

   private fun getLine(mixer: Mixer, audioFormat: AudioFormat, lineBuffer: Int): TargetDataLine {
      val lineInfo = mixer.targetLineInfo.firstOrNull()
      val line = mixer.getLine(lineInfo) as TargetDataLine
      line.open(audioFormat, lineBuffer)
      line.start()
      return line
   }
}

object OctaveGenerator {

   private val cache: MutableMap<OctaveSettings, List<Double>> = HashMap()

   fun getOctaveFrequencies(centerFrequency_: Double, band: Double, lowerLimit_: Double, upperLimit_: Double): List<Double> {
      // set limits
      var centerFrequency = centerFrequency_
      var lowerLimit = lowerLimit_
      var upperLimit = upperLimit_
      if (lowerLimit<1) {
         lowerLimit = 1.0
      }
      if (upperLimit<1) {
         upperLimit = 1.0
      }
      if (centerFrequency<1) {
         centerFrequency = 1.0
      }
      val octaveSettings = OctaveSettings(centerFrequency, band, lowerLimit, upperLimit)
      val doubles = cache[octaveSettings]
      return if (doubles==null) {
         val octave: MutableSet<Double> = TreeSet()
         addLow(octave, centerFrequency, band, lowerLimit)
         addHigh(octave, centerFrequency, band, upperLimit)

         // if center is 1000 but upper limit is 80 then we need to filter out 80 to 1000 frequencies
         val finalLowerLimit = lowerLimit
         val finalUpperLimit = upperLimit
         val octaves = octave.filter { it in finalLowerLimit..finalUpperLimit }
         cache[octaveSettings] = octaves
         octaves
      } else {
         doubles
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

   private class OctaveSettings(private val centerFrequency: Double, private val band: Double, private val lowerLimit: Double, private val upperLimit: Double) {
      override fun equals(other: Any?): Boolean {
         if (this===other) return true
         if (other==null || javaClass!=other.javaClass) return false
         val that = other as OctaveSettings
         return that.centerFrequency.compareTo(centerFrequency)==0 && that.band.compareTo(band)==0 && that.lowerLimit.compareTo(lowerLimit)==0 && that.upperLimit.compareTo(upperLimit)==0
      }

      override fun hashCode(): Int {
         return Objects.hash(centerFrequency, band, lowerLimit, upperLimit)
      }
   }
}

class GlobalColorCalculator {
   fun getGlobalColor(frequencyBars: List<FrequencyBar>, startHz: Int, endHz: Int, peak: Peak): Color {
      if (frequencyBars.isEmpty())
         return Color.BLACK

      var sumIntensity = 0.0
      var maxIntensity = 0.0
      var sumRed = 0.0
      var sumGreen = 0.0
      var sumBlue = 0.0
      var nrBars = 0
      for (frequencyBar in frequencyBars) {
         if (startHz<=frequencyBar.hz
            && frequencyBar.hz<=endHz) {
            val barHeight = frequencyBar.height
            val barIntensity = barHeight/AppConfig.maxBarHeight
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
         Peak.AVG -> avgIntensity
         Peak.MAX -> maxIntensity
      }
      intensity *= (AppConfig.brightness/100.0)
      val baseColor = AppConfig.baseColor
      var color = Color.color(avgRed, avgGreen, avgBlue)
      color = baseColor.interpolate(color, maxIntensity)
      color = Color.hsb(color.hue, color.saturation, intensity)
      return color
   }

   enum class Peak { AVG, MAX }
}

class FrequencyBar(var hz: Double, var height: Double, var color: Color)

/**
 * This class holds state information regarding to:
 * - timeFiltering a.k.a smoothness
 * - previous bar heights that are used in bad decay calculation
 */
class FrequencyBarsFFTService: FFTListener {
   private val oldTime = System.currentTimeMillis()

   // the hzBins and the amplitudes come in pairs and access to them needs to be synchronized
   private val lock = ReentrantLock(true)

   // all of the instances have the same input from the audio dispatcher
   private var hzBins: DoubleArray? = null
   private var amplitudes: DoubleArray? = null
   private val fftTimeFilter = FFTTimeFilter()
   private val barsHeightCalculator = BarsHeightCalculator()

   override fun frame(hzBins: DoubleArray?, normalizedAmplitudes: DoubleArray?) {
      try {
         lock.lock()
         this.hzBins = hzBins
         amplitudes = normalizedAmplitudes
      } finally {
         lock.unlock()
      }
   }

   // return empty array
   val frequencyBarList: List<FrequencyBar>
      get() {
         val returnBinz: DoubleArray?
         var returnAmplitudes: DoubleArray?
         try {
            lock.lock()
            returnBinz = hzBins
            returnAmplitudes = amplitudes
         } finally {
            lock.unlock()
         }
         val frequencyBars: List<FrequencyBar>
         if (returnAmplitudes!=null) {
            returnAmplitudes = fftTimeFilter.filter(returnAmplitudes)
            returnAmplitudes = barsHeightCalculator.processAmplitudes(returnAmplitudes)
            frequencyBars = FrequencyBarsCreator.createFrequencyBars(returnBinz!!, returnAmplitudes!!)
         } else {
            // return empty array
            frequencyBars = ArrayList()
         }
         return frequencyBars
      }
}

interface FFTListener {
   fun frame(hzBins: DoubleArray?, normalizedAmplitudes: DoubleArray?)
}

object FrequencyBarsCreator {
   fun createFrequencyBars(binsHz: DoubleArray, amplitudes: DoubleArray): List<FrequencyBar> {
      val frequencyBars: MutableList<FrequencyBar> = java.util.ArrayList<FrequencyBar>(binsHz.size)
      for (i in binsHz.indices) {
         frequencyBars.add(FrequencyBar(binsHz[i], amplitudes[i], Color.BLACK))
      }
      var pos = AppConfig.spectralColorPosition.toDouble()
      val range = AppConfig.spectralColorRange.toDouble()
      val saturation = AppConfig.saturation/100.0
      val brightness = AppConfig.brightness/100.0
      val inverted = AppConfig.spectralColorInverted
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

   private fun setColor(frequencyBars: List<FrequencyBar>, pos: Double, saturation: Double, brightness: Double, i: Int) {
      val color = Color.hsb(pos, saturation, brightness)
      // interpolate opacity based on intensity
      // color = SpectralColorConfig.baseColor.interpolate(color, frequencyBars.get(i).getHeight() / AppConfig.maxBarHeight);
      frequencyBars[i].color = color
   }
}

class FFTAudioProcessor(private val audioFormat: AudioFormat, private val listenerList: List<FFTListener>): AudioProcessor {
   private val interpolator: UnivariateInterpolator = SplineInterpolator()
   private val windowFunction: WindowFunction = HannWindow()
   private val windowCorrectionFactor = 2.00
   override fun process(audioEvent: AudioEvent): Boolean {
      val interpolation = AppConfig.zeroPadding
      val audioFloatBuffer = audioEvent.floatBuffer

      // the buffer must be copied into another array for processing otherwise strange behaviour
      // the audioFloatBuffer buffer is reused because of the offset
      // modifying it will create strange issues
      val transformBuffer = FloatArray(audioFloatBuffer.size + interpolation)
      System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.size)
      val amplitudes = FloatArray(transformBuffer.size/2)
      val fft = FFT(transformBuffer.size, windowFunction)
      fft.forwardTransform(transformBuffer)
      fft.modulus(transformBuffer, amplitudes)
      val bins = IntStream.range(0, transformBuffer.size/2).mapToDouble { i: Int -> fft.binToHz(i, audioFormat.sampleRate) }.toArray()
      val doublesAmplitudes = IntStream.range(0, amplitudes.size).mapToDouble { value: Int -> amplitudes[value].toDouble() }.toArray()
      val frequencyBins: DoubleArray
      val frequencyAmplitudes: DoubleArray
      if (AppConfig.octave>0) {
         val octaveFrequencies = OctaveGenerator.getOctaveFrequencies(AppConfig.frequencyCenter.toDouble(), AppConfig.octave.toDouble(), AppConfig.frequencyStart.toDouble(), AppConfig.frequencyEnd.toDouble())
         frequencyBins = DoubleArray(octaveFrequencies.size)
         frequencyAmplitudes = DoubleArray(octaveFrequencies.size)

         // calculate the frequency step
         // lowLimit/highLimit this is the resolution for interpolating and summing bins
         val lowLimit = OctaveGenerator.getLowLimit(octaveFrequencies[0], AppConfig.octave.toDouble())
         var step = 2.0.pow(1.0/AppConfig.octave)

         // improve resolution at the cost of performance
         if (AppConfig.resolutionHighQuality)
            step /= (AppConfig.octave/2.0)

         // k is the frequency index
         var k = lowLimit

         // m is the position in the frequency vectors
         var m = 0

         // setup the interpolator
         val interpolateFunction = interpolator.interpolate(bins, doublesAmplitudes)
         for (i in octaveFrequencies.indices) {
            frequencyBins[m] = octaveFrequencies[i]
            val highLimit = OctaveGenerator.getHighLimit(octaveFrequencies[i], AppConfig.octave.toDouble())

            // group bins together
            while (k<highLimit) {
               var amplitude = interpolateFunction.value(k)
               amplitude /= doublesAmplitudes.size // normalize (n/2)
               amplitude *= windowCorrectionFactor // apply window correction
               frequencyAmplitudes[m] = frequencyAmplitudes[m] + amplitude.pow(2.0) // sum up the "normalized window corrected" energy
               k += step

               // reached upper limit
               if (k>AppConfig.frequencyEnd || k>bins[bins.size - 1]) {
                  break
               }
            }
            frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m]) // square root the energy
            if (AppConfig.maxLevel=="RMS") {
               frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m].pow(2.0)/2) // calculate the RMS of the amplitude
            }
            frequencyAmplitudes[m] = 20*log10(frequencyAmplitudes[m]) // convert to logarithmic scale
            frequencyAmplitudes[m] += AmplitudeWeightCalculator.getDbWeight(frequencyBins[m], AppConfig.weight) // use weight to adjust the spectrum
            m++
         }
      } else {
         var n = 0
         for (i in bins.indices) {
            val frequency = fft.binToHz(i, audioFormat.sampleRate)
            if (AppConfig.frequencyStart<=frequency && frequency<=AppConfig.frequencyEnd) {
               n++
            } else if (frequency>AppConfig.frequencyEnd) {
               break
            }
         }
         frequencyBins = DoubleArray(n)
         frequencyAmplitudes = DoubleArray(n)
         var m = 0
         for (i in bins.indices) {
            val frequency = fft.binToHz(i, audioFormat.sampleRate)
            if (AppConfig.frequencyStart<=frequency && frequency<=AppConfig.frequencyEnd) {
               frequencyBins[m] = frequency
               frequencyAmplitudes[m] = doublesAmplitudes[i]
               frequencyAmplitudes[m] = frequencyAmplitudes[m]/doublesAmplitudes.size // normalize (n/2)
               frequencyAmplitudes[m] = frequencyAmplitudes[m]*windowCorrectionFactor // apply window correction
               if (AppConfig.maxLevel=="RMS") {
                  frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m].pow(2.0)/2) // calculate the RMS of the amplitude
               }
               frequencyAmplitudes[m] = 20*log10(frequencyAmplitudes[m]) // convert to logarithmic scale
               frequencyAmplitudes[m] += AmplitudeWeightCalculator.getDbWeight(frequencyBins[m], AppConfig.weight) // use weight to adjust the spectrum
               m++
            }
         }
      }
      listenerList.forEach { listener: FFTListener -> listener.frame(frequencyBins, frequencyAmplitudes) }
      return true
   }

   override fun processingFinished() {}
}

class FFTTimeFilter {
   private val historyAmps: Queue<DoubleArray?> = LinkedList()

   fun filter(amps: DoubleArray): DoubleArray {
      val timeFilterSize = AppConfig.timeFilterSize
      if (timeFilterSize<2) {
         return amps
      }
      if (historyAmps.peek()?.size!=amps.size) {
         historyAmps.clear()
      }
      if (historyAmps.size<timeFilterSize) {
         historyAmps.offer(amps)
         return amps
      }
      while (historyAmps.size>timeFilterSize) {
         historyAmps.poll()
      }
      historyAmps.poll()
      historyAmps.offer(amps)
      val filtered = DoubleArray(amps.size)
      for (i in amps.indices) {
         var sumTimeFilteredAmp = 0.0
         for (currentHistoryAmps in historyAmps) {
            sumTimeFilteredAmp += currentHistoryAmps!![i]
         }
         filtered[i] = sumTimeFilteredAmp/historyAmps.size
      }
      return filtered
   }
}

class BarsHeightCalculator {
   private var oldTime = System.nanoTime()
   private var oldAmplitudes: DoubleArray? = null
   private lateinit var oldDecayFactor: DoubleArray
   fun processAmplitudes(newAmplitudes: DoubleArray): DoubleArray? {
      // init on first run or if number of newAmplitudes has changed
      if (oldAmplitudes==null || oldAmplitudes!!.size!=newAmplitudes.size) {
         oldAmplitudes = newAmplitudes
         oldDecayFactor = DoubleArray(newAmplitudes.size)
         return convertDbToPixels(newAmplitudes)
      }
      val pixelsPerSecondDecay = AppConfig.pixelsPerSecondDecay
      val secondsPassed = secondsPassed
      val pixelAmplitudes = convertDbToPixels(newAmplitudes)
      oldAmplitudes = decayPixelsAmplitudes(oldAmplitudes!!, pixelAmplitudes, pixelsPerSecondDecay.toDouble(), secondsPassed)
      return oldAmplitudes
   }

   private fun convertDbToPixels(dbAmplitude: DoubleArray): DoubleArray {
      val signalThreshold = AppConfig.signalThreshold
      val maxBarHeight = AppConfig.maxBarHeight
      val signalAmplification = AppConfig.signalAmplification
      val minBarHeight = AppConfig.minBarHeight
      val pixelsAmplitude = DoubleArray(dbAmplitude.size)
      for (i in pixelsAmplitude.indices) {
         val maxHeight = abs(signalThreshold).toDouble()
         var newHeight = dbAmplitude[i]
         newHeight += abs(signalThreshold)
         newHeight *= (signalAmplification/100.0)
         newHeight = newHeight*maxBarHeight/maxHeight
         //            newHeight = Math.round(newHeight);

         // apply limits
         if (newHeight>maxBarHeight) {
            // ceiling hit
            newHeight = maxBarHeight
         } else if (newHeight<minBarHeight) {
            // below floor
            newHeight = minBarHeight.toDouble()
         }
         pixelsAmplitude[i] = newHeight
      }
      return pixelsAmplitude
   }

   private fun decayPixelsAmplitudes(oldAmplitudes: DoubleArray, newAmplitudes: DoubleArray, pixelsPerSecond: Double, secondsPassed: Double): DoubleArray {
      val processedAmplitudes = DoubleArray(newAmplitudes.size)
      for (i in processedAmplitudes.indices) {
         val oldHeight = oldAmplitudes[i]
         val newHeight = newAmplitudes[i]
         if (newHeight>=oldHeight) {
            processedAmplitudes[i] = newHeight
            oldDecayFactor[i] = 0.0
         } else {
            //                double dbPerSecondDecay = (pixelsPerSecond + (0.01 * Math.pow(1.15, i) - 0.01)) * secondsPassed; // experiment with logarithmic function
            var dbPerSecondDecay = pixelsPerSecond*secondsPassed
            if (AppConfig.accelerationFactor>0 && oldDecayFactor[i]<1) {
               val accelerationStep = 1.0/AppConfig.accelerationFactor
               oldDecayFactor[i] = oldDecayFactor[i] + accelerationStep
               dbPerSecondDecay *= oldDecayFactor[i]
            }
            if (newHeight>oldHeight - dbPerSecondDecay) {
               processedAmplitudes[i] = newHeight
               oldDecayFactor[i] = 0.0
            } else {
               processedAmplitudes[i] = oldHeight - dbPerSecondDecay
            }
         }
      }
      return processedAmplitudes
   }

   // convert nano to ms to seconds
   private val secondsPassed: Double
      get() {
         val newTime = System.nanoTime()
         val deltaTime = newTime - oldTime
         oldTime = newTime
         // convert nano to ms to seconds
         return deltaTime/1000000.0/1000.0
      }
}

object AppConfig {
   // audio format
   var inputDevice: String = "Primary Sound Capture"
   var sampleRate: Int = 48000
   var sampleSizeInBits: Int = 16
   var channels: Int = 1
   var signed: Boolean = true
   var bigEndian: Boolean = false
   // audio buffer settings
   var bufferSize: Int = 6000
   var bufferOverlap: Int = 4976
   // audio processinng
   var frequencyStart: Int = 35
   var frequencyCenter: Int = 1000
   var frequencyEnd: Int = 17000
   var octave: Int = 25
   var resolutionHighQuality: Boolean = false
   // audio level
   var maxLevel: String = "RMS"
   var weight: WeightWindow = dBZ
   // color
   var spectralColorPosition: Int = 180
   var spectralColorRange: Int = 360
   var saturation: Int = 100
   var brightness: Int = 100
   var spectralColorInverted: Boolean = false
   var baseColor = Color.color(0.1, 0.1, 0.1)!!
   // bars
   var minBarHeight: Int = 3
   var maxBarHeight: Double = 750.0
   var barGap: Int = 8
   var barAlignment: BarPos = BarPos.CIRCLE_MIDDLE
   // bars animation
   var pixelsPerSecondDecay: Int = 250
   var accelerationFactor: Int = 10
   // audio signal processing
   var signalAmplification: Int = 138
   var signalThreshold: Int = -42
   // fft
   var timeFilterSize: Int = 2
   var interpolationResolution: Double = 6.0
   var zeroPadding: Int = 0
}

object AmplitudeWeightCalculator {
   fun getDbWeight(frequency: Double, weight: WeightWindow?): Double = when (weight) {
      WeightWindow.dBA -> getDbA(frequency)
      WeightWindow.dBB -> getDbB(frequency)
      WeightWindow.dBC -> getDbC(frequency)
      else -> 0.0 // dbz, no weight
   }

   fun getDbA(frequency: Double): Double {
      val raf = (12194.0.pow(2.0)*frequency.pow(4.0)
         /((frequency.pow(2.0) + 20.6.pow(2.0))
         *sqrt((frequency.pow(2.0) + 107.7.pow(2.0))*(frequency.pow(2.0) + 737.9.pow(2.0)))
         *(frequency.pow(2.0) + 12194.0.pow(2.0))))
      return 20*log10(raf) + 2.00
   }

   fun getDbB(frequency: Double): Double {
      val rbf = (12194.0.pow(2.0)*frequency.pow(3.0)
         /((frequency.pow(2.0) + 20.6.pow(2.0))
         *sqrt(frequency.pow(2.0) + 158.5.pow(2.0))
         *(frequency.pow(2.0) + 12194.0.pow(2.0))))
      return 20*log10(rbf) + 0.17
   }

   fun getDbC(frequency: Double): Double {
      val rcf = (12194.0.pow(2.0)*frequency.pow(2.0)
         /((frequency.pow(2.0) + 20.6.pow(2.0))
         *(frequency.pow(2.0) + 12194.0.pow(2.0))))
      return 20*log10(rcf) + 0.06
   }

   @Suppress("EnumEntryName")
   enum class WeightWindow(val window: String) {
      dBA("dBA"),
      dBB("dBB"),
      dBC("dBC"),
      dBZ("dBZ"),
   }
}

class AudioEngineRestartProcessor(audioEngine: TarsosAudioEngine): AudioProcessor {
   private var inputDevice: String = AppConfig.inputDevice
   private var sampleRate: Int = AppConfig.sampleRate
   private var bufferSize: Int = AppConfig.bufferSize
   private var bufferOverlap: Int = AppConfig.bufferOverlap
   private val audioEngine: TarsosAudioEngine = audioEngine

   override fun process(audioEvent: AudioEvent?): Boolean {
      if (isChangeDetected) {
         // use another thread to restart the audio engine
         val thread = Thread { audioEngine.restart() }
         thread.isDaemon = true
         thread.start()
      }
      return true
   }

   override fun processingFinished() {}

   private val isChangeDetected: Boolean
      get() {
         var isChanged = false
         if (inputDevice!=AppConfig.inputDevice) {
            inputDevice = AppConfig.inputDevice
            isChanged = true
         }
         if (sampleRate!=AppConfig.sampleRate) {
            sampleRate = AppConfig.sampleRate
            isChanged = true
         }
         if (bufferSize!=AppConfig.bufferSize) {
            bufferSize = AppConfig.bufferSize
            isChanged = true
         }
         if (bufferOverlap!=AppConfig.bufferOverlap) {
            bufferOverlap = AppConfig.bufferOverlap
            isChanged = true
         }
         return isChanged
      }

}