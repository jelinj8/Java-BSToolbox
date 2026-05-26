package cz.bliksoft.javautils.context;

/**
 * Context listener that blocks change-event propagation beyond its attachment
 * point.
 */
public class ContextBoundary extends AbstractContextListener<Object> {

	/** Creates a boundary that blocks propagation for the given key type. */
	public ContextBoundary(Class<?> key) {
		super(key, "ContextBoundary"); //$NON-NLS-1$
	}

	@Override
	public void fired(ContextChangedEvent<Object> event) {
		event.blockEventPropagation();
	}

}
