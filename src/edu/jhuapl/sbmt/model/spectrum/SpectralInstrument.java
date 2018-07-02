package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

public interface SpectralInstrument
{
    public double[] getBandCenters();
    public String getDisplayName();
    public QueryBase getQueryBase();
//    public DatabaseQueryBase getDatabaseQueryBase();
    public SpectrumMath getSpectrumMath();

}
