package cz.bliksoft.javautils.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.DateUtils;

/**
 * Single-threaded scheduler that executes {@link Task} instances at configured
 * times. Tasks are ordered by their next-run timestamp. Supports pause/resume
 * (preserving elapsed time) and stop/restart (resetting to initial delays).
 */
public class TaskScheduler {
	private Logger log;

	/**
	 * Creates a named scheduler. The scheduler does not start automatically; call
	 * {@link #start()}.
	 */
	public TaskScheduler(String name) {
		this.name = name;
		log = Logger.getLogger(TaskScheduler.class.getSimpleName() + "[" + name + "]");
	}

	private static TaskScheduler instance = null;

	private String name;

	/**
	 * Returns the scheduler name, falling back to {@link #toString()} if no name
	 * was set.
	 */
	public String getName() {
		if (name == null)
			return toString();
		return name;
	}

	/** Lifecycle states of the scheduler. */
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

	private Thread schedulerThread = null;
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
				// if the time is the same but it's not the same task instance, we have an
				// ambiguity.
				/*
				 * else if (o1 < o2) return -1; else return 1;
				 */
			}
			return 1;
		}
	});

	private SCHEDULER_STATUS status = SCHEDULER_STATUS.NEW;

	/** Returns the current lifecycle status of the scheduler. */
	public SCHEDULER_STATUS getStatus() {
		return status;
	}

	private long nextRun = 0;
	private Task nextTask = null;

	private long pausedTS = 0;
	private long startedTS = 0;
	private long stoppedTS = 0;

	/**
	 * Returns the epoch-millisecond timestamp when the scheduler was last stopped,
	 * or 0 if never stopped.
	 */
	public long getStoppedTSMillis() {
		return stoppedTS;
	}

	/**
	 * Returns the epoch-millisecond timestamp when the scheduler was last started,
	 * or 0 if never started.
	 */
	public long getStartedTSMillis() {
		return startedTS;
	}

	/**
	 * Returns the epoch-millisecond timestamp when the scheduler was last paused,
	 * or 0 if never paused.
	 */
	public long getPausedTSMillis() {
		return pausedTS;
	}

	/**
	 * Updates the next scheduled task based on the current head of the queue. Must
	 * be called inside {@code synchronized (tasks) {}}.
	 */
	private void checkHead() {
		Task nextSchedule = (status == SCHEDULER_STATUS.RUNNING ? (tasks.isEmpty() ? null : tasks.first()) : null);
		// change in schedule presence, identity or timing

		if (((nextSchedule == null) != (nextTask == null))) {
			setNextSchedule(nextSchedule);
			return;
		}
		if ((nextSchedule != null) && (nextSchedule.nextRun != nextRun || !nextSchedule.equals(nextTask))) {
			setNextSchedule(nextSchedule);
		}
	}

	/**
	 * Called when the next scheduled event changes.
	 *
	 * @param task the new next task, or {@code null} if no task is scheduled
	 */
	private void setNextSchedule(Task task) {
		if (task != null) {
			nextRun = task.nextRun;
			nextTask = task;
			log.fine("Next task is \"" + task.getName() + "\" on " + DateUtils.millisTimestampString(task.nextRun)
					+ " (in " + DateUtils.millisIntervalString(task.nextRun - DateUtils.millis()) + ")");

			if (!waitSemaphore.tryAcquire())
				waitSemaphore.release(); // schedule is changing and it's not immediately after task start
		} else {
			log.fine("No scheduled task.");
			nextRun = 0;
			nextTask = null;
		}
	}

	/**
	 * Schedules a task for execution using its configured
	 * {@link Task#getInitialDelay()}. If the task is already scheduled it is
	 * rescheduled.
	 *
	 * @param task the task to schedule
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
	 * Schedules a task to run after a fixed delay in milliseconds.
	 *
	 * @param task  the task to schedule
	 * @param delay delay from now in milliseconds
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
	// * Schedules the first execution of a task after an interval.
	// *
	// * @param task
	// * @param initialDelay delay execution by ms
	// * @param repeatInterval task repeat interval setting
	// */
	// public void schedule(Task task, long repeatInterval) {
	// task.setRepeatInterval(repeatInterval);
	// schedule(task);
	// }

	/**
	 * Removes a task from the schedule. Has no effect if the task was not
	 * scheduled.
	 *
	 * @param task the task to remove
	 */
	public void unschedule(Task task) {
		synchronized (tasks) {
			if (tasks.remove(task))
				log.fine("Unscheduled task " + task.getName());
			else
				log.fine("Task " + task.getName() + " was not scheduled");
			checkHead();
		}
	}

	/**
	 * Starts the scheduler thread. Resumes from pause (shifting scheduled times) or
	 * restarts after stop. Has no effect if already running.
	 */
	public void start() {
		if (status == SCHEDULER_STATUS.RUNNING)
			return;
		log.fine(">> >> >> >> >> >> >> Task scheduler " + getName() + " starting (" + tasks.size() + " task(s))...");

		List<Task> taskList = new ArrayList<>();
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
		schedulerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (status == SCHEDULER_STATUS.RUNNING) {
					try {
						// if we have a scheduled task and the scheduler is running, wait for it
						if ((nextTask != null) && (status == SCHEDULER_STATUS.RUNNING)) {
							if (waitSemaphore.tryAcquire(nextRun - DateUtils.millis(), TimeUnit.MILLISECONDS)) {
								log.fine("Wait for task interrupted");
							} else {
								log.log(Level.FINE, "Wait for task finished {0}",
										(nextTask == null ? " (NO TASK)" : ""));
							}
						} else {
							// no scheduled task — waiting for a change
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
									log.finer(">>> Starting task \"" + taskToPerform.getName() + "\"");
									try {
										taskToPerform.doRun();
									} catch (Exception e) {
										log.severe("Error performing scheduled task: " + e.getMessage());
									}
									log.finer("Task \"" + taskToPerform.getName() + "\" processing took "
											+ DateUtils.millisIntervalString(taskToPerform.getLastTimeConsumed())
											+ " >>>");

									if (repeatInterval > 0) {
										scheduleIn(nextTask, repeatInterval);
									}
								}
							}
						} else {
							log.log(Level.FINE, "{0}: No task to run now.", status);
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}, "TaskScheduler[" + name + "]");
		schedulerThread.start();
		log.log(Level.FINE, "|> Started, {0} task(s).", tasks.size());
	}

	/**
	 * Pauses execution of scheduled tasks. When resumed via {@link #start()}, tasks
	 * continue as if no time had passed — their scheduled times are shifted forward
	 * by the pause duration.
	 */
	public void pause() {
		status = SCHEDULER_STATUS.PAUSED;
		log.fine("|| Paused.");
		pausedTS = DateUtils.millis();
		synchronized (tasks) {
			checkHead();
		}
		// waitSemaphore.release();
	}

	/**
	 * Stops the scheduler and its thread. After a subsequent {@link #start()} call,
	 * all tasks are rescheduled from scratch using their
	 * {@link Task#getInitialDelay()}.
	 */
	public void stop() {
		status = SCHEDULER_STATUS.STOPPED;
		nextRun = 0;
		nextTask = null;
		stoppedTS = DateUtils.millis();

		log.fine("[] Task scheduler stopping...");
		waitSemaphore.release();
		try {
			schedulerThread.join();
		} catch (InterruptedException e) {
			log.warning("Error terminating scheduler thread: " + e.getLocalizedMessage());
		}
		schedulerThread = null;

		log.fine("Stopped.");
	}

	/**
	 * Returns the default shared scheduler instance, creating and starting it on
	 * first call.
	 */
	public static TaskScheduler getDefault() {
		if (instance == null) {
			instance = new TaskScheduler("default");
			instance.start();
		}

		return instance;
	}

	/** Returns {@code true} if no tasks are currently scheduled. */
	public boolean isEmpty() {
		synchronized (tasks) {
			return tasks.isEmpty();
		}
	}
}
