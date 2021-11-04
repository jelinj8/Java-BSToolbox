package cz.bliksoft.javautils.binding.interfaces;

import cz.bliksoft.javautils.binding.beans.BeanState;

/**
 * poskytovatel informací o stavu, ve kterém se entita nachází
 * 
 * @author hroch
 */
public interface IBeanStateProvider {
	public static final String PROP_BEAN_STATE = "beanState"; //$NON-NLS-1$

	public BeanState getBeanState();

	public void setBeanState(BeanState newState);

	default void deleteBean() {
		if (getBeanState().isCreated()) {
			this.setBeanState(BeanState.SAVED_DELETED);
		} else {
			this.setBeanState(BeanState.DELETED);
		}
	}

	default void modifyBean() {
		switch (this.getBeanState()) {
		case CHILDREN_MODIFIED:
		case SAVED:
		case INITIAL:
			this.setBeanState(BeanState.MODIFIED);
			InnerTools.notifyParentChildModified(this);
			break;
		case CREATED:
			break;
		case DELETED:
			break;
		case MODIFIED:
			break;
		case SAVED_DELETED:
			break;
		case DUMMY:
			break;
		}
	}

	default void childModifyBean() {
		if (this.getBeanState() == BeanState.SAVED || this.getBeanState() == BeanState.INITIAL) {
			this.setBeanState(BeanState.CHILDREN_MODIFIED);
			InnerTools.notifyParentChildModified(this);
		}
	}

	default boolean getBeanModified() {
		return getBeanState().isModified();
	}

	default boolean getBeanCreated() {
		return getBeanState().isCreated();
	}

	default boolean getBeanDeleted() {
		return getBeanState().isDeleted();
	}

	class InnerTools {

		private InnerTools() {
		}

		private static void notifyParentChildModified(IBeanStateProvider bean) {
			if (bean instanceof IParentedBean) {
				Object parent = ((IParentedBean) bean).getBeanParent();
				if (parent instanceof IBeanStateProvider) {
					((IBeanStateProvider) parent).childModifyBean();
				}
			}
		}
	}
}
