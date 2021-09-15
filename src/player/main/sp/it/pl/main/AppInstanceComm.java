package sp.it.pl.main;

import java.rmi.ConnectException;
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
import static sp.it.util.async.AsyncKt.runFX;

/**
 * Application instance communicator.
 * Facilitates communication between different instances of this application by firing and receiving events.
 */
public class AppInstanceComm {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppInstanceComm.class);
	private static final int PORT = 1099;
	private static final String REGISTRY_NAME = "SpitPlayerCommunicatorServer";

	public final ArrayList<Consumer<? super List<String>>> onNewInstanceHandlers = new ArrayList<>();
	private AppMediator appCommunicator;
	private Registry rmiRegistry;

	/**
	 * Opens communication channel with other instances of this application.
	 * Starts a non daemon thread, which has to
	 * be manually terminated by calling {@link #stop()}.
	 */
	public void start() {
		try {
			appCommunicator = new AppMediator();
			rmiRegistry = LocateRegistry.createRegistry(PORT);
			rmiRegistry.rebind("SpitPlayerCommunicatorServer", appCommunicator);
		} catch (RemoteException e) {
			LOGGER.warn("App instance communicator failed to start", e);
		}
	}

	/**
	 * Disposes the channel. Not calling this method will prevent the application
	 * from closing properly.
	 */
	public void stop() {
		if (rmiRegistry==null) return;
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
		// use rmi to fire another instance launched event to the
		// already running application instance
		try {
			// tutorials say we must set a SecurityManager
			// however it forbids the below to execute
			// maybe because RMISecurityManager is deprecated
			// if (System.getSecurityManager() == null)
			//     System.setSecurityManager(new RMISecurityManager());

			Mediator comm = (Mediator) LocateRegistry.getRegistry(PORT).lookup(REGISTRY_NAME);
			comm.newInstanceLaunched(params);
		} catch (NotBoundException|ConnectException e) {
			// no instance is listening -> ignore
		} catch (RemoteException e) {
			LOGGER.error("Unable to fire new app instance event.", e);
		}
	}

	interface Mediator extends Remote {
		void newInstanceLaunched(List<String> params) throws RemoteException;
	}

	class AppMediator extends UnicastRemoteObject implements Mediator {

		public AppMediator() throws RemoteException {
			super();
		}

		@Override
		public void newInstanceLaunched(List<String> params) {
			LOGGER.info("New app instance event received.");
			// run handlers on fx thread (we are on rmi thread)
			runFX(() -> onNewInstanceHandlers.forEach(c -> c.accept(params)));
		}
	}
}