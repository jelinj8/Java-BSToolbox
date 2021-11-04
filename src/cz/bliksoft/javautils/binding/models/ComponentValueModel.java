package cz.bliksoft.javautils.binding.models;

import cz.bliksoft.javautils.binding.Bindings;
import cz.bliksoft.javautils.binding.PresentationModel;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;

/**
 * Adds bound properties for the frequently used JComponent state
 * <em>enabled</em>,<em>visible</em> and JTextComponent state <em>editable</em>
 * to the ValueModel interface. ComponentValueModels can be used to set these
 * properties at the presentation model layer; any ComponentValueModel property
 * change will be reflected by components bound to that ComponentValueModel.
 * <p>
 *
 * The ComponentValueModel is similar to the Swing Action class. If you disable
 * an Action, all buttons and menu items bound to that Action will be disabled.
 * If you disable a ComponentValueModel, all components bound to that
 * ComponentValueModel will be disabled. If you set the ComponentValueModel to
 * invisible, the component bound to it will become invisible. If you set a
 * ComponentValueModel to non-editable, the JTextComponents bound to it will
 * become non-editable.
 * <p>
 *
 * Since version 1.1, PresentationModels can vend ComponentValueModels using
 * {@code #getComponentModel(String)} and
 * {@code #getBufferedComponentModel(String)}. Multiple calls to these factory
 * methods return the same ComponentValueModel.
 * <p>
 *
 * The BasicComponentFactory and the Bindings class check if the ValueModel
 * provided to create/bind a Swing component is a ComponentValueModel. If so,
 * the ComponentValueModel properties will be synchronized with the associated
 * Swing component properties.
 * <p>
 *
 * It is recommended to use ComponentValueModels only for those models that are
 * bound to view components that require GUI state changes.
 * <p>
 *
 * <strong>Example Code:</strong>
 * 
 * <pre>
 * final class AlbumView {
 *
 *  ...
 *
 *     private void initComponents() {
 *         // No state modifications required for the name field.
 *         nameField = BasicComponentFactory.createTextField(
 *             presentationModel.getModel(Album.PROPERTY_NAME));
 *         ...
 *         // Enablement shall change for the composer field
 *         composerField = BasicComponentFactory.createTextField(
 *             presentationModel.getComponentModel(Album.PROPERTY_COMPOSER));
 *         ...
 *     }
 *
 *  ...
 *
 * }
 *
 *
 * public final class AlbumPresentationModel extends PresentationModel {
 *
 *  ...
 *
 *     private void updateComposerEnablement(boolean enabled) {
 *         getComponentModel(Album.PROPERTY_COMPOSER).setEnabled(enabled);
 *     }
 *
 *  ...
 *
 * }
 * </pre>
 * <p>
 *
 * As of the Binding version 2.0 the ComponentValueModel feature is implemented
 * for text components, radio buttons, check boxes, combo boxes, and lists.
 * JColorChoosers bound using the Bindings class will ignore ComponentValueModel
 * state.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.15 $
 *
 * @see PresentationModel#getComponentModel(String)
 * @see PresentationModel#getBufferedComponentModel(String)
 * @see Bindings
 *
 * @since 2.4
 */
public interface ComponentValueModel<V> extends IValueModel<V>, ComponentModel {

	// Just a combined interface

}
