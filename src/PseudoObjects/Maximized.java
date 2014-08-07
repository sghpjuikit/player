/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

import utilities.CyclicEnum;

/**
 * Enum class for application maximized states.
 * States: ALL, LEFT, RIGHT, NONE
 */
public enum Maximized implements CyclicEnum<Maximized>{
    ALL,
    LEFT,
    RIGHT,
    LEFT_TOP,
    RIGHT_TOP,
    LEFT_BOTTOM,
    RIGHT_BOTTOM,
    NONE;
}
