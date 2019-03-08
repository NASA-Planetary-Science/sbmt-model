package edu.jhuapl.sbmt.model.spectrum;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.ProvidableFromMetadata;
import crucible.crust.metadata.impl.InstanceGetter;

public interface CustomSpectrumKeyInterface extends SpectrumKeyInterface
{
	public void setSpectrumFilename(String spectrumfilename);
	
	public String getSpectrumFilename();
	
	public void setSpectraType(ISpectraType type);
	
	static CustomSpectrumKeyInterface retrieve(Metadata objectMetadata)
	{
		final Key<String> key = Key.of("customspectrumtype");
		Key<CustomSpectrumKeyInterface> CUSTOM_SPECTRUM_KEY = Key.of("customSpectrum");
		ProvidableFromMetadata<CustomSpectrumKeyInterface> metadata = InstanceGetter.defaultInstanceGetter().of(CUSTOM_SPECTRUM_KEY);
		return metadata.provide(objectMetadata);
		
	}
}
