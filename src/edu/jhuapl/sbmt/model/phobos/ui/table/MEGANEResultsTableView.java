package edu.jhuapl.sbmt.model.phobos.ui.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;

import glum.gui.GuiUtil;
import glum.gui.misc.BooleanCellEditor;
import glum.gui.misc.BooleanCellRenderer;
import glum.gui.panel.itemList.ItemHandler;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.ItemProcessor;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.item.ItemManagerUtil;

public class MEGANEResultsTableView extends JPanel
{

	/**
	 * JButton to load spectra from file
	 */
	private JButton loadSpectrumButton;

	/**
	 * JButton to remove spectra from table
	 */
	private JButton hideSpectrumButton;

	/**
	 * JButton to show spectra in renderer
	 */
	private JButton showSpectrumButton;

	private JButton addSpectraButton;



	/**
	 * JButton to save spectra to file
	 */
	private JButton saveSpectrumButton;

    protected JTable resultList;
    private JLabel resultsLabel;

    //for table
    private JButton selectAllB, selectInvertB, selectNoneB;
    private MEGANECollection meganeCollection;
    private ItemListPanel<MEGANEFootprint> meganeILP;
    private ItemHandler<MEGANEFootprint> meganeItemHandler;


	public MEGANEResultsTableView(MEGANECollection collection)
	{
		this.meganeCollection = collection;
		collection.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				resultList.repaint();
			}
		});
		init();
	}


    protected void init()
    {
        resultsLabel = new JLabel("0 Results");
        resultList = buildTable();
    }

    public void setup()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new TitledBorder(null, "Available Spectra", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        JPanel panel_4 = new JPanel();
        add(panel_4);
        panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));

        panel_4.add(resultsLabel);

        Component horizontalGlue = Box.createHorizontalGlue();
        panel_4.add(horizontalGlue);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new java.awt.Dimension(150, 550));
        add(scrollPane);

        scrollPane.setViewportView(resultList);
    }

    private JTable buildTable()
    {
    	ActionListener listener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();

				List<MEGANEFootprint> tmpL = meganeCollection.getSelectedItems().asList();
				if (source == selectAllB)
					ItemManagerUtil.selectAll(meganeCollection);
				else if (source == selectNoneB)
					ItemManagerUtil.selectNone(meganeCollection);
				else if (source == selectInvertB)
				{
					ItemManagerUtil.selectInvert(meganeCollection);
				}
			}
		};

    	// Table header

		loadSpectrumButton = GuiUtil.formButton(listener, UIManager.getIcon("FileView.directoryIcon"));
		loadSpectrumButton.setToolTipText(ToolTipUtil.getItemLoad());

		saveSpectrumButton = GuiUtil.formButton(listener, UIManager.getIcon("FileView.hardDriveIcon"));
		saveSpectrumButton.setToolTipText(ToolTipUtil.getItemSave());
		saveSpectrumButton.setEnabled(false);

		showSpectrumButton = GuiUtil.formButton(listener, IconUtil.getItemShow());
		showSpectrumButton.setToolTipText(ToolTipUtil.getItemShow());
		showSpectrumButton.setEnabled(false);

		hideSpectrumButton = GuiUtil.formButton(listener, IconUtil.getItemHide());
		hideSpectrumButton.setToolTipText(ToolTipUtil.getItemHide());
		hideSpectrumButton.setEnabled(false);

		addSpectraButton = GuiUtil.formButton(listener, "Add");
		addSpectraButton.setToolTipText("Combine Footprints");
		addSpectraButton.setEnabled(false);



		selectInvertB = GuiUtil.formButton(listener, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(listener, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(listener, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
//		titleL = new JLabel("Spectra: ---");
//		buttonPanel.add(titleL, "growx,span,split");
		buttonPanel.add(loadSpectrumButton);
		buttonPanel.add(saveSpectrumButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(showSpectrumButton);
		buttonPanel.add(hideSpectrumButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(addSpectraButton);
		buttonPanel.add(Box.createHorizontalGlue());

		buttonPanel.add(selectInvertB, "w 24!,h 24!");
		buttonPanel.add(selectNoneB, "w 24!,h 24!");
		buttonPanel.add(selectAllB, "w 24!,h 24!,wrap 2");
		add(buttonPanel);

		// Table Content
		QueryComposer<MEGANEColumnLookup> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(MEGANEColumnLookup.Map, Boolean.class, "Map", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Status, String.class, "Status", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.TimeWindow, Double.class, "UTC", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Latitude, Double.class, "Lat. (deg)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Longitude, Double.class, "Long. (deg)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Altitude, Double.class, "Alt. (km)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.NormalizedAlt, Double.class, "Norm. Alt.", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Signal, Double.class, "Signal Cont. (%)", null);

		tmpComposer.setEditor(MEGANEColumnLookup.Map, new BooleanCellEditor());
		tmpComposer.setRenderer(MEGANEColumnLookup.Map, new BooleanCellRenderer());

		tmpComposer.getItem(MEGANEColumnLookup.Status).defaultSize *= 2;
		tmpComposer.getItem(MEGANEColumnLookup.TimeWindow).defaultSize *= 5;

		MEGANEItemHandler meganeItemHandler = new MEGANEItemHandler(meganeCollection, tmpComposer);
		ItemProcessor<MEGANEFootprint> tmpIP = meganeCollection;
		meganeILP = new ItemListPanel<>(meganeItemHandler, tmpIP, true);
		meganeILP.setSortingEnabled(true);
		JTable spectrumTable = meganeILP.getTable();
		spectrumTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		spectrumTable.addMouseListener(new SpectrumTablePopupListener<>(spectrumCollection, boundaryCollection, spectrumPopupMenu, spectrumTable));

		return spectrumTable;
    }

    public JTable getResultList()
    {
        return resultList;
    }

    public JLabel getResultsLabel()
    {
        return resultsLabel;
    }

    public void setResultsLabel(JLabel resultsLabel)
    {
        this.resultsLabel = resultsLabel;
    }

	public ItemHandler<MEGANEFootprint> getMEGANETableHandler()
	{
		return meganeItemHandler;
	}

	/**
	 * @return the loadSpectrumButton
	 */
	public JButton getLoadSpectrumButton()
	{
		return loadSpectrumButton;
	}


	/**
	 * @return the hideSpectrumButton
	 */
	public JButton getHideSpectrumButton()
	{
		return hideSpectrumButton;
	}


	/**
	 * @return the showSpectrumButton
	 */
	public JButton getShowSpectrumButton()
	{
		return showSpectrumButton;
	}


	/**
	 * @return the saveSpectrumButton
	 */
	public JButton getSaveSpectrumButton()
	{
		return saveSpectrumButton;
	}

	/**
	 * @return the addSpectrumButton
	 */
	public JButton getAddSpectrumButton()
	{
		return addSpectraButton;
	}


}
