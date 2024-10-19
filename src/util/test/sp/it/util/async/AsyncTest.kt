package sp.it.util.async

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.reflect.jvm.jvmName
import sp.it.util.async.future.Fut

class AsyncTest: FreeSpec({

   Fut::class.jvmName - {

      "executors" - {
         fun ct() = Thread.currentThread()

         val ct = ct()
         "futOfBlock" {
            Fut.futOfBlock { ct() }.blockAndGetOrThrow() shouldBe ct
         }
         "futOfBlock + then" {
            Fut.futOfBlock { ct() }.then { it to ct() }.blockAndGetOrThrow() shouldBe (ct to ct)
            Fut.futOfBlock { ct() }.then(CURR) { it to ct() }.blockAndGetOrThrow() shouldBe (ct to ct)
         }
         "fut(ct) + then(ct)" {
            Fut.fut(ct).then { it to ct() }.then { it to ct() }.blockAndGetOrThrow().also {
               it.first.first shouldBe ct
               it.first.second shouldBe ct
               it.second shouldBe ct
            }
            Fut.fut(ct).then(CURR) { it to ct() }.then(CURR) { it to ct() }.blockAndGetOrThrow().also {
               it.first.first shouldBe ct
               it.first.second shouldBe ct
               it.second shouldBe ct
            }
         }
         "fut(ct) + then(VT).then(VT).then()" {
            Fut.fut(ct).then(VT) { it to ct() }.then(VT) { it to ct() }.then() { (it to ct()) }.blockAndGetOrThrow().also {
               it.first.first.first shouldBe ct
               it.first.first.second.isVirtual shouldBe true
               it.first.first.second shouldBe it.first.second
               it.first.first.second shouldBe it.second
            }
         }
      }
   }

})
