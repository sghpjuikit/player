@file:JvmName("Util")

package util.graphics.css

import javafx.css.PseudoClass

fun pseudoClass(name: String) = PseudoClass.getPseudoClass(name)!!