package util.graphics.fxml;

import java.io.IOException;
import javafx.fxml.FXMLLoader;

/**
 * Loader enriched by convention loading for simple exceptionless loading.
 * <p/>
 * Usage example (in a object's constructor for example) :
 * <pre>{@code 
   new ConventionFxmlLoader(this).loadNoEx();
 * }</pre>
 *
 * @author Martin Polakovic
 */
public class ConventionFxmlLoader extends FXMLLoader {

	public ConventionFxmlLoader() {}

	/** Invokes {@link #setConvention(java.lang.Object)} */
	public ConventionFxmlLoader(Object root_controller) {
		setConvention(root_controller);
	}

	/** Invokes {@link #setConvention(java.lang.Class, java.lang.Object) } */
	public ConventionFxmlLoader(Class c, Object root_controller) {
		setConvention(c, root_controller);
	}

	/** Invokes {@link #setConvention(java.lang.Class, java.lang.Object, java.lang.Object)} */
	public ConventionFxmlLoader(Class c, Object root, Object controller) {
		setConvention(c, root, controller);
	}

	/** Equivalent to {@link #setConvention(java.lang.Class, java.lang.Object)}
	 *  where class is the class of the root-controller.
	 * <p/>
	 * Note, that using this method for root+controller with inheritance is dangerous
	 * as it will look for incorrect fxml resource id subclass attempts to use the
	 * same resource as its superclass.
	 * Use only for final classes.
	 */
	public void setConvention(Object root_controller) {
		Class c = root_controller.getClass();
		setConvention(c, root_controller);
	}

	/** Equivalent to {@link #setConvention(java.lang.Class, java.lang.Object, java.lang.Object)}
	with root and controller being the same object. */
	public void setConvention(Class c, Object root_controller) {
		setConvention(c, root_controller, root_controller);
	}

	/** Loads fxml resource of the same name as the object's class from location
	 * of the class. Sets the object as both root and controller.
	 * <p/>
	 * For example class MyClass.class would have MyClass.fxml in its location.
	 * <p/>
	 * Equivalent to
	 * <pre>{@code
		Class c = root_controller.getClass();
		String name = c.getSimpleName() + ".fxml";
		setLocation(c.getResource(name));
		setRoot(root_controller);
		setController(root_controller);
	 * }</pre>
	 *
	 * @param c class' name will be used to look up the fxml resource
	 * @param root the root
	 * @param controller the controller
	 *
	 * @see #loadNoEx()
	 */
	public void setConvention(Class c, Object root, Object controller) {
		String name = c.getSimpleName() + ".fxml";
		setLocation(c.getResource(name));
		setRoot(root);
		setController(controller);
	}

	/**
	 * Equivalent to {@link #load()}, but throws runtime exception instead.
	 * Use only when adhering to convention can guarantee any loading error is a
	 * programming error.
	 *
	 * @throws RuntimeException when underlying load() throws IOException
	 * @see #setConvention(java.lang.Object)
	 */
	public <T> T loadNoEx() {
		try {
			return load();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't load fxml resource. " + e);
		}
	}

}