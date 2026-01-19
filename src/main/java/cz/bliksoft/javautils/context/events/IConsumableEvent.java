package cz.bliksoft.javautils.context.events;

public interface IConsumableEvent {

	default public void consume() { };
	default public boolean isConsumed() {
		return false;
	}
	
}
