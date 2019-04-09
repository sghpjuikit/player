package webReader;

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import mu.KLogging
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.fxmlLoaderForController
import sp.it.pl.main.APP
import sp.it.pl.main.scaleEM
import sp.it.util.access.VarEnum
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.dev.Dependency
import sp.it.util.file.div
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.type.Util.getFieldValue
import sp.it.util.type.Util.invokeMethodP1
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WebBarInterpreter

@Widget.Info(
        name = "WebReader",
        author = "Martin Polakovic",
        description = "Very simple web browser widget.",
        version = "0.8.0",
        year = "2015",
        group = OTHER
)
@ExperimentalController("Insufficient features and of questionable use")
class WebReader(widget: Widget): SimpleController(widget) {

    private val inputHtml = inputs.create<String>("Html") { loadHtml(it) }
    private val inputUrl = inputs.create<String>("Url") { loadPage(it) }
    @FXML private lateinit var addressBar: TextField
    @FXML private lateinit var webView: WebView
    private var engine: WebEngine

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = EditMode.APP)
    private var url by c("https://duckduckgo.com/")

    @IsConfig(name = "Search engine")
    private val searchEngine by cv(DuckDuckGoQBuilder as SearchUriBuilder) {
        VarEnum.ofInstances(DuckDuckGoQBuilder, SearchUriBuilder::class.java, APP.instances)
    }

    @IsConfig(name = "No background")
    private val noBgr by cv(false)

    init {
        root.prefSize = 300.scaleEM() x 500.scaleEM()

        fxmlLoaderForController(this).loadNoEx<Any>()

        engine = webView.engine
        engine.userDataDirectory = userLocation/"browser"
        engine.locationProperty() attach {
            url = it
            addressBar.text = url
        }
        addressBar.onEventDown(KEY_PRESSED, ENTER) {
            val text = addressBar.text
            val url = WebBarInterpreter.toUrlString(text, searchEngine.get())
            loadPage(url)
        }

        // engine.documentProperty() sync { if (noBgr.get()) engine.setTransparentBgrColor() } on onClose

        refresh()
    }

    @FXML
    fun refresh() {
        if (addressBar.text.isEmpty()) {
            // For now we do not reload the page, just set up the address
            // loadPage(url);
            addressBar.text = url
        }
        loadPage(" ")
    }

    fun loadHtml(html: String?) {
        //        val s = "<body text=white>" + html + "</body>";
        val s = html?.replaceFirst("<body", """<body text=blue style="background-color:transparent"""")
        engine.loadContent(s ?: "")
    }

    fun loadPage(url: String?) {
        if (url!=null) {
            engine.load(url)
        }
    }

    companion object: KLogging() {

        // https://bugs.openjdk.java.net/browse/JDK-8090547?jql=text%20~%20"WebView%20transparent"
        // https://gist.github.com/riccardobl/18603f9de508b1ab6c9e
        @Dependency("requires access to javafx.web/com.sun.webkit.WebPage")
        private fun WebEngine.setTransparentBgrColor() {
            try {
                // Use reflection to retrieve the WebEngine's private 'page' field.
                val webPage = getFieldValue<Any>(this, "page") ?: return
                invokeMethodP1<Any, Int>(webPage, "setBackgroundColor", Int::class.javaPrimitiveType, java.awt.Color(255, 255, 255, 1).rgb)
            } catch (e: Exception) {
                logger.error(e) { "Could not change background color to transparent" }
            }
        }

    }
}