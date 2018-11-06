package webReader;

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import mu.KLogging
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.initClose
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cv
import sp.it.pl.util.dev.Dependency
import sp.it.pl.util.file.childOf
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.type.Util.getFieldValue
import sp.it.pl.util.type.Util.invokeMethodP1
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.SearchUriBuilder
import sp.it.pl.web.WebBarInterpreter

@Widget.Info(
        name = "WebReader",
        author = "Martin Polakovic",
        description = "Very simple web browser widget.",
        version = "0.8",
        year = "2015",
        group = OTHER
)
class WebReader: FXMLController() {

    @FXML private lateinit var addressBar: TextField
    @FXML private lateinit var webView: WebView
    private lateinit var engine: WebEngine

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = EditMode.APP)
    private var url by c("https://duckduckgo.com/")

    @IsConfig(name = "Search engine")
    private val searchEngine by cv(DuckDuckGoQBuilder as SearchUriBuilder) {
        VarEnum.ofInstances(DuckDuckGoQBuilder, SearchUriBuilder::class.java, APP.instances)
    }

    @IsConfig(name = "No background")
    private val noBgr by cv(false)

    override fun init() {
        engine = webView.engine
        engine.userDataDirectory = userLocation.childOf("browser")
        engine.locationProperty() attach {
            url = it
            addressBar.text = url
        }
        addressBar.setOnKeyPressed { e ->
            if (e.code==KeyCode.ENTER) {
                val text = addressBar.text
                val url = WebBarInterpreter.toUrlString(text, searchEngine.get())
                loadPage(url)
            }
        }

        initClose { engine.documentProperty() sync { if (noBgr.get()) engine.setTransparentBgrColor() } }
        inputs.create<String>("Html") { loadHtml(it) }
        inputs.create<String>("Url") { loadPage(it) }
    }

    @FXML
    override fun refresh() {
        // TODO: improve this
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

    // https://bugs.openjdk.java.net/browse/JDK-8090547?jql=text%20~%20"WebView%20transparent"
    // https://gist.github.com/riccardobl/18603f9de508b1ab6c9e
    @Dependency("requires access to javafx.web/com.sun.webkit.WebPage")
    private fun WebEngine.setTransparentBgrColor() {
        // TODO: jigsaw
        try {
            // Use reflection to retrieve the WebEngine's private 'page' field.
            val webPage = getFieldValue<Any>(this, "page") ?: return
            invokeMethodP1<Any, Int>(webPage, "setBackgroundColor", Int::class.javaPrimitiveType, java.awt.Color(255, 255, 255, 1).rgb) // TODO: fix
        } catch (e: Exception) {
            logger.error(e) { "Could not change background color to transparent" }
        }
    }

    companion object: KLogging()
}