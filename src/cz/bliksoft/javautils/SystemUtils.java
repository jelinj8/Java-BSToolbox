package cz.bliksoft.javautils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class SystemUtils {
	static Logger log = Logger.getLogger(SystemUtils.class.getName());

	static AtomicBoolean keepWorking = new AtomicBoolean(true);

	static List<Runnable> interruptConsumers = new ArrayList<>();

	static void addTerminatingListener(Runnable listener) {
		interruptConsumers.add(listener);
	}

	static void removeTerminatingListener(Runnable listener) {
		interruptConsumers.remove(listener);
	}

	public static void interrupt() {
		log.info("Setting the end waiting flag.");
		keepWorking.set(false);
	}

	public static void keepRunningUntilInterrupted() {

		log.info("Registering shutdown hook.");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			keepWorking.set(false);
		}));

		log.info("Waiting for shutdown signal.");
		try {
			while (keepWorking.get()) {
				Thread.sleep(1000);
			}
			log.info("Interruption initiated.");
			for (Runnable l : interruptConsumers) {
				l.run();
			}
		} catch (

		InterruptedException e) {
			log.info("Interrupted.");
		}
	}
}
