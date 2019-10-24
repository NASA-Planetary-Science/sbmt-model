package edu.jhuapl.sbmt.model.spectrum.instruments;

import edu.jhuapl.sbmt.model.spectrum.ISpectralInstrument;
import edu.jhuapl.sbmt.model.spectrum.SpectraType;
import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public abstract class BasicSpectrumInstrument implements ISpectralInstrument, MetadataManager
{
    protected String bandCenterUnit;
    protected String displayName;
    protected QueryBase queryBase;
    protected SpectrumMath spectrumMath;
    protected double[] bandCenters;

    public BasicSpectrumInstrument()
    {

    }

    public BasicSpectrumInstrument(String bandCenterUnit, String displayName,
            QueryBase queryBase, SpectrumMath spectrumMath)
    {
        super();
        this.bandCenterUnit = bandCenterUnit;
        this.displayName = displayName;
        this.queryBase = queryBase;
        this.spectrumMath = spectrumMath;
//        System.out.println(
//                "BasicSpectrumInstrument: BasicSpectrumInstrument: band centers " + bandCenters + " for " + displayName);
//        this.bandCenters = bandCenters;
    }

//    public BasicSpectrumInstrument(SpectraType spectraType)
//    {
//        this.bandCenterUnit = spectraType.getBandCenterUnit();
//        this.displayName = spectraType.getDisplayName();
//        this.queryBase = spectraType.getQueryBase();
//        this.spectrumMath = spectraType.getSpectrumMath();
//        this.bandCenters = spectraType.getBandCenters();
//    }

//    @Override
    public double[] getBandCenters()
    {
//        System.out.println("BasicSpectrumInstrument: getBandCenters: getting bandcenters " + bandCenters);
        return bandCenters;
    }

    @Override
    public String getBandCenterUnit()
    {
        return bandCenterUnit;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public QueryBase getQueryBase()
    {
        return queryBase;
    }

    @Override
    public SpectrumMath getSpectrumMath()
    {
        return spectrumMath;
    }

//    abstract public Spectrum getSpectrumInstance(String filename,
//            SmallBodyModel smallBodyModel) throws IOException;


    //metadata interface
//    Key<String> bandCenterUnitKey = Key.of("bandwidthCenterUnits");
    Key<String> spectraNameKey = Key.of("displayName");

    @Override
    public void retrieve(Metadata source)
    {
        displayName = read(spectraNameKey, source);
        SpectraType spectraType = SpectraType.findSpectraTypeForDisplayName(displayName);
        this.queryBase = spectraType.getQueryBase();
        this.spectrumMath = spectraType.getSpectrumMath();
        this.bandCenters = spectraType.getBandCenters();
        this.bandCenterUnit = spectraType.getBandCenterUnit();
    }



    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        write(spectraNameKey, displayName, configMetadata);
        return configMetadata;
    }


    private <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
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
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicSpectrumInstrument other = (BasicSpectrumInstrument) obj;
		if (displayName == null)
		{
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		return true;
	}



}
