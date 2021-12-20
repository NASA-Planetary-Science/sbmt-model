package edu.jhuapl.sbmt.model.phobos.ui;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import edu.jhuapl.sbmt.query.filter.ui.table.NumericFilterTableView;

public class MEGANEFootprintFilterPanel extends JPanel
{
	private JToggleButton selectRegionButton;
	private JButton clearRegionButton;
	private JButton submitButton;
	private NumericFilterTableView filterTables;

	public MEGANEFootprintFilterPanel()
	{
		initGUI();
	}

	private void initGUI()
	{
		setBorder(BorderFactory.createTitledBorder("Filter Footprints"));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		selectRegionButton = new JToggleButton("Select Region");
        clearRegionButton = new JButton("Clear Region");
        submitButton = new JButton("Search");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        filterTables = new NumericFilterTableView();
//        filterTables.setup(searchModel.getNumericFilterModel(), searchModel.getNonNumericFilterModel(), searchModel.getTimeWindowModel());
        add(filterTables);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(selectRegionButton);
        buttonPanel.add(clearRegionButton);
        buttonPanel.add(submitButton);
        add(Box.createVerticalGlue());
        add(buttonPanel);
        setPreferredSize(new Dimension(400, 400));
	}

	/**
	 * @return the selectRegionButton
	 */
	public JToggleButton getSelectRegionButton()
	{
		return selectRegionButton;
	}

	/**
	 * @return the clearRegionButton
	 */
	public JButton getClearRegionButton()
	{
		return clearRegionButton;
	}

	/**
	 * @return the submitButton
	 */
	public JButton getSubmitButton()
	{
		return submitButton;
	}

	/**
	 * @return the filterTables
	 */
	public NumericFilterTableView getFilterTables()
	{
		return filterTables;
	}

}

