package sp.it.util.async

import io.kotest.core.spec.style.FreeSpec
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
            Fut.fut(arrayOf(ct)).then { it + ct() }.then { it + ct() }.blockAndGetOrThrow().also {
               it[0] shouldBe ct
               it[1] shouldBe ct
               it[2] shouldBe ct
            }
            Fut.fut(arrayOf(ct)).then(CURR) { it + ct() }.then(CURR) { it + ct() }.blockAndGetOrThrow().also {
               it[0] shouldBe ct
               it[1] shouldBe ct
               it[2] shouldBe ct
            }
         }
         "fut(ct) + then(VT).then(VT).then()" {
            Fut.fut(arrayOf(ct)).then(VT) { it + ct() }.then(VT) { it + ct() }.then() { it + ct() }.blockAndGetOrThrow().also {
               it[0] shouldBe ct
               it[1].isVirtual shouldBe true
               it[2] shouldBe it[1]
               it[3] shouldBe it[1]
            }
         }
      }
   }

})