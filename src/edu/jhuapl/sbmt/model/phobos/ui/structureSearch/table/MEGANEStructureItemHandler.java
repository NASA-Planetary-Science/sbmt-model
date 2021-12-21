package edu.jhuapl.sbmt.model.phobos.ui.structureSearch.table;

import edu.jhuapl.saavtk.structure.Structure;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANEStructureCollection;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

public class MEGANEStructureItemHandler extends BasicItemHandler<Structure, MEGANEStructureColumnLookup>
{
	MEGANEStructureCollection structures;

	public MEGANEStructureItemHandler(MEGANEStructureCollection aManager, QueryComposer<MEGANEStructureColumnLookup> aComposer)
	{
		super(aComposer);

		this.structures = aManager;
	}

	@Override
	public Object getColumnValue(Structure structure, MEGANEStructureColumnLookup aEnum)
	{
		switch (aEnum)
		{
			case STRUCTURE_NAME:
				return structure.getName();
			case STRUCTURE_TYPE:
				return structure.getClass().getSimpleName();

			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(Structure structure, MEGANEStructureColumnLookup aEnum, Object aValue)
	{
		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}
}
