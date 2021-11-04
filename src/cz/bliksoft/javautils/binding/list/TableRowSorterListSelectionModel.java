/*
 * Copyright (c) 2012-2015 JGoodies Software GmbH. All Rights Reserved.
 *
 * This software is the proprietary information of JGoodies Software GmbH.
 * Use is subject to license terms.
 *
 */

package cz.bliksoft.javautils.binding.list;

import static cz.bliksoft.javautils.Preconditions.checkNotNull;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cz.bliksoft.javautils.binding.Bindings;

/**
 * A ListSelectionModel for a sorted table that is synchronized with another
 * <em>delegate</em> ListSelectionModel and converts its unsorted indices to the
 * sorted indices provided by the table's row sorter.
 * <p>
 * 
 * If a JTable has a RowSorter, the indices in the table's ListSelectionModel
 * reflect the table's sort state. In other words, sorting modifies the
 * selection indices. This makes it difficult to share ListSelectionModels for
 * multiple JLists/JTables - which is a fundamental feature of the
 * <em>Presentation Model</em> pattern. For example, if the first row is
 * selected in unsorted state, the ListSelectionModel has a minSelectionIndex of
 * 0. Accessing a ListModel or TableModel at row index 0 will get you the first
 * row. Let's say you sort the table and the first row is now displayed as the
 * third row, then the ListSelectionModel gets a minSelectionIndex of 2.
 * <p>
 * 
 * This class is used to keep an unsorted ListSelectionModel (typically provided
 * by a presentation model) in synch with the sorted indices of the table's
 * sorted ListSelectionModel.
 * <p>
 * 
 * Instances of this class are automatically created when using
 * {@link Bindings#bind(JTable, javax.swing.ListModel, ListSelectionModel)} on a
 * sorted table.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.1 $ $Date: 2013/01/30 18:35:09 $
 */
public final class TableRowSorterListSelectionModel extends DefaultListSelectionModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1344961228854792161L;
	private final ListSelectionModel delegate;
	private final JTable table;
	private final ListSelectionListener updateListener;

	// Instance Creation ******************************************************

	public TableRowSorterListSelectionModel(ListSelectionModel listSelectionModel, JTable table) {
		this.delegate = checkNotNull(listSelectionModel, "The delegate ListSelectionModel must not be null.");
		this.table = checkNotNull(table, "The table must not be null.");
		this.updateListener = new ViewUpdateListener();
		initEventHandling();
	}

	private void initEventHandling() {
		delegate.addListSelectionListener(updateListener);
		initViewSelectionFromModel();
	}

	private void initViewSelectionFromModel() {
		int firstModelIndex = delegate.getMinSelectionIndex();
		int lastModelIndex = delegate.getMaxSelectionIndex();
		super.clearSelection();
		for (int modelIndex = firstModelIndex; modelIndex <= lastModelIndex; modelIndex++) {
			int viewIndex = convertIndexToView(modelIndex);
			boolean modelSelected = delegate.isSelectedIndex(modelIndex);
			if (modelSelected) {
				super.addSelectionInterval(viewIndex, viewIndex);
			}
		}
	}

	private void updateViewSelectionFromModel(int firstModelIndex, int lastModelIndex) {
		for (int modelIndex = firstModelIndex; modelIndex <= lastModelIndex; modelIndex++) {
			int viewIndex = convertIndexToView(modelIndex);
			boolean modelSelected = delegate.isSelectedIndex(modelIndex);
			boolean viewSelected = isSelectedIndex(viewIndex);
			// System.out.print(String.format("%1$s -> %2$s: ", modelIndex, viewIndex));
			if (modelSelected == viewSelected) {
				// System.out.println("synchronized");
				continue;
			}
			if (modelSelected) {
				// System.out.println("add");
				super.addSelectionInterval(viewIndex, viewIndex);
			} else {
				// System.out.println("remove");
				super.removeSelectionInterval(viewIndex, viewIndex);
			}
		}
//		System.out.println("RowSorterLSM min index=" + getMinSelectionIndex());
//		System.out.println("RowSorterLSM max index=" + getMaxSelectionIndex());
	}

	// Accessors **************************************************************

	/**
	 * Changes the selection to be between {@code index0} and {@code index1}
	 * inclusive. {@code index0} doesn't have to be less than or equal to
	 * {@code index1}.
	 * <p>
	 * In {@code SINGLE_SELECTION} selection mode, only the second index is used.
	 * <p>
	 * If this represents a change to the current selection, then each
	 * {@code ListSelectionListener} is notified of the change.
	 * 
	 * @param index0 one end of the interval.
	 * @param index1 other end of the interval
	 * @see #addListSelectionListener
	 */
	@Override
	public void setSelectionInterval(int index0, int index1) {
		// System.out.println(String.format("#setSelectionInterval(%1$s, %2$s)", index0,
		// index1));
		super.setSelectionInterval(index0, index1);

		delegate.removeListSelectionListener(updateListener);
		try {
			if (getSelectionMode() == SINGLE_SELECTION) {
				delegate.setSelectionInterval(0, convertIndexToModel(index1));
			} else {
				delegate.setValueIsAdjusting(true);
				delegate.clearSelection();
				int start = Math.min(index0, index1);
				int end = Math.max(index0, index1);
				for (int i = start; i <= end; i++) {
					int modelIndex = convertIndexToModel(i);
					delegate.addSelectionInterval(modelIndex, modelIndex);
				}
				delegate.setValueIsAdjusting(false);
			}
		} finally {
			delegate.addListSelectionListener(updateListener);
		}
	}

	/**
	 * Changes the selection to be the set union of the current selection and the
	 * indices between {@code index0} and {@code index1} inclusive. {@code index0}
	 * doesn't have to be less than or equal to {@code index1}.
	 * <p>
	 * In {@code SINGLE_SELECTION} selection mode, this is equivalent to calling
	 * {@code setSelectionInterval}, and only the second index is used. In
	 * {@code SINGLE_INTERVAL_SELECTION} selection mode, this method behaves like
	 * {@code setSelectionInterval}, unless the given interval is immediately
	 * adjacent to or overlaps the existing selection, and can therefore be used to
	 * grow the selection.
	 * <p>
	 * If this represents a change to the current selection, then each
	 * {@code ListSelectionListener} is notified of the change.
	 * 
	 * @param index0 one end of the interval.
	 * @param index1 other end of the interval
	 * @see #addListSelectionListener
	 * @see #setSelectionInterval
	 */
	@Override
	public void addSelectionInterval(int index0, int index1) {
		// System.out.println(String.format("#addSelectionInterval(%1$s, %2$s)", index0,
		// index1));
		super.addSelectionInterval(index0, index1);

		delegate.removeListSelectionListener(updateListener);
		try {
			if (getSelectionMode() == SINGLE_SELECTION) {
				delegate.addSelectionInterval(0, convertIndexToModel(index1));
			} else {
				delegate.setValueIsAdjusting(true);
				int start = Math.min(index0, index1);
				int end = Math.max(index0, index1);
				for (int i = start; i <= end; i++) {
					int modelIndex = convertIndexToModel(i);
					delegate.addSelectionInterval(modelIndex, modelIndex);
				}
				delegate.setValueIsAdjusting(false);
			}
		} finally {
			delegate.addListSelectionListener(updateListener);
		}
	}

	/**
	 * Changes the selection to be the set difference of the current selection and
	 * the indices between {@code index0} and {@code index1} inclusive.
	 * {@code index0} doesn't have to be less than or equal to {@code index1}.
	 * <p>
	 * In {@code SINGLE_INTERVAL_SELECTION} selection mode, if the removal would
	 * produce two disjoint selections, the removal is extended through the greater
	 * end of the selection. For example, if the selection is {@code 0-10} and you
	 * supply indices {@code 5,6} (in any order) the resulting selection is
	 * {@code 0-4}.
	 * <p>
	 * If this represents a change to the current selection, then each
	 * {@code ListSelectionListener} is notified of the change.
	 * 
	 * @param index0 one end of the interval.
	 * @param index1 other end of the interval
	 * @see #addListSelectionListener
	 */
	@Override
	public void removeSelectionInterval(int index0, int index1) {
		// System.out.println(String.format("#removeSelectionInterval(%1$s, %2$s)",
		// index0, index1));
		super.removeSelectionInterval(index0, index1);

		delegate.removeListSelectionListener(updateListener);
		try {
			if (getSelectionMode() == SINGLE_SELECTION) {
				delegate.removeSelectionInterval(0, convertIndexToModel(index1));
			} else {
				delegate.setValueIsAdjusting(true);
				int start = Math.min(index0, index1);
				int end = Math.max(index0, index1);
				for (int i = start; i <= end; i++) {
					int modelIndex = convertIndexToModel(i);
					delegate.removeSelectionInterval(modelIndex, modelIndex);
				}
				delegate.setValueIsAdjusting(false);
			}
		} finally {
			delegate.addListSelectionListener(updateListener);
		}
	}

	/**
	 * Change the selection to the empty set. If this represents a change to the
	 * current selection then notify each ListSelectionListener.
	 * 
	 * @see #addListSelectionListener
	 */
	@Override
	public void clearSelection() {
		super.clearSelection();

		delegate.removeListSelectionListener(updateListener);
		try {
			delegate.clearSelection();
		} finally {
			delegate.addListSelectionListener(updateListener);
		}
	}

	/**
	 * Insert length indices beginning before/after index. This is typically called
	 * to sync the selection model with a corresponding change in the data model.
	 */
	@Override
	public void insertIndexInterval(int index, int length, boolean before) {
		throw new UnsupportedOperationException("#insertIndexInterval not yet implemented");
	}

	/**
	 * Remove the indices in the interval index0,index1 (inclusive) from the
	 * selection model. This is typically called to sync the selection model width a
	 * corresponding change in the data model.
	 */
	@Override
	public void removeIndexInterval(int index0, int index1) {
		throw new UnsupportedOperationException("#removeIndexInterval not yet implemented");
	}

	/**
	 * Sets the selection mode. The following list describes the accepted selection
	 * modes:
	 * <ul>
	 * <li>{@code ListSelectionModel.SINGLE_SELECTION} - Only one list index can be
	 * selected at a time. In this mode, {@code setSelectionInterval} and
	 * {@code addSelectionInterval} are equivalent, both replacing the current
	 * selection with the index represented by the second argument (the "lead").
	 * <li>{@code ListSelectionModel.SINGLE_INTERVAL_SELECTION} - Only one
	 * contiguous interval can be selected at a time. In this mode,
	 * {@code addSelectionInterval} behaves like {@code setSelectionInterval}
	 * (replacing the current selection), unless the given interval is immediately
	 * adjacent to or overlaps the existing selection, and can therefore be used to
	 * grow it.
	 * <li>{@code ListSelectionModel.MULTIPLE_INTERVAL_SELECTION} - In this mode,
	 * there's no restriction on what can be selected.
	 * </ul>
	 * 
	 * @see #getSelectionMode
	 * @throws IllegalArgumentException if the selection mode isn't one of those
	 *                                  allowed
	 */
	@Override
	public void setSelectionMode(int selectionMode) {
		delegate.setSelectionMode(selectionMode);
	}

	/**
	 * Returns the current selection mode.
	 *
	 * @return the current selection mode
	 * @see #setSelectionMode
	 */
	@Override
	public int getSelectionMode() {
		return delegate.getSelectionMode();
	}

	// Mapping Indices ********************************************************

	@SuppressWarnings("rawtypes")
	private int convertIndexToModel(int index) {
		if (index == -1) {
			return -1;
		}
		RowSorter rowSorter = table.getRowSorter();
		return rowSorter == null ? index : rowSorter.convertRowIndexToModel(index);
	}

	@SuppressWarnings("rawtypes")
	private int convertIndexToView(int index) {
		if (index == -1) {
			return -1;
		}
		RowSorter rowSorter = table.getRowSorter();
		return rowSorter == null ? index : rowSorter.convertRowIndexToView(index);
	}

	// Helper Classes *********************************************************

	private final class ViewUpdateListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			// System.out.println(String.format("model changed in [%1$s, %2$s];
			// adjusting=%3$s",
			// e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting()));
			if (e.getValueIsAdjusting()) {
				return;
			}
			updateViewSelectionFromModel(e.getFirstIndex(), inBounds(e.getLastIndex()));
		}

		@SuppressWarnings("rawtypes")
		private int inBounds(int index) {
			RowSorter rowSorter = table.getRowSorter();
			int rowCount = rowSorter == null ? table.getRowCount() : rowSorter.getViewRowCount();
			return Math.min(index, rowCount - 1);
		}

	}

}
