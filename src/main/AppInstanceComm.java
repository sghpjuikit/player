/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.async.Async;


/**
 * Application instance communicator.
 * Facilitates communication between different instances of this application by
 * firing and receiving events.
 * 
 * @author Plutonium_
 */
public class AppInstanceComm {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AppInstanceComm.class);
    private static final int PORT = 1099;
    private static final String REGISTRY_NAME = "PlayerFxCommunicatorServer";
    
    private AppMediator appCommunicator;
    private Registry rmiRegistry;
    public ArrayList<Consumer<List<String>>> onNewInstanceHandlers = new ArrayList<>();
    
    /**
     * Opens communication channel with other instances of this application.
     * Starts a non daemon thread, which has to
     * be manually terminated by calling {@link #stop()}.
     */
    public void start() throws RemoteException {
        appCommunicator = new AppMediator();
        rmiRegistry = LocateRegistry.createRegistry(PORT);
        rmiRegistry.rebind("PlayerFxCommunicatorServer", appCommunicator);
    }
    
    /**
     * Disposes the channel. Not calling this method will prevent the application
     * from closing properly.
     */
    public void stop() {
        if(rmiRegistry==null) return;
        try {
            UnicastRemoteObject.unexportObject(appCommunicator, true);
            rmiRegistry.unbind(REGISTRY_NAME);
            rmiRegistry = null;
        } catch (Exception e) {
            LOGGER.error("Unable to stop app instance communication.", e);
        }
    }
    
    /**
     * Fires new app instance event. Any instance of this application listening
     * will receive it. Run when application starts.
     */
    public void fireNewInstanceEvent(List<String> params) {
        // use rmi to fire an another instance launched event to the
        // already running application instance
        try {
            // tutorials say we must must set a SecurityManager
            // however it forbids the below to execute
            // maybe because RMISecurityManager is deprecated
            // if (System.getSecurityManager() == null)
            //     System.setSecurityManager(new RMISecurityManager());

           Mediator comm = (Mediator) LocateRegistry.getRegistry(PORT).lookup(REGISTRY_NAME);
           comm.newInstanceLaunched(params);
        } catch(NotBoundException e) {
            // no instance is listening -> ignore
        } catch (RemoteException e) {
            LOGGER.error("Unable to fire new app instance event.", e);
        }
    }
    
    
    interface Mediator extends Remote {
        public void newInstanceLaunched(List<String> params) throws RemoteException;
    }
    class AppMediator extends UnicastRemoteObject implements Mediator {


        public AppMediator() throws RemoteException {
            super();
        }

        @Override
        public void newInstanceLaunched(List<String> params) {
            LOGGER.info("New app instance event received.");
            // run handlers on fx thread (we are on rmi thread)
            Async.runFX(() -> onNewInstanceHandlers.forEach(c -> c.accept(params)));
        }


    }
}
