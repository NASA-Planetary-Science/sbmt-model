package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.saavtk.model.FileType;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.StorableAsMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

public class CustomSpectrumKey implements CustomSpectrumKeyInterface, StorableAsMetadata<CustomSpectrumKey>
{
	public final String name;

    public final FileType fileType;

    public final ISpectralInstrument instrument;

    public ISpectraType spectrumType;

    public String spectrumFilename;

    public String pointingFilename;

    final static Key<String> nameKey = Key.of("name");
    final static Key<String> pointingFileTypeKey = Key.of("pointingFileType");
    final static Key<String> spectrumFileNameKey = Key.of("spectrumfilename");
    final static Key<String> spectraTypeKey = Key.of("spectratype");
    final static Key<String> pointingFilenameKey = Key.of("pointingFilename");
    final static Key<String> instrumentKey = Key.of("instrument");

    private static final Key<CustomSpectrumKey> CUSTOM_SPECTRUM_KEY = Key.of("customSpectrum");


	public CustomSpectrumKey(String name, FileType fileType, ISpectralInstrument instrument, ISpectraType spectrumType, String spectrumFilename, String pointingFilename)
	{
		this.name = name;
		this.fileType = fileType;
		this.instrument = instrument;
		this.spectrumType = spectrumType;
		this.spectrumFilename = spectrumFilename;
		this.pointingFilename = pointingFilename;
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
	public Key<CustomSpectrumKey> getKey()
	{
		return CUSTOM_SPECTRUM_KEY;
	}

	@Override
	public Metadata store()
	{
		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
        result.put(nameKey, name);
        result.put(spectrumFileNameKey, spectrumFilename);
        result.put(instrumentKey, instrument.toString());
        result.put(spectraTypeKey, spectrumType.toString());
        result.put(pointingFileTypeKey, fileType.toString());
        result.put(pointingFilenameKey, pointingFilename);
        return result;
	}

	public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(CUSTOM_SPECTRUM_KEY, (metadata) -> {

	        String name = metadata.get(nameKey);
	        String spectrumFilename = metadata.get(spectrumFileNameKey);
	        ISpectraType spectrumType = SpectraType.valueOf(metadata.get(spectraTypeKey));
	        FileType fileType = FileType.valueOf(metadata.get(pointingFileTypeKey));
	        ISpectralInstrument instrument = SpectrumInstrumentFactory.getInstrumentForName(name);
	        String pointingFilename = metadata.get(pointingFilenameKey);
	        CustomSpectrumKey result = new CustomSpectrumKey(name, fileType, instrument, spectrumType, spectrumFilename, pointingFilename);

			return result;
		});
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

	@Override
	public String getPointingFilename()
	{
		return pointingFilename;
	}

	public void setPointingFilename(String pointingFilename)
	{
		this.pointingFilename = pointingFilename;
	}

}
