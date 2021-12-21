package edu.jhuapl.sbmt.model.phobos.ui.structureSearch;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.table.MEGANEStructureTableView;

public class MEGANESearchStructurePanel extends JPanel
{
	private JButton submitButton;
	private MEGANEStructureTableView table;

	public MEGANESearchStructurePanel(MEGANEStructureCollection collection)
	{
		this.table = new MEGANEStructureTableView(collection);
		table.setup();
		initGUI();
	}

	private void initGUI()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel buttonPanel = new JPanel();
		submitButton = new JButton("Search");
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(submitButton);

        add(table);
        add(Box.createVerticalGlue());
        add(buttonPanel);

		setPreferredSize(new Dimension(400, 400));
        setMaximumSize(new Dimension(400, 400));
	}

	public void updateStructuresList()
	{
		populateStructuresList();
	}

	private void populateStructuresList()
	{
//		collection.updateItems();

	}

	/**
	 * @return the submitButton
	 */
	public JButton getSubmitButton()
	{
		return submitButton;
	}
}
