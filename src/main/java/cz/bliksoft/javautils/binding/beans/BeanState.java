package cz.bliksoft.javautils.binding.beans;

import cz.bliksoft.javautils.Messages;

/**
 * sada hodnot rozlišujících různé stavy entit - podle úprav, vytváření a
 * ukládání
 * 
 * @author hroch
 */
public enum BeanState {

	/**
	 * uloženo
	 */
	SAVED,
	/**
	 * pozměněná dříve uložená entita
	 */
	MODIFIED,
	/**
	 * nově vytvořená entita
	 */
	CREATED,
	/**
	 * entita obsahuje pozměněné potomky
	 */
	CHILDREN_MODIFIED,
	/**
	 * označeno pro odstranění
	 */
	DELETED,
	/**
	 * entita po vymazání z úložiště
	 */
	SAVED_DELETED,
	/**
	 * nedefinovaný stav
	 */
	DUMMY,
	/**
	 * počáteční stav
	 */
	INITIAL;

	public boolean isModified() {
		return (this == MODIFIED) || (this == CREATED) || (this == DELETED);
	}

	public boolean isCreated() {
		return this == CREATED;
	}

	public boolean isDeleted() {
		return (this == DELETED) || (this == SAVED_DELETED);
	}

	/**
	 * textová reprezentace stavu entity
	 */
	@Override
	public String toString() {
		switch (this) {
		case MODIFIED:
			return "modified";//$NON-NLS-1$
		case CREATED:
			return "new";//$NON-NLS-1$
		case DELETED:
			return "deleted";//$NON-NLS-1$
		case SAVED_DELETED:
			return "savedDeleted";//$NON-NLS-1$
		case CHILDREN_MODIFIED:
			return "childrenMod";//$NON-NLS-1$
		case DUMMY:
			return "dummy";//$NON-NLS-1$
		case INITIAL:
			return "initial";//$NON-NLS-1$
		case SAVED:
			return "saved";//$NON-NLS-1$
		default:
			return "UNKNOWN STATE";//$NON-NLS-1$
		}
	}

	public String getName() {
		switch (this) {
		case MODIFIED:
			return Messages.getString("BeanState.modified"); //$NON-NLS-1$
		case CREATED:
			return Messages.getString("BeanState.new"); //$NON-NLS-1$
		case DELETED:
			return Messages.getString("BeanState.deleted"); //$NON-NLS-1$
		case SAVED_DELETED:
			return Messages.getString("BeanState.savedDeleted"); //$NON-NLS-1$
		case CHILDREN_MODIFIED:
			return Messages.getString("BeanState.childrenModified"); //$NON-NLS-1$
		case DUMMY:
			return Messages.getString("BeanState.dummy"); //$NON-NLS-1$
		case INITIAL:
			return Messages.getString("BeanState.initial"); //$NON-NLS-1$
		case SAVED:
		default:
			return Messages.getString("BeanState.saved"); //$NON-NLS-1$
		}
	}

}
