package cz.bliksoft.javautils.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import cz.bliksoft.javautils.DateUtils;

public class TaskScheduler {
	private Logger log;

	public TaskScheduler(String name) {
		this.name = name;
		log = Logger.getLogger(TaskScheduler.class.getSimpleName() + "[" + name + "]");
	}

	private static TaskScheduler instance = null;

	private String name;

	public String getName() {
		if (name == null)
			return toString();
		return name;
	}

	public enum SCHEDULER_STATUS {
		NEW, RUNNING, PAUSED, STOPPED;

		@Override
		public String toString() {
			switch (this) {
			case NEW:
				return "NEW";
			case PAUSED:
				return "PAUSED";
			case RUNNING:
				return "RUNNING";
			case STOPPED:
				return "STOPPED";
			default:
				return "???";
			}
		}

	}

	private Thread SchedulerThread = null;
	private final Semaphore waitSemaphore = new Semaphore(0, true);

	TreeSet<Task> tasks = new TreeSet<>(new Comparator<Task>() {
		@Override
		public int compare(Task o1, Task o2) {
			if (o1.nextRun < o2.nextRun)
				return -1;
			else if (o1.nextRun > o2.nextRun)
				return 1;
			else if (o1.nextRun == o2.nextRun) {
				if (o1 == o2)
					return 0;
				// v případě, že je čas nastaven stejně, ale nejedná se o stejnou úlohu, máme tu
				// nejednoznačnost.
				/*
				 * else if (o1 < o2) return -1; else return 1;
				 */
			}
			return 1;
		}
	});

	private SCHEDULER_STATUS status = SCHEDULER_STATUS.NEW;

	public SCHEDULER_STATUS getStatus() {
		return status;
	}

	private long nextRun = 0;
	private Task nextTask = null;

	private long pausedTS = 0;
	private long startedTS = 0;
	private long stoppedTS = 0;

	/**
	 * podle aktuální nejbližší úlohy upraví příští úlohu musí se volat v
	 * synchronized (tasks) { }
	 */
	private void checkHead() {
		Task nextSchedule = (status == SCHEDULER_STATUS.RUNNING ? (tasks.isEmpty() ? null : tasks.first()) : null);
		// změna přítomnosti plánu, jeho identity nebo načasování

		if (((nextSchedule == null) != (nextTask == null))) {
			setNextSchedule(nextSchedule);
			return;
		}
		if ((nextSchedule != null) && (nextSchedule.nextRun != nextRun || !nextSchedule.equals(nextTask))) {
			setNextSchedule(nextSchedule);
		}
	}

	/**
	 * zavolá se, pokud dojde ke změně následující události
	 * 
	 * @param task
	 *            úloha
	 */
	private void setNextSchedule(Task task) {
		if (task != null) {
			nextRun = task.nextRun;
			nextTask = task;
			log.info("Next task is \"" + task.getName() + "\" on " + DateUtils.millisTimestampString(task.nextRun)
					+ " (in " + DateUtils.millisIntervalString(task.nextRun - DateUtils.millis()) + ")");

			if (!waitSemaphore.tryAcquire())
				waitSemaphore.release(); // dochází ke změně plánů a není to bezprostředně po spuštění úlohy
		} else {
			log.fine("No scheduled task.");
			nextRun = 0;
			nextTask = null;
		}
	}

	/**
	 * naplánuje spuštění úlohy
	 * 
	 * @param task
	 *            úloha
	 */
	public void schedule(Task task) {
		synchronized (tasks) {
			if (tasks.remove(task))
				log.fine("rescheduling existing task " + task.getName() + " to "
						+ DateUtils.millisTimestampString(task.nextRun) + " (in "
						+ DateUtils.millisIntervalString(task.nextRun - DateUtils.millis()) + ")");
			task.nextRun = (DateUtils.millis() + Math.abs(task.getInitialDelay()));
			tasks.add(task);
			checkHead();
		}
	}

	/**
	 * naplánuje spuštění úlohy po intervalu (ms)
	 * 
	 * @param task
	 *            úloha
	 * @param delay
	 *            zpoždění (ms)
	 */
	public void scheduleIn(Task task, long delay) {
		synchronized (tasks) {
			if (tasks.remove(task))
				log.fine("rescheduling existing task " + task.getName());
			task.nextRun = (DateUtils.millis() + delay);
			tasks.add(task);
			checkHead();
		}
	}

	// /**
	// * naplánuje první spuštění úlohy po intervalu
	// *
	// * @param task
	// * @param initialDelay
	// * odložit spuštění o ms
	// * @param repeatInterval
	// * nastavení opakovacího intervalu úlohy
	// */
	// public void schedule(Task task, long repeatInterval) {
	// task.setRepeatInterval(repeatInterval);
	// schedule(task);
	// }

	public void unschedule(Task task) {
		synchronized (tasks) {
			if (tasks.remove(task))
				log.fine("Unscheduled task " + task.getName());
			else
				log.fine("Task " + task.getName() + " was not scheduled");
			checkHead();
		}
	}

	public void start() {
		if (status == SCHEDULER_STATUS.RUNNING)
			return;
		log.fine(">> >> >> >> >> >> >> Task scheduler " + getName() + " starting (" + tasks.size() + " task(s))...");

		List<Task> taskList = new ArrayList<Task>();
		long ts = DateUtils.millis();
		
		synchronized (tasks) {
			while (!tasks.isEmpty()) {
				taskList.add(tasks.pollFirst());
			}
			if ((status == SCHEDULER_STATUS.NEW) || (status == SCHEDULER_STATUS.STOPPED)) {
				for (Task tsk : taskList) {
					tsk.nextRun = ts + Math.abs(tsk.getInitialDelay());
					tasks.add(tsk);
				}
			} else if (status == SCHEDULER_STATUS.PAUSED) {
				long tsAdjust = DateUtils.millis() - pausedTS;
				log.fine("Adjusting plans by " + DateUtils.millisIntervalString(tsAdjust));
				for (Task tsk : taskList) {
					tsk.nextRun = tsk.nextRun + tsAdjust;
					tasks.add(tsk);
				}
			}

			status = SCHEDULER_STATUS.RUNNING;
			checkHead();
		}

		startedTS = DateUtils.millis();
		SchedulerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (status == SCHEDULER_STATUS.RUNNING) {
					try {
						// pokud máme plán a je spuštěno, počkáme na něj
						if ((nextTask != null) && (status == SCHEDULER_STATUS.RUNNING)) {
							if (waitSemaphore.tryAcquire(nextRun - DateUtils.millis(), TimeUnit.MILLISECONDS)) {
								log.fine("Wait for task interrupted");
							} else {
								log.fine("Wait for task finished" + (nextTask == null ? " (NO TASK)" : ""));
							}
						} else {
							// nemáme plán, čekáme na změnu
							log.fine("Waiting for a change of plans" + ((nextTask == null) ? " (no tasks)" : "")
									+ ((status == SCHEDULER_STATUS.RUNNING) ? "" : " " + status));
							waitSemaphore.acquire();
							log.fine("Scheduler status changed, checking...");
						}

						if ((DateUtils.millis() > nextRun)) {
							if (status == SCHEDULER_STATUS.RUNNING) {
								Task taskToPerform = null;
								long repeatInterval = 0;
								synchronized (tasks) {
									taskToPerform = nextTask;
									if (taskToPerform != null)
										repeatInterval = taskToPerform.getRepeatInterval();
								}
								if (taskToPerform != null) {
									waitSemaphore.release();
									if (repeatInterval < 0) {
										scheduleIn(nextTask, repeatInterval);
									}
									log.info(">>> Starting task \"" + taskToPerform.getName() + "\"");
									try {
										taskToPerform.doRun();
									} catch (Exception e) {
										log.severe("Error performing scheduled task: " + e.getMessage());
									}
									log.info("Task \"" + taskToPerform.getName() + "\" processing took "
											+ DateUtils.millisIntervalString(taskToPerform.getLastTimeConsumed())
											+ " >>>");

									if (repeatInterval > 0) {
										scheduleIn(nextTask, repeatInterval);
									}
								}
							}
						} else {
							log.fine(status + ": No task to run now.");
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}, "TaskScheduler[" + name + "]");
		SchedulerThread.start();
		log.info("|> Started, " + tasks.size() + " task(s).");
	}

	/**
	 * pozastaví provádění naplánovaných úloh - po znovuspustění budou úlohy
	 * pokračovat jako by mezitím neběžel čas (posune se čas spuštění o dobu, kdy
	 * byl plánovač pozastaven)
	 */
	public void pause() {
		status = SCHEDULER_STATUS.PAUSED;
		log.info("|| Paused.");
		pausedTS = DateUtils.millis();
		synchronized (tasks) {
			checkHead();
		}
		// waitSemaphore.release();
	}

	/**
	 * zastaví plánovač. Po znovuspuštění budou úlohy znovu naplánovány podle jejich
	 * initialDelay
	 */
	public void stop() {
		status = SCHEDULER_STATUS.STOPPED;
		nextRun = 0;
		nextTask = null;
		stoppedTS = DateUtils.millis();

		log.fine("[] Task scheduler stopping...");
		waitSemaphore.release();
		try {
			SchedulerThread.join();
		} catch (InterruptedException e) {
			log.warning("Error terminating scheduler thread: " + e.getLocalizedMessage());
		}
		SchedulerThread = null;

		log.info("Stopped.");
	}

	public static TaskScheduler getDefault() {
		if (instance == null) {
			instance = new TaskScheduler("default");
			instance.start();
		}

		return instance;
	}

	public boolean isEmpty() {
		synchronized (tasks) {
			return tasks.isEmpty();
		}
	}
}
