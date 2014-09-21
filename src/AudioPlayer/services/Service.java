/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services;

/**
 *
 * @author Plutonium_
 */
public interface Service {
    void start();
    boolean isRunning();
    void stop();
    boolean isSupported();
    boolean isDependency();
}
