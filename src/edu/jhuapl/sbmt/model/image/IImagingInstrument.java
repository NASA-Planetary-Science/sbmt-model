package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.sbmt.client.SpectralMode;
import edu.jhuapl.sbmt.query.IQueryBase;

import crucible.crust.metadata.api.Metadata;

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
