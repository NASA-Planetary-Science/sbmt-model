package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.metadata.Key;
import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.metadata.MetadataManager;
import edu.jhuapl.saavtk.metadata.SettableMetadata;
import edu.jhuapl.saavtk.metadata.Version;
import edu.jhuapl.sbmt.client.SpectralMode;
import edu.jhuapl.sbmt.query.IQueryBase;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.query.database.GenericPhpQuery;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;

public class ImagingInstrument implements MetadataManager, IImagingInstrument
{
    public SpectralMode spectralMode;
    public QueryBase searchQuery;
    public ImageSource[] searchImageSources;
    public ImageType type;
    public Instrument instrumentName;
    public double rotation;
    public String flip;

    public ImagingInstrument()
    {
        this(SpectralMode.MONO, null, null, null, null, 0.0, "None");
    }
    public ImagingInstrument(double rotation, String flip)
    {
        this(SpectralMode.MONO, null, ImageType.GENERIC_IMAGE, null, null, rotation, flip);
    }

//    public ImagingInstrument(ImageType type, Instrument instrumentName)
//    {
//        this(SpectralMode.MONO, null, type, null, instrumentName, 0.0, "None");
//    }

//    public ImagingInstrument(SpectralMode spectralMode)
//    {
//        this(spectralMode, null, null, null, null, 0.0, "None");
//    }

    public ImagingInstrument(SpectralMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName)
    {
        this(spectralMode, searchQuery, type, searchImageSources, instrumentName, 0.0, "None");
    }

    public ImagingInstrument(SpectralMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName, double rotation, String flip)
    {
        this.spectralMode = spectralMode;
        this.searchQuery = searchQuery;
        this.type = type;
        this.searchImageSources = searchImageSources;
        this.instrumentName = instrumentName;
        this.rotation = rotation;
        this.flip = flip;
    }

//    c.imagingInstruments = new ImagingInstrument[] {
//            new ImagingInstrument(
//                    SpectralMode.MONO,
//                    new GenericPhpQuery("/GASKELL/EROS/MSI", "EROS", "/GASKELL/EROS/MSI/gallery"),
//                    ImageType.MSI_IMAGE,
//                    new ImageSource[]{ImageSource.GASKELL_UPDATED, ImageSource.SPICE},
//                    Instrument.MSI
//                    )
//    };

    public ImagingInstrument clone()
    {
        return new ImagingInstrument(spectralMode, searchQuery.clone(), type, searchImageSources.clone(), instrumentName, rotation, flip);
    }

    public ImageType getType()
	{
		return type;
	}

	public ImageSource[] getSearchImageSources()
	{
		return searchImageSources;
	}

	public SpectralMode getSpectralMode()
	{
		return spectralMode;
	}

    Key<String> spectralModeKey = Key.of("spectralMode");
    Key<String> queryType = Key.of("queryType");
    Key<Metadata> queryKey = Key.of("query");
//    Key<String> rootPathKey = Key.of("rootPath");
//    Key<String> tablePrefixKey = Key.of("tablePrefix");
//    Key<String> galleryPrefixKey = Key.of("galleryPrefix");
    Key<String> imageTypeKey = Key.of("imageType");
    Key<String[]> imageSourcesKey = Key.of("imageSources");
    Key<String> instrumentKey = Key.of("instrument");

    @Override
    public void retrieve(Metadata source)
    {
        spectralMode = SpectralMode.valueOf(read(spectralModeKey, source));
        String searchType = read(queryType, source);
        Metadata queryMetadata = read(queryKey, source);
        if (searchType.equals(FixedListQuery.class.getSimpleName()))
        {
            searchQuery = new FixedListQuery();

        }
        else    //it's a GenericPHPQuery
        {
            searchQuery = new GenericPhpQuery();
        }
        searchQuery.retrieve(queryMetadata);

        type = ImageType.valueOf(read(imageTypeKey, source));
        String[] imageSources = read(imageSourcesKey, source);
        searchImageSources = new ImageSource[imageSources.length];
        int i=0;
        for (String src : imageSources)
        {
            searchImageSources[i++] = ImageSource.valueOf(src);
        }
        instrumentName = Instrument.valueOf(read(instrumentKey, source));
    }

    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        writeEnum(spectralModeKey, spectralMode, configMetadata);
        write(queryType, searchQuery.getClass().getSimpleName(), configMetadata);
        write(queryKey, searchQuery.store(), configMetadata);
        writeEnum(imageTypeKey, type, configMetadata);
        writeEnums(imageSourcesKey, searchImageSources, configMetadata);
        writeEnum(instrumentKey, instrumentName, configMetadata);
        return configMetadata;
    }


    private <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
        }
    }

    private <T> void writeEnum(Key<String> key, Enum value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value.name());
        }
    }

    private <T> void writeEnums(Key<String[]> key, Enum[] values, SettableMetadata configMetadata)
    {
        if (values != null)
        {
            String[] names = new String[values.length];
            int i=0;
            for (Enum val : values)
            {
                names[i++] = val.name();
            }
            configMetadata.put(key, names);
        }
    }

    private <T> T read(Key<T> key, Metadata configMetadata)
    {
        T value = configMetadata.get(key);
        if (value != null)
            return value;
        return null;
    }

	@Override
	public String getFlip()
	{
		return flip;
	}

	@Override
	public double getRotation()
	{
		return rotation;
	}

	public IQueryBase getSearchQuery()
	{
		return searchQuery;
	}

	public Instrument getInstrumentName()
	{
		return instrumentName;
	}
}

