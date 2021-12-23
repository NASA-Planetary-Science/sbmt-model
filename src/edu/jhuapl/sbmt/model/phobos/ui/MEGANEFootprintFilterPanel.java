package edu.jhuapl.sbmt.model.phobos.ui;

import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.jhuapl.sbmt.model.phobos.ui.regionSearch.MEGANESearchRegionPanel;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANESearchStructurePanel;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANEStructureCollection;

public class MEGANEFootprintFilterPanel extends JPanel
{
	private JComboBox<FilterTypes> filterTypes;
	private MEGANESearchStructurePanel searchStructurePanel;
	private MEGANESearchRegionPanel searchCirclePanel;
	private MEGANEStructureCollection structureCollection;

	public MEGANEFootprintFilterPanel(MEGANEStructureCollection structureCollection)
	{
		this.structureCollection = structureCollection;
		initGUI();
	}

	private void initGUI()
	{
		filterTypes = new JComboBox<FilterTypes>(FilterTypes.values());
		setBorder(BorderFactory.createTitledBorder("Filter Footprints"));
		JPanel cardPanel = new JPanel(new CardLayout());
		searchCirclePanel = new MEGANESearchRegionPanel();
		searchStructurePanel = new MEGANESearchStructurePanel(structureCollection);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
		filterPanel.add(new JLabel("By:"));
		filterPanel.add(filterTypes);
		add(filterPanel);

		cardPanel.add(searchCirclePanel, FilterTypes.CIRCLE.getFullName());
		cardPanel.add(searchStructurePanel, FilterTypes.STRUCTURE.getFullName());
		add(cardPanel);

		filterTypes.addItemListener(e -> {
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
			cl.show(cardPanel, ((FilterTypes)e.getItem()).getFullName());
			if (((FilterTypes)e.getItem()).getFullName().equals(FilterTypes.STRUCTURE.getFullName()))
			{
				structureCollection.updateItems();
//				searchStructurePanel.updateStructuresList();
			}
			cardPanel.repaint();
		});
	}

	/**
	 * @return the searchStructurePanel
	 */
	public MEGANESearchStructurePanel getSearchStructurePanel()
	{
		return searchStructurePanel;
	}

	/**
	 * @return the searchCirclePanel
	 */
	public MEGANESearchRegionPanel getSearchCirclePanel()
	{
		return searchCirclePanel;
	}
}

enum FilterTypes {

	CIRCLE("Region and Attributes"),
	STRUCTURE("Structure");

	private String name;

	private FilterTypes(String name)
	{
		this.name = name;
	}

	public String getFullName()
	{
		return name;
	}

	public String toString()
	{
		return name;
	}
}
