package cz.bliksoft.javautils.context;

/**
 * jednoduchý hlídáček kontextu, který zastaví šíření události do nadřazených
 * kontextů
 *
 */
public class ContextBoundary extends AbstractContextListener<Object> {

	public ContextBoundary(Class<?> key) {
		super(key, "ContextBoundary"); //$NON-NLS-1$
	}

	@Override
	public void fired(ContextChangedEvent<Object> event) {
		event.blockEventPropagation();
	}

}
