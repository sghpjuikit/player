@file:JvmName("Util")

package sp.it.pl.util.graphics.css

import javafx.css.PseudoClass

fun pseudoClass(name: String) = PseudoClass.getPseudoClass(name)!!