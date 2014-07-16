/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Configuration.ValueAccessor;
import java.util.function.Supplier;
import javafx.util.Callback;

/**
 * Object that can get and set a password.
 * <p>
 * Implementing objects can be used inside API that instead of providing the setters and
 * getters that work with the String, wrap the getter and setter into functional
 * interfaces - as getter and setter of this object
 * that will be provided instead.
 * <p>
 * Does not store the password anywhere not even in itself, but rather accesses 
 * it through getter and setter dynamically.
 * <p>
 * This class helps avoid the layer where the password is passed on directly and
 * instead can provide direct low level access to true password fetching
 * mechanism trasparently.
 * <p>
 * <pre>An example of naive implementation in lambda notation:
 *     new Password(pwd -> {}, () -> "");
 * </pre>
 * The above example always returns empty password string and it setter does not
 * do anything.
 *
 * @author Plutonium_
 */
public class PasswordAccessor extends ValueAccessor<String> {

    public PasswordAccessor(Callback<String, Boolean> setter, Supplier<String> getter) {
        super(setter, getter);
    }
    
    /**
     * Convenience method delegating to {@link #getValue()}
     * Gets the password.
     * @return the password String or null if error.
     */
    public String getPassword() {
        return getValue();
    }

    /**
     * Convenience method delegating to {@link #setValue()}
     * Sets the password.
     * @param txt
     * @return the success flag.
     */
    public boolean setPassword(String txt) {
        return setValue(txt);
    }
}
