package sp.it.pl.ui.objects

import java.io.File
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import sp.it.pl.main.runAsAppProgram
import sp.it.util.async.flowTimer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.text.splitTrimmed
import sp.it.util.ui.lay

/**
 * Component for displaying Nvidia GPU information periodically.
 * The timer is active only if this is part of scene graph.
 */
class GpuNvidiaInfo: StackPane() {
   val text = Text()
   val monitor = Subscribed {
      val updater = GlobalScope.launch(Dispatchers.JavaFx) {
         flowTimer(0, 5000).collect {
            File("C:/Program Files/NVIDIA Corporation/NVSMI/nvidia-smi.exe")
               .runAsAppProgram(
                  // `-q` print all
                  // `--help-query-gpu` help with --query-gpu
                  "-i 0",
                  "--query-gpu=clocks.mem,clocks.max.mem,clocks.sm,clocks.max.sm,clocks.gr,clocks.max.gr,memory.used,memory.total,driver_version,gpu_name,power.draw",
                  "--format=csv,noheader",
                  then = {}
               )
               .ui {
                  val valuesRaw = it.trim().splitTrimmed(",")
                  fun String.valueOnly() = trim().takeWhile { it.isDigit() }
                  fun valueOutOf(i: Int) = valuesRaw[i].valueOnly() + "/" + valuesRaw[i+1].trim()
                  text.text = """
                     |Gpu: ${valuesRaw[9]}
                     |Driver: ${valuesRaw[8]}
                     |Memory: ${valueOutOf(6)}
                     |Clock:
                     | Memory: ${valueOutOf(0)}
                     | Sm: ${valueOutOf(2)}
                     | Graphics: ${valueOutOf(4)}
                     |Draw: ${valuesRaw[10]}
                     """.trimMargin()
               }
         }
      }
      Subscription { updater.cancel() }
   }

   init {
      styleClass += "gpu-nvidia-info"
      lay += text

      sceneProperty().attach { monitor.subscribe(it!=null) }
   }

}