package cz.bliksoft.javautils.modules;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.logging.LogUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class Modules {
	private Modules() {
	}

	static Logger log; // init in loadModules (called after log4j is initialized)

	private static Set<Class<? extends IModule>> autoloadedModules = new HashSet<>();
	private static Set<String> forceEnabledModules = new HashSet<>();
	private static Set<String> enabledModules = new HashSet<>();
	private static Set<String> disabledModules = new HashSet<>();

	/**
	 * registr modulů
	 */
	protected static Map<String, IModule> modules = new HashMap<>();
	protected static List<IModule> sortedModules = null;

	public static void autoloadModule(Class<? extends IModule> module) {
		autoloadedModules.add(module);
	}

	public static void forceEnableModule(String className) {
		forceEnabledModules.add(className);
	}

	public static boolean isForceEnabled(String className) {
		return forceEnabledModules.contains(className);
	}

	public static void enableModule(String className) {
		enabledModules.add(className);
	}

	public static void disableModule(String className) {
		disabledModules.add(className);
	}

	/**
	 * Find all defined BSApp modules by serviceLoader, import XmlFilesystem from
	 * enabled modules.
	 */
	public static void loadModules() {
		log = LogManager.getLogger();
		forceEnabledModules.add("cz.bliksoft.javautils.app.BaseAppModule");

		ServiceLoader<IModule> loader = ServiceLoader.load(IModule.class);

		boolean allEnabled = enabledModules.contains("*");

		sortedModules = new ArrayList<>();

		Set<Class<? extends IModule>> localAutoloadedModules = new HashSet<>(autoloadedModules);

		Iterator<IModule> modulesIterator = loader.iterator();
		while (modulesIterator.hasNext()) {
			try {
				IModule pd = modulesIterator.next();
				// remove from special loading cycle to prevent duplicities
				localAutoloadedModules.remove(pd.getClass());
				registerModule(pd, allEnabled);
			} catch (ServiceConfigurationError e) {
				log.error(ModulesMessages.getString("Modules.ClassNotAModule"), e.getMessage()); //$NON-NLS-1$
			}
		}

		localAutoloadedModules.forEach(mc -> {
			try {
				IModule pd = mc.getDeclaredConstructor().newInstance();
				registerModule(pd, allEnabled);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.error(ModulesMessages.getString("Modules.ClassNotAModule"), e.getMessage()); //$NON-NLS-1$
			}
		});

		Collections.sort(sortedModules,
				(IModule m1, IModule m2) -> Integer.compare(m1.getModuleLoadingOrder(), m2.getModuleLoadingOrder()));

		for (IModule pd : sortedModules) {
			modules.put(pd.getClass().getName(), pd);

			if (pd.isEnabled()) {
				log.log(Level.INFO, ModulesMessages.getString("Modules.ModuleFound"), pd.getModuleName(), //$NON-NLS-1$
						pd.getVersionInfo()); // $NON-NLS-2$
				InputStream is = pd.getFilesystemXml();
				if (is != null) {
					try {
						FileSystem.getDefault().importXml(is, "module:" + pd.getModuleName()); //$NON-NLS-1$
					} catch (Exception e) {
						log.error(ModulesMessages.getString("Modules.FailedToLoadRootXMLForModule"), pd.getModuleName(), //$NON-NLS-1$
								LogUtils.traceToString(e));
						throw e;
					}
				} else {
					log.warn(ModulesMessages.getString("Modules.ModuleMissingRootXML"), pd.getModuleName()); //$NON-NLS-1$
				}

			} else {
				log.log(Level.INFO, ModulesMessages.getString("Modules.ModuleDisabledByCfg"), pd.getClass().getName(), //$NON-NLS-1$
						pd.getModuleName()); // $NON-NLS-2$
			}
		}

		FileSystem.loadTranslations();
	}

	private static void registerModule(IModule pd, boolean forceEnabled) {
		boolean enabled = forceEnabled;
		if (enabled && disabledModules.contains(pd.getClass().getName())) {
			enabled = false;
		}
		if (!enabled && (enabledModules.contains(pd.getClass().getName()))
				|| (forceEnabledModules.contains(pd.getClass().getName()))) {
			enabled = true;
		}

		pd.setEnabled(enabled);

		sortedModules.add(pd);
	}

	public static Map<String, IModule> getModules() {
		return modules;
	}

	/**
	 * Initialize loaded modules
	 */
	public static void initModules() {
		log.log(Level.DEBUG, ModulesMessages.getString("Modules.ModulesInitialionStart"));
		for (IModule pd : sortedModules) {
			if (pd.isEnabled()) {
				try {
					if (!ModuleBase.class.equals(pd.getClass().getMethod("init").getDeclaringClass())) { //$NON-NLS-1$
						log.log(Level.DEBUG, ModulesMessages.getString("Modules.ModuleInitializationStart"),
								pd.getModuleName());
						pd.init();
						log.log(Level.DEBUG, ModulesMessages.getString("Modules.ModuleInitializationCompleted"), //$NON-NLS-1$
								pd.getModuleName()); // $NON-NLS-2$
					} else {
						log.debug(ModulesMessages.getString("Modules.ModuleNotProvidingInitMethod"), //$NON-NLS-1$
								pd.getModuleName());
					}
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				} catch (Exception e) {
					log.error("Modules initialization crashed: " + pd.getModuleName(), e);
					throw e;
				}
			}
		}
		log.log(Level.DEBUG, ModulesMessages.getString("Modules.ModulesInitialionCompleted"));
	}

	/**
	 * start modules main function (e.g. UI startup)
	 */
	public static void installModules() {
		if (modules.isEmpty()) {
			log.info(ModulesMessages.getString("Modules.NoModulesToStart")); //$NON-NLS-1$
		} else {
			log.info(ModulesMessages.getString("Modules.ModulesStartup")); //$NON-NLS-1$
			// instalace modulů
			for (IModule md : sortedModules) {
				if (md.isEnabled()) {
					try {
						if (!ModuleBase.class.equals(md.getClass().getMethod("install").getDeclaringClass())) { //$NON-NLS-1$
							log.log(Level.DEBUG,
									ModulesMessages.getString("Modules.ModuleInstalationStart", md.getModuleName())); //$NON-NLS-1$
							md.install();
							log.log(Level.DEBUG, ModulesMessages.getString("Modules.ModuleInstalationCompleted", //$NON-NLS-1$
									md.getModuleName()));
						} else {
							log.debug(ModulesMessages.getString("Modules.ModuleNotProvidingInstallMethod"), //$NON-NLS-1$
									md.getModuleName());
						}
					} catch (NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					} catch (Exception e) {
						log.error("Modules installation crashed: " + md.getModuleName(), e);
						throw e;
					}
				}
			}
			log.info(ModulesMessages.getString("Modules.StartupFinished")); //$NON-NLS-1$
		}
	}

	public static void cleanup() {
		for (IModule pd : modules.values()) {
			try {
				if (pd.isEnabled()) {
					log.info("Cleanup: " + pd.getModuleName());
					pd.cleanup();
				}
			} catch (Exception e) {
				log.error("Failed cleanup: " + pd.getModuleName(), e);
			}
		}
	}
}
