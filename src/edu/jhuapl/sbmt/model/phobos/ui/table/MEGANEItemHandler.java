package edu.jhuapl.sbmt.model.phobos.ui.table;

import java.text.DecimalFormat;

import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

public class MEGANEItemHandler extends BasicItemHandler<MEGANEFootprint, MEGANEColumnLookup>
{
	private final MEGANECollection meganeCollection;

	public MEGANEItemHandler(MEGANECollection aManager, QueryComposer<MEGANEColumnLookup> aComposer)
	{
		super(aComposer);

		meganeCollection = aManager;
	}

	@Override
	public Object getColumnValue(MEGANEFootprint footprint, MEGANEColumnLookup aEnum)
	{
		DecimalFormat formatter = new DecimalFormat("##.####");
		switch (aEnum)
		{
			case Map:
				return meganeCollection.isFootprintMapped(footprint);
			case TimeWindow:
				return TimeUtil.et2str(footprint.getDateTime());
			case Latitude:
				return formatter.format(Math.toDegrees(footprint.getLatRadians()));
			case Longitude:
				return formatter.format(Math.toDegrees(footprint.getLonRadians()));
			case Altitude:
				return formatter.format(footprint.getAltKm());
			case NormalizedAlt:
				return formatter.format(footprint.getNormalizedAlt());
			case Status:
				return meganeCollection.getStatus(footprint);
			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(MEGANEFootprint footprint, MEGANEColumnLookup aEnum, Object aValue)
	{
		if (aEnum == MEGANEColumnLookup.Map)
		{
			meganeCollection.setFootprintMapped(footprint, (Boolean)aValue);
		}
		else
			throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}
}