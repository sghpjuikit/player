package sp.it.util.ui

import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.Pos.BASELINE_CENTER
import javafx.geometry.Pos.BASELINE_LEFT
import javafx.geometry.Pos.BASELINE_RIGHT
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.VPos

operator fun HPos.times(v: VPos): Pos = when (this) {
   HPos.LEFT -> when (v) { VPos.TOP -> TOP_LEFT; VPos.CENTER -> CENTER_LEFT; VPos.BOTTOM -> BOTTOM_LEFT; VPos.BASELINE -> BASELINE_LEFT }
   HPos.CENTER -> when (v) { VPos.TOP -> TOP_CENTER; VPos.CENTER -> CENTER; VPos.BOTTOM -> BOTTOM_CENTER; VPos.BASELINE -> BASELINE_CENTER }
   HPos.RIGHT -> when (v) { VPos.TOP -> TOP_RIGHT; VPos.CENTER -> CENTER_RIGHT; VPos.BOTTOM -> BOTTOM_RIGHT; VPos.BASELINE -> BASELINE_RIGHT }
}

operator fun VPos.times(h: HPos): Pos = when (h) {
   HPos.LEFT -> when (this) { VPos.TOP -> TOP_LEFT; VPos.CENTER -> CENTER_LEFT; VPos.BOTTOM -> BOTTOM_LEFT; VPos.BASELINE -> BASELINE_LEFT }
   HPos.CENTER -> when (this) { VPos.TOP -> TOP_CENTER; VPos.CENTER -> CENTER; VPos.BOTTOM -> BOTTOM_CENTER; VPos.BASELINE -> BASELINE_CENTER }
   HPos.RIGHT -> when (this) { VPos.TOP -> TOP_RIGHT; VPos.CENTER -> CENTER_RIGHT; VPos.BOTTOM -> BOTTOM_RIGHT; VPos.BASELINE -> BASELINE_RIGHT }
}