package edu.jhuapl.sbmt.model.spectrum;

import crucible.crust.metadata.api.Metadata;
import edu.jhuapl.saavtk.model.FileType;

public interface SpectrumKeyInterface
{

	String getName();

	FileType getFileType();

	ISpectralInstrument getInstrument();

	ISpectraType getSpectrumType();
	
	public Metadata store();

}