package edu.jhuapl.sbmt.model.phobos.ui.table;

import java.text.DecimalFormat;

import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANEFootprint;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

public class CumulativeMEGANEFootprintItemHandler extends BasicItemHandler<CumulativeMEGANEFootprint, CumulativeMEGANEFootprintColumnLookup>
{
	private final CumulativeMEGANECollection meganeCollection;

	public CumulativeMEGANEFootprintItemHandler(CumulativeMEGANECollection aManager, QueryComposer<CumulativeMEGANEFootprintColumnLookup> aComposer)
	{
		super(aComposer);

		meganeCollection = aManager;
	}

	@Override
	public Object getColumnValue(CumulativeMEGANEFootprint footprint, CumulativeMEGANEFootprintColumnLookup aEnum)
	{
		DecimalFormat formatter = new DecimalFormat("##.####");
		switch (aEnum)
		{
			case Map:
				return meganeCollection.isFootprintMapped(footprint);
//			case TimeWindow:
//				return TimeUtil.et2str(footprint.getDateTime());
//			case Latitude:
//				return formatter.format(Math.toDegrees(footprint.getLatDegrees()));
//			case Longitude:
//				return formatter.format(Math.toDegrees(footprint.getLonDegrees()));
//			case Altitude:
//				return formatter.format(footprint.getAltKm());
//			case NormalizedAlt:
//				return formatter.format(footprint.getNormalizedAlt());
			case Status:
				return meganeCollection.getStatus(footprint);
			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(CumulativeMEGANEFootprint footprint, CumulativeMEGANEFootprintColumnLookup aEnum, Object aValue)
	{
		if (aEnum == CumulativeMEGANEFootprintColumnLookup.Map)
		{
			meganeCollection.setFootprintMapped(footprint, (Boolean)aValue);
		}
		else
			throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}
}