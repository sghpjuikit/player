/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 * Runtime exception for switch cases that represent programming error and must never execute.
 * @author Martin Polakovic
 */
public class SwitchException extends RuntimeException {

    /**
     * @param o  value of the switch case - the object passed into switch statement.
     */
    public SwitchException(Object o) {
        super("Illegal switch case (unimplemented or forbidden): " + o);
    }
}
