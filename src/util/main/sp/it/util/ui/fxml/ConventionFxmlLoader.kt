package sp.it.util.ui.fxml

import javafx.fxml.FXMLLoader
import java.io.IOException

/**
 * [FXMLLoader] enriched by convention loading for simple exception-free loading.
 *
 * Usage example: `new ConventionFxmlLoader(this).loadNoEx()`
 */
class ConventionFxmlLoader: FXMLLoader {

    /** Invokes [setConvention]  */
    constructor(root_controller: Any) {
        setConvention(root_controller)
    }

    /** Invokes [setConvention]  */
    constructor(root: Any, controller: Any) {
        setConvention(controller.javaClass, root, controller)
    }

    /**
     * Equivalent to [setConvention] where class is the class of the specified rootController.
     *
     * Note, that using this method for root+controller with inheritance is dangerous
     * as it will look for incorrect fxml resource id subclass attempts to use the
     * same resource as its superclass.
     */
    fun setConvention(rootController: Any) = setConvention(rootController.javaClass, rootController)

    /**
     * Loads fxml resource of the same name as the object's class from location
     * of the class. Sets the object as both root and controller.
     *
     * For example class MyClass.class would have MyClass.fxml in its location.
     *
     * Equivalent to:
     * ```Class c = root_controller.getClass();
     * String name = c.getSimpleName() + ".fxml";
     * setLocation(c.getResource(name));
     * setRoot(root_controller);
     * setController(root_controller);
     * ```
     *
     * @param c class' name will be used to look up the fxml resource
     * @param root the root
     * @param controller the controller
     */
    fun setConvention(c: Class<*>, root: Any, controller: Any = root) {
        location = c.getResource(c.simpleName + ".fxml")
        setRoot(root)
        setController(controller)
    }

    /**
     * Equivalent to [load], but throws runtime exception instead.
     *
     * Use when adhering to convention can guarantee any loading error is a programming error.
     */
    fun <T> loadNoEx(): T {
        try {
            return load()
        } catch (e: IOException) {
            throw RuntimeException("Couldn't load fxml resource", e)
        }
    }

    companion object {
        fun conventionRootController(rootController: Any) = ConventionFxmlLoader(rootController)
        fun conventionRootAnController(root: Any, controller: Any) = ConventionFxmlLoader(root, controller)
    }

}