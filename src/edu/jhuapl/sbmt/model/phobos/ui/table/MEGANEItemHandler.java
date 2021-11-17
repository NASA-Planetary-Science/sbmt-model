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
//				DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
//				fmt.withZone(DateTimeZone.UTC);
//				return fmt.print(footprint.getDateTime());
			case Latitude:
				return formatter.format(Math.toDegrees(footprint.getLatDegrees()));
			case Longitude:
				return formatter.format(Math.toDegrees(footprint.getLonDegrees()));
			case Altitude:
				return formatter.format(footprint.getAltKm());
			case NormalizedAlt:
				return formatter.format(footprint.getNormalizedAlt());
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
//			if (!spectrumCollection.isSpectrumMapped(spec))
//				try
//				{
//					spectrumCollection.addSpectrum(spec, spec.isCustomSpectra);
//				}
//				catch (SpectrumIOException e)
//				{
//					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(null),
//		                     e.getCause().getMessage(),
//		                     "Error",
//		                     JOptionPane.ERROR_MESSAGE);
//				}
//			else
//			{
//				boundaryCollection.removeBoundary(spec);
//				spectrumCollection.removeSpectrum(spec);
//			}
		}
		else
			throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}
}