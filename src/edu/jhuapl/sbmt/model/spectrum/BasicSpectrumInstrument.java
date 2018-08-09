package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

public class BasicSpectrumInstrument implements SpectralInstrument
{
    protected String bandCenterUnit;
    protected String displayName;
    protected QueryBase queryBase;
    protected SpectrumMath spectrumMath;
    static public double[] bandCenters;

    public BasicSpectrumInstrument(String bandCenterUnit, String displayName, QueryBase queryBase, SpectrumMath spectrumMath)
    {
        this.bandCenterUnit = bandCenterUnit;
        this.displayName = displayName;
        this.queryBase = queryBase;
        this.spectrumMath = spectrumMath;
    }

    @Override
    public double[] getBandCenters()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getBandCenterUnit()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDisplayName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public QueryBase getQueryBase()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SpectrumMath getSpectrumMath()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
