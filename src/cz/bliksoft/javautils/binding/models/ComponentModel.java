package cz.bliksoft.javautils.binding.models;

import cz.bliksoft.javautils.binding.interfaces.IObservable;

/**
 * Describes bound properties for the frequently used JComponent state
 * <em>enabled</em>,<em>visible</em> and JTextComponent state <em>editable</em>.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.15 $
 *
 * @see ComponentValueModel
 *
 * @since 2.4
 */
public interface ComponentModel extends IObservable {

	// Property Names *********************************************************

	/**
	 * The name of the property used to synchronize this model with the
	 * <em>enabled</em> property of JComponents.
	 */
	String PROPERTY_ENABLED = "enabled";

	/**
	 * The name of the property used to synchronize this model with the
	 * <em>visible</em> property of JComponents.
	 */
	String PROPERTY_VISIBLE = "visible";

	/**
	 * The name of the property used to synchronize this model with the
	 * <em>editable</em> property of JTextComponents.
	 */
	String PROPERTY_EDITABLE = "editable";

	// Properties *************************************************************

	/**
	 * Returns if this model represents an enabled or disabled component state.
	 *
	 * @return true for enabled, false for disabled
	 */
	boolean isEnabled();

	/**
	 * Enables or disabled this model, which in turn will enable or disable all
	 * Swing components bound to this model.
	 *
	 * @param b true to enable, false to disable.
	 */
	void setEnabled(boolean b);

	/**
	 * Returns if this model represents the visible or invisible component state.
	 *
	 * @return true for visible, false for invisible
	 */
	boolean isVisible();

	/**
	 * Sets this model state to visible or invisible, which in turn will make all
	 * Swing components bound to this model visible or invisible.
	 *
	 * @param b true for visible, false for invisible
	 */
	void setVisible(boolean b);

	/**
	 * Returns if this model represents the editable or non-editable text component
	 * state.
	 *
	 * @return true for editable, false for non-editable
	 */
	boolean isEditable();

	/**
	 * Sets this model state to editable or non-editable, which in turn will make
	 * all text components bound to this model editable or non-editable.
	 *
	 * @param b true for editable, false for non-editable
	 */
	void setEditable(boolean b);

}
