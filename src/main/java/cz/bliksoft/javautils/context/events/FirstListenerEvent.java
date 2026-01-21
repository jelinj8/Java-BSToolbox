package cz.bliksoft.javautils.context.events;

public abstract class FirstListenerEvent implements IConsumableEvent {
	@Override
	public boolean isConsumed() {
		return true;
	}
}
