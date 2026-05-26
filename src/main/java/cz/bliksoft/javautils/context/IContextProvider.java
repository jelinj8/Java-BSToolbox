package cz.bliksoft.javautils.context;

/**
 * Implemented by objects that own a dedicated context node (typically UI
 * components). The default implementation auto-creates a context on first
 * access.
 */
public interface IContextProvider {
	/** Returns the context associated with this provider. */
	public default Context getItemContext() {
		return Context.getContextProviderContext(this);
	}
}
