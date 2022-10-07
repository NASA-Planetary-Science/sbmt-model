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
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANEFootprint;

import glum.gui.GuiUtil;
import glum.gui.misc.BooleanCellEditor;
import glum.gui.misc.BooleanCellRenderer;
import glum.gui.panel.itemList.ItemHandler;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.ItemProcessor;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.item.ItemManagerUtil;

public class CumulativeMEGANEFootprintTableView extends JPanel
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

	/**
	 * JButton to save spectra to file
	 */
	private JButton saveSpectrumButton;

    protected JTable resultList;
    private JLabel resultsLabel;

    //for table
    private JButton selectAllB, selectInvertB, selectNoneB;
    private CumulativeMEGANECollection meganeCollection;
    private ItemListPanel<CumulativeMEGANEFootprint> meganeILP;
    private ItemHandler<CumulativeMEGANEFootprint> meganeItemHandler;


	public CumulativeMEGANEFootprintTableView(CumulativeMEGANECollection collection)
	{
		this.meganeCollection = collection;
		collection.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				resultsLabel.setText(collection.getAllItems().size() + " Results");
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
        setBorder(new TitledBorder(null, "Cumulative Footprints", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

				List<CumulativeMEGANEFootprint> tmpL = meganeCollection.getSelectedItems().asList();
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

		selectInvertB = GuiUtil.formButton(listener, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(listener, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(listener, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(loadSpectrumButton);
		buttonPanel.add(saveSpectrumButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(showSpectrumButton);
		buttonPanel.add(hideSpectrumButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(selectInvertB, "w 24!,h 24!");
		buttonPanel.add(selectNoneB, "w 24!,h 24!");
		buttonPanel.add(selectAllB, "w 24!,h 24!,wrap 2");
		add(buttonPanel);

		// Table Content
		QueryComposer<CumulativeMEGANEFootprintColumnLookup> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(CumulativeMEGANEFootprintColumnLookup.Map, Boolean.class, "Map", null);
		tmpComposer.addAttribute(CumulativeMEGANEFootprintColumnLookup.Status, String.class, "Status", null);
		tmpComposer.addAttribute(CumulativeMEGANEFootprintColumnLookup.OriginalTimes, String.class, "Original Footprint Times", null);
//		tmpComposer.addAttribute(MEGANEColumnLookup.TimeWindow, Double.class, "UTC", null);
//		tmpComposer.addAttribute(MEGANEColumnLookup.Latitude, Double.class, "Latitude (deg)", null);
//		tmpComposer.addAttribute(MEGANEColumnLookup.Longitude, Double.class, "Longitude (deg)", null);
//		tmpComposer.addAttribute(MEGANEColumnLookup.Altitude, Double.class, "Altitude (km)", null);
//		tmpComposer.addAttribute(MEGANEColumnLookup.NormalizedAlt, Double.class, "Normalized Altitude", null);

		tmpComposer.setEditor(CumulativeMEGANEFootprintColumnLookup.Map, new BooleanCellEditor());
		tmpComposer.setRenderer(CumulativeMEGANEFootprintColumnLookup.Map, new BooleanCellRenderer());

		tmpComposer.getItem(CumulativeMEGANEFootprintColumnLookup.Status).defaultSize *= 3;
		tmpComposer.getItem(CumulativeMEGANEFootprintColumnLookup.OriginalTimes).defaultSize *= 4;

		CumulativeMEGANEFootprintItemHandler meganeItemHandler = new CumulativeMEGANEFootprintItemHandler(meganeCollection, tmpComposer);
		ItemProcessor<CumulativeMEGANEFootprint> tmpIP = meganeCollection;
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

	public ItemHandler<CumulativeMEGANEFootprint> getMEGANETableHandler()
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
}
