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
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.units.version
import sp.it.util.units.year
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
   val zeroPadding by cv(0).between(0, 12256)
      .def(name = "Audio fft zero padding", info = "")
   val maxLevel by cv("RMS").values(listOf("RMS", "Peak"))
      .def(name = "Signal max level", info = "")
   var weight by c(dBZ)
      .def(name = "Signal weighting", info = "")
   val signalAmplification by cv(138).between(0, 250)
      .def(name = "Signal amplification (factor)", info = "")
   val signalThreshold by cv(-42).between(-90, 0)
      .def(name = "Signal threshold (db)", info = "")

   var frequencyStart by c(35).between(0, 24000)
      .def(name = "Frequency start", info = "")
   var frequencyCenter by c(1000).between(0, 24000)
      .def(name = "Frequency center", info = "")
   var frequencyEnd by c(17000).between(0, 24000)
      .def(name = "Frequency end", info = "")
   var octave by c(25).between(0, 48)
      .def(name = "Octave (1/n)", info = "")
   var resolutionHighQuality by c(false)
      .def(name = "Frequency precise resolution", info = "")

   val pixelsPerSecondDecay by cv(250).between(0, 2000)
      .def(name = "Animation decay (pixels/s)", info = "")
   val accelerationFactor by cv(10).between(0, 50)
      .def(name = "Animation decay acceleration (1/n)", info = "")
   val timeFilterSize by cv(2).between(0, 100)
      .def(name = "Animation smoothness", info = "")

   val barMinHeight by cv(3.0).min(0.0)
      .def(name = "Bar min height", info = "")
   val barMaxHeight by cv(750.0).min(0.0)
      .def(name = "Bar max height", info = "")
   val barGap       by cv(8).min(0)
      .def(name = "Bar gap", info = "")
   val barAlignment by cv(BarPos.CIRCLE_MIDDLE)
      .def(name = "Bar alignment", info = "")

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
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO, VISUALISATION)
      override val summaryActions = listOf<Entry>()

      val openLines = mutableMapOf<Mixer, LineUses>()

      override fun dispose() {
         super.dispose()
         openLines.values.forEach { it.line.close() }
      }
   }

   data class LineUses(val line: TargetDataLine, var count: Int)

   class SpectralView(val spektrum: Spektrum, val spectralFFTService: FrequencyBarsFFTService): Canvas() {
      private val settings = spektrum
      private val loop = Loop({ _ -> updateBars(spectralFFTService.frequencyBarList) })
      private val gc = graphicsContext2D!!

      init {
         spektrum.root.heightProperty() attach { settings.barMaxHeight.value = it.toDouble() }
      }

      fun updateBars(frequencyBarList: List<FrequencyBar>) {
         val w = spektrum.root.width
         val h = spektrum.root.height
         val barCount = frequencyBarList.size
         val barW = w/barCount.toDouble()
         gc.clearRect(0.0, 0.0, w, h)

         gc.save()
         frequencyBarList.forEachIndexed { i, bar ->
            val barH = when (settings.barAlignment.value) {
               BarPos.CIRCLE_IN, BarPos.CIRCLE_MIDDLE, BarPos.CIRCLE_OUT -> bar.height*min(h, w)/2.0/settings.barMaxHeight.value
               else -> bar.height
            }
            gc.fill = bar.color
            gc.stroke = bar.color
            when (settings.barAlignment.value) {
               BarPos.CENTER -> gc.fillRect(i*barW, (h-barH)/2, (barW-settings.barGap.value) max 1.0, barH)
               BarPos.BOTTOM -> gc.fillRect(i*barW, h-barH, (barW-settings.barGap.value) max 1.0, barH)
               BarPos.TOP -> gc.fillRect(i*(barW+settings.barGap.value), 0.0, barW, barH)
               BarPos.CIRCLE_IN, BarPos.CIRCLE_MIDDLE, BarPos.CIRCLE_OUT -> {
                  gc.lineWidth = (barW - settings.barGap.value) max 1.0
                  val max = min(h, w)/4.0*1.5 + (if (settings.barAlignment.value==BarPos.CIRCLE_IN) 0.0 else barH/4.0)
                  val min = min(h, w)/4.0*1.5 - (if (settings.barAlignment.value==BarPos.CIRCLE_OUT) 0.0 else barH/8.0)
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
            if (startHz<=frequencyBar.hz
               && frequencyBar.hz<=endHz) {
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
   class FrequencyBar(var hz: Double, var height: Double, var color: Color)

   /**
    * This class holds state information regarding to:
    * - timeFiltering a.k.a smoothness
    * - previous bar heights that are used in bad decay calculation
    */
   class FrequencyBarsFFTService(settings: Spektrum): FFTListener {
      private val settings = settings
      private val oldTime = System.currentTimeMillis()

      // the hzBins and the amplitudes come in pairs and access to them needs to be synchronized
      private val lock = ReentrantLock(true)

      // all of the instances have the same input from the audio dispatcher
      private var hzBins: DoubleArray? = null
      private var amplitudes: DoubleArray? = null
      private val fftTimeFilter = FFTTimeFilter(settings)
      private val barsHeightCalculator = BarsHeightCalculator(settings)

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
               frequencyBars = createFrequencyBars(returnBinz!!, returnAmplitudes!!)
            } else {
               // return empty array
               frequencyBars = ArrayList()
            }
            return frequencyBars
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
      }.ifError { it.printStackTrace() }
   }

   fun stop() {
      try {
         dispatcher?.stop()
         audioThread?.join((1*1000).toLong()) // wait for audio dispatcher to finish // TODO: remove?
      } catch (e: InterruptedException) {
      }

      mixerRef.ifNotNull {
         val lineUses = Spektrum.openLines[it]
         if (lineUses!=null) {
            lineUses.count--
            if (lineUses.count == 0) {
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
         lineUses.count++
         lineUses.line
      }
   }
}

object OctaveGenerator {
   private val cache = HashMap<OctaveSettings, List<Double>>()

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

interface FFTListener {
   fun frame(hzBins: DoubleArray?, normalizedAmplitudes: DoubleArray?)
}

class FFTAudioProcessor(audioFormat: AudioFormat, listenerList: List<FFTListener>, settings: Spektrum): AudioProcessor {
   private val audioFormat = audioFormat
   private val listenerList = listenerList
   private val settings = settings
   private val interpolator: UnivariateInterpolator = SplineInterpolator()
   private val windowFunction: WindowFunction = HannWindow()
   private val windowCorrectionFactor = 2.00
   override fun process(audioEvent: AudioEvent): Boolean {
      val interpolation = settings.zeroPadding.value
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

      if (settings.octave>0) {
         val octaveFrequencies = OctaveGenerator.getOctaveFrequencies(settings.frequencyCenter.toDouble(), settings.octave.toDouble(), settings.frequencyStart.toDouble(), settings.frequencyEnd.toDouble())
         frequencyBins = DoubleArray(octaveFrequencies.size)
         frequencyAmplitudes = DoubleArray(octaveFrequencies.size)

         // calculate the frequency step
         // lowLimit/highLimit this is the resolution for interpolating and summing bins
         val lowLimit = OctaveGenerator.getLowLimit(octaveFrequencies[0], settings.octave.toDouble())
         var step = 2.0.pow(1.0/settings.octave)

         // improve resolution at the cost of performance
         if (settings.resolutionHighQuality)
            step /= (settings.octave/2.0)

         // k is the frequency index
         var k = lowLimit

         // m is the position in the frequency vectors
         var m = 0

         // setup the interpolator
         val interpolateFunction = interpolator.interpolate(bins, doublesAmplitudes)
         for (i in octaveFrequencies.indices) {
            frequencyBins[m] = octaveFrequencies[i]
            val highLimit = OctaveGenerator.getHighLimit(octaveFrequencies[i], settings.octave.toDouble())

            // group bins together
            while (k<highLimit) {
               var amplitude = interpolateFunction.value(k)
               amplitude /= doublesAmplitudes.size // normalize (n/2)
               amplitude *= windowCorrectionFactor // apply window correction
               frequencyAmplitudes[m] = frequencyAmplitudes[m] + amplitude.pow(2.0) // sum up the "normalized window corrected" energy
               k += step

               // reached upper limit
               if (k>settings.frequencyEnd || k>bins[bins.size - 1]) {
                  break
               }
            }
            frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m]) // square root the energy
            if (settings.maxLevel.value=="RMS") {
               frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m].pow(2.0)/2) // calculate the RMS of the amplitude
            }
            frequencyAmplitudes[m] = 20*log10(frequencyAmplitudes[m]) // convert to logarithmic scale
            frequencyAmplitudes[m] += settings.weight.calculateAmplitudeWight(frequencyBins[m]) // use weight to adjust the spectrum
            m++
         }
      } else {
         var n = 0
         for (i in bins.indices) {
            val frequency = fft.binToHz(i, audioFormat.sampleRate)
            if (settings.frequencyStart<=frequency && frequency<=settings.frequencyEnd) {
               n++
            } else if (frequency>settings.frequencyEnd) {
               break
            }
         }
         frequencyBins = DoubleArray(n)
         frequencyAmplitudes = DoubleArray(n)
         var m = 0
         for (i in bins.indices) {
            val frequency = fft.binToHz(i, audioFormat.sampleRate)
            if (settings.frequencyStart<=frequency && frequency<=settings.frequencyEnd) {
               frequencyBins[m] = frequency
               frequencyAmplitudes[m] = doublesAmplitudes[i]
               frequencyAmplitudes[m] = frequencyAmplitudes[m]/doublesAmplitudes.size // normalize (n/2)
               frequencyAmplitudes[m] = frequencyAmplitudes[m]*windowCorrectionFactor // apply window correction
               if (settings.maxLevel.value=="RMS") {
                  frequencyAmplitudes[m] = sqrt(frequencyAmplitudes[m].pow(2.0)/2) // calculate the RMS of the amplitude
               }
               frequencyAmplitudes[m] = 20*log10(frequencyAmplitudes[m]) // convert to logarithmic scale
               frequencyAmplitudes[m] += settings.weight.calculateAmplitudeWight(frequencyBins[m]) // use weight to adjust the spectrum
               m++
            }
         }
      }
      listenerList.forEach { listener: FFTListener -> listener.frame(frequencyBins, frequencyAmplitudes) }
      return true
   }

   override fun processingFinished() {}
}

class FFTTimeFilter(settings: Spektrum) {
   private val settings = settings
   private val historyAmps: Queue<DoubleArray?> = LinkedList()

   fun filter(amps: DoubleArray): DoubleArray {
      val timeFilterSize = settings.timeFilterSize.value
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

class BarsHeightCalculator(settings: Spektrum) {
   private val settings = settings
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
      val pixelsPerSecondDecay = settings.pixelsPerSecondDecay.value
      val secondsPassed = secondsPassed
      val pixelAmplitudes = convertDbToPixels(newAmplitudes)
      oldAmplitudes = decayPixelsAmplitudes(oldAmplitudes!!, pixelAmplitudes, pixelsPerSecondDecay.toDouble(), secondsPassed)
      return oldAmplitudes
   }

   private fun convertDbToPixels(dbAmplitude: DoubleArray): DoubleArray {
      val signalThreshold = settings.signalThreshold.value
      val maxBarHeight = settings.barMaxHeight.value
      val signalAmplification = settings.signalAmplification.value
      val minBarHeight = settings.barMinHeight.value
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
            newHeight = minBarHeight
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
            if (settings.accelerationFactor.value>0 && oldDecayFactor[i]<1) {
               val accelerationStep = 1.0/settings.accelerationFactor.value
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

@Suppress("EnumEntryName")
enum class WeightWindow(val calculateAmplitudeWight: (Double) -> Double) {
   dBA({ f ->
      val raf = (12194.0.pow(2.0)*f.pow(4.0)
         /((f.pow(2.0) + 20.6.pow(2.0))
         *sqrt((f.pow(2.0) + 107.7.pow(2.0))*(f.pow(2.0) + 737.9.pow(2.0)))
         *(f.pow(2.0) + 12194.0.pow(2.0))))
      20*log10(raf) + 2.00
   }),
   dBB({ f ->
      val rbf = (12194.0.pow(2.0)*f.pow(3.0)
         /((f.pow(2.0) + 20.6.pow(2.0))
         *sqrt(f.pow(2.0) + 158.5.pow(2.0))
         *(f.pow(2.0) + 12194.0.pow(2.0))))
      20*log10(rbf) + 0.17
   }),
   dBC({ f ->
      val rcf = (12194.0.pow(2.0)*f.pow(2.0)
         /((f.pow(2.0) + 20.6.pow(2.0))
         *(f.pow(2.0) + 12194.0.pow(2.0))))
      20*log10(rcf) + 0.06
   }),
   dBZ({
      0.0
   })
}