package cz.bliksoft.javautils.context.events;

/**
 * event is automatically consumed by first listener that gets notified
 */
public abstract class FirstListenerEvent implements IConsumableEvent {
	@Override
	public boolean isConsumed() {
		return true;
	}
}
