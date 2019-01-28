package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.sbmt.client.SpectralMode;
import edu.jhuapl.sbmt.query.IQueryBase;

public interface IImagingInstrument
{
	public Metadata store();

	public ImageType getType();

	public String getFlip();

	public double getRotation();

	public IQueryBase getSearchQuery();

	public ImageSource[] getSearchImageSources();

	public SpectralMode getSpectralMode();

	public Instrument getInstrumentName();
}
