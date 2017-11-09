package edu.jhuapl.sbmt.model.bennu.ovirs;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;
import edu.jhuapl.sbmt.query.QueryBase;

public class OVIRS implements SpectralInstrument
{

    @Override
    public double[] getBandCenters()
    {
        return bandCenters;
    }

    @Override
    public String getDisplayName()
    {
        return "OVIRS";
    }

    // these band centers are taken from
    static final public double[] bandCenters = {
        };

    @Override
    public QueryBase getQueryBase()
    {
        return OVIRSQuery.getInstance();
    }

    @Override
    public SpectrumMath getSpectrumMath()
    {
        return OVIRSSpectrumMath.getInstance();
    }

}
