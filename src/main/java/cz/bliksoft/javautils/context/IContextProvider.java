package cz.bliksoft.javautils.context;

/**
 * Objekt poskytující vlastní systémový kontext (obvykle komponenta)<BR>
 * Pokud nemá nějaké speciální požadavky na práci s kontextem, není nutno nic
 * implementovat - default implementace se postará...
 *
 */
public interface IContextProvider {
	/**
	 * vrátí kontext
	 *
	 * @return
	 */
	public default Context getItemContext() {
		return Context.getContextProviderContext(this);
	}
}
