package cz.bliksoft.javautils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SystemUtils {
	static Logger log = Logger.getLogger(SystemUtils.class.getName());

	static Object monitorObject = new Object();

	static AtomicBoolean keepWorking = new AtomicBoolean(true);

	static List<Runnable> interruptConsumers = new ArrayList<>();

	public static void addTerminatingListener(Runnable listener) {
		interruptConsumers.add(listener);
	}

	public static void removeTerminatingListener(Runnable listener) {
		interruptConsumers.remove(listener);
	}

	public static void interrupt() {
		log.info("Initiating termination");
		keepWorking.set(false);
		synchronized (monitorObject) {
			monitorObject.notifyAll();
		}
	}

	public static void keepRunningUntilInterrupted() {

		installShutdownHook();

		log.info("Waiting for shutdown signal");
		try {
			while (keepWorking.get()) {
				synchronized (monitorObject) {
					monitorObject.wait();
				}
			}
			log.info("Interruption initiated");
			for (Runnable l : interruptConsumers) {
				l.run();
			}
		} catch (

		InterruptedException e) {
			log.info("Interrupted.");
		}
	}

	private static Boolean installed = false;

	public static void installShutdownHook() {
		synchronized (installed) {
			if (installed)
				return;

			log.fine("Registering shutdown hook");

			Thread shutdownHookThread = new Thread() {
				public void run() {
					log.warning("Interrupt hook activated");
					keepWorking.set(false);
					synchronized (monitorObject) {
						monitorObject.notifyAll();
					}
				}
			};
			shutdownHookThread.setName("ShutdownHook");

			Runtime.getRuntime().addShutdownHook(shutdownHookThread);

			installed = true;
		}
	}

	public static boolean isInterrupting() {
		return !keepWorking.get();
	}

	public static boolean isRunning() {
		return keepWorking.get();
	}

}
