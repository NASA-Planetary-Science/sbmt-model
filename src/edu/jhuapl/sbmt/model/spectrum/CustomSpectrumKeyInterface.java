package edu.jhuapl.sbmt.model.spectrum;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.ProvidesGenericObjectFromMetadata;
import crucible.crust.metadata.impl.InstanceGetter;

public interface CustomSpectrumKeyInterface extends SpectrumKeyInterface
{
	public void setSpectrumFilename(String spectrumfilename);

	public String getSpectrumFilename();

	public String getPointingFilename();

	public void setPointingFilename(String pointingFilename);

	public void setSpectraType(ISpectraType type);

	static CustomSpectrumKeyInterface retrieve(Metadata objectMetadata)
	{
		final Key<String> key = Key.of("customspectrumtype");
		Key<CustomSpectrumKeyInterface> CUSTOM_SPECTRUM_KEY = Key.of("customSpectrum");
		ProvidesGenericObjectFromMetadata<CustomSpectrumKeyInterface> metadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(CUSTOM_SPECTRUM_KEY);
		return metadata.provide(objectMetadata);

	}
}
