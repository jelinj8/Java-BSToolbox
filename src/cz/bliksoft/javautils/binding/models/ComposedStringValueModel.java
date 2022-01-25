package cz.bliksoft.javautils.binding.models;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.binding.adapters.BeanAdapter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;

public class ComposedStringValueModel<S> extends AbstractValueModel<String> {
	private static final Logger log = Logger.getLogger(ComposedStringValueModel.class.getName());

	// private ArrayList<PropertyChangeListener> listeners = new ArrayList<>();

	private ArrayList<ArrayList<Object>> gettersTree;

	private String formatString;
	private String[] definition;

	private IValueModel<S> vModel;

	private Class<?> valueClass;

	public ComposedStringValueModel(Class<?> valueClass, IValueModel<S> sourceModel, String formatString,
			String... definition) {
		this.valueClass = valueClass;
		this.definition = definition;
		this.gettersTree = new ArrayList<>();
		this.formatString = formatString;
		for (String parDef : this.definition) {
			this.gettersTree.add(this.getGetterBranch(parDef));
		}

		this.vModel = sourceModel;
		vModel.addValueChangeListener(evt -> ComposedStringValueModel.this
				.firePropertyChange(IValueModel.PROPERTY_VALUE, null, ComposedStringValueModel.this.getValue()));
	}

	public ComposedStringValueModel(Class<?> beanClass, BeanAdapter<S> beanAdapter, String formatString,
			String... definition) {
		this.valueClass = beanClass;
		this.definition = definition;
		this.gettersTree = new ArrayList<>();
		this.formatString = formatString;
		for (String parDef : this.definition) {
			this.gettersTree.add(this.getGetterBranch(parDef));
		}

		this.vModel = beanAdapter.getBeanChannel();
		vModel.addValueChangeListener(evt -> ComposedStringValueModel.this
				.firePropertyChange(IValueModel.PROPERTY_VALUE, null, ComposedStringValueModel.this.getValue()));

		beanAdapter.addBeanPropertyChangeListener(evt -> {
			for (String def : ComposedStringValueModel.this.definition) {
				if (def.contains(evt.getPropertyName().substring(1))) {
					ComposedStringValueModel.this.firePropertyChange(IValueModel.PROPERTY_VALUE, null,
							ComposedStringValueModel.this.getValue());
					log.info("Changed property: " + evt.getPropertyName());
					break;
				}
			}
		});
	}

	private ArrayList<Object> getGetterBranch(String path) {
		ArrayList<Object> result = new ArrayList<>();
		String[] pathElements = path.split("\\.");
		Class<?> subject = this.valueClass;
		String element = "";
		try {
			for (int i = 0; i < pathElements.length; i++) {
				element = pathElements[i];
				if (element.endsWith("()")) { //$NON-NLS-1$
					Method m = subject.getMethod(element.replace("()", "")); //$NON-NLS-1$
					subject = m.getReturnType();
					result.add(m);
				} else {
					try {
						Method m = subject
								.getMethod("get" + element.substring(0, 1).toUpperCase() + element.substring(1)); //$NON-NLS-1$
						subject = m.getReturnType();
						result.add(m);
					} catch (Exception E) {
						Field f = subject.getField(element);
						subject = f.getType();
						result.add(f);
					}
				}
			}
		} catch (Exception E) {
			log.severe(MessageFormat.format("Chyba při načítání definice CompositeValueModelu ({0}){1}\n{2}",
					this.definition, (StringUtils.hasText(element) ? " na " + element : ""), E));
		}
		return result;
	}

	@Override
	public String getValue() {
		return this.getValue(this.vModel.getValue());
	}

	public String getValue(Object _bean) {
		try {
			Object object = _bean;
			if (object != null) {
				ArrayList<Object> params = new ArrayList<>();
				for (ArrayList<Object> branch : this.gettersTree) {
					Object param = object;
					for (Object getter : branch) {
						if (param == null) {
							break;
						}
						if (getter instanceof Method) {
							param = ((Method) getter).invoke(param);
						} else if (getter instanceof Field) {
							param = ((Field) getter).get(param);
						}
					}
					if (param == null) {
						if (log.isLoggable(Level.FINE))
							log.fine(MessageFormat.format("Getter byl vyhodnocen jako NULL ({0})",
									StringUtils.concatenateList(".", branch)));
						return null;
					} else {
						params.add(param);
					}
				}
				if (this.formatString == null) {
					if (params.isEmpty())
						return null;
					else {
						Object res = params.get(0);
						if (res != null)
							return res.toString();
						else
							return null;
					}
				} else {
					return MessageFormat.format(this.formatString, params.toArray());
//					for (Object o : params) {
//						if (o != null)
//							return MessageFormat.format(this.formatString, params.toArray());
//					}
//					return null;
				}
			} else
				return null;
		} catch (Exception e) {
			log.severe(MessageFormat.format("getValue error on ComposedValueModel ({0})\n{1}", this.definition, e));//$NON-NLS-1$

		}
		return null;
	}

	@Override
	public void setValue(String arg0) {
		throw new UnsupportedOperationException("Setter not supported!");
	}

}
