package edu.jhuapl.sbmt.model.spectrum.instruments;

import java.io.IOException;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.Spectrum;
import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

import crucible.crust.metadata.api.MetadataManager;

public interface SpectralInstrument extends MetadataManager
{
    public double[] getBandCenters();
    public String getBandCenterUnit();
    public String getDisplayName();
    public QueryBase getQueryBase();
//    public DatabaseQueryBase getDatabaseQueryBase();
    public SpectrumMath getSpectrumMath();
    public Spectrum getSpectrumInstance(String filename, SmallBodyModel smallBodyModel) throws IOException;

}
