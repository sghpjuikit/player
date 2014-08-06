/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

/**
 * Object that closes.
 * <p>
 * Close operation is mostly to free resources and stop thebehavior of the
 * object.
 *
 * @author Plutonium_
 */
public interface Closable {
    public void close();
}
