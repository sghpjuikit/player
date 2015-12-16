/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import util.File.FileUtil;


/**
 * {@link WidgetFactory} producing {@link FXMLWidget}.
 * <p>
 * This factory is able to create the widgets dynamically from external fxml
 * files and compiled classes of their respective controller objects.
 * <p>
 * This requires a standard where controller object's class name must be the same as
 * its class file and the name must be derived from the .fxml file name (without
 * extension of course) exactly by concatenating the word 'Controller' to it.
 * <p>
 * For example Tagger.fxml would require TaggerController.class file of the
 * compiled controller.
 * <p>
 * Note that the resource (the fxml file) is not directly passed onto the widget.
 * Instead the widget requests it from its (this) factory. This avoids problems
 * with the resource being a strong dependency that could prevent widget loading
 * after the application has been moved or the widget resource is no longer
 * available.
 * <p>
 * @author uranium
 */
public final class FXMLWidgetFactory extends WidgetFactory<FXMLWidget> {
    public final URL url;
    public final File location;

    /**
     * @param _name
     * @param resource
     */
    public FXMLWidgetFactory(String _name, File fxmlFile) {
        super(_name, obtainControllerClass(fxmlFile));
        this.location = fxmlFile.getParentFile().getAbsoluteFile();System.out.println("factory location " + this.location);
        try {
            url = fxmlFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("FXML widget factory failed to create for: " + fxmlFile, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public FXMLWidget create() {System.out.println("creating fxml widget");
        return new FXMLWidget(name,this);
    }

    private static Class<?> obtainControllerClass(File fxmlFile) {
        try {
            File dir = fxmlFile.getParentFile();
            String name_widget = FileUtil.getName(fxmlFile);
            String name_class = name_widget + "Controller";


            ClassLoader controllerClassloader = createControllerClassLoader(dir, name_widget);
            // debug - checks if the
            // boolean isDifferentClassInstance =
            //         createControllerClassLoader(dir, name_widget).loadClass(name_class) ==
            //         createControllerClassLoader(dir, name_widget).loadClass(name_class);
            // System.out.println(isDifferentClassInstance);

            return controllerClassloader.loadClass(name_class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("FXML widget factory controller class loading failed for: " + fxmlFile, e);
        }
    }

    /**
     * Creates class loader which first tries to load the class in the provided directory and only
     * delegates to its parent class loader if it fails.
     * <p>
     * This is not normal class loader behavior, since class loader first consults parent to avoid
     * loading any class more than once. Thus, multiple instances of this class loader load
     * different class even when loading the same class file!
     * If the controller attempts to load the same class more than once it throws LinkageError
     * (attempted  duplicate class definition).
     * <p>
     * Normally, this be accomplished easily using different instances of URLClassLoader, but
     * in case the loaded class is on the classpath this will not work because the parent class
     * loader is consulted first and it will find the class (since it is on the classpath) and load
     * it (and prevent loading it ever again, unless we use class loader which will not consult it).
     * <p>
     * We care, because if we limit ourselves (with loading classes multiple times) to classes not
     * on the classpath, all external widget classes must not be on the classpath, meaning we have
     * to create separate project for every widget, since if they were part of this project,
     * Netbeans (which can only put compiled class files into one place) would automatically put
     * them on classpath. Now we dont need multiple projects as we dont mind widget class files on
     * the classpath since this class loader will load them from their widget location.
     * <p>
     * To explain - yes, external widgets have 2 copies of compiled class files now. One where its
     * source files (.java) are, where they are loaded from and the other in the application jar.
     * The class files must be copied manually to from classpath widget's locations when they change.
     * The widget classes belong to no package (have no package declaration).
     */
    private static ClassLoader createControllerClassLoader(File widget_dir, String widget_name) {
        return new ClassLoader(){

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return name.startsWith(widget_name)
                        ? loadCl(name)
                        : super.loadClass(name);
            }

            Class<?> loadCl(final String name) throws ClassNotFoundException {
                AccessControlContext acc = AccessController.getContext();
                try {
                    PrivilegedExceptionAction action = new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws ClassNotFoundException {
                            try {
                                FileInputStream fi = new FileInputStream(new File(widget_dir,name+".class"));
                                byte[] classBytes = new byte[fi.available()];
                                fi.read(classBytes);
                                return defineClass(name, classBytes, 0, classBytes.length);
                            } catch(Exception e ) {
                                throw new ClassNotFoundException(name);
                            }
                        }
                    };
                    return (Class)AccessController.doPrivileged(action, acc);
                } catch (java.security.PrivilegedActionException pae) {
                    return super.findClass(name);
                }
            }
        };
    }
}