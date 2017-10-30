@file:JvmName("Util")
@file:Suppress("unused")

package util.math

import javafx.util.Duration

fun millis(value: Int) = Duration.millis(value.toDouble())!!

fun millis(value: Double) = Duration.millis(value)!!

fun seconds(value: Int) = Duration.seconds(value.toDouble())!!

fun seconds(value: Double) = Duration.seconds(value)!!

fun minutes(value: Int) = Duration.minutes(value.toDouble())!!

fun minutes(value: Double) = Duration.minutes(value)!!