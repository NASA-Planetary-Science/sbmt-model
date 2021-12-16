package edu.jhuapl.sbmt.model.phobos.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANEStructureCollection;
import edu.jhuapl.sbmt.model.phobos.ui.table.MEGANEResultsTableView;

public class MEGANESearchPanel extends JPanel
{
	private JButton databaseLoadButton;
	private JLabel databaseNameLabel;
	private MEGANEResultsTableView tableView;
	private MEGANEFootprintFilterPanel filterPanel;

	public MEGANESearchPanel(MEGANECollection collection, MEGANEStructureCollection structureCollection, ModelManager modelManager)
	{
		databaseLoadButton = new JButton("Load Database");
		databaseNameLabel = new JLabel();
		tableView = new MEGANEResultsTableView(collection);
		tableView.setup();
		this.filterPanel = new MEGANEFootprintFilterPanel(structureCollection, modelManager);
		initGUI();
	}

	private void initGUI()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(databaseLoadButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(databaseNameLabel);
		add(buttonPanel);
		add(filterPanel);
		add(tableView);
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

	/**
	 * @return the filterPanel
	 */
	public MEGANEFootprintFilterPanel getFilterPanel()
	{
		return filterPanel;
	}
}
