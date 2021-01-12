package edu.jhuapl.sbmt.model.image;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.jhuapl.saavtk.util.FillDetector;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.query.IQueryBase;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.query.database.GenericPhpQuery;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public class ImagingInstrument implements MetadataManager, IImagingInstrument
{
    public SpectralImageMode spectralMode;
    public QueryBase searchQuery;
    public ImageSource[] searchImageSources;
    private ImageType type;
    public Instrument instrumentName;
    private double rotation;
    private String flip;
    private Set<Float> fillValues;


    public ImagingInstrument()
    {
        this(SpectralImageMode.MONO, null, null, null, null, 0.0, "None", null);
    }
    public ImagingInstrument(double rotation, String flip)
    {
        this(SpectralImageMode.MONO, null, ImageType.GENERIC_IMAGE, null, null, rotation, flip, null);
    }

//    public ImagingInstrument(ImageType type, Instrument instrumentName)
//    {
//        this(SpectralMode.MONO, null, type, null, instrumentName, 0.0, "None");
//    }

//    public ImagingInstrument(SpectralMode spectralMode)
//    {
//        this(spectralMode, null, null, null, null, 0.0, "None");
//    }

    public ImagingInstrument(SpectralImageMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName)
    {
        this(spectralMode, searchQuery, type, searchImageSources, instrumentName, 0.0, "None", null);
    }

    public ImagingInstrument(SpectralImageMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName, double rotation, String flip)
    {
        this(spectralMode, searchQuery, type, searchImageSources, instrumentName, rotation, flip, null);
    }

    public ImagingInstrument(SpectralImageMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName, double rotation, String flip, Collection<Float> fillValues)
    {
        this.spectralMode = spectralMode;
        this.searchQuery = searchQuery;
        this.type = type;
        this.searchImageSources = searchImageSources;
        this.instrumentName = instrumentName;
        this.rotation = rotation;
        this.flip = flip;
        this.fillValues = fillValues != null ? new LinkedHashSet<>(fillValues) : null;
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
        return new ImagingInstrument(spectralMode, searchQuery.clone(), type, searchImageSources.clone(), instrumentName, rotation, flip, fillValues);
    }

    public ImageType getType()
	{
		return type;
	}

	public ImageSource[] getSearchImageSources()
	{
		return searchImageSources;
	}

	public SpectralImageMode getSpectralMode()
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
    Key<String> flipKey = Key.of("flip");
    Key<Double> rotationKey = Key.of("rotation");
    Key<Set<Float>> fillValuesKey = Key.of("fillValues");

    @Override
    public void retrieve(Metadata source)
    {
        spectralMode = SpectralImageMode.valueOf(read(spectralModeKey, source));
        String searchType = read(queryType, source);
        Metadata queryMetadata = read(queryKey, source);
        // Do not use, e.g., GenericPhpQuery.class.getSimpleName() method because if the class
        // gets renamed this would not be able to read previously-saved metadata.
        searchQuery = searchType.equals("GenericPhpQuery") ? new GenericPhpQuery() : new FixedListQuery<>();
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
        flip = read(flipKey, source);
        rotation = read(rotationKey, source);
        fillValues = read(fillValuesKey, source);
    }

    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 1));
        writeEnum(spectralModeKey, spectralMode, configMetadata);
        // Do not use, e.g., GenericPhpQuery.class.getSimpleName() method because if the class
        // gets renamed this would start writing something different that could not be read
        // by the retrieve method above.
        if (searchQuery.getClass() == GenericPhpQuery.class)
        {
            write(queryType, "GenericPhpQuery", configMetadata);
        }
        else if (searchQuery.getClass() == FixedListQuery.class)
        {
            write(queryType, "FixedListQuery", configMetadata);
        }
        else
        {
            // Writing the metadata is actually not a problem -- searchQuery.store() should work for any query.
            // However, throw an exception here in the interest of failing fast/early. Do not write
            // metadata here that cannot be read by the retrieve method above. If adding another query type,
            // first fix the retrieve method to read it, then add support here to write it.
            throw new UnsupportedOperationException("Unable to write metadata for query type " + searchQuery.getClass().getSimpleName());
        }
        write(queryKey, searchQuery.store(), configMetadata);
        write(imageTypeKey, type.name(), configMetadata);
        writeEnums(imageSourcesKey, searchImageSources, configMetadata);
        writeEnum(instrumentKey, instrumentName, configMetadata);
        write(flipKey, flip, configMetadata);
        write(rotationKey, rotation, configMetadata);
        write(fillValuesKey, fillValues, configMetadata);
        return configMetadata;
    }


    private <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
        }
    }

    private void writeEnum(Key<String> key, Enum<?> value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value.name());
        }
    }

    private void writeEnums(Key<String[]> key, Enum<?>[] values, SettableMetadata configMetadata)
    {
        if (values != null)
        {
            String[] names = new String[values.length];
            int i=0;
            for (Enum<?> val : values)
            {
                names[i++] = val.name();
            }
            configMetadata.put(key, names);
        }
    }

    private <T> T read(Key<T> key, Metadata configMetadata)
    {
        if (configMetadata.hasKey(key))
        {
            return configMetadata.get(key);
        }

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

	@Override
	public FillDetector<Float> getFillDetector(Image image)
	{
	    return fillValues == null ? ImageDataUtil.getDefaultFillDetector() : ImageDataUtil.getMultiFillValueDetector(fillValues);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flip == null) ? 0 : flip.hashCode());
		result = prime * result + ((instrumentName == null) ? 0 : instrumentName.hashCode());
		result = prime * result + ((queryType == null) ? 0 : queryType.hashCode());
		long temp;
		temp = Double.doubleToLongBits(rotation);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(searchImageSources);
		result = prime * result + ((searchQuery == null) ? 0 : searchQuery.hashCode());
		result = prime * result + ((spectralMode == null) ? 0 : spectralMode.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((fillValues == null) ? 0 : fillValues.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
		{
			System.err.println("ImagingInstrument: equals: obj is null");
			return false;
		}
		if (getClass() != obj.getClass())
		{
			System.err.println("ImagingInstrument: equals: classes don't match " + getClass() + " " + obj.getClass());
			return false;
		}
		ImagingInstrument other = (ImagingInstrument) obj;
		if (flip == null)
		{
			if (other.flip != null)
			{
				System.err.println("ImagingInstrument: equals: one flip is null, other not");
				return false;
			}
		} else if (!flip.equals(other.flip))
		{
			System.err.println("ImagingInstrument: equals: flips don't equal " + flip + " " + other.flip);
			return false;
		}
		if (instrumentName != other.instrumentName)
		{
			System.err.println("ImagingInstrument: equals: instrument names don't equal");
			return false;
		}
		if (queryType == null)
		{
			if (other.queryType != null)
				return false;
		} else if (!queryType.equals(other.queryType))
		{
			System.err.println("ImagingInstrument: equals: query types unequal");
			return false;
		}
		if (Double.doubleToLongBits(rotation) != Double.doubleToLongBits(other.rotation))
		{
			System.err.println("ImagingInstrument: equals: rotation unequal");
			return false;
		}
		if (!Arrays.equals(searchImageSources, other.searchImageSources))
		{
			System.err.println("ImagingInstrument: equals: search images sources unequal");
			return false;
		}
		if (searchQuery == null)
		{
			if (other.searchQuery != null)
				return false;
		} else if (!searchQuery.equals(other.searchQuery))
		{
			System.err.println("ImagingInstrument: equals: search query unequal");
			return false;
		}
		if (spectralMode != other.spectralMode)
		{
			System.err.println("ImagingInstrument: equals: spectral modes unequal");
			return false;
		}
		if (type != other.type)
		{
			System.err.println("ImagingInstrument: equals: types unequal");
			return false;
		}
        if (fillValues == null)
        {
            if (other.fillValues != null)
                return false;
        } else if (!fillValues.equals(other.fillValues))
        {
            System.err.println("ImagingInstrument: equals: fill values unequal");
            return false;
        }
		return true;
	}


}

