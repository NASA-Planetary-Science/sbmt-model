package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

public interface ISpectraType
{
    public QueryBase getQueryBase();

    public SpectrumMath getSpectrumMath();

    public String getDisplayName();

    public double[] getBandCenters();

    public String getBandCenterUnit();
}
