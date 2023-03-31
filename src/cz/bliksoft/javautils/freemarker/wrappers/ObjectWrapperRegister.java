package cz.bliksoft.javautils.freemarker.wrappers;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class ObjectWrapperRegister extends DefaultObjectWrapper {
	static Logger log = Logger.getLogger(ObjectWrapperRegister.class.getName());

	private static boolean skipJ8TimeAPI = false;
	private static Object java8TimeAPIWrapper = null;

	private static List<Class<?>> registeredInputs = new ArrayList<>();
	private static Map<Class<?>, Function<Object, Object>> converters = new HashMap<>();

	private static Function<Object, Object> lastResortConverter = null;

	private static ObjectWrapperRegister instance = null;

	private ObjectWrapperRegister(Version incompatibleImprovements) {
		super(incompatibleImprovements);

		if (!skipJ8TimeAPI && java8TimeAPIWrapper == null) {
			try {
				Class<?> j8api = Class.forName("no.api.freemarker.java8.Java8ObjectWrapper");
				if (j8api != null) {
					Constructor<?> ctor = j8api.getConstructor(freemarker.template.Version.class);
					java8TimeAPIWrapper = ctor.newInstance(Configuration.VERSION_2_3_30);
					log.info("no.api.freemarker.java8.Java8ObjectWrapper loaded, registering ObjectWrapper");
				}
			} catch (ClassNotFoundException e) {
				log.info("no.api.freemarker.java8.Java8ObjectWrapper not present");
				skipJ8TimeAPI = true;
			} catch (Exception e) {
				log.severe("Failed attempt to load no.api.freemarker.java8.Java8ObjectWrapper: " + e.getMessage());
				skipJ8TimeAPI = true;
			}
		}
	}

	public static ObjectWrapperRegister getInstance(Version incompatibleImprovements) {
		if (instance == null)
			instance = new ObjectWrapperRegister(incompatibleImprovements);
		return instance;
	}

	public static void addConverter(Class<?> cls, Function<Object, Object> converter) {
		if (registeredInputs.contains(cls)) {
			log.severe(MessageFormat.format("Attempt to re-register converter for {0}", cls.getName()));
			return;
		}

		registeredInputs.add(cls);
		converters.put(cls, converter);
	}

	public static void registerDefaults() {
		log.info("Registering default converters");
		addConverter(Optional.class, (t) -> {
			if (((Optional<?>) t).isPresent())
				return ((Optional<?>) t).get();
			else
				return null;
		});

		addConverter(File.class, (o) -> {
			return ((File) o).getPath();
		});
	}

	public static void useToString() {
		lastResortConverter = (o) -> {
			try {
				return o.toString();
			} catch (Exception e) {
				log.severe(MessageFormat.format("Failed wrapping {0} by casting to String", o.getClass()));
				return null;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
		Class c = obj.getClass();
		for (Class<?> cls : registeredInputs) {
			if (cls.isAssignableFrom(obj.getClass())) {
				log.fine(MessageFormat.format("Wrapping {0} by registered converter for {1}", c.getName(),
						cls.getName()));
				return wrap(converters.get(cls).apply(obj));
			}
		}

		if (lastResortConverter != null) {
			if (!(obj instanceof Temporal) || java8TimeAPIWrapper == null) {
				log.severe(MessageFormat.format("Using lastResortConverter for {0}", c.getName()));
				Object preconverted = lastResortConverter.apply(obj); 
				return wrap(preconverted);
			}
		}

		if (java8TimeAPIWrapper != null) {
			log.fine(MessageFormat.format("Wrapping {0} as TemplateModel using java8TimeAPIWrapper", c.getName()));
			return ((ObjectWrapper) java8TimeAPIWrapper).wrap(obj);
		} else {
			log.fine(
					MessageFormat.format("Wrapping {0} as TemplateModel using default BeanTemplateModel", c.getName()));
			return super.handleUnknownType(obj);
		}
	}

}
