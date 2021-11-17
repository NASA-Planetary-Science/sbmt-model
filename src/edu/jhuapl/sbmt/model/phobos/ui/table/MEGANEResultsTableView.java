package edu.jhuapl.sbmt.model.phobos.ui.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.gui.table.EphemerisTimeRenderer;
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

	public MEGANEResultsTableView(MEGANECollection collection)
	{
		this.meganeCollection = collection;
		init();
	}

	private JButton loadFootprintsListButton;
    private JButton removeFootprintButton;
//    private JButton showBoundariesButton;
    private JButton showFootprintButton;
    private JButton saveFootprintsListButton;
    private JButton saveSelectedFootprintsListButton;
//    private SpectrumPopupMenu<S> spectrumPopupMenu;
    protected JTable resultList;
    private JLabel resultsLabel;

    //for table
    private JLabel titleL;
    private JButton selectAllB, selectInvertB, selectNoneB;
    private MEGANECollection meganeCollection;
    private ItemListPanel<MEGANEFootprint> meganeILP;
    private ItemHandler<MEGANEFootprint> meganeItemHandler;

//    /**
//     * @wbp.parser.constructor
//     */
//    public SpectrumResultsTableView(SpectraCollection<S> spectrumCollection, SpectrumBoundaryCollection<S> boundaryCollection, SpectrumPopupMenu<S> spectrumPopupMenu)
//    {
//        this.spectrumPopupMenu = spectrumPopupMenu;
//        this.spectrumCollection = spectrumCollection;
//        this.boundaryCollection = boundaryCollection;
//        init();
//    }

    protected void init()
    {
        resultsLabel = new JLabel("0 Results");
        resultList = buildTable();
        removeFootprintButton = new JButton("Remove Footprint");
        showFootprintButton = new JButton("Show Footprint");
        loadFootprintsListButton = new JButton("Load...");
        saveFootprintsListButton = new JButton("Save...");
        saveSelectedFootprintsListButton = new JButton("Save Selected...");
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
        scrollPane.setPreferredSize(new java.awt.Dimension(150, 150));
        add(scrollPane);

        scrollPane.setViewportView(resultList);

        JPanel panel = new JPanel();
        add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JPanel panel_1 = new JPanel();
        add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        panel_1.add(showFootprintButton);

//        panel_1.add(showBoundariesButton);

        panel_1.add(removeFootprintButton);


        JPanel panel_2 = new JPanel();
        add(panel_2);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

        panel_2.add(loadFootprintsListButton);

        panel_2.add(saveFootprintsListButton);

        panel_2.add(saveSelectedFootprintsListButton);


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
		selectInvertB = GuiUtil.formButton(listener, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(listener, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(listener, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		titleL = new JLabel("Spectra: ---");
		buttonPanel.add(titleL, "growx,span,split");
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(selectInvertB, "w 24!,h 24!");
		buttonPanel.add(selectNoneB, "w 24!,h 24!");
		buttonPanel.add(selectAllB, "w 24!,h 24!,wrap 2");
		add(buttonPanel);

		// Table Content
		QueryComposer<MEGANEColumnLookup> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(MEGANEColumnLookup.Map, Boolean.class, "Map", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.TimeWindow, Double.class, "UTC", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Latitude, Double.class, "Latitude (deg)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Longitude, Double.class, "Longitude (deg)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.Altitude, Double.class, "Altitude (km)", null);
		tmpComposer.addAttribute(MEGANEColumnLookup.NormalizedAlt, Double.class, "Normalized Altitude", null);

		EphemerisTimeRenderer tmpTimeRenderer = new EphemerisTimeRenderer(false);
		tmpComposer.setEditor(MEGANEColumnLookup.Map, new BooleanCellEditor());
		tmpComposer.setRenderer(MEGANEColumnLookup.Map, new BooleanCellRenderer());


		tmpComposer.getItem(MEGANEColumnLookup.TimeWindow).defaultSize *= 7;
//		tmpComposer.getItem(MEGANEColumnLookup.Latitude).defaultSize *= 1.5;
//		tmpComposer.getItem(MEGANEColumnLookup.Longitude).defaultSize *= 1.5;
//		tmpComposer.getItem(MEGANEColumnLookup.Altitude).defaultSize *= 1.5;
//		tmpComposer.getItem(MEGANEColumnLookup.NormalizedAlt).defaultSize *= 1.5;

		MEGANEItemHandler meganeItemHandler = new MEGANEItemHandler(meganeCollection, tmpComposer);
		ItemProcessor<MEGANEFootprint> tmpIP = meganeCollection;
		meganeILP = new ItemListPanel<>(meganeItemHandler, tmpIP, true);
		meganeILP.setSortingEnabled(true);
//		configureColumnWidths();
		JTable spectrumTable = meganeILP.getTable();
		spectrumTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		spectrumTable.addMouseListener(new SpectrumTablePopupListener<>(spectrumCollection, boundaryCollection, spectrumPopupMenu, spectrumTable));

//		spectrumCollection.addListener(new ItemEventListener()
//		{
//
//			@Override
//			public void handleItemEvent(Object aSource, ItemEventType aEventType)
//			{
//				if (aEventType == ItemEventType.ItemsMutated)
//				{
//					spectrumTableHandler = new SpectrumItemHandler<S>(spectrumCollection, boundaryCollection, tmpComposer);
//					ItemProcessor<S> tmpIP = spectrumCollection;
////					spectrumILP = new ItemListPanel<>(spectrumTableHandler, tmpIP, true);
//				}
//
//			}
//		});

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

    public JButton getLoadFootprintListButton()
    {
        return loadFootprintsListButton;
    }

//    public JButton getShowBoundariesButton()
//    {
//        return showBoundariesButton;
//    }

    public JButton getShowFootprintButton()
    {
        return showFootprintButton;
    }

    public JButton getRemoveFootprintButton()
    {
        return removeFootprintButton;
    }

    public JButton getSaveFootprintsListButton()
    {
        return saveFootprintsListButton;
    }

    public JButton getSaveSelectedFootprintsListButton()
    {
        return saveSelectedFootprintsListButton;
    }

    public void setResultsLabel(JLabel resultsLabel)
    {
        this.resultsLabel = resultsLabel;
    }

//    public SpectrumPopupMenu<S> getSpectrumPopupMenu()
//    {
//        return spectrumPopupMenu;
//    }
//
//    public void setSpectrumPopupMenu(SpectrumPopupMenu<S> spectrumPopupMenu)
//    {
//        this.spectrumPopupMenu = spectrumPopupMenu;
//    }

	public ItemHandler<MEGANEFootprint> getMEGANETableHandler()
	{
		return meganeItemHandler;
	}

//	private void configureColumnWidths()
//	{
////		int maxPts = 99;
////		String sourceStr = "Data Source";
////		for (BasicSpectrum spec : spectrumCollection.getAllItems())
////		{
////			maxPts = Math.max(maxPts, spec.getNumberOfPoints());
////			String tmpStr = SpectrumItemHandler.getSourceFileString(aTrack);
////			if (tmpStr.length() > sourceStr.length())
////				sourceStr = tmpStr;
////		}
//
//		JTable tmpTable = spectrumILP.getTable();
////		String trackStr = "" + tmpTable.getRowCount();
////		String pointStr = "" + maxPts;
//		String dateTimeStr = "9999-88-88T00:00:00.000000";
//		int minW = 30;
//
////		ColorProvider blackCP = new ConstColorProvider(Color.BLACK);
//		Object[] nomArr = { true, true, true, true, minW, dateTimeStr, dateTimeStr };
//		for (int aCol = 0; aCol < nomArr.length; aCol++)
//		{
//			TableCellRenderer tmpRenderer = tmpTable.getCellRenderer(0, aCol);
//			Component tmpComp = tmpRenderer.getTableCellRendererComponent(tmpTable, nomArr[aCol], false, false, 0, aCol);
//			int tmpW = Math.max(minW, tmpComp.getPreferredSize().width + 1);
//			tmpTable.getColumnModel().getColumn(aCol).setPreferredWidth(tmpW + 10);
//		}
//	}

}
