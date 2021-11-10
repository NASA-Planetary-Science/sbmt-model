package edu.jhuapl.sbmt.model.phobos.ui.table;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;

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
		//TODO: Switch to using an index so the get all items doesn't take so long to look up
		switch (aEnum)
		{
			case Map:
				return meganeCollection.isFootprintMapped(footprint);
			case TimeWindow:
				DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
				fmt.withZone(DateTimeZone.UTC);
				return fmt.print(footprint.getDateTime());
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