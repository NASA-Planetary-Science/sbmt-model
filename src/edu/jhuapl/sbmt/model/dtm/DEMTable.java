package edu.jhuapl.sbmt.model.dtm;

import java.util.List;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk2.event.Event;
import edu.jhuapl.saavtk2.event.EventListener;
import edu.jhuapl.saavtk2.table.TableColumn;
import edu.jhuapl.saavtk2.table.TableEntryChangedEvent;
import edu.jhuapl.saavtk2.table.TableSwingWrapper;
import edu.jhuapl.saavtk2.table.search.MapAndShowTable;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class DEMTable extends MapAndShowTable {

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

	public DEMTable() {
		super(Lists.newArrayList(new TableColumn[]{
				MapAndShowTable.Columns.Map,
				MapAndShowTable.Columns.Show,
				DEMTable.Columns.Bndr,
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
		return key.displayName;
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

    public void signalDemBoundaryShown(int row)
    {
        fire(new ShowDEMBoundaryEvent(this, availableKeys.get(row)));
    }

    public void signalDemBoundaryHidden(int row)
    {
        fire(new HideDEMBoundaryEvent(this, availableKeys.get(row)));
    }

    protected class MapAndShowBehavior implements EventListener
    {

        TableSwingWrapper swingTableWrapper;
        public MapAndShowBehavior(TableSwingWrapper swingTableWrapper)
        {
            this.swingTableWrapper=swingTableWrapper;
        }

        @Override
        public void handle(Event event)
        {
            if (event instanceof TableEntryChangedEvent) // this event handler adds the behavior where show checkbox should be marked "false" when map checkbox is marked "false"
            {
                TableEntryChangedEvent eventCast=(TableEntryChangedEvent)event;
                if (eventCast.getTableColumn()==MapAndShowTable.Columns.Map)
                {
                    int row=eventCast.getRow();
                    Boolean map=(Boolean)event.getValue();
                    if (map)  // here we fire events that are meant to be listened for externally
                    {
                        signalDemCreated(row);
                    }
                    else
                    {
                        signalDemDeleted(row);
                    }
                    //
                    setItemMapped(row, map);    // this fires events needed internally for the table and its swing counterpart to synchronize
                    swingTableWrapper.setCellEditable(row, MapAndShowTable.Columns.Show, map); // if mapped then the show checkbox is editable
                    if (map)    // update the swing gui table model before re-enabling event listing
                    {
                        swingTableWrapper.getComponent().setValueAt(true, row, getColumnNumber(MapAndShowTable.Columns.Show));
                    }
                    else
                    {
                        swingTableWrapper.getComponent().setValueAt(false, row, getColumnNumber(MapAndShowTable.Columns.Show));
                    }
                }
                else if (eventCast.getTableColumn()==MapAndShowTable.Columns.Show)
                {
                    int row=eventCast.getRow();
                    Boolean show=(Boolean)event.getValue();
                    if (show)  // here we fire events that are meant to be listened for externally
                    {
                        signalDemShown(row);
                    }
                    else
                    {
                        signalDemHidden(row);
                    }
                }
                else if (eventCast.getTableColumn()==DEMTable.Columns.Bndr)
                {
                    int row=eventCast.getRow();
                    Boolean show=(Boolean)event.getValue();
                    if (show)  // here we fire events that are meant to be listened for externally
                    {
                        signalDemBoundaryShown(row);
                    }
                    else
                    {
                        signalDemBoundaryHidden(row);
                    }

                }

            }
        }
    }

    public static TableSwingWrapper createSwingWrapper(final DEMTable table)
	{
		final TableSwingWrapper swingTableWrapper=new TableSwingWrapper(table);
		swingTableWrapper.setColumnEditable(MapAndShowTable.Columns.Show, false);
		swingTableWrapper.setColumnEditable(DEMTable.Columns.Bndr, true);
		table.addListener(table.new MapAndShowBehavior(swingTableWrapper));
		return swingTableWrapper;
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
