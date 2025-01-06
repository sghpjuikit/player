package sp.it.util.async

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import java.util.concurrent.Executors
import kotlin.coroutines.ContinuationInterceptor
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import sp.it.util.async.coroutine.NEW
import sp.it.util.async.coroutine.VT

class CoroutineTest: FreeSpec({

   suspend fun nonblockingCall() = delay(1000)
   fun blockingCall() = Thread.sleep(1000)
   fun CoroutineScope.currentDispatcher() = coroutineContext[ContinuationInterceptor]

   "withContext()" - {

      "uses specified dispatcher" {
         runBlocking(VT) { withContext(NEW) {         currentDispatcher() } } shouldBe NEW
      }

      "continues on original dispatcher" {
         runBlocking(VT) { withContext(NEW) { Unit }; currentDispatcher() } shouldBe VT
      }

   }

   "async()" - {
      suspend fun shouldBeSeq(testName: String, block: suspend () -> Unit) = "$testName should be sequential" { measureTimeMillis { block() } shouldBeInRange (2000L..2500L) }
      suspend fun shouldBePar(testName: String, block: suspend () -> Unit) = "$testName should be parallel" { measureTimeMillis { block() } shouldBeInRange (1000L..1500L) }

      "uses specified dispatcher" {
         runBlocking(VT) { async(NEW) {         currentDispatcher() }.await() } shouldBe NEW
      }

      "inherits dispatcher (if not specified)" {
         runBlocking(VT) { async {              currentDispatcher() }.await() } shouldBe VT
      }

      "continues on original dispatcher" {
         runBlocking(VT) { async(NEW) { Unit }; currentDispatcher() } shouldBe VT
      }

      shouldBePar("runBlocking(VT_NEW) with async { blocking call }") {
         runBlocking(Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()) {
            val deferred1 = async { blockingCall() }
            val deferred2 = async { blockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
      shouldBePar("runBlocking(NEW) with async { blocking call }") {
         runBlocking(NEW) {
            val deferred1 = async { blockingCall() }
            val deferred2 = async { blockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
      shouldBeSeq("runBlocking(VT) with async { blocking call }") {  // same as runBlocking(VT) with async(VT)
         runBlocking(VT) {
            val deferred1 = async { blockingCall() }
            val deferred2 = async { blockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
      shouldBeSeq("runBlocking(VT) with async(VT) { blocking call }") { // VT avoids dispatch if it already is on VT causing blocking propagation
         runBlocking(VT) {
            val deferred1 = async(VT) { blockingCall() }
            val deferred2 = async(VT) { blockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
      shouldBePar("runBlocking(*) with async(NEW) { blocking call }") {
         runBlocking(VT) {
            val deferred1 = async(NEW) { blockingCall() }
            val deferred2 = async(NEW) { blockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
      shouldBePar("runBlocking(*) with async { non blocking call }") {
         runBlocking(VT) {
            val deferred1 = async { nonblockingCall() }
            val deferred2 = async { nonblockingCall() }
            deferred1.await()
            deferred2.await()
         }
      }
   }
})
