package cz.bliksoft.javautils.dialogs;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import javax.swing.JLabel;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;

import javax.swing.border.EmptyBorder;

public class ValueDLG extends JPanel {
	private JLabel lblCaptionLabel;
	private JTextField textField;
	int result = JOptionPane.CANCEL_OPTION;

	public ValueDLG() {
		setBorder(new EmptyBorder(3, 3, 3, 3));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		lblCaptionLabel = new JLabel("New label");
		GridBagConstraints gbc_lblCaptionLabel = new GridBagConstraints();
		gbc_lblCaptionLabel.anchor = GridBagConstraints.WEST;
		gbc_lblCaptionLabel.gridwidth = 2;
		gbc_lblCaptionLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblCaptionLabel.gridx = 0;
		gbc_lblCaptionLabel.gridy = 0;
		add(lblCaptionLabel, gbc_lblCaptionLabel);

		textField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.anchor = GridBagConstraints.WEST;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 1;
		add(textField, gbc_textField);
		textField.setColumns(30);
	}

	public static String prompt(String title, String prompt, String value, String cancelValue) throws Exception {
		ValueDLG dlg = new ValueDLG();
		dlg.getCaptionLabel().setText(prompt);
		dlg.getTextField().setText(value);

		if (SwingUtilities.isEventDispatchThread()) {
			dlg.result = JOptionPane.showConfirmDialog(null, dlg, title, JOptionPane.OK_CANCEL_OPTION);
		} else {
			EventQueue.invokeAndWait(() -> {
				dlg.result = JOptionPane.showConfirmDialog(null, dlg, title, JOptionPane.OK_CANCEL_OPTION);
			});
		}

		if (dlg.result == JOptionPane.OK_OPTION) {
			return dlg.getTextField().getText();
		} else {
			return cancelValue;
		}
	}

	public JLabel getCaptionLabel() {
		return lblCaptionLabel;
	}

	public JTextField getTextField() {
		return textField;
	}
}
