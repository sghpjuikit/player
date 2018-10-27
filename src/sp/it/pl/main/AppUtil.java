package sp.it.pl.main;

import java.io.File;
import java.net.URLConnection;
import javafx.application.Application;
import static sp.it.pl.util.file.Util.isValidatedDirectory;
import static sp.it.pl.util.file.UtilKt.childOf;

public class AppUtil {
    public static App APP = null;

    public static void main(String[] args) {

        // Relocate temp & home under working directory
        // It is our principle to leave no trace of ever running on the system
        // User can also better see what the application is doing
        File tmp = childOf(new File("").getAbsoluteFile(), "user", "tmp");
        isValidatedDirectory(tmp);
        System.setProperty("java.io.tmpdir", tmp.getAbsolutePath());
        System.setProperty("user.home", tmp.getAbsolutePath());

        // Disable url caching, which may cause jar files being held in memory
        URLConnection.setDefaultUseCaches("file", false);

        Application.launch(App.class, args);
    }

}