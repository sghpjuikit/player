package sp.it.pl.layout

import sp.it.pl.layout.controller.io.IOLayer

class LayoutUi(container: Layout): ComponentUiBase<Layout>(container) {

   val io = IOLayer(this).apply { id = "layout-io" }

   override val root get() = component.root!!
   override fun dispose() = io.dispose()
   override fun show() = Unit
   override fun hide() = Unit
}