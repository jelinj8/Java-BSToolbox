package cz.bliksoft.javautils.binding.beans;

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
		return (this != SAVED) && (this != SAVED_DELETED) && (this != CHILDREN_MODIFIED) && (this != INITIAL) && (this != DUMMY);
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
			return "modif";//$NON-NLS-1$
		case CREATED:
			return "new";//$NON-NLS-1$
		case DELETED:
			return "del";//$NON-NLS-1$
		case SAVED_DELETED:
			return "s_del";//$NON-NLS-1$
		case CHILDREN_MODIFIED:
			return "childrenMod";//$NON-NLS-1$
		case DUMMY:
			return "dummy";//$NON-NLS-1$
		case INITIAL:
			return "initial";//$NON-NLS-1$
		case SAVED:
		default:
			return "saved";//$NON-NLS-1$

		}
	}

	public String getName() {
		switch (this) {
		case MODIFIED:
			return "modified";
		case CREATED:
			return "new";
		case DELETED:
			return "deleted";
		case SAVED_DELETED:
			return "saved deleted";
		case CHILDREN_MODIFIED:
			return "children modified";
		case DUMMY:
			return "dummy";
		case INITIAL:
			return "initial";
		case SAVED:
		default:
			return "saved";

		}
	}
//	public String getName() {
//		switch (this) {
//		case MODIFIED:
//			return BSFWMessages.getString("EntityState.stateNameModified"); //$NON-NLS-1$
//		case CREATED:
//			return BSFWMessages.getString("EntityState.stateNameNew"); //$NON-NLS-1$
//		case DELETED:
//			return BSFWMessages.getString("EntityState.stateNameDel"); //$NON-NLS-1$
//		case SAVED_DELETED:
//			return BSFWMessages.getString("EntityState.stateNameDelSaved"); //$NON-NLS-1$
//		case CHILDREN_MODIFIED:
//			return BSFWMessages.getString("EntityState.stateNameChildModified"); //$NON-NLS-1$
//		case DUMMY:
//			return BSFWMessages.getString("EntityState.stateNameDummy"); //$NON-NLS-1$
//		case SAVED:
//		default:
//			return BSFWMessages.getString("EntityState.stateNameSaved"); //$NON-NLS-1$
//
//		}
//	}

}
