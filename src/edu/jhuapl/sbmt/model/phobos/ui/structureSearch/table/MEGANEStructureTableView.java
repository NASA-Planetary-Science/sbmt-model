package edu.jhuapl.sbmt.model.phobos.ui.structureSearch.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.saavtk.structure.Structure;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANEStructureCollection;

import glum.gui.GuiUtil;
import glum.gui.panel.itemList.ItemHandler;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.ItemProcessor;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.item.ItemManagerUtil;

public class MEGANEStructureTableView extends JPanel
{
	protected JTable resultList;

	// for table
	private JButton selectAllB, selectInvertB, selectNoneB;
	MEGANEStructureCollection structures;
	private ItemListPanel<Structure> meganeILP;
	private ItemHandler<Structure> meganeItemHandler;

	public MEGANEStructureTableView(MEGANEStructureCollection structures)
	{
		this.structures = structures;
		init();
	}

	protected void init()
	{
		resultList = buildTable();
	}

	public void setup()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		setBorder(new TitledBorder(null, "Search by Structure", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel panel_4 = new JPanel();
		add(panel_4);
		panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));

		Component horizontalGlue = Box.createHorizontalGlue();
		panel_4.add(horizontalGlue);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new java.awt.Dimension(150, 750));
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

				List<Structure> tmpL = structures.getSelectedItems().asList();
				if (source == selectAllB)
					ItemManagerUtil.selectAll(structures);
				else if (source == selectNoneB)
					ItemManagerUtil.selectNone(structures);
				else if (source == selectInvertB)
				{
					ItemManagerUtil.selectInvert(structures);
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

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(selectInvertB, "w 24!,h 24!");
		buttonPanel.add(selectNoneB, "w 24!,h 24!");
		buttonPanel.add(selectAllB, "w 24!,h 24!,wrap 2");
		add(buttonPanel);

		// Table Content
		QueryComposer<MEGANEStructureColumnLookup> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(MEGANEStructureColumnLookup.STRUCTURE_NAME, String.class, "Structure Name", null);
		tmpComposer.addAttribute(MEGANEStructureColumnLookup.STRUCTURE_TYPE, String.class, "Structure Type", null);

		tmpComposer.getItem(MEGANEStructureColumnLookup.STRUCTURE_NAME).defaultSize *= 2;

		MEGANEStructureItemHandler meganeItemHandler = new MEGANEStructureItemHandler(structures, tmpComposer);
		ItemProcessor<Structure> tmpIP = structures;
		meganeILP = new ItemListPanel<>(meganeItemHandler, tmpIP, true);
		meganeILP.setSortingEnabled(true);
		JTable spectrumTable = meganeILP.getTable();
		spectrumTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// spectrumTable.addMouseListener(new
		// SpectrumTablePopupListener<>(spectrumCollection, boundaryCollection,
		// spectrumPopupMenu, spectrumTable));

		return spectrumTable;
	}

	public JTable getResultList()
	{
		return resultList;
	}

	public void setStructures(MEGANEStructureCollection collection)
	{
		this.structures = collection;
	}

//	public ItemHandler<MEGANEFootprint> getMEGANETableHandler()
//	{
//		return meganeItemHandler;
//	}
}
