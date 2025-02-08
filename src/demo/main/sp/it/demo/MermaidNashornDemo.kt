package sp.it.demo

import java.util.Base64
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javax.script.ScriptEngineManager

fun main(args: Array<String>) {
   Application.launch(MermaidDemoNashorn::class.java, *args)
}

/** JavaFX mermaid diagram rendering using nashorn js engine. Currently not fulyl functional. */
class MermaidDemoNashorn: Application() {
   override fun start(stage: Stage) {
      // Create an ImageView to display the rendered image
      val imageView = ImageView()

      // Generate the Mermaid diagram as an image
      val mermaidCode = "graph TD; A-->B; B-->C; C-->D;"

      // Load the Mermaid library
      val nashornScript = """
      load('https://cdn.jsdelivr.net/npm/mermaid@8.0.0/dist/mermaid.min.js');
      
      function renderDiagram(code) {
        return mermaid.render('test', code, {});
      }
      
      var svgContent = renderDiagram('${mermaidCode}');
      """

      val manager = ScriptEngineManager()
      val engine = manager.getEngineByName("nashorn")
      engine.eval(nashornScript)

      // Read the SVG content from the console output
      val svgContent = engine.eval("print('svgContent')") as String

      // Convert SVG to Image
      val image = Image("data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgContent.toByteArray()))
      imageView.image = image

      // Set up the scene
      val root = StackPane(imageView)
      val scene = Scene(root, 800.0, 600.0)
      stage.title = "Mermaid Diagram with Nashorn"
      stage.scene = scene
      stage.show()
   }

}