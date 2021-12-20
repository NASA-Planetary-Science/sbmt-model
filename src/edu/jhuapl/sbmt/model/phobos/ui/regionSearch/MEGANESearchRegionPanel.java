package edu.jhuapl.sbmt.model.phobos.ui.regionSearch;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import edu.jhuapl.sbmt.model.phobos.model.MEGANESearchModel;
import edu.jhuapl.sbmt.query.filter.ui.table.NumericFilterTableView;

@Deprecated
public class MEGANESearchRegionPanel extends JPanel
{
	private JToggleButton selectRegionButton;
	private JButton clearRegionButton;
	private JButton submitButton;

	public MEGANESearchRegionPanel(MEGANESearchModel searchModel)
	{
		initGUI(searchModel);
	}

	private void initGUI(MEGANESearchModel searchModel)
	{
		selectRegionButton = new JToggleButton("Select Region");
        clearRegionButton = new JButton("Clear Region");
        submitButton = new JButton("Search");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        var numericTableView = new NumericFilterTableView();
        add(numericTableView);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(selectRegionButton);
        buttonPanel.add(clearRegionButton);
        buttonPanel.add(submitButton);
        add(Box.createVerticalGlue());
        add(buttonPanel);
        setPreferredSize(new Dimension(400, 400));
        setMaximumSize(new Dimension(400, 600));
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
}
