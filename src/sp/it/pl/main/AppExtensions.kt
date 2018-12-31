package sp.it.pl.main

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.isExperimental

/** @return whether user can use this factory, exactly: APP.developerMode || ![ComponentFactory.isExperimental] */
fun ComponentFactory<*>.isUsableByUser() = APP.developerMode || !isExperimental()