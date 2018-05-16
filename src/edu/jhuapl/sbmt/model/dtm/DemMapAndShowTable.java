package edu.jhuapl.sbmt.model.dtm;

import java.util.List;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk2.event.Event;
import edu.jhuapl.saavtk2.event.EventListener;
import edu.jhuapl.saavtk2.table.TableColumn;
import edu.jhuapl.saavtk2.table.TableEntryChangedEvent;
import edu.jhuapl.saavtk2.table.TableSwingWrapper;
import edu.jhuapl.saavtk2.table.search.MapAndShowTable;
import edu.jhuapl.sbmt.model.dem.DEMCollection;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class DemMapAndShowTable extends MapAndShowTable {

	protected static enum Columns implements TableColumn
	{
		Bndr(Boolean.class);

		Class<?> type;

		private Columns(Class<?> type) {
			this.type=type;
		}

		@Override
		public String getName() {
			return name();
		}

		@Override
		public Class<?> getType() {
			return type;
		}

	}



	final List<DEMKey> availableKeys=Lists.newArrayList();

	public DemMapAndShowTable(DEMCollection collection) {
		super(Lists.newArrayList(new TableColumn[]{
				MapAndShowTable.Columns.Map,
				MapAndShowTable.Columns.Show,
				DemMapAndShowTable.Columns.Bndr,
				MapAndShowTable.Columns.Desc
		}));
	}

	@Deprecated
	@Override
	public void appendRow(Object[] data) {
		throw new Error("Use appendRow(DtmMetaData) method instead");
	}

	public void appendRow(DEMKey key)
	{
		this.availableKeys.add(key);
		super.appendRow(new Object[]{false,false,false, generateDescription(key)});
	}

	protected String generateDescription(DEMKey key)
	{
		return key.toString();
	}

	@Override
	public void removeRow(int r) {
		availableKeys.remove(r);
		super.removeRow(r);
	}

	public void signalDemCreated(int row)
	{
	    fire(new CreateDEMEvent(this, availableKeys.get(row)));
	}

    public void signalDemDeleted(int row)
    {
        fire(new DeleteDEMEvent(this, availableKeys.get(row)));
    }


    public void signalDemShown(int row)
    {
        fire(new ShowDEMEvent(this, availableKeys.get(row)));
    }

    public void signalDemHidden(int row)
    {
        fire(new HideDEMEvent(this, availableKeys.get(row)));
    }

    public static TableSwingWrapper createSwingWrapper(final DemMapAndShowTable table)
	{
		final TableSwingWrapper swingTable=new TableSwingWrapper(table);
		swingTable.setColumnEditable(MapAndShowTable.Columns.Show, false);
		swingTable.setColumnEditable(DemMapAndShowTable.Columns.Bndr, false);
		table.addListener(new EventListener() {

			@Override
			public void handle(Event event) {
				if (event instanceof TableEntryChangedEvent) // this event handler adds the behavior where show checkbox should be marked "false" when map checkbox is marked "false"
				{
					TableEntryChangedEvent eventCast=(TableEntryChangedEvent)event;
					if (eventCast.getTableColumn()==MapAndShowTable.Columns.Map)
					{
						int row=eventCast.getRow();
						Boolean map=(Boolean)event.getValue();
						if (map)  // here we fire events that are meant to be listened for externally
						{
						    table.signalDemCreated(row);
						}
						else
						{
							table.signalDemDeleted(row);
						}
						//
						table.setItemMapped(row, map);    // this fires events needed internally for the table and its swing counterpart to synchronize
						swingTable.setCellEditable(row, MapAndShowTable.Columns.Show, map);	// if mapped then the show checkbox is editable
						swingTable.setCellEditable(row, DemMapAndShowTable.Columns.Bndr, map);
						if (!map)	// update the swing gui table model before re-enabling event listing
						{
							swingTable.getComponent().setValueAt(false, row, table.getColumnNumber(MapAndShowTable.Columns.Show));
							swingTable.getComponent().setValueAt(false, row, table.getColumnNumber(DemMapAndShowTable.Columns.Bndr));
						}
					}
					if (eventCast.getTableColumn()==MapAndShowTable.Columns.Show)
					{
						int row=eventCast.getRow();
						Boolean show=(Boolean)event.getValue();
						if (show)  // here we fire events that are meant to be listened for externally
						{
							table.signalDemShown(row);
						}
						else
						{
							table.signalDemHidden(row);
						}
					}

				}
			}
		});
		return swingTable;
	}

/*	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JFrame frame=new BasicFrame();
				DEMFactory factory=new DummyDEMFactory();
				DtmMapAndShowTable table=new DtmMapAndShowTable(factory);
				TableSwingWrapper swingTable=DtmMapAndShowTable.createSwingWrapper(table);
				JScrollPane scrollPane=new JScrollPane(swingTable.getComponent());
				frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

				for (int i=0; i<100; i++)
				{
					//table.appendRow(new DummyDtmMetaData());
				}

				for (int i=0; i<75; i++)
				{
					int idx=(int)((double)table.getNumberOfRows()*Math.random());
					table.removeRow(idx);
				}

			}
		});

	}*/

}
