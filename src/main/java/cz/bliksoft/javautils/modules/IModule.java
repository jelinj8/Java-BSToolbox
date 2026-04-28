package cz.bliksoft.javautils.modules;

import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public interface IModule {

	/**
	 * @return název modulu
	 */
	public String getModuleName();

	/**
	 * kořenové XML virtuálního filesystému
	 * 
	 * @return
	 */
	public InputStream getFilesystemXml();

	/**
	 * inicializace modulu při načtení
	 */
	public void init();

	/**
	 * úklid před uzavřením aplikace
	 */
	public void cleanup();

	/**
	 * inicializace modulu po načtení celého systému
	 */
	public void install();

	/**
	 * mapa jazykových překladů
	 * 
	 * @return
	 */
	public Map<String, String> getTranslations();

	// public String getVersion();

	/**
	 * povolení či zakázání modulu pomocí konfigurace
	 * 
	 * @return
	 */
	public boolean isEnabled();

	/**
	 * povolení či zakázání modulu pomocí konfigurace
	 */
	public void setEnabled(boolean enabled);

	/**
	 * informace o verzi modulu
	 * 
	 * @return
	 */
	public String getVersionInfo();

	/**
	 * hodnota určující pořadí načítání modulů
	 * 
	 * @return
	 */
	public int getModuleLoadingOrder();
}
