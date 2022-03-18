package edu.jhuapl.sbmt.model.phobos.controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.ui.color.MEGANEFootprintColorConfigPanel;

/**
 * Controller that governs the coloring views in the State History tab
 * @author steelrj1
 *
 */
public class MEGANEFootprintColoringOptionsController implements ActionListener
{
	private MEGANEFootprintColorConfigPanel colorConfigPanel;

	private MEGANECollection rendererManager;

	private CumulativeMEGANECollection cumulativeCollection;

	/**
	 * @param historyModel
	 * @param renderer
	 */
	public MEGANEFootprintColoringOptionsController(MEGANECollection rendererManager, CumulativeMEGANECollection cumulativeCollection)
	{
		this.rendererManager = rendererManager;
		this.cumulativeCollection = cumulativeCollection;
		this.colorConfigPanel = new MEGANEFootprintColorConfigPanel(this, rendererManager, cumulativeCollection);
		System.out.println("MEGANEFootprintColoringOptionsController: MEGANEFootprintColoringOptionsController: color config panel " + colorConfigPanel);
		colorConfigPanel.setBorder(BorderFactory.createTitledBorder("Footprint Coloring"));
	}

	public void actionPerformed(ActionEvent e)
	{
		if (colorConfigPanel == null) return;
		GroupColorProvider srcGCP = colorConfigPanel.getSourceGroupColorProvider();
		rendererManager.installGroupColorProviders(srcGCP);
		cumulativeCollection.installGroupColorProviders(srcGCP);
	};

	/**
	 * The panel associated with this controller
	 * @return
	 */
	public JPanel getView()
	{
		return colorConfigPanel;
	}

	public void setEnabled(boolean enabled)
	{
		this.colorConfigPanel.setEnabled(enabled);
	}
}
