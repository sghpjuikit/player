@file:Suppress("RedundantSemicolon")

package sp.it.pl.ui.objects

import java.io.File
import javafx.geometry.HPos.LEFT
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlin.math.roundToInt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import sp.it.pl.main.runAsAppProgram
import sp.it.pl.main.toUi
import sp.it.util.async.FX
import sp.it.util.async.IO
import sp.it.util.async.flowTimer
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.toSubscription
import sp.it.util.text.splitTrimmed
import sp.it.util.type.atomic
import sp.it.util.ui.gridPane
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.separator
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.FileSize.Companion.Gi

/**
 * Component for displaying Nvidia GPU information periodically.
 * The timer is active only if this is part of scene graph.
 */
class GpuNvidiaInfo: StackPane() {
   private val sysInfo = SystemInfo()
   private val sysInfoProc = sysInfo.hardware.processor

   private val procLabel = label("CPU")
   private val procLoad = Num01Ui("Load", "cpu-load", "%")
   private val procClock = Ran01Ui("Clock", "cpu-clock", " GHz")
   private val sysMem = Num01Ui("Memory", "mem", " GiB")
   private val infoInitial = Info("n/a", Num01(0, 1), Ran01(0, 0, 0, 1), Num01(0, 1))

   private val gpuLabel = label("GPU")
   val text = Text()
   val progressClockMem = SliderCircular(150.0)
   val progressClockSm = SliderCircular(205.0)
   val progressClockGr = SliderCircular(260.0)
   val labelClock = label()
   val labelClockPer = label()

   private val gpuLoad = Num01Ui("Load", "gpu-load", "%")
   private val gpuPow = Num01Ui("Draw", "gpu-pow", " W")
   private val gpuMem = Num01Ui("Memory", "gpu-mem", " MiB")

   @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
   val monitor = Subscribed {
      var oldTicks by atomic(LongArray(CentralProcessor.TickType.values().size))
      Subscription(
         flowTimer(0, 5000)
            .map {
               val mem = sysInfo.hardware.memory
               val proc = sysInfoProc

               Info(
                  proc.processorIdentifier.name,
                  Num01(proc.getSystemCpuLoadBetweenTicks(oldTicks).times(100.0), 100.0),
                  Ran01(
                     (proc.currentFreq.minOrNull() ?: 0)/1000000000.0,
                     (proc.currentFreq.maxOrNull() ?: 0)/1000000000.0,
                     if (proc.currentFreq.isEmpty()) 0 else proc.currentFreq.average()/1000000000.0,
                     proc.maxFreq/1000000000.0
                  ),
                  Num01((mem.total - mem.available)/Gi.toDouble(), mem.total/Gi.toDouble())
               ).apply {
                  oldTicks = sysInfoProc.systemCpuLoadTicks
               }
            }
            .flowOn(IO)
            .onEach {
               procLabel.text = "CPU: ${it.procName.trim()}"
               procLoad update it.procLoad
               procClock update it.procClock
               sysMem update it.mem
            }
            .flowOn(FX)
            .launchIn(GlobalScope)
            .toSubscription(),
         flowTimer(0, 5000)
            .onEach {
               File("C:/Program Files/NVIDIA Corporation/NVSMI/nvidia-smi.exe")
                  .runAsAppProgram(
                     // `-q` print all
                     // `--help-query-gpu` help with --query-gpu
                     "-i 0",
                     "--query-gpu=clocks.mem,clocks.max.mem,clocks.sm,clocks.max.sm,clocks.gr,clocks.max.gr,memory.used,memory.total,driver_version,gpu_name,power.draw,power.limit,utilization.gpu",
                     "--format=csv,noheader",
                     then = {}
                  )
                  .ui {
                     val valuesRaw = it.trim().splitTrimmed(",")
                     fun valueOnly(i: Int) = valuesRaw[i].trim().takeWhile { it.isDigit() }
                     fun valueOutOf(i: Int) = valueOnly(i) + "/" + valuesRaw[i + 1].trim()
                     fun value01(i: Int) = (valueOnly(i).toDouble()/valueOnly(i + 1).toDouble()) min 1.0

                     text.text = """
                        |Gpu: ${valuesRaw[9]}
                        |Driver: ${valuesRaw[8]}
                        |Load: ${valuesRaw[11]}
                        |Memory: ${valueOutOf(6)}
                        |Clock:
                        | Memory: ${valueOutOf(0)}
                        | Sm: ${valueOutOf(2)}
                        | Graphics: ${valueOutOf(4)}
                        |Draw: ${valueOutOf(10)}
                        """.trimMargin()
                     gpuLabel.text = "GPU: ${valuesRaw[9].trim()} (${valuesRaw[8].trim()})"
                     gpuLoad update Num01(valueOnly(12).toDouble(), 100)
                     gpuMem update Num01(valueOnly(6).toDouble(), valueOnly(7).toDouble())
                     gpuPow update Num01(valueOnly(10).toDouble(), valueOnly(11).toDouble())
                     progressClockMem.value.value = value01(0)
                     progressClockSm.value.value = value01(2)
                     progressClockGr.value.value = value01(4)
                     labelClock.text = """
                        |Memory clock: ${valueOutOf(0)}
                        |Sm clock: ${valueOutOf(2)}
                        |Graphics clock: ${valueOutOf(4)}
                        """.trimMargin()
                     labelClockPer.text = "" + (progressClockGr.value.value + progressClockSm.value.value + progressClockMem.value.value).times(100).div(3.0).roundToInt() + "%"
                  }
            }
            .launchIn(GlobalScope)
            .toSubscription()
      )
   }

   init {
      styleClass += "gpu-nvidia-info"
      lay += gridPane {
         minSize = 0 x 0
         maxSize = Double.MAX_VALUE.x2
         vgap += 25
         hgap += 25

         val baseRow = 6

         lay(row = baseRow + -6, column = 1, hAlignment = LEFT) += procLabel

         lay(row = baseRow + -5, column = 0, hAlignment = LEFT) += procLoad
         lay(row = baseRow + -5, column = 1, hAlignment = LEFT) += Sep(procLoad.labelInfo)
         lay(row = baseRow + -4, column = 0, hAlignment = LEFT) += procClock
         lay(row = baseRow + -4, column = 1, hAlignment = LEFT) += Sep(procClock.labelInfo)
         lay(row = baseRow + -3, column = 0, hAlignment = LEFT) += sysMem
         lay(row = baseRow + -3, column = 1, hAlignment = LEFT) += Sep(sysMem.labelInfo)

         lay(row = baseRow + -2, column = 1, hAlignment = LEFT) += gpuLabel

         lay(row = baseRow + -1, column = 0, hAlignment = LEFT) += gpuLoad
         lay(row = baseRow + -1, column = 1, hAlignment = LEFT) += Sep(gpuLoad.labelInfo)
         lay(row = baseRow + 0, column = 0, hAlignment = LEFT) += gpuMem
         lay(row = baseRow + 0, column = 1, hAlignment = LEFT) += Sep(gpuMem.labelInfo)
         lay(row = baseRow + 1, column = 0, hAlignment = LEFT) += gpuPow
         lay(row = baseRow + 1, column = 1, hAlignment = LEFT) += Sep(gpuPow.labelInfo)
         lay(row = baseRow + 2, column = 0, hAlignment = LEFT) += stackPane {
            lay += progressClockGr.apply {
               valueSymmetrical.value = true
               editable.value = false
               styleClass += "clock-gr"
            }
            lay += progressClockSm.apply {
               valueSymmetrical.value = true
               editable.value = false
               styleClass += "clock-sm"
            }
            lay += progressClockMem.apply {
               valueSymmetrical.value = true
               editable.value = false
               styleClass += "clock-mem"
            }
            lay += labelClockPer
         }
         lay(row = baseRow + 2, column = 1, hAlignment = LEFT) += labelClock
      }

      sceneProperty().attach { monitor.subscribe(it!=null) }
   }

   private class Info(val procName: String, val procLoad: Num01, val procClock: Ran01, val mem: Num01)

   private class Num01(val cur: Number, val tot: Number)

   private class Num01Ui(val name: String, val cssStyleclass: String, val unit: String): StackPane() {
      val progress = SliderCircular(150.0)
      val labelPer = label()
      val labelInfo = label()

      init {
         lay += progress.apply {
            valueSymmetrical.value = true
            editable.value = false
            styleClass += cssStyleclass
         }
         lay += labelPer
      }

      infix fun update(value: Num01) {
         progress.value.value = (value.cur.toDouble()/value.tot.toDouble()).clip(0.0, 1.0)
         labelPer.text = "" + (progress.value.value).times(100).roundToInt() + "%"
         labelInfo.text = "$name: ${value.cur.toUiShort()}/${value.tot.toUiShort()}$unit"
      }
   }

   private class Ran01(val curMin: Number, val curMax: Number, val curAvg: Number, val tot: Number)

   private class Ran01Ui(val name: String, val cssStyleclass: String, val unit: String): StackPane() {
      val progressMin = SliderCircular(150.0)
      val progressMax = SliderCircular(175.0)
      val labelPer = label()
      val labelInfo = label()

      init {
         lay += progressMax.apply {
            valueSymmetrical.value = true
            editable.value = false
            styleClass += "$cssStyleclass-max"
         }
         lay += progressMin.apply {
            valueSymmetrical.value = true
            editable.value = false
            styleClass += "$cssStyleclass-min"
         }
         lay += labelPer
      }

      infix fun update(value: Ran01) {
         val totD = value.tot.toDouble()
         val isSimple = value.curMin==value.curMax
         progressMin.value.value = (value.curMax.toDouble()/totD).clip(0.0, 1.0)
         progressMax.value.value = (value.curMin.toDouble()/totD).clip(0.0, 1.0)
         labelPer.text  = "" + (value.curAvg.toDouble()/totD).clip(0.0, 1.0).times(100).roundToInt() + "%"
         labelInfo.text = if (isSimple) "$name: ${value.curMin.toUiShort()}/${value.tot.toUiShort()}$unit"
                          else "$name: ${value.curMin.toUiShort() + "-" + value.curMax.toUiShort()}/${value.tot.toUiShort()}$unit"
      }

   }

   private class Sep(label: Label): VBox() {
      init {
         spacing = 0.0
         alignment = CENTER_LEFT
         lay += label
         lay += separator().apply { translateX = -50.0 }
         lay += label()
      }
   }

   companion object {
      private fun Number.toUiShort(): String = when (this) {
         is Float -> toDouble().toUiShort()
         is Double -> "%.1f".format(this).removeSuffix(".0")
         else -> toUi()
      }
   }
}