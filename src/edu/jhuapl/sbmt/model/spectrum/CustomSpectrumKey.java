package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.saavtk.model.FileType;

import crucible.crust.metadata.api.Metadata;

public class CustomSpectrumKey implements CustomSpectrumKeyInterface
{
	public final String name;

    public final FileType fileType;

    public final ISpectralInstrument instrument;

    public ISpectraType spectrumType;
    
    public String spectrumFilename;

	public CustomSpectrumKey(String name, FileType fileType, ISpectralInstrument instrument, ISpectraType spectrumType, String spectrumFilename)
	{
		this.name = name;
		this.fileType = fileType;
		this.instrument = instrument;
		this.spectrumType = spectrumType;
		this.spectrumFilename = spectrumFilename;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public FileType getFileType()
	{
		return fileType;
	}

	@Override
	public ISpectralInstrument getInstrument()
	{
		return instrument;
	}

	@Override
	public ISpectraType getSpectrumType()
	{
		return spectrumType;
	}

	@Override
	public Metadata store()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSpectrumFilename(String spectrumFilename)
	{
		this.spectrumFilename = spectrumFilename;
	}

	@Override
	public String getSpectrumFilename()
	{
		return spectrumFilename;
	}

	@Override
	public void setSpectraType(ISpectraType type)
	{
		this.spectrumType = type;
	}

}
