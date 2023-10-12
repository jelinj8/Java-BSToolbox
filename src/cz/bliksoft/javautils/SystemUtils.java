package cz.bliksoft.javautils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SystemUtils {
	static Logger log = Logger.getLogger(SystemUtils.class.getName());

	static AtomicBoolean keepWorking = new AtomicBoolean(true);

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
		try
		{
			while (keepWorking.get()) {
				Thread.sleep(1000);
			}
		} catch (

		InterruptedException e) {
			log.info("Interrupted.");
		}
	}
}
