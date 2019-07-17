package sp.it.pl.layout.widget.feature


@Feature(
   name = "Display text",
   description = "Displays the text",
   type = TextDisplayFeature::class
)
interface TextDisplayFeature {
   fun showText(text: String)
}