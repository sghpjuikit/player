package sp.it.util.async.executor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import kotlin.reflect.jvm.jvmName
import kotlinx.coroutines.invoke
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.delay
import sp.it.util.async.executor.EventReducer.HandlerEvery
import sp.it.util.async.executor.EventReducer.HandlerLast
import sp.it.util.units.millis

class EventReducerTest: FreeSpec({

   fun setupJavaFXThread() {
      val latch = CountDownLatch(1)
      val jfxPanel = JFXPanel() // Initializes the JavaFX environment.
      Platform.runLater { latch.countDown() } // Runs a task on the JavaFX thread.
      latch.await() // Wait for JavaFX application thread to start
   }

   setupJavaFXThread()

   HandlerEvery::class.jvmName {
      FX {
         val es = mutableListOf<String>()
         val er = HandlerEvery<String>(100.0, String::plus, es::plusAssign)

         es shouldBe listOf()
         er.push("Hel")
         es shouldBe listOf()
         er.push("l")
         es shouldBe listOf()
         er.push("o")
         es shouldBe listOf()
         delay(20.millis)
         es shouldBe listOf()
         er.push(", wor")
         es shouldBe listOf()
         er.push("l")
         es shouldBe listOf()
         er.push("d")
         es shouldBe listOf()
         er.push("!")
         delay(333.millis)
         es shouldBe listOf("Hello, world!")
      }
   }

   HandlerLast::class.jvmName {
      FX {
         val es = mutableListOf<String>()
         val er = HandlerLast<String>(100.0, String::plus, es::plusAssign)

         es shouldBe listOf()
         er.push("Hello")
         es shouldBe listOf()
         delay(75.millis)
         es shouldBe listOf()
         er.push(", world!")
         es shouldBe listOf()
         delay(150.millis)
         es shouldBe listOf("Hello, world!")
      }
   }
})