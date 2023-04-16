package sp.it.pl.main

import javafx.beans.property.Property
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem
import sp.it.util.access.v
import sp.it.util.ui.acceleratorText

fun menuItemBool(text: String? = "", bool: Property<Boolean> = v(false), keys: String? = null) = SelectionMenuItem(text, bool).apply { acceleratorText = keys }