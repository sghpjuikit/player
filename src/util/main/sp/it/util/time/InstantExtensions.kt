package sp.it.util.time

import javafx.util.Duration as DurationFx
import java.time.Instant
import java.time.temporal.TemporalAmount

fun Instant.isOlderThan(time: TemporalAmount): Boolean = Instant.now().isAfter(plus(time))

fun Instant.isOlderThanFx(time: DurationFx): Boolean = Instant.now().isAfter(plus(java.time.Duration.ofMillis(time.toMillis().toLong())))

fun Instant.isYoungerThan(time: TemporalAmount) = Instant.now().isBefore(plus(time))

fun Instant.isYoungerThanFx(time: DurationFx): Boolean = Instant.now().isBefore(plus(java.time.Duration.ofMillis(time.toMillis().toLong())))