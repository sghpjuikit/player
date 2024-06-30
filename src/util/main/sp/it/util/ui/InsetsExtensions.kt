package sp.it.util.ui

import javafx.geometry.Insets

operator fun Insets.plus(insets: Insets) = Insets(top + insets.top, right + insets.right, bottom + insets.bottom, left + insets.left)

operator fun Insets.minus(insets: Insets) = Insets(top - insets.top, right - insets.right, bottom - insets.bottom, left - insets.left)