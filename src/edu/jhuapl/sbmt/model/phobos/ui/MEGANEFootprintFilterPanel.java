package edu.jhuapl.sbmt.model.phobos.ui;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import edu.jhuapl.sbmt.query.filter.ui.table.NumericFilterTableView;

public class MEGANEFootprintFilterPanel extends JPanel
{
	private JToggleButton selectRegionButton;
	private JButton clearRegionButton;
	private JButton submitButton;
	private NumericFilterTableView filterTables;
	private JLabel status;

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
        status = new JLabel("Ready.");
        status.setMaximumSize(new Dimension(150, 30));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        filterTables = new NumericFilterTableView();
        add(filterTables);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(selectRegionButton);
        buttonPanel.add(clearRegionButton);
        buttonPanel.add(submitButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(status);
//        buttonPanel.add(Box.createHorizontalGlue());
        add(Box.createVerticalGlue());
        add(buttonPanel);
        setPreferredSize(new Dimension(400, 400));
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		filterTables.setEnabled(enabled);
		submitButton.setEnabled(enabled);
		clearRegionButton.setEnabled(enabled);
		selectRegionButton.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	public void setStatus(String statusText)
	{
		this.status.setText(statusText);
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