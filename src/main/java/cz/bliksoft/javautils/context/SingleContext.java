package cz.bliksoft.javautils.context;

import java.util.ArrayList;

import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import cz.bliksoft.javautils.StringUtils;

/**
 * implementace kontextu pro uchovávání jedné hodnoty
 * 
 */
public class SingleContext<T> extends Context {
//	private static final Logger log = LogManager.getLogger();

	public SingleContext(String comment) {
		super(comment);
	}

	// public SingleContext(JTree tree, String _comment, Class<?>... limits) {
	// super(tree.getName() + " " + _comment); //$NON-NLS-1$
	// this.bind(tree, limits);
	// }
	//
	// public SingleContext(JList<T> list, String _comment, Class<?>... limits) {
	// super(list.getName() + " " + _comment); //$NON-NLS-1$
	// this.bind(list, limits);
	// }

	T value = null;

	public void setValue(T value) {
		if (value == this.value)
			return;

		Object oldValue = this.value;

		// Logger.getLogger(SingleContext.class.getName()).log(Level.INFO,
		// "ContextChangeFiring - ctx:''{0}'', new value:''{1}''", new Object[] { this,
		// value });
		// log.debug("SingleContext {} value changed from {} to {}", comment,
		// ObjectUtils.getAbbrevDescription(oldValue),
		// ObjectUtils.getAbbrevDescription(value));
		this.value = value;
		if (oldValue != null) {
			this.notifyListeners(new ContextSearchResult(this, oldValue.getClass()));
		}
		if (value != null) {
			this.notifyListeners(new ContextSearchResult(this, value.getClass(), value));
		}
	}

	public T getValue() {
		return this.value;
	}

	// public Object getValue() {
	// return this.value;
	// }

	@Override
	protected void notifyContextAllRemoved(Context lowestLevel) {
		super.notifyContextAllRemoved(lowestLevel);
		if (this.value != null) {
			lowestLevel.notifyListeners(new ContextSearchResult(this, this.value.getClass()));
		}
	}

	@Override
	protected void notifyContextAllAdded(Context lowestLevel) {
		super.notifyContextAllAdded(lowestLevel);
		if (this.value != null) {
			lowestLevel.notifyListeners(new ContextSearchResult(this, this.value.getClass(), this.value));
		}
	}

	@Override
	public ContextSearchResult getValue(Object key) {
		if (key instanceof Class) {
			if ((this.value != null) && (((Class<?>) key).isAssignableFrom(this.value.getClass())))
				return new ContextSearchResult(this, key, this.value);
		}
		return super.getValue(key);
	}

	public final SingleContext<T> bind(final JList<T> list, Class<?>... classes) {
		// final SingleContext result = new SingleContext(list.getName() +
		// " selection");
		final SingleContext<T> result = this;
		final ArrayList<Class<?>> limitedClasses = (classes.length > 0 ? new ArrayList<Class<?>>() : null);
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (limitedClasses == null) {
					result.setValue(list.getSelectedValue());
				} else {
					if ((list.getSelectedValue() != null)
							&& (limitedClasses.contains(list.getSelectedValue().getClass()))) {
						result.setValue(list.getSelectedValue());
					} else {
						result.setValue(null);
					}
				}
			}
		});
		return result;
	}

	public final SingleContext<T> bind(final JTree tree, Class<?>... classes) {
		// final SingleContext result = new SingleContext(tree.getName() +
		// " selection");
		final SingleContext<T> result = this;
		final ArrayList<Class<?>> limitedClasses = (classes.length > 0 ? new ArrayList<Class<?>>() : null);
		tree.addTreeSelectionListener(new TreeSelectionListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
					if ((limitedClasses == null)) {

						result.setValue((T) node.getUserObject());
					} else {
						if ((node.getUserObject() != null)
								&& (limitedClasses.contains(node.getUserObject().getClass()))) {
							result.setValue((T) node.getUserObject());
						} else {
							result.setValue(null);
						}
					}
				} else {
					result.setValue(null);
				}
			}
		});
		return result;
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "SingleCTX: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "SingleCTX";
	}
}
