package edu.jhuapl.sbmt.model.spectrum;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;
import edu.jhuapl.sbmt.query.IQueryBase;

import crucible.crust.metadata.api.StorableAsMetadata;

public interface ISpectralInstrument extends StorableAsMetadata<ISpectralInstrument>
{
    public double[] getBandCenters();
    public String getBandCenterUnit();
    public String getDisplayName();
    public IQueryBase getQueryBase();
//    public DatabaseQueryBase getDatabaseQueryBase();
    public SpectrumMath getSpectrumMath();
    public Spectrum getSpectrumInstance(String filename, ISmallBodyModel smallBodyModel) throws IOException;

}
