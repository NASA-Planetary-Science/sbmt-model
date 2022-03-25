package edu.jhuapl.sbmt.model.phobos.ui;

import java.awt.Color;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANESearchModel;
import edu.jhuapl.sbmt.model.phobos.ui.table.CumulativeMEGANEFootprintTableView;
import edu.jhuapl.sbmt.model.phobos.ui.table.MEGANEResultsTableView;

import glum.gui.GuiUtil;

public class MEGANESearchPanel extends JPanel
{
	private JButton databaseLoadButton;
	private JLabel databaseNameLabel;
	private MEGANEResultsTableView tableView;
	private CumulativeMEGANEFootprintTableView cumulativeFootprintTableView;
	private MEGANEFootprintFilterPanel filterPanel;
	private JButton updateColorsButton;

	public MEGANESearchPanel(MEGANECollection collection, CumulativeMEGANECollection cumulativeCollection, MEGANESearchModel searchModel)
	{
		databaseLoadButton = new JButton("Load Database");
		databaseNameLabel = new JLabel("Please load a database to view");
		databaseNameLabel.setForeground(Color.red);
		updateColorsButton = GuiUtil.formButton(null, "Plot Colors");
		updateColorsButton.setToolTipText("Adjust Coloring");
		updateColorsButton.setEnabled(false);
		tableView = new MEGANEResultsTableView(collection);
		tableView.setup();
		cumulativeFootprintTableView = new CumulativeMEGANEFootprintTableView(cumulativeCollection);
		cumulativeFootprintTableView.setup();
		this.filterPanel = new MEGANEFootprintFilterPanel();
		filterPanel.getFilterTables().setup(searchModel.getNumericFilterModel(), searchModel.getNonNumericFilterModel(), searchModel.getTimeWindowModel());
		initGUI();
	}

	private void initGUI()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(databaseLoadButton);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(databaseNameLabel);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(updateColorsButton);
		add(buttonPanel);
		add(filterPanel);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add(tableView, "Database");
		tabbedPane.add(cumulativeFootprintTableView, "Cumulative");
		add(tabbedPane);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		filterPanel.setEnabled(enabled);
		tableView.setEnabled(enabled);
		cumulativeFootprintTableView.setEnabled(enabled);
	}


	public JButton getDatabaseLoadButton()
	{
		return databaseLoadButton;
	}

	public JLabel getDatabaseNameLabel()
	{
		return databaseNameLabel;
	}

	/**
	 * @return the tableView
	 */
	public MEGANEResultsTableView getTableView()
	{
		return tableView;
	}

	public CumulativeMEGANEFootprintTableView getCumulativeTableView()
	{
		return cumulativeFootprintTableView;
	}

	/**
	 * @return the filterPanel
	 */
	public MEGANEFootprintFilterPanel getFilterPanel()
	{
		return filterPanel;
	}

	/**
	 * @return the updateColorsButton
	 */
	public JButton getUpdateColorsButton()
	{
		return updateColorsButton;
	}

	public void setStatus(String statusText)
	{
		filterPanel.setStatus(statusText);
	}
}
