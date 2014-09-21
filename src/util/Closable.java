/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

/**
 * Object that closes.
 * <p>
 * Close operation is mostly to free resources and stop the behavior of the
 * object.
 *
 * @author Plutonium_
 */
public interface Closable {
    
    /** Closes this object. */
    public void close();
}
